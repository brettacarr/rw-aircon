package com.rw.aircon.service

import com.rw.aircon.dto.*
import com.rw.aircon.model.AutoModeConfig
import com.rw.aircon.model.AutoModeZone
import com.rw.aircon.model.ControlMode
import com.rw.aircon.repository.AutoModeConfigRepository
import com.rw.aircon.repository.AutoModeZoneRepository
import com.rw.aircon.repository.ZoneRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Service for managing Auto Mode configuration.
 *
 * Auto Mode automatically maintains zone temperatures within user-defined min/max ranges.
 * This service handles:
 * - Getting and updating Auto Mode configuration
 * - Validating zone configurations (temperature ranges, Guest zone restrictions)
 * - Activating/deactivating Auto Mode
 */
@Service
class AutoModeService(
    private val autoModeConfigRepository: AutoModeConfigRepository,
    private val autoModeZoneRepository: AutoModeZoneRepository,
    private val zoneRepository: ZoneRepository,
    private val controlModeService: ControlModeService,
    private val autoModeExecutionService: AutoModeExecutionService
) {
    private val log = LoggerFactory.getLogger(AutoModeService::class.java)

    companion object {
        const val GUEST_ZONE_ID = 2L
    }

    /**
     * Get the current Auto Mode configuration with all zone settings.
     */
    fun getConfig(): AutoModeConfigResponse {
        val config = getOrCreateConfig()
        val zones = zoneRepository.findAll()
        val zoneConfigs = autoModeZoneRepository.findAll().associateBy { it.zoneId }

        val zoneResponses = zones.map { zone ->
            val zoneConfig = zoneConfigs[zone.id] ?: AutoModeZone(
                zoneId = zone.id,
                enabled = zone.id != GUEST_ZONE_ID, // Guest zone disabled by default
                minTemp = 20.0,
                maxTemp = 24.0
            )
            AutoModeZoneResponse(
                zoneId = zone.id,
                zoneName = zone.name,
                enabled = zoneConfig.enabled,
                minTemp = zoneConfig.minTemp,
                maxTemp = zoneConfig.maxTemp
            )
        }

        return AutoModeConfigResponse(
            active = config.active,
            priorityZoneId = config.priorityZoneId,
            updatedAt = config.updatedAt.toString(),
            zones = zoneResponses
        )
    }

    /**
     * Update the Auto Mode configuration.
     */
    @Transactional
    fun updateConfig(request: AutoModeConfigRequest): AutoModeConfigResponse {
        log.info("Updating Auto Mode configuration")

        // Validate request
        validateConfigRequest(request)

        val config = getOrCreateConfig()

        // Update main config
        val updatedConfig = config.copy(
            priorityZoneId = request.priorityZoneId,
            updatedAt = Instant.now()
        )
        autoModeConfigRepository.save(updatedConfig)

        // Update zone configurations
        for (zoneRequest in request.zones) {
            val existing = autoModeZoneRepository.findByZoneId(zoneRequest.zoneId)
            val zoneConfig = if (existing != null) {
                existing.copy(
                    enabled = zoneRequest.enabled,
                    minTemp = zoneRequest.minTemp,
                    maxTemp = zoneRequest.maxTemp
                )
            } else {
                AutoModeZone(
                    zoneId = zoneRequest.zoneId,
                    enabled = zoneRequest.enabled,
                    minTemp = zoneRequest.minTemp,
                    maxTemp = zoneRequest.maxTemp
                )
            }
            zoneConfig.validate()
            autoModeZoneRepository.save(zoneConfig)
        }

        log.info("Auto Mode configuration updated successfully")

        // Trigger evaluation to refresh status with new config values
        // This ensures the status endpoint returns updated min/max temps immediately
        if (isActive()) {
            log.debug("Triggering Auto Mode evaluation to refresh status")
            autoModeExecutionService.triggerEvaluation()
        }

        return getConfig()
    }

    /**
     * Activate Auto Mode.
     * This sets the control mode to AUTO and makes Auto Mode active.
     */
    @Transactional
    fun activate(): AutoModeConfigResponse {
        log.info("Activating Auto Mode")

        // Validate that at least one non-Guest zone is enabled
        val enabledNonGuestCount = autoModeZoneRepository.countEnabledNonGuestZones()
        if (enabledNonGuestCount == 0L) {
            throw IllegalStateException("Cannot activate Auto Mode: at least one non-Guest zone must be enabled")
        }

        // Set control mode to AUTO
        controlModeService.setControlMode(ControlMode.AUTO)

        // Activate Auto Mode
        val config = getOrCreateConfig()
        val updatedConfig = config.copy(
            active = true,
            updatedAt = Instant.now()
        )
        autoModeConfigRepository.save(updatedConfig)

        log.info("Auto Mode activated")

        // Trigger immediate evaluation to populate zone statuses
        // This ensures the status endpoint returns zone data right away
        log.debug("Triggering initial Auto Mode evaluation")
        autoModeExecutionService.triggerEvaluation()

        return getConfig()
    }

    /**
     * Deactivate Auto Mode.
     * This sets the control mode to MANUAL.
     */
    @Transactional
    fun deactivate(): AutoModeConfigResponse {
        log.info("Deactivating Auto Mode")

        // Set control mode to MANUAL
        controlModeService.setControlMode(ControlMode.MANUAL)

        // Deactivate Auto Mode
        val config = getOrCreateConfig()
        val updatedConfig = config.copy(
            active = false,
            updatedAt = Instant.now()
        )
        autoModeConfigRepository.save(updatedConfig)

        log.info("Auto Mode deactivated")
        return getConfig()
    }

    /**
     * Check if Auto Mode is currently active.
     */
    fun isActive(): Boolean {
        return controlModeService.getControlMode() == ControlMode.AUTO
    }

    /**
     * Get the enabled zone configurations for Auto Mode execution.
     */
    fun getEnabledZones(): List<AutoModeZone> {
        return autoModeZoneRepository.findByEnabledTrue()
    }

    /**
     * Get the priority zone ID, or null if not set.
     */
    fun getPriorityZoneId(): Long? {
        return getOrCreateConfig().priorityZoneId
    }

    /**
     * Get or create the singleton Auto Mode configuration.
     */
    private fun getOrCreateConfig(): AutoModeConfig {
        return autoModeConfigRepository.findConfig() ?: run {
            log.info("Creating default Auto Mode configuration")
            val config = AutoModeConfig.createDefault()
            autoModeConfigRepository.save(config)
        }
    }

    /**
     * Validate an Auto Mode configuration request.
     */
    private fun validateConfigRequest(request: AutoModeConfigRequest) {
        // Validate priority zone is not Guest zone
        if (request.priorityZoneId == GUEST_ZONE_ID) {
            throw IllegalArgumentException("Guest zone cannot be set as priority zone")
        }

        // Count enabled non-Guest zones
        val enabledNonGuestZones = request.zones.filter { it.enabled && it.zoneId != GUEST_ZONE_ID }
        if (enabledNonGuestZones.isEmpty()) {
            throw IllegalArgumentException("At least one non-Guest zone must be enabled")
        }

        // Validate each zone configuration
        for (zone in request.zones) {
            validateZoneConfig(zone)
        }

        // Validate priority zone is one of the enabled zones (if set)
        if (request.priorityZoneId != null) {
            val priorityEnabled = request.zones.find { it.zoneId == request.priorityZoneId }?.enabled ?: false
            if (!priorityEnabled) {
                throw IllegalArgumentException("Priority zone must be enabled")
            }
        }
    }

    /**
     * Validate a single zone configuration.
     */
    private fun validateZoneConfig(zone: AutoModeZoneRequest) {
        if (zone.minTemp < AutoModeZone.MIN_ALLOWED_TEMP) {
            throw IllegalArgumentException(
                "Zone ${zone.zoneId}: minimum temperature must be at least ${AutoModeZone.MIN_ALLOWED_TEMP}°C"
            )
        }
        if (zone.maxTemp > AutoModeZone.MAX_ALLOWED_TEMP) {
            throw IllegalArgumentException(
                "Zone ${zone.zoneId}: maximum temperature must not exceed ${AutoModeZone.MAX_ALLOWED_TEMP}°C"
            )
        }
        if (zone.maxTemp - zone.minTemp < AutoModeZone.MIN_TEMP_GAP) {
            throw IllegalArgumentException(
                "Zone ${zone.zoneId}: temperature range must be at least ${AutoModeZone.MIN_TEMP_GAP}°C"
            )
        }
    }
}
