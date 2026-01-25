package com.rw.aircon.service

import com.rw.aircon.dto.AutoModeConfigRequest
import com.rw.aircon.dto.AutoModeZoneRequest
import com.rw.aircon.model.AutoModeConfig
import com.rw.aircon.model.AutoModeZone
import com.rw.aircon.model.ControlMode
import com.rw.aircon.model.Zone
import com.rw.aircon.repository.AutoModeConfigRepository
import com.rw.aircon.repository.AutoModeZoneRepository
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

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AutoModeServiceTest {

    @Mock
    private lateinit var autoModeConfigRepository: AutoModeConfigRepository

    @Mock
    private lateinit var autoModeZoneRepository: AutoModeZoneRepository

    @Mock
    private lateinit var zoneRepository: ZoneRepository

    @Mock
    private lateinit var controlModeService: ControlModeService

    private lateinit var autoModeService: AutoModeService

    private val testZones = listOf(
        Zone(id = 1, name = "Living", myAirZoneId = "z01"),
        Zone(id = 2, name = "Guest", myAirZoneId = "z02"),
        Zone(id = 3, name = "Upstairs", myAirZoneId = "z03")
    )

    @BeforeEach
    fun setUp() {
        autoModeService = AutoModeService(
            autoModeConfigRepository,
            autoModeZoneRepository,
            zoneRepository,
            controlModeService
        )

        // Default setup
        whenever(zoneRepository.findAll()).thenReturn(testZones)
    }

    // ============ getConfig tests ============

    @Test
    fun `getConfig creates default config when none exists`() {
        // Given
        val defaultConfig = AutoModeConfig.createDefault().copy(id = 1)

        whenever(autoModeConfigRepository.findConfig())
            .thenReturn(null)
            .thenReturn(defaultConfig)
        whenever(autoModeConfigRepository.save(any<AutoModeConfig>())).thenReturn(defaultConfig)
        whenever(autoModeZoneRepository.findAll()).thenReturn(emptyList())

        // When
        val result = autoModeService.getConfig()

        // Then
        assertFalse(result.active)
        assertNull(result.priorityZoneId)
        assertEquals(3, result.zones.size)
        verify(autoModeConfigRepository).save(any<AutoModeConfig>())
    }

    @Test
    fun `getConfig returns existing config with zone settings`() {
        // Given
        val config = AutoModeConfig(
            id = 1,
            active = true,
            priorityZoneId = 1,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val zoneConfigs = listOf(
            AutoModeZone(id = 1, zoneId = 1, enabled = true, minTemp = 20.0, maxTemp = 24.0),
            AutoModeZone(id = 2, zoneId = 3, enabled = true, minTemp = 18.0, maxTemp = 22.0)
        )
        whenever(autoModeConfigRepository.findConfig()).thenReturn(config)
        whenever(autoModeZoneRepository.findAll()).thenReturn(zoneConfigs)

        // When
        val result = autoModeService.getConfig()

        // Then
        assertTrue(result.active)
        assertEquals(1L, result.priorityZoneId)
        assertEquals(3, result.zones.size)

        // Check Living zone
        val livingZone = result.zones.find { it.zoneId == 1L }
        assertNotNull(livingZone)
        assertTrue(livingZone!!.enabled)
        assertEquals(20.0, livingZone.minTemp)
        assertEquals(24.0, livingZone.maxTemp)
    }

    // ============ updateConfig tests ============

    @Test
    fun `updateConfig updates config and zone settings`() {
        // Given
        val existingConfig = AutoModeConfig(
            id = 1,
            active = false,
            priorityZoneId = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val updatedConfig = existingConfig.copy(priorityZoneId = 1)

        // First findConfig returns existing, then after save returns updated
        whenever(autoModeConfigRepository.findConfig())
            .thenReturn(existingConfig)
            .thenReturn(updatedConfig)
        whenever(autoModeConfigRepository.save(any<AutoModeConfig>())).thenReturn(updatedConfig)
        whenever(autoModeZoneRepository.findByZoneId(any())).thenReturn(null)
        whenever(autoModeZoneRepository.save(any<AutoModeZone>())).thenAnswer { it.arguments[0] }
        whenever(autoModeZoneRepository.findAll()).thenReturn(emptyList())

        val request = AutoModeConfigRequest(
            priorityZoneId = 1,
            zones = listOf(
                AutoModeZoneRequest(zoneId = 1, enabled = true, minTemp = 21.0, maxTemp = 25.0),
                AutoModeZoneRequest(zoneId = 3, enabled = true, minTemp = 19.0, maxTemp = 23.0)
            )
        )

        // When
        val result = autoModeService.updateConfig(request)

        // Then
        assertEquals(1L, result.priorityZoneId)
        verify(autoModeConfigRepository).save(argThat<AutoModeConfig> { priorityZoneId == 1L })
        verify(autoModeZoneRepository, times(2)).save(any<AutoModeZone>())
    }

    @Test
    fun `updateConfig rejects Guest zone as priority zone`() {
        // Given
        val config = AutoModeConfig.createDefault()
        whenever(autoModeConfigRepository.findConfig()).thenReturn(config)

        val request = AutoModeConfigRequest(
            priorityZoneId = 2, // Guest zone
            zones = listOf(
                AutoModeZoneRequest(zoneId = 1, enabled = true, minTemp = 20.0, maxTemp = 24.0)
            )
        )

        // When/Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            autoModeService.updateConfig(request)
        }
        assertTrue(exception.message!!.contains("Guest zone"))
    }

    @Test
    fun `updateConfig rejects config with only Guest zone enabled`() {
        // Given
        val config = AutoModeConfig.createDefault()
        whenever(autoModeConfigRepository.findConfig()).thenReturn(config)

        val request = AutoModeConfigRequest(
            priorityZoneId = null,
            zones = listOf(
                AutoModeZoneRequest(zoneId = 1, enabled = false, minTemp = 20.0, maxTemp = 24.0),
                AutoModeZoneRequest(zoneId = 2, enabled = true, minTemp = 20.0, maxTemp = 24.0), // Only Guest enabled
                AutoModeZoneRequest(zoneId = 3, enabled = false, minTemp = 20.0, maxTemp = 24.0)
            )
        )

        // When/Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            autoModeService.updateConfig(request)
        }
        assertTrue(exception.message!!.contains("non-Guest zone"))
    }

    @Test
    fun `updateConfig rejects invalid temperature range - min too low`() {
        // Given
        val config = AutoModeConfig.createDefault()
        whenever(autoModeConfigRepository.findConfig()).thenReturn(config)

        val request = AutoModeConfigRequest(
            priorityZoneId = null,
            zones = listOf(
                AutoModeZoneRequest(zoneId = 1, enabled = true, minTemp = 15.0, maxTemp = 24.0) // Min < 16
            )
        )

        // When/Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            autoModeService.updateConfig(request)
        }
        assertTrue(exception.message!!.contains("minimum temperature"))
    }

    @Test
    fun `updateConfig rejects invalid temperature range - max too high`() {
        // Given
        val config = AutoModeConfig.createDefault()
        whenever(autoModeConfigRepository.findConfig()).thenReturn(config)

        val request = AutoModeConfigRequest(
            priorityZoneId = null,
            zones = listOf(
                AutoModeZoneRequest(zoneId = 1, enabled = true, minTemp = 20.0, maxTemp = 35.0) // Max > 32
            )
        )

        // When/Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            autoModeService.updateConfig(request)
        }
        assertTrue(exception.message!!.contains("maximum temperature"))
    }

    @Test
    fun `updateConfig rejects invalid temperature range - gap too small`() {
        // Given
        val config = AutoModeConfig.createDefault()
        whenever(autoModeConfigRepository.findConfig()).thenReturn(config)

        val request = AutoModeConfigRequest(
            priorityZoneId = null,
            zones = listOf(
                AutoModeZoneRequest(zoneId = 1, enabled = true, minTemp = 22.0, maxTemp = 23.0) // Gap < 2
            )
        )

        // When/Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            autoModeService.updateConfig(request)
        }
        assertTrue(exception.message!!.contains("range must be at least"))
    }

    @Test
    fun `updateConfig rejects disabled priority zone`() {
        // Given
        val config = AutoModeConfig.createDefault()
        whenever(autoModeConfigRepository.findConfig()).thenReturn(config)

        val request = AutoModeConfigRequest(
            priorityZoneId = 1, // Priority zone is disabled
            zones = listOf(
                AutoModeZoneRequest(zoneId = 1, enabled = false, minTemp = 20.0, maxTemp = 24.0),
                AutoModeZoneRequest(zoneId = 3, enabled = true, minTemp = 20.0, maxTemp = 24.0)
            )
        )

        // When/Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            autoModeService.updateConfig(request)
        }
        assertTrue(exception.message!!.contains("Priority zone must be enabled"))
    }

    // ============ activate/deactivate tests ============

    @Test
    fun `activate enables Auto Mode and sets control mode`() {
        // Given
        val config = AutoModeConfig.createDefault().copy(id = 1)
        val activeConfig = config.copy(active = true)

        // First call returns inactive, after save returns active
        whenever(autoModeConfigRepository.findConfig())
            .thenReturn(config)
            .thenReturn(activeConfig)
        whenever(autoModeConfigRepository.save(any<AutoModeConfig>())).thenReturn(activeConfig)
        whenever(autoModeZoneRepository.countEnabledNonGuestZones()).thenReturn(2L)
        whenever(autoModeZoneRepository.findAll()).thenReturn(emptyList())

        // When
        val result = autoModeService.activate()

        // Then
        assertTrue(result.active)
        verify(controlModeService).setControlMode(ControlMode.AUTO)
        verify(autoModeConfigRepository).save(argThat<AutoModeConfig> { active })
    }

    @Test
    fun `activate fails when no non-Guest zones enabled`() {
        // Given
        whenever(autoModeZoneRepository.countEnabledNonGuestZones()).thenReturn(0L)

        // When/Then
        val exception = assertThrows(IllegalStateException::class.java) {
            autoModeService.activate()
        }
        assertTrue(exception.message!!.contains("non-Guest zone"))
    }

    @Test
    fun `deactivate disables Auto Mode and returns to manual`() {
        // Given
        val config = AutoModeConfig(
            id = 1,
            active = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val inactiveConfig = config.copy(active = false)

        // First call returns active, after save returns inactive
        whenever(autoModeConfigRepository.findConfig())
            .thenReturn(config)
            .thenReturn(inactiveConfig)
        whenever(autoModeConfigRepository.save(any<AutoModeConfig>())).thenReturn(inactiveConfig)
        whenever(autoModeZoneRepository.findAll()).thenReturn(emptyList())

        // When
        val result = autoModeService.deactivate()

        // Then
        assertFalse(result.active)
        verify(controlModeService).setControlMode(ControlMode.MANUAL)
        verify(autoModeConfigRepository).save(argThat<AutoModeConfig> { !active })
    }

    // ============ isActive tests ============

    @Test
    fun `isActive returns true when control mode is AUTO`() {
        // Given
        whenever(controlModeService.getControlMode()).thenReturn(ControlMode.AUTO)

        // When
        val result = autoModeService.isActive()

        // Then
        assertTrue(result)
    }

    @Test
    fun `isActive returns false when control mode is MANUAL`() {
        // Given
        whenever(controlModeService.getControlMode()).thenReturn(ControlMode.MANUAL)

        // When
        val result = autoModeService.isActive()

        // Then
        assertFalse(result)
    }

    // ============ getEnabledZones tests ============

    @Test
    fun `getEnabledZones returns only enabled zones`() {
        // Given
        val enabledZones = listOf(
            AutoModeZone(id = 1, zoneId = 1, enabled = true, minTemp = 20.0, maxTemp = 24.0),
            AutoModeZone(id = 2, zoneId = 3, enabled = true, minTemp = 18.0, maxTemp = 22.0)
        )
        whenever(autoModeZoneRepository.findByEnabledTrue()).thenReturn(enabledZones)

        // When
        val result = autoModeService.getEnabledZones()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.enabled })
    }
}
