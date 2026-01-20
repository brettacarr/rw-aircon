package com.rw.aircon.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * DTOs for MyAir API responses.
 * Maps the JSON structure from GET /getSystemData endpoint.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
data class MyAirResponse(
    val aircons: AirconsWrapper? = null,
    val system: SystemInfo? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AirconsWrapper(
    val ac1: Aircon? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Aircon(
    val info: AirconInfo? = null,
    val zones: Map<String, ZoneInfo>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AirconInfo(
    val state: String? = null,          // "on" or "off"
    val mode: String? = null,           // "cool", "heat", "vent", "dry"
    val fan: String? = null,            // "low", "medium", "high", "auto", "autoAA"
    val setTemp: Double? = null,        // 16-32
    val myZone: Int? = null,            // 0=disabled, 1-10=zone controlling system temp
    val name: String? = null,
    val noOfZones: Int? = null,
    val filterCleanStatus: Int? = null, // 0=clean, >0=needs cleaning
    val airconErrorCode: String? = null,
    val countDownToOff: Int? = null,
    val countDownToOn: Int? = null,
    val freshAirStatus: String? = null,
    val aaAutoFanModeEnabled: Boolean? = null,
    val climateControlModeEnabled: Boolean? = null,
    val climateControlModeIsRunning: Boolean? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ZoneInfo(
    val name: String? = null,
    val state: String? = null,          // "open" or "close"
    val type: Int? = null,              // 0=percentage, >0=temperature control
    val value: Int? = null,             // 5-100 for percentage control
    val setTemp: Double? = null,        // 16-32 for temp control
    val measuredTemp: Double? = null,   // current sensor reading
    val number: Int? = null,
    val rssi: Int? = null,              // signal strength
    val error: Int? = null,             // 0=ok, >0=sensor error
    val maxDamper: Int? = null,
    val minDamper: Int? = null,
    val motion: Int? = null,
    val motionConfig: Int? = null,
    val tempSensorClash: Boolean? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SystemInfo(
    val name: String? = null,
    val sysType: String? = null,
    val tspIp: String? = null,
    val noOfAircons: Int? = null,
    val suburbTemp: Double? = null,           // outdoor temperature
    val isValidSuburbTemp: Boolean? = null,   // whether outdoor temp is valid
    val needsUpdate: Boolean? = null
)
