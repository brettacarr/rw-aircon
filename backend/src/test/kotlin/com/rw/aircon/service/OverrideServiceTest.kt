package com.rw.aircon.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rw.aircon.client.MyAirClient
import com.rw.aircon.dto.OverrideCreateRequest
import com.rw.aircon.dto.ZoneOverrideRequest
import com.rw.aircon.model.Override
import com.rw.aircon.model.ScheduleEntry
import com.rw.aircon.model.Season
import com.rw.aircon.model.Zone
import com.rw.aircon.repository.OverrideRepository
import com.rw.aircon.repository.ScheduleEntryRepository
import com.rw.aircon.repository.SeasonRepository
import com.rw.aircon.repository.ZoneRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.Instant
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@ExtendWith(MockitoExtension::class)
class OverrideServiceTest {

    @Mock
    private lateinit var overrideRepository: OverrideRepository

    @Mock
    private lateinit var scheduleEntryRepository: ScheduleEntryRepository

    @Mock
    private lateinit var seasonRepository: SeasonRepository

    @Mock
    private lateinit var zoneRepository: ZoneRepository

    @Mock
    private lateinit var myAirClient: MyAirClient

    @Mock
    private lateinit var myAirCacheService: MyAirCacheService

    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    private lateinit var overrideService: OverrideService

    @BeforeEach
    fun setUp() {
        overrideService = OverrideService(
            overrideRepository,
            scheduleEntryRepository,
            seasonRepository,
            zoneRepository,
            myAirClient,
            myAirCacheService,
            objectMapper
        )
    }

    @Test
    fun `getActiveOverride returns null when no active override`() {
        // Given
        whenever(overrideRepository.findActiveOverride(any())).thenReturn(null)

        // When
        val result = overrideService.getActiveOverride()

        // Then
        assertNull(result)
    }

    @Test
    fun `getActiveOverride returns override when active`() {
        // Given
        val override = Override(
            id = 1,
            createdAt = Instant.now(),
            expiresAt = Instant.now().plus(1, ChronoUnit.HOURS),
            mode = "cool",
            systemTemp = 22,
            zoneOverrides = null
        )
        whenever(overrideRepository.findActiveOverride(any())).thenReturn(override)

        // When
        val result = overrideService.getActiveOverride()

        // Then
        assertNotNull(result)
        assertEquals(1L, result!!.id)
        assertEquals("cool", result.mode)
        assertEquals(22, result.systemTemp)
        assertTrue(result.remainingMinutes > 0)
    }

    @Test
    fun `hasActiveOverride returns true when override exists`() {
        // Given
        val override = Override(
            id = 1,
            createdAt = Instant.now(),
            expiresAt = Instant.now().plus(1, ChronoUnit.HOURS)
        )
        whenever(overrideRepository.findActiveOverride(any())).thenReturn(override)

        // When
        val result = overrideService.hasActiveOverride()

        // Then
        assertTrue(result)
    }

    @Test
    fun `hasActiveOverride returns false when no override exists`() {
        // Given
        whenever(overrideRepository.findActiveOverride(any())).thenReturn(null)

        // When
        val result = overrideService.hasActiveOverride()

        // Then
        assertFalse(result)
    }

    @Test
    fun `createOverride validates invalid duration`() {
        // Given
        val request = OverrideCreateRequest(duration = "invalid")

        // When/Then
        val exception = assertThrows<IllegalArgumentException> {
            overrideService.createOverride(request)
        }
        assertTrue(exception.message!!.contains("Invalid duration"))
    }

    @Test
    fun `createOverride validates invalid mode`() {
        // Given
        val request = OverrideCreateRequest(duration = "1h", mode = "invalid")

        // When/Then
        val exception = assertThrows<IllegalArgumentException> {
            overrideService.createOverride(request)
        }
        assertTrue(exception.message!!.contains("Invalid mode"))
    }

    @Test
    fun `createOverride validates system temperature too low`() {
        // Given
        val request = OverrideCreateRequest(duration = "1h", systemTemp = 10)

        // When/Then
        val exception = assertThrows<IllegalArgumentException> {
            overrideService.createOverride(request)
        }
        assertTrue(exception.message!!.contains("between 16 and 32"))
    }

    @Test
    fun `createOverride validates system temperature too high`() {
        // Given
        val request = OverrideCreateRequest(duration = "1h", systemTemp = 40)

        // When/Then
        val exception = assertThrows<IllegalArgumentException> {
            overrideService.createOverride(request)
        }
        assertTrue(exception.message!!.contains("between 16 and 32"))
    }

    @Test
    fun `createOverride validates zone temperature`() {
        // Given
        val request = OverrideCreateRequest(
            duration = "1h",
            zoneOverrides = listOf(ZoneOverrideRequest(zoneId = 1, temp = 40))
        )

        // When/Then
        val exception = assertThrows<IllegalArgumentException> {
            overrideService.createOverride(request)
        }
        assertTrue(exception.message!!.contains("between 16 and 32"))
    }

    @Test
    fun `createOverride with 1h duration creates override expiring in 1 hour`() {
        // Given
        val request = OverrideCreateRequest(duration = "1h")
        whenever(overrideRepository.findAllActive(any())).thenReturn(emptyList())
        whenever(overrideRepository.save(any<Override>())).thenAnswer { invocation ->
            val override = invocation.getArgument<Override>(0)
            override.copy(id = 1)
        }

        // When
        val result = overrideService.createOverride(request)

        // Then
        assertNotNull(result)
        assertTrue(result.remainingMinutes in 55..60) // Allow for test execution time
        verify(overrideRepository).save(argThat<Override> { override ->
            val now = Instant.now()
            override.expiresAt.isAfter(now.plus(55, ChronoUnit.MINUTES)) &&
                override.expiresAt.isBefore(now.plus(65, ChronoUnit.MINUTES))
        })
    }

    @Test
    fun `createOverride with 2h duration creates override expiring in 2 hours`() {
        // Given
        val request = OverrideCreateRequest(duration = "2h")
        whenever(overrideRepository.findAllActive(any())).thenReturn(emptyList())
        whenever(overrideRepository.save(any<Override>())).thenAnswer { invocation ->
            val override = invocation.getArgument<Override>(0)
            override.copy(id = 1)
        }

        // When
        val result = overrideService.createOverride(request)

        // Then
        assertTrue(result.remainingMinutes in 115..120)
    }

    @Test
    fun `createOverride with 4h duration creates override expiring in 4 hours`() {
        // Given
        val request = OverrideCreateRequest(duration = "4h")
        whenever(overrideRepository.findAllActive(any())).thenReturn(emptyList())
        whenever(overrideRepository.save(any<Override>())).thenAnswer { invocation ->
            val override = invocation.getArgument<Override>(0)
            override.copy(id = 1)
        }

        // When
        val result = overrideService.createOverride(request)

        // Then
        assertTrue(result.remainingMinutes in 235..240)
    }

    @Test
    fun `createOverride deletes existing active overrides`() {
        // Given
        val existingOverride = Override(
            id = 1,
            createdAt = Instant.now().minus(30, ChronoUnit.MINUTES),
            expiresAt = Instant.now().plus(30, ChronoUnit.MINUTES)
        )
        val request = OverrideCreateRequest(duration = "1h")
        whenever(overrideRepository.findAllActive(any())).thenReturn(listOf(existingOverride))
        whenever(overrideRepository.save(any<Override>())).thenAnswer { invocation ->
            val override = invocation.getArgument<Override>(0)
            override.copy(id = 2)
        }

        // When
        overrideService.createOverride(request)

        // Then
        verify(overrideRepository).deleteAll(listOf(existingOverride))
    }

    @Test
    fun `createOverride with mode applies mode setting`() {
        // Given
        val request = OverrideCreateRequest(duration = "1h", mode = "cool")
        whenever(overrideRepository.findAllActive(any())).thenReturn(emptyList())
        whenever(overrideRepository.save(any<Override>())).thenAnswer { invocation ->
            val override = invocation.getArgument<Override>(0)
            override.copy(id = 1)
        }
        whenever(myAirClient.setSystemInfo(any())).thenReturn(true)

        // When
        overrideService.createOverride(request)

        // Then
        verify(myAirClient).setSystemInfo(argThat<Map<String, String>> { this["mode"] == "cool" })
    }

    @Test
    fun `createOverride with systemTemp applies temperature setting`() {
        // Given
        val request = OverrideCreateRequest(duration = "1h", systemTemp = 22)
        whenever(overrideRepository.findAllActive(any())).thenReturn(emptyList())
        whenever(overrideRepository.save(any<Override>())).thenAnswer { invocation ->
            val override = invocation.getArgument<Override>(0)
            override.copy(id = 1)
        }
        whenever(myAirClient.setSystemInfo(any())).thenReturn(true)

        // When
        overrideService.createOverride(request)

        // Then
        verify(myAirClient).setSystemInfo(argThat<Map<String, String>> { this["setTemp"] == "22" })
    }

    @Test
    fun `createOverride with zoneOverrides applies zone settings`() {
        // Given
        val zones = listOf(Zone(id = 1, name = "Living", myAirZoneId = "z01"))
        val request = OverrideCreateRequest(
            duration = "1h",
            zoneOverrides = listOf(ZoneOverrideRequest(zoneId = 1, temp = 24, enabled = true))
        )
        whenever(overrideRepository.findAllActive(any())).thenReturn(emptyList())
        whenever(overrideRepository.save(any<Override>())).thenAnswer { invocation ->
            val override = invocation.getArgument<Override>(0)
            override.copy(id = 1)
        }
        whenever(zoneRepository.findAll()).thenReturn(zones)
        whenever(myAirClient.setZone(any(), any())).thenReturn(true)

        // When
        overrideService.createOverride(request)

        // Then
        verify(myAirClient).setZone(eq("z01"), argThat<Map<String, String>> { this["setTemp"] == "24" })
        verify(myAirClient).setZone(eq("z01"), argThat<Map<String, String>> { this["state"] == "open" })
    }

    @Test
    fun `cancelOverride returns false when no active override`() {
        // Given
        whenever(overrideRepository.findActiveOverride(any())).thenReturn(null)

        // When
        val result = overrideService.cancelOverride()

        // Then
        assertFalse(result)
        verify(overrideRepository, never()).delete(any())
    }

    @Test
    fun `cancelOverride deletes active override and returns true`() {
        // Given
        val override = Override(
            id = 1,
            createdAt = Instant.now(),
            expiresAt = Instant.now().plus(1, ChronoUnit.HOURS)
        )
        whenever(overrideRepository.findActiveOverride(any())).thenReturn(override)

        // When
        val result = overrideService.cancelOverride()

        // Then
        assertTrue(result)
        verify(overrideRepository).delete(override)
    }

    @Test
    fun `createOverride with until_next uses 4h default when no active season`() {
        // Given
        val request = OverrideCreateRequest(duration = "until_next")
        whenever(overrideRepository.findAllActive(any())).thenReturn(emptyList())
        whenever(seasonRepository.findByActiveTrueOrderByPriorityDesc()).thenReturn(emptyList())
        whenever(overrideRepository.save(any<Override>())).thenAnswer { invocation ->
            val override = invocation.getArgument<Override>(0)
            override.copy(id = 1)
        }

        // When
        val result = overrideService.createOverride(request)

        // Then
        // Should default to 4 hours when no schedule is found
        assertTrue(result.remainingMinutes in 235..240)
    }

    @Test
    fun `override isExpired returns true for expired override`() {
        // Given
        val override = Override(
            id = 1,
            createdAt = Instant.now().minus(2, ChronoUnit.HOURS),
            expiresAt = Instant.now().minus(1, ChronoUnit.HOURS)
        )

        // When/Then
        assertTrue(override.isExpired())
        assertFalse(override.isActive())
    }

    @Test
    fun `override isExpired returns false for active override`() {
        // Given
        val override = Override(
            id = 1,
            createdAt = Instant.now(),
            expiresAt = Instant.now().plus(1, ChronoUnit.HOURS)
        )

        // When/Then
        assertFalse(override.isExpired())
        assertTrue(override.isActive())
    }

    @Test
    fun `cleanupExpiredOverrides removes expired overrides`() {
        // Given
        val expiredOverride = Override(
            id = 1,
            createdAt = Instant.now().minus(2, ChronoUnit.HOURS),
            expiresAt = Instant.now().minus(1, ChronoUnit.HOURS)
        )
        val activeOverride = Override(
            id = 2,
            createdAt = Instant.now(),
            expiresAt = Instant.now().plus(1, ChronoUnit.HOURS)
        )
        whenever(overrideRepository.findAll()).thenReturn(listOf(expiredOverride, activeOverride))

        // When
        overrideService.cleanupExpiredOverrides()

        // Then
        verify(overrideRepository).deleteAll(listOf(expiredOverride))
    }
}
