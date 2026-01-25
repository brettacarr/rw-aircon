package com.rw.aircon.service

import com.rw.aircon.client.MyAirClient
import com.rw.aircon.dto.*
import com.rw.aircon.model.AutoModeZone
import com.rw.aircon.model.ControlMode
import com.rw.aircon.repository.AutoModeZoneRepository
import com.rw.aircon.repository.OverrideRepository
import com.rw.aircon.repository.ZoneRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Service responsible for executing Auto Mode temperature control.
 *
 * Runs every minute to check zone temperatures and make heating/cooling decisions.
 * The decision logic follows these priorities:
 * 1. Check for active override - if present, skip execution
 * 2. Check if Auto Mode is active - if not, skip execution
 * 3. Check myZone temperature first (highest priority)
 * 4. Check other enabled zones
 * 5. If all zones in range, turn system off
 *
 * Temperature targets include 0.5°C hysteresis to prevent rapid cycling.
 */
@Service
class AutoModeExecutionService(
    private val autoModeZoneRepository: AutoModeZoneRepository,
    private val zoneRepository: ZoneRepository,
    private val overrideRepository: OverrideRepository,
    private val myAirClient: MyAirClient,
    private val myAirCacheService: MyAirCacheService,
    private val controlModeService: ControlModeService
) {
    private val log = LoggerFactory.getLogger(AutoModeExecutionService::class.java)

    // Execution state for status reporting
    @Volatile
    private var lastExecutionState: ExecutionState = ExecutionState.initial()

    /**
     * Scheduled task that runs every minute to evaluate and apply Auto Mode settings.
     */
    @Scheduled(cron = "0 * * * * *") // Every minute at :00 seconds
    fun evaluateAndApply() {
        try {
            // Check if Auto Mode is active
            if (controlModeService.getControlMode() != ControlMode.AUTO) {
                log.debug("Auto Mode is not active, skipping execution")
                return
            }

            // Check for active override
            val activeOverride = overrideRepository.findActiveOverride(Instant.now())
            if (activeOverride != null) {
                log.debug("Active override present (expires at {}), skipping Auto Mode execution",
                    activeOverride.expiresAt)
                lastExecutionState = lastExecutionState.copy(
                    reason = "Paused - manual override active until ${activeOverride.expiresAt}"
                )
                return
            }

            executeAutoMode()

        } catch (e: Exception) {
            log.error("Error during Auto Mode execution: {}", e.message, e)
            lastExecutionState = lastExecutionState.copy(
                reason = "Error: ${e.message}",
                lastChecked = Instant.now()
            )
        }
    }

    /**
     * Execute the Auto Mode decision logic.
     */
    private fun executeAutoMode() {
        log.debug("Executing Auto Mode evaluation")

        // Get current system state from MyAir
        val (myAirData, _) = myAirCacheService.getSystemData()
        if (myAirData == null) {
            log.warn("Could not get MyAir system data, skipping Auto Mode execution")
            lastExecutionState = lastExecutionState.copy(
                reason = "Unable to get system data",
                lastChecked = Instant.now()
            )
            return
        }

        val systemInfo = myAirData.aircons?.ac1?.info
        val myAirZones = myAirData.aircons?.ac1?.zones

        if (systemInfo == null || myAirZones == null) {
            log.warn("Incomplete MyAir data, skipping Auto Mode execution")
            lastExecutionState = lastExecutionState.copy(
                reason = "Incomplete system data",
                lastChecked = Instant.now()
            )
            return
        }

        // Get enabled Auto Mode zones
        val enabledZones = autoModeZoneRepository.findByEnabledTrue()
        if (enabledZones.isEmpty()) {
            log.debug("No zones enabled for Auto Mode")
            lastExecutionState = lastExecutionState.copy(
                systemState = "off",
                reason = "No zones enabled",
                lastChecked = Instant.now(),
                zoneStatuses = emptyList(),
                triggeringZone = null
            )
            return
        }

        val myZoneNumber = systemInfo.myZone ?: 0

        // Map zone configurations with current temperatures
        val zones = zoneRepository.findAll()
        val zoneMap = zones.associateBy { it.id }
        val zoneStatusList = mutableListOf<ZoneTemperatureStatus>()

        for (autoZone in enabledZones) {
            val zone = zoneMap[autoZone.zoneId] ?: continue
            val myAirZone = myAirZones[zone.myAirZoneId] ?: continue

            val currentTemp = myAirZone.measuredTemp ?: continue // Skip zones without temperature reading
            val status = ZoneTemperatureStatus(
                zoneId = autoZone.zoneId,
                zoneName = zone.name,
                myAirZoneId = zone.myAirZoneId,
                currentTemp = currentTemp,
                config = autoZone,
                isMyZone = myAirZone.number == myZoneNumber && myZoneNumber != 0
            )
            zoneStatusList.add(status)
        }

        if (zoneStatusList.isEmpty()) {
            log.warn("No zone temperature data available")
            return
        }

        // Determine action based on zone temperatures
        val systemIsOn = systemInfo.state == "on"
        val decision = determineAction(zoneStatusList, systemIsOn)

        // Update execution state for status reporting
        lastExecutionState = ExecutionState(
            systemState = decision.action.name.lowercase(),
            targetTemp = decision.targetTemp,
            reason = decision.reason,
            triggeringZone = decision.triggeringZone?.let { tz ->
                TriggeringZoneInfo(
                    zoneId = tz.zoneId,
                    zoneName = tz.zoneName,
                    currentTemp = tz.currentTemp,
                    deviation = tz.deviation
                )
            },
            zoneStatuses = zoneStatusList.map { zs ->
                ZoneStatusInfo(
                    zoneId = zs.zoneId,
                    zoneName = zs.zoneName,
                    enabled = true,
                    currentTemp = zs.currentTemp,
                    minTemp = zs.config.minTemp,
                    maxTemp = zs.config.maxTemp,
                    status = when {
                        zs.currentTemp < zs.config.minTemp -> "below_min"
                        zs.currentTemp > zs.config.maxTemp -> "above_max"
                        else -> "in_range"
                    }
                )
            },
            lastChecked = Instant.now()
        )

        // Apply the decision
        applyDecision(decision)
    }

    /**
     * Determine what action to take based on zone temperatures.
     */
    private fun determineAction(
        zones: List<ZoneTemperatureStatus>,
        systemIsOn: Boolean
    ): AutoModeDecision {
        // Separate myZone from other zones (myZone has priority)
        val myZone = zones.find { it.isMyZone }
        val otherZones = zones.filter { !it.isMyZone }

        // Step 1: Check myZone first (if any zone is currently myZone)
        if (myZone != null) {
            val myZoneDecision = checkZoneTemperature(myZone)
            if (myZoneDecision != null) {
                return myZoneDecision
            }
        }

        // Step 2: Check other zones
        // Find zone with largest deviation from range
        var worstZone: ZoneTemperatureStatus? = null
        var worstDeviation = 0.0
        var worstAction: AutoModeAction = AutoModeAction.OFF

        for (zone in (if (myZone != null) otherZones else zones)) {
            when {
                zone.config.needsHeating(zone.currentTemp) -> {
                    val deviation = zone.config.minTemp - zone.currentTemp
                    if (deviation > worstDeviation) {
                        worstDeviation = deviation
                        worstZone = zone
                        worstAction = AutoModeAction.HEAT
                    }
                }
                zone.config.needsCooling(zone.currentTemp) -> {
                    val deviation = zone.currentTemp - zone.config.maxTemp
                    if (deviation > worstDeviation) {
                        worstDeviation = deviation
                        worstZone = zone
                        worstAction = AutoModeAction.COOL
                    }
                }
            }
        }

        // If any zone needs adjustment
        if (worstZone != null && worstDeviation > 0) {
            val targetTemp = if (worstAction == AutoModeAction.HEAT) {
                worstZone.config.getHeatingTarget()
            } else {
                worstZone.config.getCoolingTarget()
            }

            return AutoModeDecision(
                action = worstAction,
                targetTemp = targetTemp,
                reason = "${worstZone.zoneName} is ${String.format("%.1f", worstDeviation)}°C ${if (worstAction == AutoModeAction.HEAT) "below minimum" else "above maximum"}",
                triggeringZone = TriggeringZone(
                    zoneId = worstZone.zoneId,
                    zoneName = worstZone.zoneName,
                    currentTemp = worstZone.currentTemp,
                    deviation = if (worstAction == AutoModeAction.HEAT) -worstDeviation else worstDeviation
                )
            )
        }

        // Step 3: All zones in range - turn system off
        return AutoModeDecision(
            action = AutoModeAction.OFF,
            targetTemp = null,
            reason = "All zones within range",
            triggeringZone = null
        )
    }

    /**
     * Check a single zone's temperature and return a decision if action is needed.
     */
    private fun checkZoneTemperature(zone: ZoneTemperatureStatus): AutoModeDecision? {
        val config = zone.config

        return when {
            config.needsHeating(zone.currentTemp) -> {
                val deviation = config.minTemp - zone.currentTemp
                AutoModeDecision(
                    action = AutoModeAction.HEAT,
                    targetTemp = config.getHeatingTarget(),
                    reason = "${zone.zoneName} (myZone) is ${String.format("%.1f", deviation)}°C below minimum",
                    triggeringZone = TriggeringZone(
                        zoneId = zone.zoneId,
                        zoneName = zone.zoneName,
                        currentTemp = zone.currentTemp,
                        deviation = -deviation
                    )
                )
            }
            config.needsCooling(zone.currentTemp) -> {
                val deviation = zone.currentTemp - config.maxTemp
                AutoModeDecision(
                    action = AutoModeAction.COOL,
                    targetTemp = config.getCoolingTarget(),
                    reason = "${zone.zoneName} (myZone) is ${String.format("%.1f", deviation)}°C above maximum",
                    triggeringZone = TriggeringZone(
                        zoneId = zone.zoneId,
                        zoneName = zone.zoneName,
                        currentTemp = zone.currentTemp,
                        deviation = deviation
                    )
                )
            }
            else -> null // Zone is in range
        }
    }

    /**
     * Apply the Auto Mode decision to the MyAir system.
     */
    private fun applyDecision(decision: AutoModeDecision) {
        when (decision.action) {
            AutoModeAction.HEAT -> {
                log.info("Auto Mode: Heating to {}°C - {}", decision.targetTemp, decision.reason)
                // Turn on system, set mode to heat, set target temp
                try {
                    myAirClient.setSystemInfo(mapOf("state" to "on"))
                    myAirClient.setSystemInfo(mapOf("mode" to "heat"))
                    decision.targetTemp?.let { temp ->
                        myAirClient.setSystemInfo(mapOf("setTemp" to temp.toInt().toString()))
                    }
                } catch (e: Exception) {
                    log.error("Failed to apply heating decision: {}", e.message)
                }
            }
            AutoModeAction.COOL -> {
                log.info("Auto Mode: Cooling to {}°C - {}", decision.targetTemp, decision.reason)
                // Turn on system, set mode to cool, set target temp
                try {
                    myAirClient.setSystemInfo(mapOf("state" to "on"))
                    myAirClient.setSystemInfo(mapOf("mode" to "cool"))
                    decision.targetTemp?.let { temp ->
                        myAirClient.setSystemInfo(mapOf("setTemp" to temp.toInt().toString()))
                    }
                } catch (e: Exception) {
                    log.error("Failed to apply cooling decision: {}", e.message)
                }
            }
            AutoModeAction.OFF -> {
                log.info("Auto Mode: Turning off - {}", decision.reason)
                try {
                    myAirClient.setSystemInfo(mapOf("state" to "off"))
                } catch (e: Exception) {
                    log.error("Failed to turn system off: {}", e.message)
                }
            }
        }
    }

    /**
     * Get the current Auto Mode execution status.
     */
    fun getStatus(): AutoModeStatusResponse {
        val isActive = controlModeService.isAutoMode()

        // If not active, return minimal status
        if (!isActive) {
            return AutoModeStatusResponse(
                active = false,
                systemState = "off",
                targetTemp = null,
                reason = "Auto Mode is not active",
                triggeringZone = null,
                zoneStatuses = emptyList(),
                lastChecked = lastExecutionState.lastChecked.toString()
            )
        }

        return AutoModeStatusResponse(
            active = true,
            systemState = lastExecutionState.systemState,
            targetTemp = lastExecutionState.targetTemp,
            reason = lastExecutionState.reason,
            triggeringZone = lastExecutionState.triggeringZone,
            zoneStatuses = lastExecutionState.zoneStatuses,
            lastChecked = lastExecutionState.lastChecked.toString()
        )
    }

    /**
     * Manually trigger Auto Mode evaluation (for testing).
     */
    fun triggerEvaluation() {
        log.info("Manual Auto Mode evaluation triggered")
        evaluateAndApply()
    }

    // Internal data classes

    private enum class AutoModeAction {
        HEAT, COOL, OFF
    }

    private data class AutoModeDecision(
        val action: AutoModeAction,
        val targetTemp: Double?,
        val reason: String,
        val triggeringZone: TriggeringZone?
    )

    private data class TriggeringZone(
        val zoneId: Long,
        val zoneName: String,
        val currentTemp: Double,
        val deviation: Double
    )

    private data class ZoneTemperatureStatus(
        val zoneId: Long,
        val zoneName: String,
        val myAirZoneId: String,
        val currentTemp: Double,
        val config: AutoModeZone,
        val isMyZone: Boolean
    )

    private data class ExecutionState(
        val systemState: String,
        val targetTemp: Double?,
        val reason: String,
        val triggeringZone: TriggeringZoneInfo?,
        val zoneStatuses: List<ZoneStatusInfo>,
        val lastChecked: Instant
    ) {
        companion object {
            fun initial() = ExecutionState(
                systemState = "off",
                targetTemp = null,
                reason = "Not yet evaluated",
                triggeringZone = null,
                zoneStatuses = emptyList(),
                lastChecked = Instant.now()
            )
        }
    }
}
