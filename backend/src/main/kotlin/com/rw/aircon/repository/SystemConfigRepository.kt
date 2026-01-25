package com.rw.aircon.repository

import com.rw.aircon.model.ControlMode
import com.rw.aircon.model.SystemConfig
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

/**
 * Repository for managing SystemConfig entities.
 *
 * Only one SystemConfig should exist in the database (singleton pattern).
 * Use getOrCreateConfig() in the service layer to ensure this invariant.
 */
@Repository
interface SystemConfigRepository : JpaRepository<SystemConfig, Long> {

    /**
     * Find the single configuration record.
     * Returns the first record ordered by ID, or null if none exists.
     */
    @Query("SELECT c FROM SystemConfig c ORDER BY c.id ASC")
    fun findConfig(): SystemConfig?

    /**
     * Get the current control mode.
     */
    @Query("SELECT c.controlMode FROM SystemConfig c ORDER BY c.id ASC")
    fun getControlMode(): ControlMode?
}
