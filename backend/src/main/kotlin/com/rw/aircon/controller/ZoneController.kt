package com.rw.aircon.controller

import com.rw.aircon.dto.*
import com.rw.aircon.service.ZoneService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/zones")
class ZoneController(
    private val zoneService: ZoneService
) {
    private val log = LoggerFactory.getLogger(ZoneController::class.java)

    /**
     * GET /api/zones - List all zones with current state
     */
    @GetMapping
    fun getAllZones(): ResponseEntity<List<ZoneResponse>> {
        log.debug("Getting all zones")
        val zones = zoneService.getAllZones()
        return ResponseEntity.ok(zones)
    }

    /**
     * GET /api/zones/{id} - Get a single zone
     */
    @GetMapping("/{id}")
    fun getZone(@PathVariable id: Long): ResponseEntity<ZoneResponse> {
        log.debug("Getting zone: {}", id)
        val zone = zoneService.getZone(id)
        return if (zone != null) {
            ResponseEntity.ok(zone)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * POST /api/zones/{id}/target - Set zone target temperature
     */
    @PostMapping("/{id}/target")
    fun setTargetTemperature(
        @PathVariable id: Long,
        @RequestBody request: ZoneTargetRequest
    ): ResponseEntity<Any> {
        log.debug("Setting zone {} temperature to: {}", id, request.temperature)
        return try {
            val success = zoneService.setZoneTemperature(id, request.temperature)
            if (success) {
                ResponseEntity.ok().build()
            } else {
                ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ErrorResponse("service_unavailable", "Failed to communicate with MyAir API"))
            }
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest()
                .body(ErrorResponse("invalid_request", e.message ?: "Invalid request"))
        } catch (e: IllegalStateException) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * POST /api/zones/{id}/power - Set zone open/close state
     */
    @PostMapping("/{id}/power")
    fun setZonePower(
        @PathVariable id: Long,
        @RequestBody request: ZonePowerRequest
    ): ResponseEntity<Any> {
        log.debug("Setting zone {} state to: {}", id, request.state)
        return try {
            val success = zoneService.setZonePower(id, request.state)
            if (success) {
                ResponseEntity.ok().build()
            } else {
                ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ErrorResponse("service_unavailable", "Failed to communicate with MyAir API"))
            }
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest()
                .body(ErrorResponse("invalid_request", e.message ?: "Invalid request"))
        } catch (e: IllegalStateException) {
            ResponseEntity.notFound().build()
        }
    }
}
