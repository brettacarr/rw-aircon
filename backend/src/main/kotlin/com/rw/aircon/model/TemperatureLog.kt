package com.rw.aircon.model

import jakarta.persistence.*
import java.time.Instant

/**
 * Entity for logging temperature readings per zone.
 * Logged every 5 minutes to track historical temperature data.
 */
@Entity
@Table(name = "temperature_log", indexes = [
    Index(name = "idx_temperature_log_zone_timestamp", columnList = "zone_id, timestamp"),
    Index(name = "idx_temperature_log_timestamp", columnList = "timestamp")
])
data class TemperatureLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val timestamp: Instant = Instant.now(),

    @Column(name = "zone_id", nullable = false)
    val zoneId: Int = 0,

    @Column(name = "current_temp", nullable = false)
    val currentTemp: Double = 0.0,

    @Column(name = "target_temp", nullable = false)
    val targetTemp: Double = 0.0,

    @Column(name = "zone_enabled", nullable = false)
    val zoneEnabled: Boolean = true
)
