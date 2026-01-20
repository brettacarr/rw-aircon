package com.rw.aircon.service

import com.rw.aircon.client.MyAirClient
import com.rw.aircon.dto.ZoneResponse
import com.rw.aircon.model.Zone
import com.rw.aircon.repository.ZoneRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service for zone operations.
 * Handles mapping between database zone IDs and MyAir zone IDs.
 */
@Service
class ZoneService(
    private val zoneRepository: ZoneRepository,
    private val myAirCacheService: MyAirCacheService,
    private val myAirClient: MyAirClient
) {
    private val log = LoggerFactory.getLogger(ZoneService::class.java)

    companion object {
        const val MIN_TEMP = 16
        const val MAX_TEMP = 32
    }

    /**
     * Gets all zones with their current state from MyAir API.
     */
    fun getAllZones(): List<ZoneResponse> {
        val dbZones = zoneRepository.findAll()
        val (myAirData, _) = myAirCacheService.getSystemData()

        val myAirZones = myAirData?.aircons?.ac1?.zones ?: emptyMap()
        val myZone = myAirData?.aircons?.ac1?.info?.myZone ?: 0

        return dbZones.mapNotNull { dbZone ->
            val zoneInfo = myAirZones[dbZone.myAirZoneId]
            if (zoneInfo != null) {
                ZoneResponse(
                    id = dbZone.id,
                    name = zoneInfo.name ?: dbZone.name,
                    myAirZoneId = dbZone.myAirZoneId,
                    state = zoneInfo.state ?: "close",
                    type = zoneInfo.type ?: 1,
                    value = zoneInfo.value ?: 100,
                    setTemp = zoneInfo.setTemp?.toInt() ?: 24,
                    measuredTemp = zoneInfo.measuredTemp ?: 0.0,
                    rssi = zoneInfo.rssi,
                    error = zoneInfo.error,
                    isMyZone = (zoneInfo.number == myZone)
                )
            } else {
                log.warn("Zone {} not found in MyAir data", dbZone.myAirZoneId)
                null
            }
        }
    }

    /**
     * Gets a single zone by database ID.
     */
    fun getZone(id: Long): ZoneResponse? {
        if (!zoneRepository.existsById(id)) return null
        return getAllZones().find { it.id == id }
    }

    /**
     * Gets the database zone entity by ID.
     */
    fun getZoneEntity(id: Long): Zone? {
        return zoneRepository.findById(id).orElse(null)
    }

    /**
     * Sets the target temperature for a zone.
     * @throws IllegalArgumentException if temperature is out of range
     * @throws IllegalStateException if zone not found
     */
    fun setZoneTemperature(id: Long, temperature: Int): Boolean {
        validateTemperature(temperature)

        val zone = zoneRepository.findById(id).orElse(null)
            ?: throw IllegalStateException("Zone with ID $id not found")

        log.info("Setting zone {} ({}) temperature to {}", id, zone.myAirZoneId, temperature)
        return myAirClient.setZone(zone.myAirZoneId, mapOf("setTemp" to temperature))
    }

    /**
     * Sets the zone open/close state.
     * @throws IllegalArgumentException if state is invalid or trying to close myZone
     * @throws IllegalStateException if zone not found
     */
    fun setZonePower(id: Long, state: String): Boolean {
        val validStates = listOf("open", "close")
        if (state !in validStates) {
            throw IllegalArgumentException("Invalid state: $state. Must be one of: $validStates")
        }

        val zone = zoneRepository.findById(id).orElse(null)
            ?: throw IllegalStateException("Zone with ID $id not found")

        // Check if trying to close the myZone
        if (state == "close") {
            val (myAirData, _) = myAirCacheService.getSystemData()
            val myZone = myAirData?.aircons?.ac1?.info?.myZone ?: 0
            val zoneInfo = myAirData?.aircons?.ac1?.zones?.get(zone.myAirZoneId)

            if (zoneInfo?.number == myZone && myZone != 0) {
                throw IllegalArgumentException("Cannot close zone ${zone.name}: it is currently the controlling zone (myZone)")
            }
        }

        log.info("Setting zone {} ({}) state to {}", id, zone.myAirZoneId, state)
        return myAirClient.setZone(zone.myAirZoneId, mapOf("state" to state))
    }

    /**
     * Validates temperature is within allowed range.
     */
    private fun validateTemperature(temperature: Int) {
        if (temperature < MIN_TEMP || temperature > MAX_TEMP) {
            throw IllegalArgumentException(
                "Temperature must be between $MIN_TEMP and $MAX_TEMP, got: $temperature"
            )
        }
    }
}
