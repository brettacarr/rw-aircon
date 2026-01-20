package com.rw.aircon.dto

/**
 * DTOs for REST API requests and responses.
 * These are used by our controllers to communicate with the frontend.
 */

// ============ System DTOs ============

data class SystemStatusResponse(
    val state: String,                      // "on" or "off"
    val mode: String,                       // "cool", "heat", "vent", "dry"
    val fan: String,                        // "low", "medium", "high", "auto", "autoAA"
    val setTemp: Int,                       // 16-32
    val myZone: Int,                        // 0=disabled, 1-10=zone number
    val outdoorTemp: Double?,               // null if not available
    val isValidOutdoorTemp: Boolean,
    val filterCleanStatus: Int,             // 0=clean, >0=needs cleaning
    val airconErrorCode: String?,
    val zones: List<ZoneResponse>
)

data class SystemPowerRequest(
    val state: String                       // "on" or "off"
)

data class SystemModeRequest(
    val mode: String                        // "cool", "heat", "vent", "dry"
)

data class FanSpeedRequest(
    val fan: String                         // "low", "medium", "high", "auto", "autoAA"
)

data class SystemTemperatureRequest(
    val temperature: Int                    // 16-32
)

data class MyZoneRequest(
    val zone: Int                           // 0-10
)

// ============ Zone DTOs ============

data class ZoneResponse(
    val id: Long,                           // database ID
    val name: String,                       // zone name from MyAir
    val myAirZoneId: String,                // e.g., "z01"
    val state: String,                      // "open" or "close"
    val type: Int,                          // 0=percentage, >0=temperature
    val value: Int,                         // percentage value (for type=0)
    val setTemp: Int,                       // target temperature
    val measuredTemp: Double,               // current temperature from sensor
    val rssi: Int?,                         // signal strength
    val error: Int?,                        // 0=ok, >0=error
    val isMyZone: Boolean                   // true if this zone controls system temp
)

data class ZoneTargetRequest(
    val temperature: Int                    // 16-32
)

data class ZonePowerRequest(
    val state: String                       // "open" or "close"
)

// ============ Health DTOs ============

data class HealthResponse(
    val status: String,                     // "ok" or "degraded"
    val myairConnected: Boolean,
    val lastSuccessfulPoll: String?         // ISO timestamp or null
)

// ============ Error DTOs ============

data class ErrorResponse(
    val error: String,
    val message: String
)
