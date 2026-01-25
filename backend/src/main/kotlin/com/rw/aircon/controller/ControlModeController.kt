package com.rw.aircon.controller

import com.rw.aircon.dto.ControlModeRequest
import com.rw.aircon.dto.ControlModeResponse
import com.rw.aircon.dto.ErrorResponse
import com.rw.aircon.service.ControlModeService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for control mode operations.
 *
 * Control mode determines how the HVAC system is managed:
 * - manual: User directly controls all settings
 * - auto: System automatically adjusts based on min/max temperature ranges
 * - schedule: Settings follow the configured season/schedule
 */
@RestController
@RequestMapping("/api/control-mode")
class ControlModeController(
    private val controlModeService: ControlModeService
) {
    private val log = LoggerFactory.getLogger(ControlModeController::class.java)

    /**
     * Get the current control mode.
     */
    @GetMapping
    fun getControlMode(): ResponseEntity<ControlModeResponse> {
        log.debug("GET /api/control-mode - Getting control mode")
        val response = controlModeService.getControlModeResponse()
        return ResponseEntity.ok(response)
    }

    /**
     * Set the control mode.
     * Valid modes: "manual", "auto", "schedule"
     */
    @PutMapping
    fun setControlMode(@RequestBody request: ControlModeRequest): ResponseEntity<ControlModeResponse> {
        log.info("PUT /api/control-mode - Setting control mode to {}", request.mode)

        // Validate mode
        val validModes = setOf("manual", "auto", "schedule")
        if (request.mode.lowercase() !in validModes) {
            throw IllegalArgumentException("Invalid mode: ${request.mode}. Must be one of: $validModes")
        }

        val response = controlModeService.setControlModeFromString(request.mode)
        return ResponseEntity.ok(response)
    }

    /**
     * Exception handler for IllegalArgumentException.
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.badRequest().body(
            ErrorResponse(error = "validation_error", message = e.message ?: "Invalid request")
        )
    }
}
