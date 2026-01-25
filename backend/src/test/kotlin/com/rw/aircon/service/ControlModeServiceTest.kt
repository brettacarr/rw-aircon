package com.rw.aircon.service

import com.rw.aircon.model.ControlMode
import com.rw.aircon.model.SystemConfig
import com.rw.aircon.repository.SystemConfigRepository
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
class ControlModeServiceTest {

    @Mock
    private lateinit var systemConfigRepository: SystemConfigRepository

    private lateinit var controlModeService: ControlModeService

    @BeforeEach
    fun setUp() {
        controlModeService = ControlModeService(systemConfigRepository)
    }

    @Test
    fun `getControlMode returns MANUAL when no config exists`() {
        // Given
        val defaultConfig = SystemConfig(
            id = 1,
            controlMode = ControlMode.MANUAL,
            modeChangedAt = Instant.now()
        )
        // First call returns null, subsequent calls return the saved config
        whenever(systemConfigRepository.findConfig())
            .thenReturn(null)
            .thenReturn(defaultConfig)
        whenever(systemConfigRepository.save(any<SystemConfig>())).thenReturn(defaultConfig)

        // When
        val result = controlModeService.getControlMode()

        // Then
        assertEquals(ControlMode.MANUAL, result)
        verify(systemConfigRepository).save(any<SystemConfig>())
    }

    @Test
    fun `getControlMode returns stored control mode`() {
        // Given
        val config = SystemConfig(
            id = 1,
            controlMode = ControlMode.AUTO,
            modeChangedAt = Instant.now()
        )
        whenever(systemConfigRepository.findConfig()).thenReturn(config)

        // When
        val result = controlModeService.getControlMode()

        // Then
        assertEquals(ControlMode.AUTO, result)
    }

    @Test
    fun `setControlMode updates and saves new mode`() {
        // Given
        val existingConfig = SystemConfig(
            id = 1,
            controlMode = ControlMode.MANUAL,
            modeChangedAt = Instant.now()
        )
        val updatedConfig = existingConfig.copy(controlMode = ControlMode.AUTO)

        whenever(systemConfigRepository.findConfig()).thenReturn(existingConfig)
        whenever(systemConfigRepository.save(any<SystemConfig>())).thenReturn(updatedConfig)

        // When
        val result = controlModeService.setControlMode(ControlMode.AUTO)

        // Then
        assertEquals("auto", result.mode)
        verify(systemConfigRepository).save(argThat<SystemConfig> { controlMode == ControlMode.AUTO })
    }

    @Test
    fun `setControlMode does not save when mode unchanged`() {
        // Given
        val config = SystemConfig(
            id = 1,
            controlMode = ControlMode.SCHEDULE,
            modeChangedAt = Instant.now()
        )
        whenever(systemConfigRepository.findConfig()).thenReturn(config)

        // When
        val result = controlModeService.setControlMode(ControlMode.SCHEDULE)

        // Then
        assertEquals("schedule", result.mode)
        verify(systemConfigRepository, never()).save(any<SystemConfig>())
    }

    @Test
    fun `setControlModeFromString parses valid mode strings`() {
        // Given
        val manualConfig = SystemConfig(id = 1, controlMode = ControlMode.MANUAL, modeChangedAt = Instant.now())
        val autoConfig = SystemConfig(id = 1, controlMode = ControlMode.AUTO, modeChangedAt = Instant.now())
        val scheduleConfig = SystemConfig(id = 1, controlMode = ControlMode.SCHEDULE, modeChangedAt = Instant.now())

        whenever(systemConfigRepository.findConfig())
            .thenReturn(manualConfig)
            .thenReturn(autoConfig)
            .thenReturn(autoConfig)
        whenever(systemConfigRepository.save(any<SystemConfig>()))
            .thenReturn(autoConfig)
            .thenReturn(scheduleConfig)

        // When/Then - "auto"
        val autoResult = controlModeService.setControlModeFromString("auto")
        assertEquals("auto", autoResult.mode)

        // When/Then - "schedule"
        val scheduleResult = controlModeService.setControlModeFromString("schedule")
        assertEquals("schedule", scheduleResult.mode)
    }

    @Test
    fun `setControlModeFromString defaults to MANUAL for invalid strings`() {
        // Given
        val autoConfig = SystemConfig(id = 1, controlMode = ControlMode.AUTO, modeChangedAt = Instant.now())
        val manualConfig = SystemConfig(id = 1, controlMode = ControlMode.MANUAL, modeChangedAt = Instant.now())

        whenever(systemConfigRepository.findConfig()).thenReturn(autoConfig)
        whenever(systemConfigRepository.save(any<SystemConfig>())).thenReturn(manualConfig)

        // When
        val result = controlModeService.setControlModeFromString("invalid")

        // Then
        assertEquals("manual", result.mode)
    }

    @Test
    fun `isManualMode returns true only when in manual mode`() {
        // Given
        whenever(systemConfigRepository.findConfig()).thenReturn(
            SystemConfig(controlMode = ControlMode.MANUAL, modeChangedAt = Instant.now())
        )

        // When/Then
        assertTrue(controlModeService.isManualMode())
        assertFalse(controlModeService.isAutoMode())
        assertFalse(controlModeService.isScheduleMode())
    }

    @Test
    fun `isAutoMode returns true only when in auto mode`() {
        // Given
        whenever(systemConfigRepository.findConfig()).thenReturn(
            SystemConfig(controlMode = ControlMode.AUTO, modeChangedAt = Instant.now())
        )

        // When/Then
        assertFalse(controlModeService.isManualMode())
        assertTrue(controlModeService.isAutoMode())
        assertFalse(controlModeService.isScheduleMode())
    }

    @Test
    fun `isScheduleMode returns true only when in schedule mode`() {
        // Given
        whenever(systemConfigRepository.findConfig()).thenReturn(
            SystemConfig(controlMode = ControlMode.SCHEDULE, modeChangedAt = Instant.now())
        )

        // When/Then
        assertFalse(controlModeService.isManualMode())
        assertFalse(controlModeService.isAutoMode())
        assertTrue(controlModeService.isScheduleMode())
    }

    @Test
    fun `getControlModeResponse returns formatted response`() {
        // Given
        val changedAt = Instant.parse("2026-01-25T10:00:00Z")
        val config = SystemConfig(
            id = 1,
            controlMode = ControlMode.AUTO,
            modeChangedAt = changedAt
        )
        whenever(systemConfigRepository.findConfig()).thenReturn(config)

        // When
        val result = controlModeService.getControlModeResponse()

        // Then
        assertEquals("auto", result.mode)
        assertEquals(changedAt.toString(), result.changedAt)
    }
}
