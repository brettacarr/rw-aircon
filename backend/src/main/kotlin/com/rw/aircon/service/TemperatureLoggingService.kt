package com.rw.aircon.service

import com.rw.aircon.config.TemperatureLoggingProperties
import com.rw.aircon.model.SystemLog
import com.rw.aircon.model.TemperatureLog
import com.rw.aircon.repository.SystemLogRepository
import com.rw.aircon.repository.TemperatureLogRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Service for logging temperature readings on a scheduled interval.
 * Polls MyAir API and logs temperature data for each zone and system state.
 */
@Service
class TemperatureLoggingService(
    private val myAirCacheService: MyAirCacheService,
    private val temperatureLogRepository: TemperatureLogRepository,
    private val systemLogRepository: SystemLogRepository,
    private val properties: TemperatureLoggingProperties
) {
    private val log = LoggerFactory.getLogger(TemperatureLoggingService::class.java)

    /**
     * Scheduled task to log temperature readings.
     * Runs every 5 minutes by default (configurable via temperature-logging.interval-minutes).
     */
    @Scheduled(fixedRateString = "#{@temperatureLoggingProperties.intervalMinutes * 60 * 1000}")
    fun logTemperatures() {
        log.debug("Starting scheduled temperature logging")

        val (myAirData, _) = myAirCacheService.getSystemData()

        if (myAirData == null) {
            log.warn("Unable to log temperatures: MyAir API data unavailable")
            return
        }

        val timestamp = Instant.now()
        val info = myAirData.aircons?.ac1?.info
        val zones = myAirData.aircons?.ac1?.zones ?: emptyMap()
        val system = myAirData.system

        // Log system state
        try {
            val systemLog = SystemLog(
                timestamp = timestamp,
                mode = info?.mode ?: "unknown",
                outdoorTemp = if (system?.isValidSuburbTemp == true) system.suburbTemp else null,
                systemOn = info?.state == "on"
            )
            systemLogRepository.save(systemLog)
            log.debug("Logged system state: mode={}, systemOn={}, outdoorTemp={}",
                systemLog.mode, systemLog.systemOn, systemLog.outdoorTemp)
        } catch (e: Exception) {
            log.error("Failed to log system state: {}", e.message)
        }

        // Log each zone's temperature
        zones.forEach { (zoneId, zoneInfo) ->
            try {
                // Extract zone number from zoneId (e.g., "z01" -> 1)
                val zoneNumber = zoneId.removePrefix("z").toIntOrNull()
                if (zoneNumber == null) {
                    log.warn("Could not parse zone number from: {}", zoneId)
                    return@forEach
                }

                val temperatureLog = TemperatureLog(
                    timestamp = timestamp,
                    zoneId = zoneNumber,
                    currentTemp = zoneInfo.measuredTemp ?: 0.0,
                    targetTemp = zoneInfo.setTemp?.toDouble() ?: 0.0,
                    zoneEnabled = zoneInfo.state == "open"
                )
                temperatureLogRepository.save(temperatureLog)
                log.debug("Logged zone {} temperature: current={}, target={}, enabled={}",
                    zoneNumber, temperatureLog.currentTemp, temperatureLog.targetTemp, temperatureLog.zoneEnabled)
            } catch (e: Exception) {
                log.error("Failed to log temperature for zone {}: {}", zoneId, e.message)
            }
        }

        log.debug("Completed temperature logging for {} zones", zones.size)
    }
}
