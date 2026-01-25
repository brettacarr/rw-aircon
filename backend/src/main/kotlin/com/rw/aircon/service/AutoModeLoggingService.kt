package com.rw.aircon.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.rw.aircon.dto.AutoModeLogListResponse
import com.rw.aircon.dto.AutoModeLogResponse
import com.rw.aircon.dto.AutoModeLogZoneTemp
import com.rw.aircon.model.AutoModeLog
import com.rw.aircon.repository.AutoModeLogRepository
import com.rw.aircon.repository.ZoneRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Service for logging Auto Mode actions and retrieving log history.
 *
 * Provides methods to:
 * - Log actions when Auto Mode makes decisions (heat, cool, off)
 * - Retrieve recent logs with pagination
 * - Convert between entity and DTO representations
 */
@Service
class AutoModeLoggingService(
    private val autoModeLogRepository: AutoModeLogRepository,
    private val zoneRepository: ZoneRepository,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(AutoModeLoggingService::class.java)

    /**
     * Log a heating action.
     */
    fun logHeatingAction(
        reason: String,
        triggeringZoneId: Long?,
        previousMode: String?,
        zoneTemps: List<ZoneTempSnapshot>
    ) {
        logAction(
            action = AutoModeLog.ACTION_HEAT_ON,
            reason = reason,
            triggeringZoneId = triggeringZoneId,
            previousMode = previousMode,
            newMode = "heat",
            zoneTemps = zoneTemps
        )
    }

    /**
     * Log a cooling action.
     */
    fun logCoolingAction(
        reason: String,
        triggeringZoneId: Long?,
        previousMode: String?,
        zoneTemps: List<ZoneTempSnapshot>
    ) {
        logAction(
            action = AutoModeLog.ACTION_COOL_ON,
            reason = reason,
            triggeringZoneId = triggeringZoneId,
            previousMode = previousMode,
            newMode = "cool",
            zoneTemps = zoneTemps
        )
    }

    /**
     * Log a system off action.
     */
    fun logSystemOffAction(
        reason: String,
        previousMode: String?,
        zoneTemps: List<ZoneTempSnapshot>
    ) {
        logAction(
            action = AutoModeLog.ACTION_SYSTEM_OFF,
            reason = reason,
            triggeringZoneId = null,
            previousMode = previousMode,
            newMode = "off",
            zoneTemps = zoneTemps
        )
    }

    /**
     * Log a mode change action (e.g., from manual activation/deactivation).
     */
    fun logModeChange(
        reason: String,
        previousMode: String?,
        newMode: String?
    ) {
        logAction(
            action = AutoModeLog.ACTION_MODE_CHANGE,
            reason = reason,
            triggeringZoneId = null,
            previousMode = previousMode,
            newMode = newMode,
            zoneTemps = emptyList()
        )
    }

    private fun logAction(
        action: String,
        reason: String,
        triggeringZoneId: Long?,
        previousMode: String?,
        newMode: String?,
        zoneTemps: List<ZoneTempSnapshot>
    ) {
        try {
            val zoneTempJson = if (zoneTemps.isNotEmpty()) {
                objectMapper.writeValueAsString(zoneTemps)
            } else {
                null
            }

            val logEntry = AutoModeLog(
                timestamp = Instant.now(),
                action = action,
                reason = reason,
                triggeringZoneId = triggeringZoneId,
                systemMode = previousMode,
                newSystemMode = newMode,
                zoneTemps = zoneTempJson
            )

            autoModeLogRepository.save(logEntry)
            log.debug("Logged Auto Mode action: {} - {}", action, reason)
        } catch (e: Exception) {
            log.error("Failed to log Auto Mode action: {}", e.message, e)
        }
    }

    /**
     * Get recent Auto Mode logs with optional limit.
     */
    fun getLogs(limit: Int = 50): AutoModeLogListResponse {
        val pageable = PageRequest.of(0, limit)
        val logs = autoModeLogRepository.findByOrderByTimestampDesc(pageable)
        val total = autoModeLogRepository.countLogs()

        // Build zone ID to name map for enriching responses
        val zones = zoneRepository.findAll()
        val zoneNameMap = zones.associate { it.id to it.name }

        val logResponses = logs.map { logEntry ->
            toResponse(logEntry, zoneNameMap)
        }

        return AutoModeLogListResponse(
            logs = logResponses,
            total = total
        )
    }

    /**
     * Get logs filtered by action type.
     */
    fun getLogsByAction(action: String, limit: Int = 50): AutoModeLogListResponse {
        val pageable = PageRequest.of(0, limit)
        val logs = autoModeLogRepository.findByActionOrderByTimestampDesc(action, pageable)
        val total = autoModeLogRepository.countLogs()

        val zones = zoneRepository.findAll()
        val zoneNameMap = zones.associate { it.id to it.name }

        val logResponses = logs.map { logEntry ->
            toResponse(logEntry, zoneNameMap)
        }

        return AutoModeLogListResponse(
            logs = logResponses,
            total = total
        )
    }

    private fun toResponse(logEntry: AutoModeLog, zoneNameMap: Map<Long, String>): AutoModeLogResponse {
        val zoneTemps = logEntry.zoneTemps?.let { json ->
            try {
                objectMapper.readValue<List<ZoneTempSnapshot>>(json).map { snapshot ->
                    AutoModeLogZoneTemp(
                        zoneId = snapshot.zoneId,
                        zoneName = snapshot.zoneName,
                        currentTemp = snapshot.currentTemp,
                        minTemp = snapshot.minTemp,
                        maxTemp = snapshot.maxTemp
                    )
                }
            } catch (e: Exception) {
                log.warn("Failed to parse zone temps JSON: {}", e.message)
                null
            }
        }

        return AutoModeLogResponse(
            id = logEntry.id,
            timestamp = logEntry.timestamp.toString(),
            action = logEntry.action,
            reason = logEntry.reason,
            triggeringZoneId = logEntry.triggeringZoneId,
            triggeringZoneName = logEntry.triggeringZoneId?.let { zoneNameMap[it] },
            systemMode = logEntry.systemMode,
            newSystemMode = logEntry.newSystemMode,
            zoneTemps = zoneTemps
        )
    }

    /**
     * Data class for zone temperature snapshot (used for JSON serialization).
     */
    data class ZoneTempSnapshot(
        val zoneId: Long,
        val zoneName: String,
        val currentTemp: Double,
        val minTemp: Double,
        val maxTemp: Double
    )
}
