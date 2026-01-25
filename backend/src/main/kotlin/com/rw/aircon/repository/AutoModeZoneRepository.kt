package com.rw.aircon.repository

import com.rw.aircon.model.AutoModeZone
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

/**
 * Repository for managing AutoModeZone entities.
 *
 * Each zone can have at most one AutoModeZone configuration.
 * The unique constraint on zone_id is enforced at the database level.
 */
@Repository
interface AutoModeZoneRepository : JpaRepository<AutoModeZone, Long> {

    /**
     * Find the Auto Mode configuration for a specific zone.
     */
    fun findByZoneId(zoneId: Long): AutoModeZone?

    /**
     * Find all enabled zones for Auto Mode.
     */
    fun findByEnabledTrue(): List<AutoModeZone>

    /**
     * Delete the configuration for a specific zone.
     */
    fun deleteByZoneId(zoneId: Long)

    /**
     * Find all enabled non-Guest zones.
     * Guest zone (id=2) cannot be the sole enabled zone or priority zone.
     */
    @Query("SELECT z FROM AutoModeZone z WHERE z.enabled = true AND z.zoneId != 2")
    fun findEnabledNonGuestZones(): List<AutoModeZone>

    /**
     * Count enabled non-Guest zones.
     */
    @Query("SELECT COUNT(z) FROM AutoModeZone z WHERE z.enabled = true AND z.zoneId != 2")
    fun countEnabledNonGuestZones(): Long
}
