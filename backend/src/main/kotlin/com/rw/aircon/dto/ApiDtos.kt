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

// ============ Override DTOs ============

/**
 * Response representing the current active override (if any).
 */
data class OverrideResponse(
    val id: Long,
    val createdAt: String,              // ISO timestamp
    val expiresAt: String,              // ISO timestamp
    val mode: String?,                   // null = no mode override
    val systemTemp: Int?,                // null = no system temp override
    val zoneOverrides: List<ZoneOverrideResponse>,
    val remainingMinutes: Long           // minutes until expiration
)

data class ZoneOverrideResponse(
    val zoneId: Long,
    val temp: Int?,                      // null = no temp override for this zone
    val enabled: Boolean?                // null = no state override for this zone
)

/**
 * Request to create a new override.
 * Duration can be specified as:
 * - "1h", "2h", "4h" - fixed duration
 * - "until_next" - until the next scheduled period begins
 */
data class OverrideCreateRequest(
    val duration: String,                // "1h", "2h", "4h", or "until_next"
    val mode: String? = null,            // optional mode override
    val systemTemp: Int? = null,         // optional system temp override
    val zoneOverrides: List<ZoneOverrideRequest>? = null  // optional zone-specific overrides
)

data class ZoneOverrideRequest(
    val zoneId: Long,
    val temp: Int? = null,               // optional temp override for this zone
    val enabled: Boolean? = null         // optional state override for this zone
)

// ============ Control Mode DTOs ============

/**
 * Response containing the current control mode.
 */
data class ControlModeResponse(
    val mode: String,                       // "manual", "auto", or "schedule"
    val changedAt: String                   // ISO timestamp when mode was last changed
)

/**
 * Request to change the control mode.
 */
data class ControlModeRequest(
    val mode: String                        // "manual", "auto", or "schedule"
)

// ============ Auto Mode DTOs ============

/**
 * Response containing the full Auto Mode configuration.
 */
data class AutoModeConfigResponse(
    val active: Boolean,
    val priorityZoneId: Long?,
    val updatedAt: String,                  // ISO timestamp
    val zones: List<AutoModeZoneResponse>
)

/**
 * Per-zone configuration for Auto Mode.
 */
data class AutoModeZoneResponse(
    val zoneId: Long,
    val zoneName: String,
    val enabled: Boolean,
    val minTemp: Double,
    val maxTemp: Double
)

/**
 * Request to update Auto Mode configuration.
 */
data class AutoModeConfigRequest(
    val priorityZoneId: Long? = null,
    val zones: List<AutoModeZoneRequest>
)

/**
 * Per-zone configuration update for Auto Mode.
 */
data class AutoModeZoneRequest(
    val zoneId: Long,
    val enabled: Boolean,
    val minTemp: Double,
    val maxTemp: Double
)

/**
 * Status of Auto Mode execution.
 * Explains what the system is currently doing and why.
 */
data class AutoModeStatusResponse(
    val active: Boolean,
    val systemState: String,                // "off", "heating", or "cooling"
    val targetTemp: Double?,                // current target temperature
    val reason: String,                     // human-readable explanation
    val triggeringZone: TriggeringZoneInfo?, // zone that triggered current action
    val zoneStatuses: List<ZoneStatusInfo>,
    val lastChecked: String                 // ISO timestamp of last evaluation
)

/**
 * Information about the zone that triggered the current action.
 */
data class TriggeringZoneInfo(
    val zoneId: Long,
    val zoneName: String,
    val currentTemp: Double,
    val deviation: Double                   // how far outside range (negative=below, positive=above)
)

/**
 * Status of a single zone in Auto Mode.
 */
data class ZoneStatusInfo(
    val zoneId: Long,
    val zoneName: String,
    val enabled: Boolean,
    val currentTemp: Double,
    val minTemp: Double,
    val maxTemp: Double,
    val status: String                      // "in_range", "below_min", or "above_max"
)

// ============ Error DTOs ============

data class ErrorResponse(
    val error: String,
    val message: String
)
