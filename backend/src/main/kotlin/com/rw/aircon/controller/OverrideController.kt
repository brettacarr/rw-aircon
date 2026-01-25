package com.rw.aircon.controller

import com.rw.aircon.dto.*
import com.rw.aircon.service.OverrideService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for manual override management.
 *
 * Overrides allow users to temporarily override scheduled settings for a specified duration.
 * Only one override can be active at a time.
 *
 * Endpoints:
 * - GET    /api/override - Get current active override (if any)
 * - POST   /api/override - Create new override (replaces any existing)
 * - DELETE /api/override - Cancel current override
 */
@RestController
@RequestMapping("/api/override")
class OverrideController(
    private val overrideService: OverrideService
) {
    private val log = LoggerFactory.getLogger(OverrideController::class.java)

    /**
     * GET /api/override - Get the current active override
     *
     * Returns 200 with override details if one is active, or 204 No Content if none.
     */
    @GetMapping
    fun getOverride(): ResponseEntity<OverrideResponse> {
        log.debug("Getting active override")
        val override = overrideService.getActiveOverride()
        return if (override != null) {
            ResponseEntity.ok(override)
        } else {
            ResponseEntity.noContent().build()
        }
    }

    /**
     * POST /api/override - Create a new override
     *
     * Duration options:
     * - "1h" - 1 hour
     * - "2h" - 2 hours
     * - "4h" - 4 hours
     * - "until_next" - Until the next scheduled period begins
     *
     * Any existing active override will be replaced.
     */
    @PostMapping
    fun createOverride(@RequestBody request: OverrideCreateRequest): ResponseEntity<Any> {
        log.info("Creating override with duration: {}", request.duration)
        return try {
            val override = overrideService.createOverride(request)
            ResponseEntity.status(HttpStatus.CREATED).body(override)
        } catch (e: IllegalArgumentException) {
            log.warn("Invalid override request: {}", e.message)
            ResponseEntity.badRequest()
                .body(ErrorResponse("invalid_request", e.message ?: "Invalid request"))
        }
    }

    /**
     * DELETE /api/override - Cancel the current active override
     *
     * Returns 204 No Content on success, or 404 if no override was active.
     */
    @DeleteMapping
    fun cancelOverride(): ResponseEntity<Void> {
        log.info("Cancelling active override")
        return if (overrideService.cancelOverride()) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
