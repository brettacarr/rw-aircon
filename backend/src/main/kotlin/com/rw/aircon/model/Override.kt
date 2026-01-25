package com.rw.aircon.model

import jakarta.persistence.*
import java.time.Instant

/**
 * Override entity representing a temporary manual override of scheduled settings.
 *
 * When an override is active (not expired), it takes precedence over any scheduled settings.
 * Overrides are created when users make manual changes during an active schedule,
 * allowing them to temporarily deviate from the schedule for a specified duration.
 *
 * Duration types:
 * - Fixed duration: 1h, 2h, 4h - override expires after the specified time
 * - Until next: Override expires when the next scheduled period begins
 *
 * Zone overrides are stored as JSON in zoneOverrides field for flexibility.
 */
@Entity
@Table(name = "override")
data class Override(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * Timestamp when this override was created.
     */
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    /**
     * Timestamp when this override expires.
     * After expiration, the override is ignored and schedule resumes.
     */
    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    /**
     * Overridden system mode (cool, heat, vent, dry).
     * Null means do not change mode from current state.
     */
    @Column(name = "mode")
    val mode: String? = null,

    /**
     * Overridden system target temperature (16-32).
     * Null means do not change system temperature.
     */
    @Column(name = "system_temp")
    val systemTemp: Int? = null,

    /**
     * JSON array of zone override settings.
     * Format: [{"zoneId": 1, "temp": 22, "enabled": true}, ...]
     * Empty array or null means no zone-specific overrides.
     */
    @Column(name = "zone_overrides", columnDefinition = "TEXT")
    val zoneOverrides: String? = null
) {
    /**
     * Check if this override has expired.
     */
    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)

    /**
     * Check if this override is currently active (not expired).
     */
    fun isActive(): Boolean = !isExpired()
}
