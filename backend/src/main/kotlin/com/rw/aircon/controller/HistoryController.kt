package com.rw.aircon.controller

import com.rw.aircon.dto.ErrorResponse
import com.rw.aircon.dto.SystemHistoryResponse
import com.rw.aircon.dto.ZoneHistoryResponse
import com.rw.aircon.service.HistoryService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Controller for temperature history endpoints.
 */
@RestController
@RequestMapping("/api/history")
class HistoryController(
    private val historyService: HistoryService
) {
    private val log = LoggerFactory.getLogger(HistoryController::class.java)

    /**
     * Gets temperature history for a specific zone.
     *
     * @param id Zone database ID
     * @param from Start of time range (ISO timestamp). Defaults to 24 hours ago.
     * @param to End of time range (ISO timestamp). Defaults to now.
     */
    @GetMapping("/zones/{id}")
    fun getZoneHistory(
        @PathVariable id: Int,
        @RequestParam(required = false) from: String?,
        @RequestParam(required = false) to: String?
    ): ResponseEntity<*> {
        return try {
            val now = Instant.now()
            val toInstant = to?.let { parseInstant(it) } ?: now
            val fromInstant = from?.let { parseInstant(it) } ?: now.minus(24, ChronoUnit.HOURS)

            log.debug("Getting history for zone {} from {} to {}", id, fromInstant, toInstant)

            val history = historyService.getZoneHistory(id, fromInstant, toInstant)
            if (history != null) {
                ResponseEntity.ok(history)
            } else {
                ResponseEntity.notFound().build<ZoneHistoryResponse>()
            }
        } catch (e: Exception) {
            log.error("Failed to get zone history: {}", e.message)
            ResponseEntity.badRequest().body(
                ErrorResponse("invalid_request", e.message ?: "Invalid request")
            )
        }
    }

    /**
     * Gets system state history.
     *
     * @param from Start of time range (ISO timestamp). Defaults to 24 hours ago.
     * @param to End of time range (ISO timestamp). Defaults to now.
     */
    @GetMapping("/system")
    fun getSystemHistory(
        @RequestParam(required = false) from: String?,
        @RequestParam(required = false) to: String?
    ): ResponseEntity<*> {
        return try {
            val now = Instant.now()
            val toInstant = to?.let { parseInstant(it) } ?: now
            val fromInstant = from?.let { parseInstant(it) } ?: now.minus(24, ChronoUnit.HOURS)

            log.debug("Getting system history from {} to {}", fromInstant, toInstant)

            val history = historyService.getSystemHistory(fromInstant, toInstant)
            ResponseEntity.ok(history)
        } catch (e: Exception) {
            log.error("Failed to get system history: {}", e.message)
            ResponseEntity.badRequest().body(
                ErrorResponse("invalid_request", e.message ?: "Invalid request")
            )
        }
    }

    /**
     * Parses an ISO timestamp string to Instant.
     */
    private fun parseInstant(timestamp: String): Instant {
        return Instant.parse(timestamp)
    }
}
