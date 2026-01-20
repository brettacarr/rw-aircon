package com.rw.aircon.controller

import com.rw.aircon.dto.*
import com.rw.aircon.service.SystemService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/system")
class SystemController(
    private val systemService: SystemService
) {
    private val log = LoggerFactory.getLogger(SystemController::class.java)

    /**
     * GET /api/system/status - Get current system state
     */
    @GetMapping("/status")
    fun getStatus(): ResponseEntity<SystemStatusResponse> {
        log.debug("Getting system status")
        val status = systemService.getSystemStatus()
        return if (status != null) {
            ResponseEntity.ok(status)
        } else {
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
        }
    }

    /**
     * POST /api/system/power - Turn system on/off
     */
    @PostMapping("/power")
    fun setPower(@RequestBody request: SystemPowerRequest): ResponseEntity<Any> {
        log.debug("Setting power to: {}", request.state)
        return try {
            val success = systemService.setPower(request.state)
            if (success) {
                ResponseEntity.ok().build()
            } else {
                ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ErrorResponse("service_unavailable", "Failed to communicate with MyAir API"))
            }
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest()
                .body(ErrorResponse("invalid_request", e.message ?: "Invalid request"))
        }
    }

    /**
     * POST /api/system/mode - Set AC mode (cool, heat, vent, dry)
     */
    @PostMapping("/mode")
    fun setMode(@RequestBody request: SystemModeRequest): ResponseEntity<Any> {
        log.debug("Setting mode to: {}", request.mode)
        return try {
            val success = systemService.setMode(request.mode)
            if (success) {
                ResponseEntity.ok().build()
            } else {
                ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ErrorResponse("service_unavailable", "Failed to communicate with MyAir API"))
            }
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest()
                .body(ErrorResponse("invalid_request", e.message ?: "Invalid request"))
        }
    }

    /**
     * POST /api/system/fan - Set fan speed
     */
    @PostMapping("/fan")
    fun setFanSpeed(@RequestBody request: FanSpeedRequest): ResponseEntity<Any> {
        log.debug("Setting fan speed to: {}", request.fan)
        return try {
            val success = systemService.setFanSpeed(request.fan)
            if (success) {
                ResponseEntity.ok().build()
            } else {
                ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ErrorResponse("service_unavailable", "Failed to communicate with MyAir API"))
            }
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest()
                .body(ErrorResponse("invalid_request", e.message ?: "Invalid request"))
        }
    }

    /**
     * POST /api/system/temperature - Set system target temperature (when myZone=0)
     */
    @PostMapping("/temperature")
    fun setTemperature(@RequestBody request: SystemTemperatureRequest): ResponseEntity<Any> {
        log.debug("Setting temperature to: {}", request.temperature)
        return try {
            val success = systemService.setTemperature(request.temperature)
            if (success) {
                ResponseEntity.ok().build()
            } else {
                ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ErrorResponse("service_unavailable", "Failed to communicate with MyAir API"))
            }
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest()
                .body(ErrorResponse("invalid_request", e.message ?: "Invalid request"))
        }
    }

    /**
     * POST /api/system/myzone - Set controlling zone
     */
    @PostMapping("/myzone")
    fun setMyZone(@RequestBody request: MyZoneRequest): ResponseEntity<Any> {
        log.debug("Setting myZone to: {}", request.zone)
        return try {
            val success = systemService.setMyZone(request.zone)
            if (success) {
                ResponseEntity.ok().build()
            } else {
                ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ErrorResponse("service_unavailable", "Failed to communicate with MyAir API"))
            }
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest()
                .body(ErrorResponse("invalid_request", e.message ?: "Invalid request"))
        }
    }
}
