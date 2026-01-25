package com.rw.aircon.repository

import com.rw.aircon.model.AutoModeLog
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface AutoModeLogRepository : JpaRepository<AutoModeLog, Long> {

    /**
     * Find recent logs ordered by timestamp descending (newest first).
     * Uses pagination to support the limit parameter.
     */
    fun findByOrderByTimestampDesc(pageable: Pageable): List<AutoModeLog>

    /**
     * Find logs by action type, ordered by timestamp descending.
     */
    fun findByActionOrderByTimestampDesc(action: String, pageable: Pageable): List<AutoModeLog>

    /**
     * Find logs within a time range.
     */
    fun findByTimestampBetweenOrderByTimestampDesc(
        from: Instant,
        to: Instant
    ): List<AutoModeLog>

    /**
     * Delete logs older than the specified timestamp for data retention.
     */
    @Modifying
    @Query("DELETE FROM AutoModeLog l WHERE l.timestamp < :cutoff")
    fun deleteByTimestampBefore(cutoff: Instant): Int

    /**
     * Count total logs (for pagination info).
     */
    @Query("SELECT COUNT(l) FROM AutoModeLog l")
    fun countLogs(): Long
}
