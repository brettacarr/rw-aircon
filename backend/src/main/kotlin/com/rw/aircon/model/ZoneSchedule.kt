package com.rw.aircon.model

import jakarta.persistence.*

/**
 * ZoneSchedule defines per-zone settings for a schedule entry.
 *
 * Each ScheduleEntry can have multiple ZoneSchedule records, one per zone.
 * This allows different temperature targets and on/off states per zone
 * during each scheduled time period.
 */
@Entity
@Table(
    name = "zone_schedule",
    indexes = [
        Index(name = "idx_zone_schedule_entry", columnList = "schedule_entry_id")
    ]
)
data class ZoneSchedule(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * Reference to the parent schedule entry.
     */
    @Column(name = "schedule_entry_id", nullable = false)
    val scheduleEntryId: Long = 0,

    /**
     * Reference to the zone (database ID, not MyAir zone ID).
     */
    @Column(name = "zone_id", nullable = false)
    val zoneId: Long = 0,

    /**
     * Target temperature for this zone during the scheduled period (16-32°C).
     */
    @Column(name = "target_temp", nullable = false)
    val targetTemp: Int = 22,

    /**
     * Whether the zone should be open (true) or closed (false) during this period.
     */
    @Column(nullable = false)
    val enabled: Boolean = true
)
