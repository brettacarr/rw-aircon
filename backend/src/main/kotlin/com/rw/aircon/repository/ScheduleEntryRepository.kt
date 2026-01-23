package com.rw.aircon.repository

import com.rw.aircon.model.ScheduleEntry
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ScheduleEntryRepository : JpaRepository<ScheduleEntry, Long> {

    /**
     * Find all schedule entries for a specific season.
     */
    fun findBySeasonIdOrderByDayOfWeekAscStartTimeAsc(seasonId: Long): List<ScheduleEntry>

    /**
     * Find schedule entries for a specific season and day of week.
     * Used for schedule execution to find applicable time periods.
     */
    fun findBySeasonIdAndDayOfWeekOrderByStartTimeAsc(seasonId: Long, dayOfWeek: Int): List<ScheduleEntry>

    /**
     * Delete all schedule entries for a season (cascade delete).
     */
    @Modifying
    fun deleteBySeasonId(seasonId: Long): Int
}
