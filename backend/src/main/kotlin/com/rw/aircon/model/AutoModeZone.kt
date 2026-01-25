package com.rw.aircon.model

import jakarta.persistence.*

/**
 * Per-zone configuration for Auto Mode.
 *
 * Each zone can have its own min/max temperature range. When Auto Mode is active,
 * the system attempts to keep all enabled zones within their configured ranges.
 *
 * Constraints:
 * - Temperature range: 16°C to 32°C (matching MyAir limits)
 * - Minimum gap: 2°C between min and max (e.g., 22-24 is valid, 22-23 is not)
 * - Guest zone (z02, id=2) cannot be the only enabled zone
 */
@Entity
@Table(
    name = "auto_mode_zone",
    uniqueConstraints = [UniqueConstraint(columnNames = ["zone_id"])]
)
data class AutoModeZone(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * Reference to the zone this configuration applies to.
     * Maps to zone.id in the database.
     */
    @Column(name = "zone_id", nullable = false)
    val zoneId: Long,

    /**
     * Whether this zone is enabled for Auto Mode temperature management.
     * Disabled zones are not considered when making heating/cooling decisions.
     */
    @Column(name = "enabled", nullable = false)
    val enabled: Boolean = true,

    /**
     * Minimum acceptable temperature for this zone (16.0-32.0).
     * When zone temperature drops below this, heating is triggered.
     * Target temp for heating = minTemp + 0.5°C (hysteresis).
     */
    @Column(name = "min_temp", nullable = false)
    val minTemp: Double = 20.0,

    /**
     * Maximum acceptable temperature for this zone (16.0-32.0).
     * When zone temperature rises above this, cooling is triggered.
     * Target temp for cooling = maxTemp - 0.5°C (hysteresis).
     */
    @Column(name = "max_temp", nullable = false)
    val maxTemp: Double = 24.0
) {
    companion object {
        const val MIN_ALLOWED_TEMP = 16.0
        const val MAX_ALLOWED_TEMP = 32.0
        const val MIN_TEMP_GAP = 2.0
        const val HYSTERESIS = 0.5

        // Guest zone ID - cannot be the sole enabled zone or priority zone
        const val GUEST_ZONE_ID = 2L
    }

    /**
     * Validate the temperature range configuration.
     * @throws IllegalArgumentException if validation fails
     */
    fun validate() {
        require(minTemp >= MIN_ALLOWED_TEMP) {
            "Minimum temperature must be at least $MIN_ALLOWED_TEMP°C"
        }
        require(maxTemp <= MAX_ALLOWED_TEMP) {
            "Maximum temperature must not exceed $MAX_ALLOWED_TEMP°C"
        }
        require(maxTemp - minTemp >= MIN_TEMP_GAP) {
            "Temperature range must be at least $MIN_TEMP_GAP°C (got ${maxTemp - minTemp}°C)"
        }
    }

    /**
     * Check if the current temperature is below the minimum (needs heating to start).
     */
    fun needsHeating(currentTemp: Double): Boolean = currentTemp < minTemp

    /**
     * Check if the current temperature is above the maximum (needs cooling to start).
     */
    fun needsCooling(currentTemp: Double): Boolean = currentTemp > maxTemp

    /**
     * Check if heating should continue (hysteresis not yet reached).
     * Heating continues until temp reaches minTemp + HYSTERESIS.
     */
    fun shouldContinueHeating(currentTemp: Double): Boolean = currentTemp < minTemp + HYSTERESIS

    /**
     * Check if cooling should continue (hysteresis not yet reached).
     * Cooling continues until temp reaches maxTemp - HYSTERESIS.
     */
    fun shouldContinueCooling(currentTemp: Double): Boolean = currentTemp > maxTemp - HYSTERESIS

    /**
     * Check if the current temperature is within the acceptable range.
     */
    fun isInRange(currentTemp: Double): Boolean = currentTemp >= minTemp && currentTemp <= maxTemp

    /**
     * Get the target temperature for heating (with hysteresis).
     */
    fun getHeatingTarget(): Double = minTemp + HYSTERESIS

    /**
     * Get the target temperature for cooling (with hysteresis).
     */
    fun getCoolingTarget(): Double = maxTemp - HYSTERESIS

    /**
     * Check if this is the Guest zone.
     */
    fun isGuestZone(): Boolean = zoneId == GUEST_ZONE_ID
}
