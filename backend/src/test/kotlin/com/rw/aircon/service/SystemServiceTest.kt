package com.rw.aircon.service

import com.rw.aircon.client.MyAirClient
import com.rw.aircon.dto.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*

@ExtendWith(MockitoExtension::class)
class SystemServiceTest {

    @Mock
    private lateinit var myAirCacheService: MyAirCacheService

    @Mock
    private lateinit var myAirClient: MyAirClient

    @Mock
    private lateinit var zoneService: ZoneService

    private lateinit var systemService: SystemService

    @BeforeEach
    fun setUp() {
        systemService = SystemService(myAirCacheService, myAirClient, zoneService)
    }

    @Test
    fun `getSystemStatus returns status when data available`() {
        // Given
        whenever(myAirCacheService.getSystemData()).thenReturn(createMockResponse() to false)
        whenever(zoneService.getAllZones()).thenReturn(emptyList())

        // When
        val result = systemService.getSystemStatus()

        // Then
        assertNotNull(result)
        assertEquals("on", result!!.state)
        assertEquals("cool", result.mode)
        assertEquals("autoAA", result.fan)
        assertEquals(24, result.setTemp)
        assertEquals(3, result.myZone)
        assertEquals(18.4, result.outdoorTemp)
        assertTrue(result.isValidOutdoorTemp == true)
    }

    @Test
    fun `getSystemStatus returns null when no data available`() {
        // Given
        whenever(myAirCacheService.getSystemData()).thenReturn(null to true)

        // When
        val result = systemService.getSystemStatus()

        // Then
        assertNull(result)
    }

    @Test
    fun `setPower validates power state - invalid`() {
        // When/Then
        val exception = assertThrows<IllegalArgumentException> {
            systemService.setPower("invalid")
        }
        assertTrue(exception.message!!.contains("Invalid"))
    }

    @Test
    fun `setPower accepts valid state`() {
        // Given
        whenever(myAirClient.setSystemInfo(any())).thenReturn(true)

        // When
        val result = systemService.setPower("on")

        // Then
        assertTrue(result)
        verify(myAirClient).setSystemInfo(mapOf("state" to "on"))
    }

    @Test
    fun `setMode validates mode - invalid`() {
        // When/Then
        val exception = assertThrows<IllegalArgumentException> {
            systemService.setMode("invalid")
        }
        assertTrue(exception.message!!.contains("Invalid mode"))
    }

    @Test
    fun `setMode accepts valid modes`() {
        // Given
        whenever(myAirClient.setSystemInfo(any())).thenReturn(true)

        // When/Then - test all valid modes
        listOf("cool", "heat", "vent", "dry").forEach { mode ->
            val result = systemService.setMode(mode)
            assertTrue(result)
        }
    }

    @Test
    fun `setFanSpeed validates fan speed - invalid`() {
        // When/Then
        val exception = assertThrows<IllegalArgumentException> {
            systemService.setFanSpeed("invalid")
        }
        assertTrue(exception.message!!.contains("Invalid fan speed"))
    }

    @Test
    fun `setFanSpeed accepts valid fan speeds`() {
        // Given
        whenever(myAirClient.setSystemInfo(any())).thenReturn(true)

        // When/Then - test all valid fan speeds
        listOf("low", "medium", "high", "auto", "autoAA").forEach { fan ->
            val result = systemService.setFanSpeed(fan)
            assertTrue(result)
        }
    }

    @Test
    fun `setTemperature validates temperature range - too low`() {
        // When/Then
        val exception = assertThrows<IllegalArgumentException> {
            systemService.setTemperature(15)
        }
        assertEquals("Temperature must be between 16 and 32, got: 15", exception.message)
    }

    @Test
    fun `setTemperature validates temperature range - too high`() {
        // When/Then
        val exception = assertThrows<IllegalArgumentException> {
            systemService.setTemperature(33)
        }
        assertEquals("Temperature must be between 16 and 32, got: 33", exception.message)
    }

    @Test
    fun `setTemperature accepts boundary values`() {
        // Given
        whenever(myAirClient.setSystemInfo(any())).thenReturn(true)

        // When
        val resultMin = systemService.setTemperature(16)
        val resultMax = systemService.setTemperature(32)

        // Then
        assertTrue(resultMin)
        assertTrue(resultMax)
    }

    @Test
    fun `setMyZone validates zone number - negative`() {
        // When/Then
        val exception = assertThrows<IllegalArgumentException> {
            systemService.setMyZone(-1)
        }
        assertTrue(exception.message!!.contains("myZone must be between"))
    }

    @Test
    fun `setMyZone validates zone number - too high`() {
        // When/Then
        val exception = assertThrows<IllegalArgumentException> {
            systemService.setMyZone(11)
        }
        assertTrue(exception.message!!.contains("myZone must be between"))
    }

    @Test
    fun `setMyZone accepts valid zone numbers`() {
        // Given
        whenever(myAirClient.setSystemInfo(any())).thenReturn(true)

        // When - 0 disables myZone, 1-10 are valid zone numbers
        val result0 = systemService.setMyZone(0)
        val result3 = systemService.setMyZone(3)
        val result10 = systemService.setMyZone(10)

        // Then
        assertTrue(result0)
        assertTrue(result3)
        assertTrue(result10)
    }

    @Test
    fun `operations return false when API call fails`() {
        // Given
        whenever(myAirClient.setSystemInfo(any())).thenReturn(false)

        // When
        val result = systemService.setPower("on")

        // Then
        assertFalse(result)
    }

    private fun createMockResponse(): MyAirResponse {
        val zones = mapOf(
            "z01" to ZoneInfo(
                name = "Living",
                state = "close",
                setTemp = 19.0,
                measuredTemp = 25.0,
                type = 1,
                number = 1
            ),
            "z02" to ZoneInfo(
                name = "Guest",
                state = "open",
                setTemp = 26.0,
                measuredTemp = 24.6,
                type = 1,
                number = 2
            ),
            "z03" to ZoneInfo(
                name = "Upstairs",
                state = "open",
                setTemp = 22.0,
                measuredTemp = 21.9,
                type = 1,
                number = 3
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
            system = SystemInfo(
                suburbTemp = 18.4,
                isValidSuburbTemp = true
            )
        )
    }
}
