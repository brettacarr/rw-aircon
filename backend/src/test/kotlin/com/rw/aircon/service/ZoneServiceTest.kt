package com.rw.aircon.service

import com.rw.aircon.client.MyAirClient
import com.rw.aircon.dto.*
import com.rw.aircon.model.Zone
import com.rw.aircon.repository.ZoneRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ZoneServiceTest {

    @Mock
    private lateinit var zoneRepository: ZoneRepository

    @Mock
    private lateinit var myAirCacheService: MyAirCacheService

    @Mock
    private lateinit var myAirClient: MyAirClient

    private lateinit var zoneService: ZoneService

    @BeforeEach
    fun setUp() {
        zoneService = ZoneService(zoneRepository, myAirCacheService, myAirClient)
    }

    @Test
    fun `getAllZones returns zones with combined data`() {
        // Given
        val dbZones = listOf(
            Zone(1L, "Living Room", "z01"),
            Zone(2L, "Guest Room", "z02"),
            Zone(3L, "Upstairs", "z03")
        )
        whenever(zoneRepository.findAll()).thenReturn(dbZones)
        whenever(myAirCacheService.getSystemData()).thenReturn(createMockResponse() to false)

        // When
        val result = zoneService.getAllZones()

        // Then
        assertEquals(3, result.size)

        val living = result.find { it.myAirZoneId == "z01" }
        assertNotNull(living)
        assertEquals("Living", living!!.name) // Uses MyAir name over DB name
        assertEquals("close", living.state)
        assertEquals(19, living.setTemp)
        assertEquals(25.0, living.measuredTemp)
        assertFalse(living.isMyZone) // myZone is 3, not 1

        val upstairs = result.find { it.myAirZoneId == "z03" }
        assertNotNull(upstairs)
        assertTrue(upstairs!!.isMyZone) // myZone is 3
    }

    @Test
    fun `setZoneTemperature validates temperature range - too low`() {
        // When/Then - validation happens before repository lookup
        val exception = assertThrows<IllegalArgumentException> {
            zoneService.setZoneTemperature(1L, 15)
        }
        assertEquals("Temperature must be between 16 and 32, got: 15", exception.message)
    }

    @Test
    fun `setZoneTemperature validates temperature range - too high`() {
        // When/Then - validation happens before repository lookup
        val exception = assertThrows<IllegalArgumentException> {
            zoneService.setZoneTemperature(1L, 33)
        }
        assertEquals("Temperature must be between 16 and 32, got: 33", exception.message)
    }

    @Test
    fun `setZoneTemperature accepts valid temperature`() {
        // Given
        val zone = Zone(1L, "Living", "z01")
        whenever(zoneRepository.findById(1L)).thenReturn(Optional.of(zone))
        whenever(myAirClient.setZone(eq("z01"), any())).thenReturn(true)

        // When
        val result = zoneService.setZoneTemperature(1L, 22)

        // Then
        assertTrue(result)
        verify(myAirClient).setZone("z01", mapOf("setTemp" to "22"))
    }

    @Test
    fun `setZoneTemperature throws when zone not found`() {
        // Given
        whenever(zoneRepository.findById(99L)).thenReturn(Optional.empty())

        // When/Then
        val exception = assertThrows<IllegalStateException> {
            zoneService.setZoneTemperature(99L, 22)
        }
        assertEquals("Zone with ID 99 not found", exception.message)
    }

    @Test
    fun `setZonePower validates state value`() {
        // When/Then - validation happens before repository lookup
        val exception = assertThrows<IllegalArgumentException> {
            zoneService.setZonePower(1L, "invalid")
        }
        assertTrue(exception.message!!.contains("Invalid state"))
    }

    @Test
    fun `setZonePower prevents closing myZone`() {
        // Given
        val zone = Zone(3L, "Upstairs", "z03")
        whenever(zoneRepository.findById(3L)).thenReturn(Optional.of(zone))
        whenever(myAirCacheService.getSystemData()).thenReturn(createMockResponse() to false)

        // When/Then - z03 is myZone (number=3, myZone=3)
        val exception = assertThrows<IllegalArgumentException> {
            zoneService.setZonePower(3L, "close")
        }
        assertTrue(exception.message!!.contains("controlling zone"))
    }

    @Test
    fun `setZonePower allows closing non-myZone`() {
        // Given
        val zone = Zone(1L, "Living", "z01")
        whenever(zoneRepository.findById(1L)).thenReturn(Optional.of(zone))
        whenever(myAirCacheService.getSystemData()).thenReturn(createMockResponse() to false)
        whenever(myAirClient.setZone(eq("z01"), any())).thenReturn(true)

        // When - z01 is not myZone
        val result = zoneService.setZonePower(1L, "close")

        // Then
        assertTrue(result)
        verify(myAirClient).setZone("z01", mapOf("state" to "close"))
    }

    @Test
    fun `setZonePower allows opening any zone`() {
        // Given
        val zone = Zone(3L, "Upstairs", "z03")
        whenever(zoneRepository.findById(3L)).thenReturn(Optional.of(zone))
        whenever(myAirClient.setZone(eq("z03"), any())).thenReturn(true)

        // When - opening is always allowed, even for myZone
        val result = zoneService.setZonePower(3L, "open")

        // Then
        assertTrue(result)
        verify(myAirClient).setZone("z03", mapOf("state" to "open"))
    }

    private fun createMockResponse(): MyAirResponse {
        val zones = mapOf(
            "z01" to ZoneInfo(
                name = "Living",
                state = "close",
                setTemp = 19.0,
                measuredTemp = 25.0,
                type = 1,
                number = 1,
                rssi = 47,
                error = 0,
                value = 100
            ),
            "z02" to ZoneInfo(
                name = "Guest",
                state = "open",
                setTemp = 26.0,
                measuredTemp = 24.6,
                type = 1,
                number = 2,
                rssi = 63,
                error = 0,
                value = 5
            ),
            "z03" to ZoneInfo(
                name = "Upstairs",
                state = "open",
                setTemp = 22.0,
                measuredTemp = 21.9,
                type = 1,
                number = 3,
                rssi = 56,
                error = 0,
                value = 100
            )
        )

        val info = AirconInfo(
            state = "on",
            mode = "cool",
            fan = "autoAA",
            setTemp = 24.0,
            myZone = 3,
            noOfZones = 3,
            filterCleanStatus = 0,
            airconErrorCode = ""
        )

        return MyAirResponse(
            aircons = AirconsWrapper(
                ac1 = Aircon(info = info, zones = zones)
            ),
            system = null
        )
    }
}
