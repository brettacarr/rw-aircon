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

// ============ History DTOs ============

data class TemperatureLogResponse(
    val timestamp: String,              // ISO timestamp
    val currentTemp: Double,
    val targetTemp: Double,
    val zoneEnabled: Boolean
)

data class SystemLogResponse(
    val timestamp: String,              // ISO timestamp
    val mode: String,
    val outdoorTemp: Double?,
    val systemOn: Boolean
)

data class ZoneHistoryResponse(
    val zoneId: Int,
    val zoneName: String,
    val from: String,                   // ISO timestamp
    val to: String,                     // ISO timestamp
    val aggregated: Boolean,            // true if hourly averages
    val data: List<TemperatureLogResponse>
)

data class SystemHistoryResponse(
    val from: String,                   // ISO timestamp
    val to: String,                     // ISO timestamp
    val data: List<SystemLogResponse>
)

// ============ Season & Schedule DTOs ============

data class SeasonResponse(
    val id: Long,
    val name: String,
    val startMonth: Int,
    val startDay: Int,
    val endMonth: Int,
    val endDay: Int,
    val priority: Int,
    val active: Boolean
)

data class SeasonCreateRequest(
    val name: String,
    val startMonth: Int,
    val startDay: Int,
    val endMonth: Int,
    val endDay: Int,
    val priority: Int = 0,
    val active: Boolean = true
)

data class SeasonUpdateRequest(
    val name: String? = null,
    val startMonth: Int? = null,
    val startDay: Int? = null,
    val endMonth: Int? = null,
    val endDay: Int? = null,
    val priority: Int? = null,
    val active: Boolean? = null
)

data class ScheduleEntryResponse(
    val id: Long,
    val seasonId: Long,
    val dayOfWeek: Int,                     // 1=Monday, 7=Sunday
    val startTime: String,                   // HH:MM
    val endTime: String,                     // HH:MM
    val mode: String,                        // cool, heat, vent, dry
    val zoneSettings: List<ZoneScheduleResponse>
)

data class ZoneScheduleResponse(
    val id: Long,
    val zoneId: Long,
    val zoneName: String,
    val targetTemp: Int,
    val enabled: Boolean
)

data class ScheduleEntryRequest(
    val dayOfWeek: Int,                     // 1=Monday, 7=Sunday
    val startTime: String,                   // HH:MM
    val endTime: String,                     // HH:MM
    val mode: String,                        // cool, heat, vent, dry
    val zoneSettings: List<ZoneScheduleRequest>
)

data class ZoneScheduleRequest(
    val zoneId: Long,
    val targetTemp: Int,                    // 16-32
    val enabled: Boolean = true
)

data class SeasonWithScheduleResponse(
    val season: SeasonResponse,
    val schedule: List<ScheduleEntryResponse>
)

data class FullScheduleUpdateRequest(
    val entries: List<ScheduleEntryRequest>
)

// ============ Error DTOs ============

data class ErrorResponse(
    val error: String,
    val message: String
)
