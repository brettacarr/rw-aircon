package com.rw.aircon.service

import com.rw.aircon.dto.ControlModeResponse
import com.rw.aircon.model.ControlMode
import com.rw.aircon.model.SystemConfig
import com.rw.aircon.repository.SystemConfigRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Service for managing the control mode of the HVAC system.
 *
 * Control mode determines how the system is managed:
 * - MANUAL: User directly controls all settings
 * - AUTO: System automatically adjusts based on min/max temperature ranges
 * - SCHEDULE: Settings follow the configured season/schedule
 *
 * Only one mode can be active at a time. Switching modes preserves
 * the configuration of other modes (e.g., switching from AUTO to MANUAL
 * doesn't delete Auto Mode settings).
 */
@Service
class ControlModeService(
    private val systemConfigRepository: SystemConfigRepository
) {
    private val log = LoggerFactory.getLogger(ControlModeService::class.java)

    /**
     * Get the current control mode.
     */
    fun getControlMode(): ControlMode {
        return getOrCreateConfig().controlMode
    }

    /**
     * Get the current control mode as a response DTO.
     */
    fun getControlModeResponse(): ControlModeResponse {
        val config = getOrCreateConfig()
        return ControlModeResponse(
            mode = config.controlMode.name.lowercase(),
            changedAt = config.modeChangedAt.toString()
        )
    }

    /**
     * Set the control mode.
     * @param mode The new control mode
     * @return The updated control mode response
     */
    @Transactional
    fun setControlMode(mode: ControlMode): ControlModeResponse {
        val config = getOrCreateConfig()

        if (config.controlMode == mode) {
            log.debug("Control mode already set to {}", mode)
            return getControlModeResponse()
        }

        log.info("Changing control mode from {} to {}", config.controlMode, mode)

        val updatedConfig = config.copy(
            controlMode = mode,
            modeChangedAt = Instant.now()
        )
        systemConfigRepository.save(updatedConfig)

        return ControlModeResponse(
            mode = mode.name.lowercase(),
            changedAt = updatedConfig.modeChangedAt.toString()
        )
    }

    /**
     * Set the control mode from a string value.
     * @param modeString The mode string ("manual", "auto", or "schedule")
     * @return The updated control mode response
     */
    @Transactional
    fun setControlModeFromString(modeString: String): ControlModeResponse {
        val mode = ControlMode.fromString(modeString)
        return setControlMode(mode)
    }

    /**
     * Check if the system is in Manual mode.
     */
    fun isManualMode(): Boolean = getControlMode() == ControlMode.MANUAL

    /**
     * Check if the system is in Auto mode.
     */
    fun isAutoMode(): Boolean = getControlMode() == ControlMode.AUTO

    /**
     * Check if the system is in Schedule mode.
     */
    fun isScheduleMode(): Boolean = getControlMode() == ControlMode.SCHEDULE

    /**
     * Get or create the singleton system configuration.
     */
    private fun getOrCreateConfig(): SystemConfig {
        return systemConfigRepository.findConfig() ?: run {
            log.info("Creating default system configuration")
            val config = SystemConfig.createDefault()
            systemConfigRepository.save(config)
        }
    }
}
