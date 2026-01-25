package com.rw.aircon.service

import com.rw.aircon.config.TemperatureLoggingProperties
import com.rw.aircon.repository.OverrideRepository
import com.rw.aircon.repository.SystemLogRepository
import com.rw.aircon.repository.TemperatureLogRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Service for managing data retention.
 * Automatically cleans up old temperature and system logs based on configured retention period.
 */
@Service
class DataRetentionService(
    private val temperatureLogRepository: TemperatureLogRepository,
    private val systemLogRepository: SystemLogRepository,
    private val overrideRepository: OverrideRepository,
    private val properties: TemperatureLoggingProperties
) {
    private val log = LoggerFactory.getLogger(DataRetentionService::class.java)

    /**
     * Scheduled task to clean up old log records.
     * Runs daily at 2:00 AM to minimize impact on normal operations.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    fun cleanupOldRecords() {
        val cutoff = Instant.now().minus(properties.retentionDays.toLong(), ChronoUnit.DAYS)
        log.info("Starting data retention cleanup. Removing records older than {} days (before {})",
            properties.retentionDays, cutoff)

        try {
            val deletedTempLogs = temperatureLogRepository.deleteByTimestampBefore(cutoff)
            log.info("Deleted {} temperature log records", deletedTempLogs)

            val deletedSysLogs = systemLogRepository.deleteByTimestampBefore(cutoff)
            log.info("Deleted {} system log records", deletedSysLogs)

            // Clean up expired overrides (no retention period needed - just delete if expired)
            overrideRepository.deleteExpired()
            log.info("Cleaned up expired overrides")

            log.info("Data retention cleanup completed successfully")
        } catch (e: Exception) {
            log.error("Failed to complete data retention cleanup: {}", e.message, e)
        }
    }
}
