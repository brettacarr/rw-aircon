package com.rw.aircon.service

import com.rw.aircon.dto.SystemLogResponse
import com.rw.aircon.dto.TemperatureLogResponse
import com.rw.aircon.dto.ZoneHistoryResponse
import com.rw.aircon.dto.SystemHistoryResponse
import com.rw.aircon.repository.SystemLogRepository
import com.rw.aircon.repository.TemperatureLogRepository
import com.rw.aircon.repository.ZoneRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Service for retrieving temperature history data.
 */
@Service
class HistoryService(
    private val temperatureLogRepository: TemperatureLogRepository,
    private val systemLogRepository: SystemLogRepository,
    private val zoneRepository: ZoneRepository
) {
    private val log = LoggerFactory.getLogger(HistoryService::class.java)

    companion object {
        // Use aggregation (hourly averages) for time ranges longer than 7 days
        const val AGGREGATION_THRESHOLD_DAYS = 7L
    }

    /**
     * Gets temperature history for a specific zone.
     * If the time range is longer than 7 days, returns hourly averages instead of raw data.
     */
    fun getZoneHistory(zoneId: Int, from: Instant, to: Instant): ZoneHistoryResponse? {
        // Find zone name
        val zone = zoneRepository.findById(zoneId.toLong()).orElse(null)
        val zoneName = zone?.name ?: "Zone $zoneId"

        val daysDiff = ChronoUnit.DAYS.between(from, to)
        val useAggregation = daysDiff >= AGGREGATION_THRESHOLD_DAYS

        val data = if (useAggregation) {
            // Get hourly averages for longer time ranges
            log.debug("Using hourly aggregation for zone {} history ({} days)", zoneId, daysDiff)
            val hourlyData = temperatureLogRepository.findHourlyAveragesByZoneIdAndTimestampBetween(
                zoneId, from.toString(), to.toString()
            )
            hourlyData.map { avg ->
                TemperatureLogResponse(
                    timestamp = avg.hour,
                    currentTemp = avg.avgCurrentTemp,
                    targetTemp = avg.avgTargetTemp,
                    zoneEnabled = true // Can't aggregate boolean, assume enabled for averages
                )
            }
        } else {
            // Get raw data for shorter time ranges
            log.debug("Using raw data for zone {} history ({} days)", zoneId, daysDiff)
            val rawLogs = temperatureLogRepository.findByZoneIdAndTimestampBetweenOrderByTimestampAsc(zoneId, from, to)
            rawLogs.map { logEntry ->
                TemperatureLogResponse(
                    timestamp = logEntry.timestamp.toString(),
                    currentTemp = logEntry.currentTemp,
                    targetTemp = logEntry.targetTemp,
                    zoneEnabled = logEntry.zoneEnabled
                )
            }
        }

        return ZoneHistoryResponse(
            zoneId = zoneId,
            zoneName = zoneName,
            from = from.toString(),
            to = to.toString(),
            aggregated = useAggregation,
            data = data
        )
    }

    /**
     * Gets system state history within the specified time range.
     */
    fun getSystemHistory(from: Instant, to: Instant): SystemHistoryResponse {
        log.debug("Getting system history from {} to {}", from, to)

        val logs = systemLogRepository.findByTimestampBetweenOrderByTimestampAsc(from, to)
        val data = logs.map { logEntry ->
            SystemLogResponse(
                timestamp = logEntry.timestamp.toString(),
                mode = logEntry.mode,
                outdoorTemp = logEntry.outdoorTemp,
                systemOn = logEntry.systemOn
            )
        }

        return SystemHistoryResponse(
            from = from.toString(),
            to = to.toString(),
            data = data
        )
    }
}
