package com.rw.aircon.service

import com.rw.aircon.dto.*
import com.rw.aircon.model.Season
import com.rw.aircon.model.ScheduleEntry
import com.rw.aircon.model.ZoneSchedule
import com.rw.aircon.repository.ScheduleEntryRepository
import com.rw.aircon.repository.SeasonRepository
import com.rw.aircon.repository.ZoneRepository
import com.rw.aircon.repository.ZoneScheduleRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime

/**
 * Service for managing seasons and their schedules.
 *
 * Handles CRUD operations for seasons, schedule entries, and zone schedules.
 * Validates date ranges, time periods, and temperature bounds.
 */
@Service
class SeasonService(
    private val seasonRepository: SeasonRepository,
    private val scheduleEntryRepository: ScheduleEntryRepository,
    private val zoneScheduleRepository: ZoneScheduleRepository,
    private val zoneRepository: ZoneRepository
) {
    private val log = LoggerFactory.getLogger(SeasonService::class.java)

    companion object {
        private val VALID_MODES = setOf("cool", "heat", "vent", "dry")
        private const val MIN_TEMP = 16
        private const val MAX_TEMP = 32
    }

    /**
     * Get all seasons ordered by priority.
     */
    fun getAllSeasons(): List<SeasonResponse> {
        return seasonRepository.findAllByOrderByPriorityDesc()
            .map { it.toResponse() }
    }

    /**
     * Get a single season by ID.
     */
    fun getSeason(id: Long): SeasonResponse? {
        return seasonRepository.findById(id)
            .map { it.toResponse() }
            .orElse(null)
    }

    /**
     * Get a season with its full schedule.
     */
    fun getSeasonWithSchedule(id: Long): SeasonWithScheduleResponse? {
        val season = seasonRepository.findById(id).orElse(null) ?: return null
        val entries = scheduleEntryRepository.findBySeasonIdOrderByDayOfWeekAscStartTimeAsc(id)
        val entryIds = entries.map { it.id }
        val zoneSchedules = if (entryIds.isNotEmpty()) {
            zoneScheduleRepository.findByScheduleEntryIdIn(entryIds)
        } else {
            emptyList()
        }

        // Group zone schedules by entry ID
        val zoneSchedulesByEntry = zoneSchedules.groupBy { it.scheduleEntryId }

        // Load zone names for response
        val zones = zoneRepository.findAll().associateBy { it.id }

        val scheduleResponses = entries.map { entry ->
            val zoneSettings = (zoneSchedulesByEntry[entry.id] ?: emptyList()).map { zs ->
                ZoneScheduleResponse(
                    id = zs.id,
                    zoneId = zs.zoneId,
                    zoneName = zones[zs.zoneId]?.name ?: "Zone ${zs.zoneId}",
                    targetTemp = zs.targetTemp,
                    enabled = zs.enabled
                )
            }
            entry.toResponse(zoneSettings)
        }

        return SeasonWithScheduleResponse(
            season = season.toResponse(),
            schedule = scheduleResponses
        )
    }

    /**
     * Create a new season.
     */
    @Transactional
    fun createSeason(request: SeasonCreateRequest): SeasonResponse {
        validateSeasonRequest(request.name, request.startMonth, request.startDay, request.endMonth, request.endDay)

        if (seasonRepository.existsByName(request.name)) {
            throw IllegalArgumentException("Season with name '${request.name}' already exists")
        }

        val season = Season(
            name = request.name,
            startMonth = request.startMonth,
            startDay = request.startDay,
            endMonth = request.endMonth,
            endDay = request.endDay,
            priority = request.priority,
            active = request.active
        )

        val saved = seasonRepository.save(season)
        log.info("Created season: {} (id={})", saved.name, saved.id)
        return saved.toResponse()
    }

    /**
     * Update an existing season.
     */
    @Transactional
    fun updateSeason(id: Long, request: SeasonUpdateRequest): SeasonResponse? {
        val existing = seasonRepository.findById(id).orElse(null) ?: return null

        val name = request.name ?: existing.name
        val startMonth = request.startMonth ?: existing.startMonth
        val startDay = request.startDay ?: existing.startDay
        val endMonth = request.endMonth ?: existing.endMonth
        val endDay = request.endDay ?: existing.endDay

        validateSeasonRequest(name, startMonth, startDay, endMonth, endDay)

        if (request.name != null && seasonRepository.existsByNameAndIdNot(request.name, id)) {
            throw IllegalArgumentException("Season with name '${request.name}' already exists")
        }

        val updated = existing.copy(
            name = name,
            startMonth = startMonth,
            startDay = startDay,
            endMonth = endMonth,
            endDay = endDay,
            priority = request.priority ?: existing.priority,
            active = request.active ?: existing.active
        )

        val saved = seasonRepository.save(updated)
        log.info("Updated season: {} (id={})", saved.name, saved.id)
        return saved.toResponse()
    }

    /**
     * Delete a season and its schedules (cascading).
     */
    @Transactional
    fun deleteSeason(id: Long): Boolean {
        if (!seasonRepository.existsById(id)) {
            return false
        }

        // Delete zone schedules first, then schedule entries, then season
        val entries = scheduleEntryRepository.findBySeasonIdOrderByDayOfWeekAscStartTimeAsc(id)
        val entryIds = entries.map { it.id }
        if (entryIds.isNotEmpty()) {
            zoneScheduleRepository.deleteByScheduleEntryIdIn(entryIds)
        }
        scheduleEntryRepository.deleteBySeasonId(id)
        seasonRepository.deleteById(id)

        log.info("Deleted season with id={}", id)
        return true
    }

    /**
     * Get the full schedule for a season.
     */
    fun getSchedule(seasonId: Long): List<ScheduleEntryResponse>? {
        if (!seasonRepository.existsById(seasonId)) {
            return null
        }

        val entries = scheduleEntryRepository.findBySeasonIdOrderByDayOfWeekAscStartTimeAsc(seasonId)
        val entryIds = entries.map { it.id }
        val zoneSchedules = if (entryIds.isNotEmpty()) {
            zoneScheduleRepository.findByScheduleEntryIdIn(entryIds)
        } else {
            emptyList()
        }

        val zoneSchedulesByEntry = zoneSchedules.groupBy { it.scheduleEntryId }
        val zones = zoneRepository.findAll().associateBy { it.id }

        return entries.map { entry ->
            val zoneSettings = (zoneSchedulesByEntry[entry.id] ?: emptyList()).map { zs ->
                ZoneScheduleResponse(
                    id = zs.id,
                    zoneId = zs.zoneId,
                    zoneName = zones[zs.zoneId]?.name ?: "Zone ${zs.zoneId}",
                    targetTemp = zs.targetTemp,
                    enabled = zs.enabled
                )
            }
            entry.toResponse(zoneSettings)
        }
    }

    /**
     * Replace the entire schedule for a season.
     * Deletes existing entries and creates new ones.
     */
    @Transactional
    fun updateSchedule(seasonId: Long, request: FullScheduleUpdateRequest): List<ScheduleEntryResponse>? {
        if (!seasonRepository.existsById(seasonId)) {
            return null
        }

        // Validate all entries
        val zones = zoneRepository.findAll()
        val validZoneIds = zones.map { it.id }.toSet()
        val zonesByIdMap = zones.associateBy { it.id }

        for (entry in request.entries) {
            validateScheduleEntry(entry, validZoneIds)
        }

        // Validate no overlapping time periods within same day
        val entriesByDay = request.entries.groupBy { it.dayOfWeek }
        for ((day, dayEntries) in entriesByDay) {
            validateNoTimeOverlaps(day, dayEntries)
        }

        // Delete existing schedule
        val existingEntries = scheduleEntryRepository.findBySeasonIdOrderByDayOfWeekAscStartTimeAsc(seasonId)
        val existingEntryIds = existingEntries.map { it.id }
        if (existingEntryIds.isNotEmpty()) {
            zoneScheduleRepository.deleteByScheduleEntryIdIn(existingEntryIds)
        }
        scheduleEntryRepository.deleteBySeasonId(seasonId)

        // Create new schedule entries
        val responses = mutableListOf<ScheduleEntryResponse>()

        for (entryRequest in request.entries) {
            val startTime = LocalTime.parse(entryRequest.startTime)
            val endTime = LocalTime.parse(entryRequest.endTime)

            val entry = scheduleEntryRepository.save(
                ScheduleEntry(
                    seasonId = seasonId,
                    dayOfWeek = entryRequest.dayOfWeek,
                    startTime = startTime,
                    endTime = endTime,
                    mode = entryRequest.mode.lowercase()
                )
            )

            // Create zone schedules
            val zoneSettings = entryRequest.zoneSettings.map { zs ->
                val saved = zoneScheduleRepository.save(
                    ZoneSchedule(
                        scheduleEntryId = entry.id,
                        zoneId = zs.zoneId,
                        targetTemp = zs.targetTemp,
                        enabled = zs.enabled
                    )
                )
                ZoneScheduleResponse(
                    id = saved.id,
                    zoneId = saved.zoneId,
                    zoneName = zonesByIdMap[saved.zoneId]?.name ?: "Zone ${saved.zoneId}",
                    targetTemp = saved.targetTemp,
                    enabled = saved.enabled
                )
            }

            responses.add(entry.toResponse(zoneSettings))
        }

        log.info("Updated schedule for season id={} with {} entries", seasonId, responses.size)
        return responses
    }

    private fun validateSeasonRequest(name: String, startMonth: Int, startDay: Int, endMonth: Int, endDay: Int) {
        if (name.isBlank()) {
            throw IllegalArgumentException("Season name cannot be blank")
        }
        if (startMonth !in 1..12) {
            throw IllegalArgumentException("Start month must be between 1 and 12")
        }
        if (endMonth !in 1..12) {
            throw IllegalArgumentException("End month must be between 1 and 12")
        }
        if (startDay !in 1..31) {
            throw IllegalArgumentException("Start day must be between 1 and 31")
        }
        if (endDay !in 1..31) {
            throw IllegalArgumentException("End day must be between 1 and 31")
        }
    }

    private fun validateScheduleEntry(entry: ScheduleEntryRequest, validZoneIds: Set<Long>) {
        if (entry.dayOfWeek !in 1..7) {
            throw IllegalArgumentException("Day of week must be between 1 (Monday) and 7 (Sunday)")
        }

        val startTime = try {
            LocalTime.parse(entry.startTime)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid start time format: ${entry.startTime}. Use HH:MM format.")
        }

        val endTime = try {
            LocalTime.parse(entry.endTime)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid end time format: ${entry.endTime}. Use HH:MM format.")
        }

        if (!endTime.isAfter(startTime)) {
            throw IllegalArgumentException("End time must be after start time")
        }

        if (entry.mode.lowercase() !in VALID_MODES) {
            throw IllegalArgumentException("Invalid mode: ${entry.mode}. Must be one of: $VALID_MODES")
        }

        for (zs in entry.zoneSettings) {
            if (zs.zoneId !in validZoneIds) {
                throw IllegalArgumentException("Invalid zone ID: ${zs.zoneId}")
            }
            if (zs.targetTemp !in MIN_TEMP..MAX_TEMP) {
                throw IllegalArgumentException("Temperature must be between $MIN_TEMP and $MAX_TEMP")
            }
        }
    }

    private fun validateNoTimeOverlaps(day: Int, entries: List<ScheduleEntryRequest>) {
        val sorted = entries.sortedBy { LocalTime.parse(it.startTime) }

        for (i in 0 until sorted.size - 1) {
            val current = sorted[i]
            val next = sorted[i + 1]

            val currentEnd = LocalTime.parse(current.endTime)
            val nextStart = LocalTime.parse(next.startTime)

            if (currentEnd.isAfter(nextStart)) {
                throw IllegalArgumentException(
                    "Time periods overlap on day $day: ${current.startTime}-${current.endTime} and ${next.startTime}-${next.endTime}"
                )
            }
        }
    }

    private fun Season.toResponse() = SeasonResponse(
        id = id,
        name = name,
        startMonth = startMonth,
        startDay = startDay,
        endMonth = endMonth,
        endDay = endDay,
        priority = priority,
        active = active
    )

    private fun ScheduleEntry.toResponse(zoneSettings: List<ZoneScheduleResponse>) = ScheduleEntryResponse(
        id = id,
        seasonId = seasonId,
        dayOfWeek = dayOfWeek,
        startTime = String.format("%02d:%02d", startTime.hour, startTime.minute),
        endTime = String.format("%02d:%02d", endTime.hour, endTime.minute),
        mode = mode,
        zoneSettings = zoneSettings
    )
}
