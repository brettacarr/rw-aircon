package com.rw.aircon.service

import com.rw.aircon.client.MyAirClient
import com.rw.aircon.dto.SystemStatusResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service for system-level operations.
 * Handles power, mode, fan, and temperature control.
 */
@Service
class SystemService(
    private val myAirCacheService: MyAirCacheService,
    private val myAirClient: MyAirClient,
    private val zoneService: ZoneService
) {
    private val log = LoggerFactory.getLogger(SystemService::class.java)

    companion object {
        const val MIN_TEMP = 16
        const val MAX_TEMP = 32
        val VALID_MODES = listOf("cool", "heat", "vent", "dry")
        val VALID_FAN_SPEEDS = listOf("low", "medium", "high", "auto", "autoAA")
        val VALID_POWER_STATES = listOf("on", "off")
    }

    /**
     * Gets the current system status including all zones.
     */
    fun getSystemStatus(): SystemStatusResponse? {
        val (myAirData, _) = myAirCacheService.getSystemData()

        if (myAirData == null) {
            log.error("Failed to get system data from MyAir API")
            return null
        }

        val info = myAirData.aircons?.ac1?.info
        val system = myAirData.system
        val zones = zoneService.getAllZones()

        return SystemStatusResponse(
            state = info?.state ?: "off",
            mode = info?.mode ?: "cool",
            fan = info?.fan ?: "auto",
            setTemp = info?.setTemp?.toInt() ?: 24,
            myZone = info?.myZone ?: 0,
            outdoorTemp = system?.suburbTemp,
            isValidOutdoorTemp = system?.isValidSuburbTemp ?: false,
            filterCleanStatus = info?.filterCleanStatus ?: 0,
            airconErrorCode = info?.airconErrorCode,
            zones = zones
        )
    }

    /**
     * Sets the system power state.
     * @throws IllegalArgumentException if state is invalid
     */
    fun setPower(state: String): Boolean {
        if (state !in VALID_POWER_STATES) {
            throw IllegalArgumentException("Invalid power state: $state. Must be one of: $VALID_POWER_STATES")
        }
        log.info("Setting system power to: {}", state)
        return myAirClient.setSystemInfo(mapOf("state" to state))
    }

    /**
     * Sets the AC mode.
     * @throws IllegalArgumentException if mode is invalid
     */
    fun setMode(mode: String): Boolean {
        if (mode !in VALID_MODES) {
            throw IllegalArgumentException("Invalid mode: $mode. Must be one of: $VALID_MODES")
        }
        log.info("Setting system mode to: {}", mode)
        return myAirClient.setSystemInfo(mapOf("mode" to mode))
    }

    /**
     * Sets the fan speed.
     * @throws IllegalArgumentException if fan speed is invalid
     */
    fun setFanSpeed(fan: String): Boolean {
        if (fan !in VALID_FAN_SPEEDS) {
            throw IllegalArgumentException("Invalid fan speed: $fan. Must be one of: $VALID_FAN_SPEEDS")
        }
        log.info("Setting fan speed to: {}", fan)
        return myAirClient.setSystemInfo(mapOf("fan" to fan))
    }

    /**
     * Sets the system target temperature (used when myZone=0).
     * @throws IllegalArgumentException if temperature is out of range
     */
    fun setTemperature(temperature: Int): Boolean {
        if (temperature < MIN_TEMP || temperature > MAX_TEMP) {
            throw IllegalArgumentException(
                "Temperature must be between $MIN_TEMP and $MAX_TEMP, got: $temperature"
            )
        }
        log.info("Setting system temperature to: {}", temperature)
        return myAirClient.setSystemInfo(mapOf("setTemp" to temperature))
    }

    /**
     * Sets the controlling zone (myZone).
     * @throws IllegalArgumentException if zone number is out of range
     */
    fun setMyZone(zone: Int): Boolean {
        if (zone < 0 || zone > 10) {
            throw IllegalArgumentException("myZone must be between 0 and 10, got: $zone")
        }
        log.info("Setting myZone to: {}", zone)
        return myAirClient.setSystemInfo(mapOf("myZone" to zone))
    }
}
