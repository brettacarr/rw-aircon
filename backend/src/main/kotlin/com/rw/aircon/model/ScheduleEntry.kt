package com.rw.aircon.model

import jakarta.persistence.*
import java.time.LocalTime

/**
 * ScheduleEntry represents a time period within a weekly schedule for a season.
 *
 * Each entry defines:
 * - Which day of the week it applies to (1=Monday, 7=Sunday)
 * - Time range (start to end)
 * - System mode for that period (cool, heat, vent, dry)
 *
 * Time periods within the same day should not overlap.
 * The schedule execution service will apply these settings when the current
 * day/time falls within the period.
 */
@Entity
@Table(
    name = "schedule_entry",
    indexes = [
        Index(name = "idx_schedule_entry_season_day", columnList = "season_id, day_of_week")
    ]
)
data class ScheduleEntry(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * Reference to the parent season.
     */
    @Column(name = "season_id", nullable = false)
    val seasonId: Long = 0,

    /**
     * Day of week (1=Monday, 2=Tuesday, ..., 7=Sunday).
     * Uses ISO-8601 standard where Monday is 1.
     */
    @Column(name = "day_of_week", nullable = false)
    val dayOfWeek: Int = 1,

    /**
     * Start time of this schedule period (HH:MM).
     * Stored as ISO LocalTime string in SQLite.
     */
    @Column(name = "start_time", nullable = false)
    val startTime: LocalTime = LocalTime.of(0, 0),

    /**
     * End time of this schedule period (HH:MM).
     * Must be after startTime. Stored as ISO LocalTime string in SQLite.
     */
    @Column(name = "end_time", nullable = false)
    val endTime: LocalTime = LocalTime.of(23, 59),

    /**
     * System mode to apply during this period (cool, heat, vent, dry).
     */
    @Column(nullable = false)
    val mode: String = "cool"
)
