package com.rw.aircon.service

import com.rw.aircon.client.MyAirClient
import com.rw.aircon.dto.*
import com.rw.aircon.model.AutoModeZone
import com.rw.aircon.model.ControlMode
import com.rw.aircon.repository.AutoModeZoneRepository
import com.rw.aircon.repository.OverrideRepository
import com.rw.aircon.repository.ZoneRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
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
    private val controlModeService: ControlModeService,
    private val autoModeLoggingService: AutoModeLoggingService
) {
    private val log = LoggerFactory.getLogger(AutoModeExecutionService::class.java)

    // Execution state for status reporting
    @Volatile
    private var lastExecutionState: ExecutionState = ExecutionState.initial()

    /**
     * Initialize Auto Mode status on application startup.
     * If Auto Mode was active before restart, trigger an evaluation to populate zone statuses.
     */
    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        if (controlModeService.isAutoMode()) {
            log.info("Auto Mode is active on startup, triggering initial evaluation")
            try {
                triggerEvaluation()
            } catch (e: Exception) {
                log.warn("Failed to run initial Auto Mode evaluation: {}", e.message)
            }
        } else {
            log.debug("Auto Mode is not active on startup, skipping initial evaluation")
        }
    }

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

        // Get current system mode for logging and hysteresis
        val systemIsOn = systemInfo.state == "on"
        val currentMode = if (systemIsOn) systemInfo.mode ?: "unknown" else "off"

        // Determine action based on zone temperatures (pass current mode for hysteresis)
        val decision = determineAction(zoneStatusList, systemIsOn, currentMode)

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

        // Apply the decision and log the action
        applyDecision(decision, currentMode, zoneStatusList)
    }

    /**
     * Determine what action to take based on zone temperatures.
     *
     * Hysteresis logic:
     * - When starting: heating triggers at temp < minTemp, cooling at temp > maxTemp
     * - When continuing: heating continues until temp >= minTemp + 0.5,
     *   cooling continues until temp <= maxTemp - 0.5
     */
    private fun determineAction(
        zones: List<ZoneTemperatureStatus>,
        systemIsOn: Boolean,
        currentMode: String
    ): AutoModeDecision {
        // Separate myZone from other zones (myZone has priority)
        val myZone = zones.find { it.isMyZone }
        val otherZones = zones.filter { !it.isMyZone }

        // Step 1: Check myZone first (if any zone is currently myZone)
        if (myZone != null) {
            val myZoneDecision = checkZoneTemperature(myZone, systemIsOn, currentMode)
            if (myZoneDecision != null) {
                return myZoneDecision
            }
        }

        // Step 2: Check other zones
        // Find zone with largest deviation from range (or needs to continue current mode)
        var worstZone: ZoneTemperatureStatus? = null
        var worstDeviation = 0.0
        var worstAction: AutoModeAction = AutoModeAction.OFF

        for (zone in (if (myZone != null) otherZones else zones)) {
            // Check if zone needs heating (either starting new or continuing)
            val needsHeat = if (systemIsOn && currentMode == "heat") {
                zone.config.shouldContinueHeating(zone.currentTemp)
            } else {
                zone.config.needsHeating(zone.currentTemp)
            }

            // Check if zone needs cooling (either starting new or continuing)
            val needsCool = if (systemIsOn && currentMode == "cool") {
                zone.config.shouldContinueCooling(zone.currentTemp)
            } else {
                zone.config.needsCooling(zone.currentTemp)
            }

            when {
                needsHeat -> {
                    // Calculate deviation from target (hysteresis target when continuing)
                    val target = zone.config.getHeatingTarget()
                    val deviation = target - zone.currentTemp
                    if (deviation > worstDeviation) {
                        worstDeviation = deviation
                        worstZone = zone
                        worstAction = AutoModeAction.HEAT
                    }
                }
                needsCool -> {
                    // Calculate deviation from target (hysteresis target when continuing)
                    val target = zone.config.getCoolingTarget()
                    val deviation = zone.currentTemp - target
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

            // Determine if this is continuing an existing operation
            val isContinuing = systemIsOn &&
                ((worstAction == AutoModeAction.HEAT && currentMode == "heat") ||
                 (worstAction == AutoModeAction.COOL && currentMode == "cool"))

            val reasonSuffix = if (isContinuing) " (continuing to target)" else ""

            return AutoModeDecision(
                action = worstAction,
                targetTemp = targetTemp,
                reason = "${worstZone.zoneName} is ${String.format("%.1f", worstDeviation)}°C from target$reasonSuffix",
                triggeringZone = TriggeringZone(
                    zoneId = worstZone.zoneId,
                    zoneName = worstZone.zoneName,
                    currentTemp = worstZone.currentTemp,
                    deviation = if (worstAction == AutoModeAction.HEAT) -worstDeviation else worstDeviation
                )
            )
        }

        // Step 3: All zones have reached their targets - turn system off
        return AutoModeDecision(
            action = AutoModeAction.OFF,
            targetTemp = null,
            reason = "All zones have reached target temperature",
            triggeringZone = null
        )
    }

    /**
     * Check a single zone's temperature and return a decision if action is needed.
     * Applies hysteresis when the system is already heating/cooling.
     */
    private fun checkZoneTemperature(
        zone: ZoneTemperatureStatus,
        systemIsOn: Boolean,
        currentMode: String
    ): AutoModeDecision? {
        val config = zone.config

        // Check if zone needs heating (either starting new or continuing)
        val needsHeat = if (systemIsOn && currentMode == "heat") {
            config.shouldContinueHeating(zone.currentTemp)
        } else {
            config.needsHeating(zone.currentTemp)
        }

        // Check if zone needs cooling (either starting new or continuing)
        val needsCool = if (systemIsOn && currentMode == "cool") {
            config.shouldContinueCooling(zone.currentTemp)
        } else {
            config.needsCooling(zone.currentTemp)
        }

        return when {
            needsHeat -> {
                val target = config.getHeatingTarget()
                val deviation = target - zone.currentTemp
                val isContinuing = systemIsOn && currentMode == "heat"
                val reasonSuffix = if (isContinuing) " (continuing to target)" else ""
                AutoModeDecision(
                    action = AutoModeAction.HEAT,
                    targetTemp = target,
                    reason = "${zone.zoneName} (myZone) is ${String.format("%.1f", deviation)}°C from target$reasonSuffix",
                    triggeringZone = TriggeringZone(
                        zoneId = zone.zoneId,
                        zoneName = zone.zoneName,
                        currentTemp = zone.currentTemp,
                        deviation = -deviation
                    )
                )
            }
            needsCool -> {
                val target = config.getCoolingTarget()
                val deviation = zone.currentTemp - target
                val isContinuing = systemIsOn && currentMode == "cool"
                val reasonSuffix = if (isContinuing) " (continuing to target)" else ""
                AutoModeDecision(
                    action = AutoModeAction.COOL,
                    targetTemp = target,
                    reason = "${zone.zoneName} (myZone) is ${String.format("%.1f", deviation)}°C from target$reasonSuffix",
                    triggeringZone = TriggeringZone(
                        zoneId = zone.zoneId,
                        zoneName = zone.zoneName,
                        currentTemp = zone.currentTemp,
                        deviation = deviation
                    )
                )
            }
            else -> null // Zone has reached target
        }
    }

    /**
     * Apply the Auto Mode decision to the MyAir system.
     */
    private fun applyDecision(
        decision: AutoModeDecision,
        previousMode: String,
        zoneStatusList: List<ZoneTemperatureStatus>
    ) {
        // Convert zone status list to snapshots for logging
        val zoneSnapshots = zoneStatusList.map { zs ->
            AutoModeLoggingService.ZoneTempSnapshot(
                zoneId = zs.zoneId,
                zoneName = zs.zoneName,
                currentTemp = zs.currentTemp,
                minTemp = zs.config.minTemp,
                maxTemp = zs.config.maxTemp
            )
        }

        when (decision.action) {
            AutoModeAction.HEAT -> {
                log.info("Auto Mode: Heating to {}°C - {}", decision.targetTemp, decision.reason)
                // Turn on system, set mode to heat, set target temp, and open enabled zones
                try {
                    myAirClient.setSystemInfo(mapOf("state" to "on"))
                    myAirClient.setSystemInfo(mapOf("mode" to "heat"))
                    decision.targetTemp?.let { temp ->
                        myAirClient.setSystemInfo(mapOf("setTemp" to temp.toInt().toString()))
                    }
                    // Open all enabled zones so they receive conditioned air
                    openEnabledZones(zoneStatusList)
                    // Log the heating action
                    autoModeLoggingService.logHeatingAction(
                        reason = decision.reason,
                        triggeringZoneId = decision.triggeringZone?.zoneId,
                        previousMode = previousMode,
                        zoneTemps = zoneSnapshots
                    )
                } catch (e: Exception) {
                    log.error("Failed to apply heating decision: {}", e.message)
                }
            }
            AutoModeAction.COOL -> {
                log.info("Auto Mode: Cooling to {}°C - {}", decision.targetTemp, decision.reason)
                // Turn on system, set mode to cool, set target temp, and open enabled zones
                try {
                    myAirClient.setSystemInfo(mapOf("state" to "on"))
                    myAirClient.setSystemInfo(mapOf("mode" to "cool"))
                    decision.targetTemp?.let { temp ->
                        myAirClient.setSystemInfo(mapOf("setTemp" to temp.toInt().toString()))
                    }
                    // Open all enabled zones so they receive conditioned air
                    openEnabledZones(zoneStatusList)
                    // Log the cooling action
                    autoModeLoggingService.logCoolingAction(
                        reason = decision.reason,
                        triggeringZoneId = decision.triggeringZone?.zoneId,
                        previousMode = previousMode,
                        zoneTemps = zoneSnapshots
                    )
                } catch (e: Exception) {
                    log.error("Failed to apply cooling decision: {}", e.message)
                }
            }
            AutoModeAction.OFF -> {
                log.info("Auto Mode: Turning off - {}", decision.reason)
                try {
                    myAirClient.setSystemInfo(mapOf("state" to "off"))
                    // Log the system off action
                    autoModeLoggingService.logSystemOffAction(
                        reason = decision.reason,
                        previousMode = previousMode,
                        zoneTemps = zoneSnapshots
                    )
                } catch (e: Exception) {
                    log.error("Failed to turn system off: {}", e.message)
                }
            }
        }
    }

    /**
     * Open all enabled zones so they receive conditioned air.
     * This ensures zones included in Auto Mode config are actually active when heating/cooling.
     */
    private fun openEnabledZones(zoneStatusList: List<ZoneTemperatureStatus>) {
        for (zone in zoneStatusList) {
            try {
                val result = myAirClient.setZone(zone.myAirZoneId, mapOf("state" to "open"))
                if (result) {
                    log.debug("Auto Mode: Opened zone {} ({})", zone.zoneName, zone.myAirZoneId)
                } else {
                    log.warn("Auto Mode: Failed to open zone {} ({})", zone.zoneName, zone.myAirZoneId)
                }
            } catch (e: Exception) {
                log.error("Auto Mode: Error opening zone {} ({}): {}", zone.zoneName, zone.myAirZoneId, e.message)
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
