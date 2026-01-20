package com.rw.aircon.repository

import com.rw.aircon.model.SystemLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface SystemLogRepository : JpaRepository<SystemLog, Long> {

    /**
     * Find system logs within a time range.
     */
    fun findByTimestampBetweenOrderByTimestampAsc(
        from: Instant,
        to: Instant
    ): List<SystemLog>

    /**
     * Delete logs older than the specified timestamp for data retention.
     */
    @Modifying
    @Query("DELETE FROM SystemLog s WHERE s.timestamp < :cutoff")
    fun deleteByTimestampBefore(cutoff: Instant): Int
}
