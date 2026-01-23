package com.rw.aircon.controller

import com.rw.aircon.dto.*
import com.rw.aircon.service.SeasonService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for season and schedule management.
 *
 * Endpoints:
 * - GET  /api/seasons          - List all seasons
 * - GET  /api/seasons/{id}     - Get season with schedule
 * - POST /api/seasons          - Create new season
 * - PUT  /api/seasons/{id}     - Update season
 * - DELETE /api/seasons/{id}   - Delete season (cascades to schedules)
 *
 * - GET  /api/seasons/{id}/schedule - Get schedule entries for season
 * - PUT  /api/seasons/{id}/schedule - Replace entire schedule for season
 */
@RestController
@RequestMapping("/api/seasons")
class SeasonController(
    private val seasonService: SeasonService
) {
    private val log = LoggerFactory.getLogger(SeasonController::class.java)

    /**
     * GET /api/seasons - List all seasons
     */
    @GetMapping
    fun getAllSeasons(): ResponseEntity<List<SeasonResponse>> {
        log.debug("Getting all seasons")
        val seasons = seasonService.getAllSeasons()
        return ResponseEntity.ok(seasons)
    }

    /**
     * GET /api/seasons/{id} - Get a single season with its schedule
     */
    @GetMapping("/{id}")
    fun getSeason(@PathVariable id: Long): ResponseEntity<SeasonWithScheduleResponse> {
        log.debug("Getting season: {}", id)
        val season = seasonService.getSeasonWithSchedule(id)
        return if (season != null) {
            ResponseEntity.ok(season)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * POST /api/seasons - Create a new season
     */
    @PostMapping
    fun createSeason(@RequestBody request: SeasonCreateRequest): ResponseEntity<Any> {
        log.debug("Creating season: {}", request.name)
        return try {
            val season = seasonService.createSeason(request)
            ResponseEntity.status(HttpStatus.CREATED).body(season)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest()
                .body(ErrorResponse("invalid_request", e.message ?: "Invalid request"))
        }
    }

    /**
     * PUT /api/seasons/{id} - Update an existing season
     */
    @PutMapping("/{id}")
    fun updateSeason(
        @PathVariable id: Long,
        @RequestBody request: SeasonUpdateRequest
    ): ResponseEntity<Any> {
        log.debug("Updating season: {}", id)
        return try {
            val season = seasonService.updateSeason(id, request)
            if (season != null) {
                ResponseEntity.ok(season)
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest()
                .body(ErrorResponse("invalid_request", e.message ?: "Invalid request"))
        }
    }

    /**
     * DELETE /api/seasons/{id} - Delete a season
     */
    @DeleteMapping("/{id}")
    fun deleteSeason(@PathVariable id: Long): ResponseEntity<Void> {
        log.debug("Deleting season: {}", id)
        return if (seasonService.deleteSeason(id)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * GET /api/seasons/{id}/schedule - Get the schedule for a season
     */
    @GetMapping("/{id}/schedule")
    fun getSchedule(@PathVariable id: Long): ResponseEntity<List<ScheduleEntryResponse>> {
        log.debug("Getting schedule for season: {}", id)
        val schedule = seasonService.getSchedule(id)
        return if (schedule != null) {
            ResponseEntity.ok(schedule)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * PUT /api/seasons/{id}/schedule - Replace the entire schedule for a season
     */
    @PutMapping("/{id}/schedule")
    fun updateSchedule(
        @PathVariable id: Long,
        @RequestBody request: FullScheduleUpdateRequest
    ): ResponseEntity<Any> {
        log.debug("Updating schedule for season: {}", id)
        return try {
            val schedule = seasonService.updateSchedule(id, request)
            if (schedule != null) {
                ResponseEntity.ok(schedule)
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest()
                .body(ErrorResponse("invalid_request", e.message ?: "Invalid request"))
        }
    }
}
