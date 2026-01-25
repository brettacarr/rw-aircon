package com.rw.aircon.model

import com.rw.aircon.config.InstantConverter
import jakarta.persistence.*
import java.time.Instant

/**
 * Entity for logging Auto Mode actions.
 *
 * Records each automatic adjustment made by the Auto Mode system,
 * including the reason for the action and zone temperatures at the time.
 * This provides visibility into system decision-making for debugging
 * and user transparency.
 */
@Entity
@Table(name = "auto_mode_log", indexes = [
    Index(name = "idx_auto_mode_log_timestamp", columnList = "timestamp"),
    Index(name = "idx_auto_mode_log_action", columnList = "action")
])
data class AutoModeLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    @Convert(converter = InstantConverter::class)
    val timestamp: Instant = Instant.now(),

    /**
     * Type of action taken: heat_on, cool_on, system_off, mode_change
     */
    @Column(nullable = false)
    val action: String,

    /**
     * Human-readable reason for the action.
     * E.g., "Living (myZone) is 1.5°C below minimum"
     */
    @Column(nullable = false)
    val reason: String,

    /**
     * ID of the zone that triggered the action, if any.
     */
    @Column(name = "triggering_zone_id")
    val triggeringZoneId: Long? = null,

    /**
     * System mode before the action (heat, cool, vent, dry, off).
     */
    @Column(name = "system_mode")
    val systemMode: String? = null,

    /**
     * System mode after the action (heat, cool, vent, dry, off).
     */
    @Column(name = "new_system_mode")
    val newSystemMode: String? = null,

    /**
     * JSON snapshot of all zone temperatures at the time of action.
     * Format: [{"zoneId":1,"zoneName":"Living","currentTemp":21.5,"minTemp":20.0,"maxTemp":24.0}]
     */
    @Column(name = "zone_temps", columnDefinition = "TEXT")
    val zoneTemps: String? = null
) {
    companion object {
        // Action type constants
        const val ACTION_HEAT_ON = "heat_on"
        const val ACTION_COOL_ON = "cool_on"
        const val ACTION_SYSTEM_OFF = "system_off"
        const val ACTION_MODE_CHANGE = "mode_change"
    }
}
