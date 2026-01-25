package com.rw.aircon.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.rw.aircon.client.MyAirClient
import com.rw.aircon.dto.*
import com.rw.aircon.model.Override
import com.rw.aircon.model.ScheduleEntry
import com.rw.aircon.repository.OverrideRepository
import com.rw.aircon.repository.ScheduleEntryRepository
import com.rw.aircon.repository.SeasonRepository
import com.rw.aircon.repository.ZoneRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.MonthDay
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Service for managing manual overrides of scheduled settings.
 *
 * Overrides take precedence over schedules when active. Only one override
 * can be active at a time. Creating a new override cancels any existing one.
 */
@Service
class OverrideService(
    private val overrideRepository: OverrideRepository,
    private val scheduleEntryRepository: ScheduleEntryRepository,
    private val seasonRepository: SeasonRepository,
    private val zoneRepository: ZoneRepository,
    private val myAirClient: MyAirClient,
    private val myAirCacheService: MyAirCacheService,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(OverrideService::class.java)

    /**
     * Get the currently active override, if any.
     */
    fun getActiveOverride(): OverrideResponse? {
        val override = overrideRepository.findActiveOverride() ?: return null
        return toOverrideResponse(override)
    }

    /**
     * Check if there is an active override.
     */
    fun hasActiveOverride(): Boolean {
        return overrideRepository.findActiveOverride() != null
    }

    /**
     * Get the active override entity, if any.
     */
    fun getActiveOverrideEntity(): Override? {
        return overrideRepository.findActiveOverride()
    }

    /**
     * Create a new override, replacing any existing active override.
     */
    @Transactional
    fun createOverride(request: OverrideCreateRequest): OverrideResponse {
        log.info("Creating override with duration: {}", request.duration)

        // Validate request
        validateOverrideRequest(request)

        // Calculate expiration time
        val expiresAt = calculateExpirationTime(request.duration)
        log.info("Override will expire at: {}", expiresAt)

        // Delete any existing active overrides
        val existingOverrides = overrideRepository.findAllActive()
        if (existingOverrides.isNotEmpty()) {
            log.info("Cancelling {} existing active override(s)", existingOverrides.size)
            overrideRepository.deleteAll(existingOverrides)
        }

        // Serialize zone overrides to JSON
        val zoneOverridesJson = request.zoneOverrides?.let { zones ->
            objectMapper.writeValueAsString(zones.map { z ->
                mapOf(
                    "zoneId" to z.zoneId,
                    "temp" to z.temp,
                    "enabled" to z.enabled
                )
            })
        }

        // Create new override
        val override = Override(
            createdAt = Instant.now(),
            expiresAt = expiresAt,
            mode = request.mode,
            systemTemp = request.systemTemp,
            zoneOverrides = zoneOverridesJson
        )

        val savedOverride = overrideRepository.save(override)
        log.info("Created override with ID: {}", savedOverride.id)

        // Apply override settings immediately
        applyOverrideSettings(savedOverride)

        return toOverrideResponse(savedOverride)
    }

    /**
     * Cancel the current active override.
     */
    @Transactional
    fun cancelOverride(): Boolean {
        val override = overrideRepository.findActiveOverride()
        if (override == null) {
            log.info("No active override to cancel")
            return false
        }

        log.info("Cancelling active override: {}", override.id)
        overrideRepository.delete(override)
        return true
    }

    /**
     * Apply override settings to the MyAir system.
     */
    fun applyOverrideSettings(override: Override) {
        log.info("Applying override settings")

        // Apply system mode if specified
        override.mode?.let { mode ->
            try {
                val result = myAirClient.setSystemInfo(mapOf("mode" to mode))
                if (result) {
                    log.info("Override: Set system mode to {}", mode)
                } else {
                    log.warn("Override: Failed to set system mode to {}", mode)
                }
            } catch (e: Exception) {
                log.error("Override: Error setting system mode: {}", e.message)
            }
        }

        // Apply system temperature if specified
        override.systemTemp?.let { temp ->
            try {
                val result = myAirClient.setSystemInfo(mapOf("setTemp" to temp.toString()))
                if (result) {
                    log.info("Override: Set system temperature to {}", temp)
                } else {
                    log.warn("Override: Failed to set system temperature to {}", temp)
                }
            } catch (e: Exception) {
                log.error("Override: Error setting system temperature: {}", e.message)
            }
        }

        // Apply zone overrides if specified
        override.zoneOverrides?.let { json ->
            try {
                val zoneOverrides: List<Map<String, Any?>> = objectMapper.readValue(json)
                val zones = zoneRepository.findAll().associateBy { it.id }

                for (zoneOverride in zoneOverrides) {
                    val zoneId = (zoneOverride["zoneId"] as Number).toLong()
                    val zone = zones[zoneId]
                    if (zone == null) {
                        log.warn("Override: Zone {} not found", zoneId)
                        continue
                    }

                    // Apply temperature if specified
                    (zoneOverride["temp"] as? Number)?.let { temp ->
                        try {
                            val result = myAirClient.setZone(zone.myAirZoneId, mapOf("setTemp" to temp.toString()))
                            if (result) {
                                log.info("Override: Set zone {} temperature to {}", zone.myAirZoneId, temp)
                            }
                        } catch (e: Exception) {
                            log.error("Override: Error setting zone {} temperature: {}", zone.myAirZoneId, e.message)
                        }
                    }

                    // Apply enabled state if specified
                    (zoneOverride["enabled"] as? Boolean)?.let { enabled ->
                        val state = if (enabled) "open" else "close"

                        // Check if this is myZone before closing
                        if (!enabled) {
                            try {
                                val (myAirData, _) = myAirCacheService.getSystemData()
                                val myZone = myAirData?.aircons?.ac1?.info?.myZone ?: 0
                                val zoneInfo = myAirData?.aircons?.ac1?.zones?.get(zone.myAirZoneId)

                                if (zoneInfo?.number == myZone && myZone != 0) {
                                    log.warn("Override: Cannot close zone {}: it is the controlling zone", zone.myAirZoneId)
                                    return@let
                                }
                            } catch (e: Exception) {
                                log.error("Override: Error checking myZone status: {}", e.message)
                            }
                        }

                        try {
                            val result = myAirClient.setZone(zone.myAirZoneId, mapOf("state" to state))
                            if (result) {
                                log.info("Override: Set zone {} state to {}", zone.myAirZoneId, state)
                            }
                        } catch (e: Exception) {
                            log.error("Override: Error setting zone {} state: {}", zone.myAirZoneId, e.message)
                        }
                    }
                }
            } catch (e: Exception) {
                log.error("Override: Error parsing zone overrides JSON: {}", e.message)
            }
        }
    }

    /**
     * Validate override create request.
     */
    private fun validateOverrideRequest(request: OverrideCreateRequest) {
        // Validate duration
        val validDurations = setOf("1h", "2h", "4h", "until_next")
        if (request.duration !in validDurations) {
            throw IllegalArgumentException("Invalid duration: ${request.duration}. Must be one of: $validDurations")
        }

        // Validate mode if specified
        request.mode?.let { mode ->
            val validModes = setOf("cool", "heat", "vent", "dry")
            if (mode !in validModes) {
                throw IllegalArgumentException("Invalid mode: $mode. Must be one of: $validModes")
            }
        }

        // Validate system temperature if specified
        request.systemTemp?.let { temp ->
            if (temp < 16 || temp > 32) {
                throw IllegalArgumentException("System temperature must be between 16 and 32")
            }
        }

        // Validate zone overrides if specified
        request.zoneOverrides?.forEach { zoneOverride ->
            zoneOverride.temp?.let { temp ->
                if (temp < 16 || temp > 32) {
                    throw IllegalArgumentException("Zone temperature must be between 16 and 32")
                }
            }
        }
    }

    /**
     * Calculate expiration time based on duration string.
     */
    private fun calculateExpirationTime(duration: String): Instant {
        return when (duration) {
            "1h" -> Instant.now().plus(1, ChronoUnit.HOURS)
            "2h" -> Instant.now().plus(2, ChronoUnit.HOURS)
            "4h" -> Instant.now().plus(4, ChronoUnit.HOURS)
            "until_next" -> calculateUntilNextPeriod()
            else -> throw IllegalArgumentException("Invalid duration: $duration")
        }
    }

    /**
     * Calculate when the next scheduled period begins.
     *
     * Looks at the current schedule to find when the next time period starts.
     * If no schedule is active or no next period is found, defaults to 4 hours.
     */
    private fun calculateUntilNextPeriod(): Instant {
        val now = LocalDate.now()
        val currentTime = LocalTime.now()
        val dayOfWeek = now.dayOfWeek.value
        val zoneId = ZoneId.systemDefault()

        // Find active season
        val activeSeasons = seasonRepository.findByActiveTrueOrderByPriorityDesc()
        val activeSeason = activeSeasons.find { season ->
            val today = MonthDay.from(now)
            isDateInSeason(today, season)
        }

        if (activeSeason == null) {
            log.info("No active season, defaulting to 4h override")
            return Instant.now().plus(4, ChronoUnit.HOURS)
        }

        // Find next scheduled period today
        val todayEntries = scheduleEntryRepository.findBySeasonIdAndDayOfWeekOrderByStartTimeAsc(
            activeSeason.id, dayOfWeek
        )

        // Find the next period that starts after current time
        val nextToday = todayEntries.find { it.startTime > currentTime }
        if (nextToday != null) {
            val nextStartDateTime = now.atTime(nextToday.startTime).atZone(zoneId).toInstant()
            log.info("Next period starts today at {}", nextToday.startTime)
            return nextStartDateTime
        }

        // Look at tomorrow
        val tomorrow = now.plusDays(1)
        val tomorrowDayOfWeek = tomorrow.dayOfWeek.value
        val tomorrowEntries = scheduleEntryRepository.findBySeasonIdAndDayOfWeekOrderByStartTimeAsc(
            activeSeason.id, tomorrowDayOfWeek
        )

        if (tomorrowEntries.isNotEmpty()) {
            val firstTomorrow = tomorrowEntries.first()
            val nextStartDateTime = tomorrow.atTime(firstTomorrow.startTime).atZone(zoneId).toInstant()
            log.info("Next period starts tomorrow at {}", firstTomorrow.startTime)
            return nextStartDateTime
        }

        // No schedule found, default to 4 hours
        log.info("No next period found, defaulting to 4h override")
        return Instant.now().plus(4, ChronoUnit.HOURS)
    }

    /**
     * Check if a date falls within a season's date range.
     */
    private fun isDateInSeason(date: MonthDay, season: com.rw.aircon.model.Season): Boolean {
        val start = MonthDay.of(season.startMonth, season.startDay)
        val end = MonthDay.of(season.endMonth, season.endDay)

        return if (start <= end) {
            date >= start && date <= end
        } else {
            date >= start || date <= end
        }
    }

    /**
     * Convert Override entity to OverrideResponse DTO.
     */
    private fun toOverrideResponse(override: Override): OverrideResponse {
        val now = Instant.now()
        val remainingMinutes = Duration.between(now, override.expiresAt).toMinutes().coerceAtLeast(0)

        val zoneOverrides = override.zoneOverrides?.let { json ->
            try {
                val zones: List<Map<String, Any?>> = objectMapper.readValue(json)
                zones.map { z ->
                    ZoneOverrideResponse(
                        zoneId = (z["zoneId"] as Number).toLong(),
                        temp = (z["temp"] as? Number)?.toInt(),
                        enabled = z["enabled"] as? Boolean
                    )
                }
            } catch (e: Exception) {
                log.error("Error parsing zone overrides: {}", e.message)
                emptyList()
            }
        } ?: emptyList()

        return OverrideResponse(
            id = override.id,
            createdAt = override.createdAt.toString(),
            expiresAt = override.expiresAt.toString(),
            mode = override.mode,
            systemTemp = override.systemTemp,
            zoneOverrides = zoneOverrides,
            remainingMinutes = remainingMinutes
        )
    }

    /**
     * Clean up expired overrides.
     */
    @Transactional
    fun cleanupExpiredOverrides() {
        val expiredOverrides = overrideRepository.findAll().filter { it.isExpired() }
        if (expiredOverrides.isNotEmpty()) {
            log.info("Cleaning up {} expired override(s)", expiredOverrides.size)
            overrideRepository.deleteAll(expiredOverrides)
        }
    }
}
