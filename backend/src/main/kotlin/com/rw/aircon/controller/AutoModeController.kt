package com.rw.aircon.controller

import com.rw.aircon.dto.*
import com.rw.aircon.service.AutoModeExecutionService
import com.rw.aircon.service.AutoModeService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for Auto Mode operations.
 *
 * Auto Mode automatically maintains zone temperatures within user-defined min/max ranges.
 * This controller provides endpoints for:
 * - Getting and updating Auto Mode configuration
 * - Activating and deactivating Auto Mode
 * - Getting the current execution status
 */
@RestController
@RequestMapping("/api/auto-mode")
class AutoModeController(
    private val autoModeService: AutoModeService,
    private val autoModeExecutionService: AutoModeExecutionService
) {
    private val log = LoggerFactory.getLogger(AutoModeController::class.java)

    /**
     * Get the current Auto Mode configuration including all zone settings.
     */
    @GetMapping
    fun getConfig(): ResponseEntity<AutoModeConfigResponse> {
        log.debug("GET /api/auto-mode - Getting Auto Mode configuration")
        val config = autoModeService.getConfig()
        return ResponseEntity.ok(config)
    }

    /**
     * Update the Auto Mode configuration.
     * Updates priority zone and per-zone temperature ranges.
     */
    @PutMapping
    fun updateConfig(@RequestBody request: AutoModeConfigRequest): ResponseEntity<AutoModeConfigResponse> {
        log.info("PUT /api/auto-mode - Updating Auto Mode configuration")
        return try {
            val config = autoModeService.updateConfig(request)
            ResponseEntity.ok(config)
        } catch (e: IllegalArgumentException) {
            log.warn("Invalid Auto Mode configuration: {}", e.message)
            throw e
        }
    }

    /**
     * Activate Auto Mode.
     * This switches the control mode to AUTO and begins automatic temperature management.
     */
    @PostMapping("/activate")
    fun activate(): ResponseEntity<AutoModeConfigResponse> {
        log.info("POST /api/auto-mode/activate - Activating Auto Mode")
        return try {
            val config = autoModeService.activate()
            ResponseEntity.ok(config)
        } catch (e: IllegalStateException) {
            log.warn("Cannot activate Auto Mode: {}", e.message)
            throw e
        }
    }

    /**
     * Deactivate Auto Mode.
     * This switches the control mode back to MANUAL.
     */
    @DeleteMapping("/activate")
    fun deactivate(): ResponseEntity<AutoModeConfigResponse> {
        log.info("DELETE /api/auto-mode/activate - Deactivating Auto Mode")
        val config = autoModeService.deactivate()
        return ResponseEntity.ok(config)
    }

    /**
     * Get the current Auto Mode execution status.
     * Shows what the system is doing (heating/cooling/off) and why.
     */
    @GetMapping("/status")
    fun getStatus(): ResponseEntity<AutoModeStatusResponse> {
        log.debug("GET /api/auto-mode/status - Getting Auto Mode status")
        val status = autoModeExecutionService.getStatus()
        return ResponseEntity.ok(status)
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

    /**
     * Exception handler for IllegalStateException.
     */
    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(e: IllegalStateException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.badRequest().body(
            ErrorResponse(error = "invalid_state", message = e.message ?: "Invalid state")
        )
    }
}
