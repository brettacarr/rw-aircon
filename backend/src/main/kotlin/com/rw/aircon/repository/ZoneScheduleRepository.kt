package com.rw.aircon.repository

import com.rw.aircon.model.ZoneSchedule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ZoneScheduleRepository : JpaRepository<ZoneSchedule, Long> {

    /**
     * Find all zone schedules for a specific schedule entry.
     */
    fun findByScheduleEntryId(scheduleEntryId: Long): List<ZoneSchedule>

    /**
     * Find zone schedules for multiple schedule entries.
     * Useful for loading all zone settings for a season's schedules.
     */
    fun findByScheduleEntryIdIn(scheduleEntryIds: List<Long>): List<ZoneSchedule>

    /**
     * Delete all zone schedules for a schedule entry (cascade delete).
     */
    @Modifying
    fun deleteByScheduleEntryId(scheduleEntryId: Long): Int

    /**
     * Delete all zone schedules for multiple schedule entries (cascade delete for season).
     */
    @Modifying
    @Query("DELETE FROM ZoneSchedule z WHERE z.scheduleEntryId IN :scheduleEntryIds")
    fun deleteByScheduleEntryIdIn(scheduleEntryIds: List<Long>): Int
}
