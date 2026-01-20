package com.rw.aircon.controller

import com.rw.aircon.dto.HealthResponse
import com.rw.aircon.service.MyAirCacheService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class HealthController(
    private val myAirCacheService: MyAirCacheService
) {
    private val log = LoggerFactory.getLogger(HealthController::class.java)

    /**
     * GET /api/health - Health check endpoint
     */
    @GetMapping("/health")
    fun getHealth(): ResponseEntity<HealthResponse> {
        log.debug("Health check requested")

        val (data, isFromCache) = myAirCacheService.getSystemData()
        val lastPoll = myAirCacheService.getLastSuccessfulPoll()

        val connected = data != null && !isFromCache
        val status = when {
            connected -> "ok"
            data != null -> "degraded"  // Have cached data but can't reach API
            else -> "degraded"          // No data at all
        }

        return ResponseEntity.ok(
            HealthResponse(
                status = status,
                myairConnected = connected,
                lastSuccessfulPoll = lastPoll?.toString()
            )
        )
    }
}
