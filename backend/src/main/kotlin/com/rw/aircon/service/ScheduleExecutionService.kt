package com.rw.aircon.service

import com.rw.aircon.client.MyAirClient
import com.rw.aircon.model.ScheduleEntry
import com.rw.aircon.model.Season
import com.rw.aircon.model.ZoneSchedule
import com.rw.aircon.repository.ScheduleEntryRepository
import com.rw.aircon.repository.SeasonRepository
import com.rw.aircon.repository.ZoneRepository
import com.rw.aircon.repository.ZoneScheduleRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalTime
import java.time.MonthDay

/**
 * Service responsible for executing scheduled HVAC settings.
 *
 * Runs every minute to check if a schedule entry applies to the current time.
 * If so, applies the configured mode and zone settings to the MyAir system.
 *
 * Schedule evaluation logic:
 * 1. Find active season(s) matching today's date
 * 2. Select highest priority season if multiple match
 * 3. Find schedule entry for current day-of-week and time
 * 4. Apply mode and per-zone settings from the entry
 */
@Service
class ScheduleExecutionService(
    private val seasonRepository: SeasonRepository,
    private val scheduleEntryRepository: ScheduleEntryRepository,
    private val zoneScheduleRepository: ZoneScheduleRepository,
    private val zoneRepository: ZoneRepository,
    private val myAirClient: MyAirClient,
    private val myAirCacheService: MyAirCacheService
) {
    private val log = LoggerFactory.getLogger(ScheduleExecutionService::class.java)

    // Track last applied entry to avoid redundant commands
    @Volatile
    private var lastAppliedEntryId: Long? = null

    @Volatile
    private var lastAppliedTime: LocalTime? = null

    /**
     * Scheduled task that runs every minute to evaluate and apply schedules.
     */
    @Scheduled(cron = "0 * * * * *") // Every minute at :00 seconds
    fun evaluateAndApplySchedule() {
        try {
            val now = LocalDate.now()
            val currentTime = LocalTime.now()
            val dayOfWeek = now.dayOfWeek.value // 1=Monday, 7=Sunday

            log.debug("Evaluating schedule for {} {} (day {})", now, currentTime, dayOfWeek)

            // Find active season for today
            val activeSeason = determineActiveSeason(now)

            if (activeSeason == null) {
                log.debug("No active season for today, skipping schedule execution")
                lastAppliedEntryId = null
                return
            }

            log.debug("Active season: {} (priority={})", activeSeason.name, activeSeason.priority)

            // Find matching schedule entry for current day and time
            val entry = findCurrentPeriod(activeSeason.id, dayOfWeek, currentTime)

            if (entry == null) {
                log.debug("No schedule entry matches current time, maintaining current state")
                // Don't clear lastAppliedEntryId - we want to track transitions
                return
            }

            // Only apply if this is a different entry than last time
            // or if we haven't applied anything yet
            if (entry.id == lastAppliedEntryId) {
                log.debug("Schedule entry {} already applied, skipping", entry.id)
                return
            }

            log.info("Applying schedule entry {} (season={}, day={}, {}-{}, mode={})",
                entry.id, activeSeason.name, dayOfWeek, entry.startTime, entry.endTime, entry.mode)

            applyScheduleSettings(entry)

            lastAppliedEntryId = entry.id
            lastAppliedTime = currentTime

        } catch (e: Exception) {
            log.error("Error during schedule execution: {}", e.message, e)
        }
    }

    /**
     * Determines the active season for a given date.
     *
     * Seasons can span year boundaries (e.g., Dec 1 - Feb 28 for "Summer" in Australia).
     * Returns the highest priority active season that includes the given date.
     */
    fun determineActiveSeason(date: LocalDate): Season? {
        val activeSeasons = seasonRepository.findByActiveTrueOrderByPriorityDesc()

        if (activeSeasons.isEmpty()) {
            return null
        }

        val today = MonthDay.from(date)

        for (season in activeSeasons) {
            if (isDateInSeason(today, season)) {
                return season
            }
        }

        return null
    }

    /**
     * Checks if a month-day falls within a season's date range.
     *
     * Handles year-wrapping seasons (e.g., Dec 1 - Feb 28) by checking
     * if the date is >= start OR <= end when end < start.
     */
    private fun isDateInSeason(date: MonthDay, season: Season): Boolean {
        val start = MonthDay.of(season.startMonth, season.startDay)
        val end = MonthDay.of(season.endMonth, season.endDay)

        return if (start <= end) {
            // Normal range: e.g., Mar 1 - May 31
            date >= start && date <= end
        } else {
            // Year-wrapping range: e.g., Dec 1 - Feb 28
            date >= start || date <= end
        }
    }

    /**
     * Finds the schedule entry that applies to the given day and time.
     *
     * Returns the entry where current time falls between start and end times.
     */
    fun findCurrentPeriod(seasonId: Long, dayOfWeek: Int, currentTime: LocalTime): ScheduleEntry? {
        val entries = scheduleEntryRepository.findBySeasonIdAndDayOfWeekOrderByStartTimeAsc(seasonId, dayOfWeek)

        for (entry in entries) {
            if (currentTime >= entry.startTime && currentTime < entry.endTime) {
                return entry
            }
        }

        return null
    }

    /**
     * Applies the settings from a schedule entry to the MyAir system.
     *
     * Sets the system mode and applies per-zone temperature and power settings.
     */
    fun applyScheduleSettings(entry: ScheduleEntry) {
        // Set system mode
        try {
            val modeResult = myAirClient.setSystemInfo(mapOf("mode" to entry.mode))
            if (modeResult) {
                log.info("Set system mode to: {}", entry.mode)
            } else {
                log.warn("Failed to set system mode to: {}", entry.mode)
            }
        } catch (e: Exception) {
            log.error("Error setting system mode: {}", e.message)
        }

        // Get zone schedules for this entry
        val zoneSchedules = zoneScheduleRepository.findByScheduleEntryId(entry.id)

        // Load zone mappings for MyAir zone IDs
        val zones = zoneRepository.findAll().associateBy { it.id }

        // Apply each zone's settings
        for (zoneSchedule in zoneSchedules) {
            val zone = zones[zoneSchedule.zoneId]
            if (zone == null) {
                log.warn("Zone {} not found in database, skipping", zoneSchedule.zoneId)
                continue
            }

            applyZoneSettings(zone.myAirZoneId, zoneSchedule)
        }
    }

    /**
     * Applies settings to a single zone.
     */
    private fun applyZoneSettings(myAirZoneId: String, zoneSchedule: ZoneSchedule) {
        // Set zone temperature
        try {
            val tempResult = myAirClient.setZone(myAirZoneId, mapOf("setTemp" to zoneSchedule.targetTemp))
            if (tempResult) {
                log.debug("Set zone {} temperature to: {}", myAirZoneId, zoneSchedule.targetTemp)
            } else {
                log.warn("Failed to set zone {} temperature", myAirZoneId)
            }
        } catch (e: Exception) {
            log.error("Error setting zone {} temperature: {}", myAirZoneId, e.message)
        }

        // Set zone state (open/close)
        val state = if (zoneSchedule.enabled) "open" else "close"

        // Check if this is the myZone - can't close it
        if (!zoneSchedule.enabled) {
            try {
                val (myAirData, _) = myAirCacheService.getSystemData()
                val myZone = myAirData?.aircons?.ac1?.info?.myZone ?: 0
                val zoneInfo = myAirData?.aircons?.ac1?.zones?.get(myAirZoneId)

                if (zoneInfo?.number == myZone && myZone != 0) {
                    log.warn("Cannot close zone {}: it is the controlling zone (myZone={})", myAirZoneId, myZone)
                    return
                }
            } catch (e: Exception) {
                log.error("Error checking myZone status: {}", e.message)
            }
        }

        try {
            val stateResult = myAirClient.setZone(myAirZoneId, mapOf("state" to state))
            if (stateResult) {
                log.debug("Set zone {} state to: {}", myAirZoneId, state)
            } else {
                log.warn("Failed to set zone {} state", myAirZoneId)
            }
        } catch (e: Exception) {
            log.error("Error setting zone {} state: {}", myAirZoneId, e.message)
        }
    }

    /**
     * Manually triggers schedule evaluation. Useful for testing and on-demand application.
     */
    fun triggerEvaluation() {
        log.info("Manual schedule evaluation triggered")
        evaluateAndApplySchedule()
    }

    /**
     * Resets the last applied entry tracking, forcing re-application on next evaluation.
     */
    fun resetAppliedState() {
        lastAppliedEntryId = null
        lastAppliedTime = null
        log.info("Schedule applied state reset")
    }

    /**
     * Gets the ID of the last applied schedule entry.
     */
    fun getLastAppliedEntryId(): Long? = lastAppliedEntryId
}
