package com.rw.aircon.service

import com.rw.aircon.client.MyAirClient
import com.rw.aircon.dto.*
import com.rw.aircon.model.AutoModeZone
import com.rw.aircon.model.ControlMode
import com.rw.aircon.model.Override
import com.rw.aircon.model.Zone
import com.rw.aircon.repository.AutoModeZoneRepository
import com.rw.aircon.repository.OverrideRepository
import com.rw.aircon.repository.ZoneRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.*
import org.mockito.quality.Strictness
import java.time.Instant
import java.time.temporal.ChronoUnit

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AutoModeExecutionServiceTest {

    @Mock
    private lateinit var autoModeZoneRepository: AutoModeZoneRepository

    @Mock
    private lateinit var zoneRepository: ZoneRepository

    @Mock
    private lateinit var overrideRepository: OverrideRepository

    @Mock
    private lateinit var myAirClient: MyAirClient

    @Mock
    private lateinit var myAirCacheService: MyAirCacheService

    @Mock
    private lateinit var controlModeService: ControlModeService

    private lateinit var autoModeExecutionService: AutoModeExecutionService

    private val testZones = listOf(
        Zone(id = 1, name = "Living", myAirZoneId = "z01"),
        Zone(id = 2, name = "Guest", myAirZoneId = "z02"),
        Zone(id = 3, name = "Upstairs", myAirZoneId = "z03")
    )

    @BeforeEach
    fun setUp() {
        autoModeExecutionService = AutoModeExecutionService(
            autoModeZoneRepository,
            zoneRepository,
            overrideRepository,
            myAirClient,
            myAirCacheService,
            controlModeService
        )

        // Default setup
        whenever(zoneRepository.findAll()).thenReturn(testZones)
        whenever(myAirClient.setSystemInfo(any())).thenReturn(true)
    }

    // ============ evaluateAndApply tests ============

    @Test
    fun `evaluateAndApply skips when not in AUTO mode`() {
        // Given
        whenever(controlModeService.getControlMode()).thenReturn(ControlMode.MANUAL)

        // When
        autoModeExecutionService.evaluateAndApply()

        // Then
        verify(myAirClient, never()).setSystemInfo(any())
        verify(autoModeZoneRepository, never()).findByEnabledTrue()
    }

    @Test
    fun `evaluateAndApply skips when override is active`() {
        // Given
        whenever(controlModeService.getControlMode()).thenReturn(ControlMode.AUTO)
        val override = Override(
            id = 1,
            createdAt = Instant.now(),
            expiresAt = Instant.now().plus(1, ChronoUnit.HOURS),
            mode = "cool"
        )
        whenever(overrideRepository.findActiveOverride(any())).thenReturn(override)

        // When
        autoModeExecutionService.evaluateAndApply()

        // Then
        verify(myAirClient, never()).setSystemInfo(any())
    }

    @Test
    fun `evaluateAndApply heats when zone is below minimum`() {
        // Given
        whenever(controlModeService.getControlMode()).thenReturn(ControlMode.AUTO)
        whenever(overrideRepository.findActiveOverride(any())).thenReturn(null)

        val zoneConfigs = listOf(
            AutoModeZone(id = 1, zoneId = 1, enabled = true, minTemp = 22.0, maxTemp = 26.0)
        )
        whenever(autoModeZoneRepository.findByEnabledTrue()).thenReturn(zoneConfigs)
        whenever(myAirCacheService.getSystemData()).thenReturn(
            createMockResponse(
                systemOn = true,
                mode = "heat",
                zones = mapOf("z01" to 20.0) // Below minimum of 22
            ) to false
        )

        // When
        autoModeExecutionService.evaluateAndApply()

        // Then
        verify(myAirClient).setSystemInfo(mapOf("state" to "on"))
        verify(myAirClient).setSystemInfo(mapOf("mode" to "heat"))
        verify(myAirClient).setSystemInfo(argThat { this["setTemp"] == "22" }) // minTemp + 0.5 = 22.5, rounded to 22
    }

    @Test
    fun `evaluateAndApply cools when zone is above maximum`() {
        // Given
        whenever(controlModeService.getControlMode()).thenReturn(ControlMode.AUTO)
        whenever(overrideRepository.findActiveOverride(any())).thenReturn(null)

        val zoneConfigs = listOf(
            AutoModeZone(id = 1, zoneId = 1, enabled = true, minTemp = 20.0, maxTemp = 24.0)
        )
        whenever(autoModeZoneRepository.findByEnabledTrue()).thenReturn(zoneConfigs)
        whenever(myAirCacheService.getSystemData()).thenReturn(
            createMockResponse(
                systemOn = true,
                mode = "cool",
                zones = mapOf("z01" to 26.0) // Above maximum of 24
            ) to false
        )

        // When
        autoModeExecutionService.evaluateAndApply()

        // Then
        verify(myAirClient).setSystemInfo(mapOf("state" to "on"))
        verify(myAirClient).setSystemInfo(mapOf("mode" to "cool"))
        verify(myAirClient).setSystemInfo(argThat { this["setTemp"] == "23" }) // maxTemp - 0.5 = 23.5, rounded to 23
    }

    @Test
    fun `evaluateAndApply turns off when all zones in range`() {
        // Given
        whenever(controlModeService.getControlMode()).thenReturn(ControlMode.AUTO)
        whenever(overrideRepository.findActiveOverride(any())).thenReturn(null)

        val zoneConfigs = listOf(
            AutoModeZone(id = 1, zoneId = 1, enabled = true, minTemp = 20.0, maxTemp = 24.0),
            AutoModeZone(id = 2, zoneId = 3, enabled = true, minTemp = 18.0, maxTemp = 26.0)
        )
        whenever(autoModeZoneRepository.findByEnabledTrue()).thenReturn(zoneConfigs)
        whenever(myAirCacheService.getSystemData()).thenReturn(
            createMockResponse(
                systemOn = true,
                mode = "cool",
                zones = mapOf(
                    "z01" to 22.0, // In range (20-24)
                    "z03" to 21.0  // In range (18-26)
                )
            ) to false
        )

        // When
        autoModeExecutionService.evaluateAndApply()

        // Then
        verify(myAirClient).setSystemInfo(mapOf("state" to "off"))
    }

    @Test
    fun `evaluateAndApply prioritizes myZone temperature`() {
        // Given
        whenever(controlModeService.getControlMode()).thenReturn(ControlMode.AUTO)
        whenever(overrideRepository.findActiveOverride(any())).thenReturn(null)

        val zoneConfigs = listOf(
            AutoModeZone(id = 1, zoneId = 1, enabled = true, minTemp = 20.0, maxTemp = 24.0),
            AutoModeZone(id = 2, zoneId = 3, enabled = true, minTemp = 18.0, maxTemp = 22.0)
        )
        whenever(autoModeZoneRepository.findByEnabledTrue()).thenReturn(zoneConfigs)
        whenever(myAirCacheService.getSystemData()).thenReturn(
            createMockResponse(
                systemOn = true,
                mode = "cool",
                myZone = 1, // z01 is myZone
                zones = mapOf(
                    "z01" to 22.0, // In range (myZone)
                    "z03" to 25.0  // Above maximum (but myZone takes priority)
                )
            ) to false
        )

        // When
        autoModeExecutionService.evaluateAndApply()

        // Then - Since myZone is in range, should check other zones and cool for z03
        verify(myAirClient).setSystemInfo(mapOf("mode" to "cool"))
    }

    @Test
    fun `evaluateAndApply handles empty enabled zones`() {
        // Given
        whenever(controlModeService.getControlMode()).thenReturn(ControlMode.AUTO)
        whenever(overrideRepository.findActiveOverride(any())).thenReturn(null)
        whenever(autoModeZoneRepository.findByEnabledTrue()).thenReturn(emptyList())

        // When
        autoModeExecutionService.evaluateAndApply()

        // Then - Should not attempt any system changes
        verify(myAirClient, never()).setSystemInfo(any())
    }

    @Test
    fun `evaluateAndApply handles missing system data gracefully`() {
        // Given
        whenever(controlModeService.getControlMode()).thenReturn(ControlMode.AUTO)
        whenever(overrideRepository.findActiveOverride(any())).thenReturn(null)
        whenever(autoModeZoneRepository.findByEnabledTrue()).thenReturn(
            listOf(AutoModeZone(id = 1, zoneId = 1, enabled = true, minTemp = 20.0, maxTemp = 24.0))
        )
        whenever(myAirCacheService.getSystemData()).thenReturn(null to false)

        // When
        autoModeExecutionService.evaluateAndApply()

        // Then - Should not throw, should not attempt system changes
        verify(myAirClient, never()).setSystemInfo(any())
    }

    // ============ getStatus tests ============

    @Test
    fun `getStatus returns inactive status when not in AUTO mode`() {
        // Given
        whenever(controlModeService.isAutoMode()).thenReturn(false)

        // When
        val result = autoModeExecutionService.getStatus()

        // Then
        assertFalse(result.active)
        assertEquals("Auto Mode is not active", result.reason)
    }

    @Test
    fun `getStatus returns execution state when in AUTO mode`() {
        // Given - Run an evaluation first
        whenever(controlModeService.getControlMode()).thenReturn(ControlMode.AUTO)
        whenever(controlModeService.isAutoMode()).thenReturn(true)
        whenever(overrideRepository.findActiveOverride(any())).thenReturn(null)

        val zoneConfigs = listOf(
            AutoModeZone(id = 1, zoneId = 1, enabled = true, minTemp = 20.0, maxTemp = 24.0)
        )
        whenever(autoModeZoneRepository.findByEnabledTrue()).thenReturn(zoneConfigs)
        whenever(myAirCacheService.getSystemData()).thenReturn(
            createMockResponse(
                systemOn = true,
                mode = "heat",
                zones = mapOf("z01" to 18.0) // Below minimum
            ) to false
        )

        autoModeExecutionService.evaluateAndApply()

        // When
        val result = autoModeExecutionService.getStatus()

        // Then
        assertTrue(result.active)
        assertEquals("heat", result.systemState)
        assertNotNull(result.triggeringZone)
        assertEquals("Living", result.triggeringZone?.zoneName)
        assertTrue(result.zoneStatuses.isNotEmpty())
    }

    // ============ Helper methods ============

    private fun createMockResponse(
        systemOn: Boolean = true,
        mode: String = "cool",
        myZone: Int = 0,
        zones: Map<String, Double> = mapOf("z01" to 22.0, "z02" to 23.0, "z03" to 21.0)
    ): MyAirResponse {
        val zoneInfoMap = mutableMapOf<String, ZoneInfo>()
        zones.forEach { (zoneId, temp) ->
            val number = zoneId.removePrefix("z").toInt()
            zoneInfoMap[zoneId] = ZoneInfo(
                name = when (zoneId) {
                    "z01" -> "Living"
                    "z02" -> "Guest"
                    "z03" -> "Upstairs"
                    else -> "Unknown"
                },
                state = "open",
                setTemp = 22.0,
                measuredTemp = temp,
                type = 1,
                number = number
            )
        }

        val info = AirconInfo(
            state = if (systemOn) "on" else "off",
            mode = mode,
            fan = "auto",
            setTemp = 22.0,
            myZone = myZone,
            noOfZones = zones.size
        )

        return MyAirResponse(
            aircons = AirconsWrapper(ac1 = Aircon(info = info, zones = zoneInfoMap)),
            system = SystemInfo(suburbTemp = 18.4, isValidSuburbTemp = true)
        )
    }
}
