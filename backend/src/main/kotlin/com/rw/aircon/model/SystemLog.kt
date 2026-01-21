package com.rw.aircon.model

import com.rw.aircon.config.InstantConverter
import jakarta.persistence.*
import java.time.Instant

/**
 * Entity for logging system-level state.
 * Logged every 5 minutes to track historical system data.
 */
@Entity
@Table(name = "system_log", indexes = [
    Index(name = "idx_system_log_timestamp", columnList = "timestamp")
])
data class SystemLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    @Convert(converter = InstantConverter::class)
    val timestamp: Instant = Instant.now(),

    @Column(nullable = false)
    val mode: String = "",

    @Column(name = "outdoor_temp")
    val outdoorTemp: Double? = null,

    @Column(name = "system_on", nullable = false)
    val systemOn: Boolean = false
)
