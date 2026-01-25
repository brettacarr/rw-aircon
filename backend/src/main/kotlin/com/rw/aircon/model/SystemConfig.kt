package com.rw.aircon.model

import jakarta.persistence.*
import java.time.Instant

/**
 * System-wide configuration entity.
 *
 * Stores global settings that don't belong to a specific feature.
 * Only one SystemConfig record should exist (singleton configuration).
 */
@Entity
@Table(name = "system_config")
data class SystemConfig(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * Current control mode: manual, auto, or schedule.
     * Determines which service controls the HVAC system.
     */
    @Column(name = "control_mode", nullable = false)
    @Enumerated(EnumType.STRING)
    val controlMode: ControlMode = ControlMode.MANUAL,

    /**
     * Timestamp when the control mode was last changed.
     */
    @Column(name = "mode_changed_at", nullable = false)
    val modeChangedAt: Instant = Instant.now()
) {
    companion object {
        /**
         * Create a default configuration with manual mode.
         */
        fun createDefault(): SystemConfig = SystemConfig(
            controlMode = ControlMode.MANUAL,
            modeChangedAt = Instant.now()
        )
    }
}
