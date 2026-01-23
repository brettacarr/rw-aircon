package com.rw.aircon.model

import jakarta.persistence.*

/**
 * Season entity representing a scheduling period with date range and priority.
 *
 * Seasons define when a set of schedules are active based on calendar dates.
 * When multiple seasons overlap on a given date, the one with highest priority wins.
 *
 * Date range uses month/day (ignoring year) to repeat annually.
 * Example: "Summer" from Dec 1 to Feb 28, "Winter" from Jun 1 to Aug 31.
 */
@Entity
@Table(name = "season")
data class Season(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val name: String = "",

    /**
     * Start month of the season (1-12).
     */
    @Column(name = "start_month", nullable = false)
    val startMonth: Int = 1,

    /**
     * Start day of month (1-31).
     */
    @Column(name = "start_day", nullable = false)
    val startDay: Int = 1,

    /**
     * End month of the season (1-12).
     */
    @Column(name = "end_month", nullable = false)
    val endMonth: Int = 12,

    /**
     * End day of month (1-31).
     */
    @Column(name = "end_day", nullable = false)
    val endDay: Int = 31,

    /**
     * Priority for overlap resolution (higher value = higher priority).
     * When multiple seasons are active on a given date, highest priority wins.
     */
    @Column(nullable = false)
    val priority: Int = 0,

    /**
     * Whether this season is currently enabled.
     * Disabled seasons are skipped during schedule evaluation.
     */
    @Column(nullable = false)
    val active: Boolean = true
)
