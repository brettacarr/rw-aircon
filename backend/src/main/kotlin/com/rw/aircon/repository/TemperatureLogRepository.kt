package com.rw.aircon.repository

import com.rw.aircon.model.TemperatureLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * Projection interface for hourly temperature averages.
 * Used when aggregating data for time ranges > 7 days to reduce payload size.
 */
interface HourlyTemperatureAverage {
    val hour: String           // ISO timestamp representing the hour
    val avgCurrentTemp: Double
    val avgTargetTemp: Double
}

@Repository
interface TemperatureLogRepository : JpaRepository<TemperatureLog, Long> {

    /**
     * Find temperature logs for a specific zone within a time range.
     */
    fun findByZoneIdAndTimestampBetweenOrderByTimestampAsc(
        zoneId: Int,
        from: Instant,
        to: Instant
    ): List<TemperatureLog>

    /**
     * Delete logs older than the specified timestamp for data retention.
     */
    @Modifying
    @Query("DELETE FROM TemperatureLog t WHERE t.timestamp < :cutoff")
    fun deleteByTimestampBefore(cutoff: Instant): Int

    /**
     * Find hourly averages for a specific zone within a time range.
     * Groups data by hour and calculates average temperatures.
     * Used for efficient data transfer when displaying longer time ranges (> 7 days).
     *
     * Timestamps are stored as ISO strings (e.g., "2026-01-20T10:30:00Z").
     * SQLite substr() extracts YYYY-MM-DDTHH for hour grouping.
     * ISO format allows lexicographic comparison for date filtering.
     */
    @Query(
        value = """
            SELECT
                substr(timestamp, 1, 13) || ':00:00Z' as hour,
                AVG(current_temp) as avgCurrentTemp,
                AVG(target_temp) as avgTargetTemp
            FROM temperature_log
            WHERE zone_id = :zoneId
              AND timestamp >= :fromTimestamp
              AND timestamp <= :toTimestamp
            GROUP BY substr(timestamp, 1, 13)
            ORDER BY hour ASC
        """,
        nativeQuery = true
    )
    fun findHourlyAveragesByZoneIdAndTimestampBetween(
        zoneId: Int,
        fromTimestamp: String,
        toTimestamp: String
    ): List<HourlyTemperatureAverage>
}
