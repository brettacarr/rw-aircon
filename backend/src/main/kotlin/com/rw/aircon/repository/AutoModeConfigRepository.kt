package com.rw.aircon.repository

import com.rw.aircon.model.AutoModeConfig
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

/**
 * Repository for managing AutoModeConfig entities.
 *
 * Only one AutoModeConfig should exist in the database (singleton pattern).
 * Use getOrCreateConfig() in the service layer to ensure this invariant.
 */
@Repository
interface AutoModeConfigRepository : JpaRepository<AutoModeConfig, Long> {

    /**
     * Find the single configuration record.
     * Returns the first record ordered by ID, or null if none exists.
     */
    @Query("SELECT c FROM AutoModeConfig c ORDER BY c.id ASC")
    fun findConfig(): AutoModeConfig?

    /**
     * Check if Auto Mode is currently active.
     */
    @Query("SELECT c.active FROM AutoModeConfig c ORDER BY c.id ASC")
    fun isAutoModeActive(): Boolean?
}
