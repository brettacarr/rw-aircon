package com.rw.aircon.repository

import com.rw.aircon.model.TemperatureLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

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
}
