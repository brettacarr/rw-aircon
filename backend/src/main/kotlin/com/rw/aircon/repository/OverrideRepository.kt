package com.rw.aircon.repository

import com.rw.aircon.model.Override
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * Repository for managing Override entities.
 *
 * Only one override should be active at a time. When creating a new
 * override, any existing override should be deleted first.
 */
@Repository
interface OverrideRepository : JpaRepository<Override, Long> {

    /**
     * Find the most recent active (non-expired) override.
     * Returns the override with the latest expiration time that hasn't expired yet.
     */
    @Query("SELECT o FROM Override o WHERE o.expiresAt > :now ORDER BY o.expiresAt DESC")
    fun findActiveOverride(now: Instant = Instant.now()): Override?

    /**
     * Find all active (non-expired) overrides.
     * Normally there should be at most one, but this handles edge cases.
     */
    @Query("SELECT o FROM Override o WHERE o.expiresAt > :now")
    fun findAllActive(now: Instant = Instant.now()): List<Override>

    /**
     * Delete all expired overrides.
     * Used for cleanup during periodic maintenance.
     */
    @Query("DELETE FROM Override o WHERE o.expiresAt <= :now")
    fun deleteExpired(now: Instant = Instant.now())
}
