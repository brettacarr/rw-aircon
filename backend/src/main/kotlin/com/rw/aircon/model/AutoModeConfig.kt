package com.rw.aircon.model

import jakarta.persistence.*
import java.time.Instant

/**
 * Configuration entity for Auto Mode.
 *
 * Auto Mode automatically maintains zone temperatures within user-defined min/max ranges.
 * The system switches between heating, cooling, and off states based on zone temperatures.
 *
 * Only one AutoModeConfig record should exist (singleton configuration).
 * The 'active' field determines if Auto Mode is the current control mode.
 */
@Entity
@Table(name = "auto_mode_config")
data class AutoModeConfig(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * Whether Auto Mode is currently active as the control mode.
     * When true, AutoModeExecutionService controls the HVAC system.
     * When false, the system is in manual or schedule mode.
     */
    @Column(name = "active", nullable = false)
    val active: Boolean = false,

    /**
     * Timestamp when this configuration was created.
     */
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    /**
     * Timestamp when this configuration was last updated.
     */
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),

    /**
     * The preferred myZone for Auto Mode.
     * This zone gets priority when determining heating/cooling needs.
     * Cannot be set to the Guest zone (z02, id=2).
     * Null means use automatic selection based on temperature deviation.
     */
    @Column(name = "priority_zone_id")
    val priorityZoneId: Long? = null
) {
    companion object {
        /**
         * Create a default configuration with Auto Mode disabled.
         */
        fun createDefault(): AutoModeConfig = AutoModeConfig(
            active = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            priorityZoneId = null
        )
    }
}
