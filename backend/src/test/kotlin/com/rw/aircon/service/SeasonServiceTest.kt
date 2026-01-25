package com.rw.aircon.service

import com.rw.aircon.dto.FullScheduleUpdateRequest
import com.rw.aircon.dto.ScheduleEntryRequest
import com.rw.aircon.dto.SeasonCreateRequest
import com.rw.aircon.dto.SeasonUpdateRequest
import com.rw.aircon.dto.ZoneScheduleRequest
import com.rw.aircon.model.ScheduleEntry
import com.rw.aircon.model.Season
import com.rw.aircon.model.Zone
import com.rw.aircon.model.ZoneSchedule
import com.rw.aircon.repository.ScheduleEntryRepository
import com.rw.aircon.repository.SeasonRepository
import com.rw.aircon.repository.ZoneRepository
import com.rw.aircon.repository.ZoneScheduleRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.LocalTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class SeasonServiceTest {

    @Mock
    private lateinit var seasonRepository: SeasonRepository

    @Mock
    private lateinit var scheduleEntryRepository: ScheduleEntryRepository

    @Mock
    private lateinit var zoneScheduleRepository: ZoneScheduleRepository

    @Mock
    private lateinit var zoneRepository: ZoneRepository

    private lateinit var seasonService: SeasonService

    @BeforeEach
    fun setUp() {
        seasonService = SeasonService(
            seasonRepository,
            scheduleEntryRepository,
            zoneScheduleRepository,
            zoneRepository
        )
    }

    @Test
    fun `getAllSeasons returns seasons ordered by priority`() {
        // Given
        val seasons = listOf(
            Season(id = 1, name = "Summer", startMonth = 12, startDay = 1, endMonth = 2, endDay = 28, priority = 10, active = true),
            Season(id = 2, name = "Winter", startMonth = 6, startDay = 1, endMonth = 8, endDay = 31, priority = 5, active = true)
        )
        whenever(seasonRepository.findAllByOrderByPriorityDesc()).thenReturn(seasons)

        // When
        val result = seasonService.getAllSeasons()

        // Then
        assertEquals(2, result.size)
        assertEquals("Summer", result[0].name)
        assertEquals("Winter", result[1].name)
    }

    @Test
    fun `getSeason returns season when found`() {
        // Given
        val season = Season(id = 1, name = "Summer", startMonth = 12, startDay = 1, endMonth = 2, endDay = 28, priority = 10, active = true)
        whenever(seasonRepository.findById(1L)).thenReturn(Optional.of(season))

        // When
        val result = seasonService.getSeason(1L)

        // Then
        assertNotNull(result)
        assertEquals("Summer", result!!.name)
        assertEquals(12, result.startMonth)
        assertEquals(1, result.startDay)
        assertEquals(2, result.endMonth)
        assertEquals(28, result.endDay)
    }

    @Test
    fun `getSeason returns null when not found`() {
        // Given
        whenever(seasonRepository.findById(999L)).thenReturn(Optional.empty())

        // When
        val result = seasonService.getSeason(999L)

        // Then
        assertNull(result)
    }

    @Test
    fun `createSeason validates name is not blank`() {
        // Given
        val request = SeasonCreateRequest(name = "", startMonth = 1, startDay = 1, endMonth = 12, endDay = 31)

        // When/Then
        val exception = assertThrows<IllegalArgumentException> {
            seasonService.createSeason(request)
        }
        assertTrue(exception.message!!.contains("cannot be blank"))
    }

    @Test
    fun `createSeason validates month range`() {
        // Given - invalid start month
        val request = SeasonCreateRequest(name = "Test", startMonth = 13, startDay = 1, endMonth = 12, endDay = 31)

        // When/Then
        val exception = assertThrows<IllegalArgumentException> {
            seasonService.createSeason(request)
        }
        assertTrue(exception.message!!.contains("Start month"))
    }

    @Test
    fun `createSeason validates day range`() {
        // Given - invalid start day
        val request = SeasonCreateRequest(name = "Test", startMonth = 1, startDay = 32, endMonth = 12, endDay = 31)

        // When/Then
        val exception = assertThrows<IllegalArgumentException> {
            seasonService.createSeason(request)
        }
        assertTrue(exception.message!!.contains("Start day"))
    }

    @Test
    fun `createSeason rejects duplicate name`() {
        // Given
        val request = SeasonCreateRequest(name = "Summer", startMonth = 12, startDay = 1, endMonth = 2, endDay = 28)
        whenever(seasonRepository.existsByName("Summer")).thenReturn(true)

        // When/Then
        val exception = assertThrows<IllegalArgumentException> {
            seasonService.createSeason(request)
        }
        assertTrue(exception.message!!.contains("already exists"))
    }

    @Test
    fun `createSeason creates season successfully`() {
        // Given
        val request = SeasonCreateRequest(name = "Summer", startMonth = 12, startDay = 1, endMonth = 2, endDay = 28, priority = 10)
        whenever(seasonRepository.existsByName("Summer")).thenReturn(false)
        whenever(seasonRepository.save(any<Season>())).thenAnswer { invocation ->
            val season = invocation.getArgument<Season>(0)
            season.copy(id = 1)
        }

        // When
        val result = seasonService.createSeason(request)

        // Then
        assertEquals(1L, result.id)
        assertEquals("Summer", result.name)
        assertEquals(12, result.startMonth)
        assertEquals(10, result.priority)
        verify(seasonRepository).save(any())
    }

    @Test
    fun `updateSeason returns null when season not found`() {
        // Given
        whenever(seasonRepository.findById(999L)).thenReturn(Optional.empty())

        // When
        val result = seasonService.updateSeason(999L, SeasonUpdateRequest(name = "Updated"))

        // Then
        assertNull(result)
    }

    @Test
    fun `updateSeason updates only provided fields`() {
        // Given
        val existing = Season(id = 1, name = "Summer", startMonth = 12, startDay = 1, endMonth = 2, endDay = 28, priority = 10, active = true)
        whenever(seasonRepository.findById(1L)).thenReturn(Optional.of(existing))
        whenever(seasonRepository.save(any<Season>())).thenAnswer { it.getArgument<Season>(0) }

        val request = SeasonUpdateRequest(priority = 20) // Only updating priority

        // When
        val result = seasonService.updateSeason(1L, request)

        // Then
        assertNotNull(result)
        assertEquals("Summer", result!!.name) // Unchanged
        assertEquals(20, result.priority)     // Changed
        assertEquals(12, result.startMonth)   // Unchanged
    }

    @Test
    fun `deleteSeason returns false when not found`() {
        // Given
        whenever(seasonRepository.existsById(999L)).thenReturn(false)

        // When
        val result = seasonService.deleteSeason(999L)

        // Then
        assertFalse(result)
    }

    @Test
    fun `deleteSeason cascades to schedules and zone schedules`() {
        // Given
        val entries = listOf(
            ScheduleEntry(id = 1, seasonId = 1, dayOfWeek = 1, startTime = LocalTime.of(8, 0), endTime = LocalTime.of(17, 0), mode = "cool")
        )
        whenever(seasonRepository.existsById(1L)).thenReturn(true)
        whenever(scheduleEntryRepository.findBySeasonIdOrderByDayOfWeekAscStartTimeAsc(1L)).thenReturn(entries)
        whenever(zoneScheduleRepository.deleteByScheduleEntryIdIn(listOf(1L))).thenReturn(1)
        whenever(scheduleEntryRepository.deleteBySeasonId(1L)).thenReturn(1)

        // When
        val result = seasonService.deleteSeason(1L)

        // Then
        assertTrue(result)
        verify(zoneScheduleRepository).deleteByScheduleEntryIdIn(listOf(1L))
        verify(scheduleEntryRepository).deleteBySeasonId(1L)
        verify(seasonRepository).deleteById(1L)
    }

    @Test
    fun `updateSchedule validates time format`() {
        // Given
        whenever(seasonRepository.existsById(1L)).thenReturn(true)
        whenever(zoneRepository.findAll()).thenReturn(listOf(Zone(id = 1, name = "Living", myAirZoneId = "z01")))

        val request = FullScheduleUpdateRequest(
            entries = listOf(
                ScheduleEntryRequest(
                    dayOfWeek = 1,
                    startTime = "invalid",
                    endTime = "17:00",
                    mode = "cool",
                    zoneSettings = emptyList()
                )
            )
        )

        // When/Then
        val exception = assertThrows<IllegalArgumentException> {
            seasonService.updateSchedule(1L, request)
        }
        assertTrue(exception.message!!.contains("Invalid start time format"))
    }

    @Test
    fun `updateSchedule validates end time after start time`() {
        // Given
        whenever(seasonRepository.existsById(1L)).thenReturn(true)
        whenever(zoneRepository.findAll()).thenReturn(listOf(Zone(id = 1, name = "Living", myAirZoneId = "z01")))

        val request = FullScheduleUpdateRequest(
            entries = listOf(
                ScheduleEntryRequest(
                    dayOfWeek = 1,
                    startTime = "17:00",
                    endTime = "08:00",
                    mode = "cool",
                    zoneSettings = emptyList()
                )
            )
        )

        // When/Then
        val exception = assertThrows<IllegalArgumentException> {
            seasonService.updateSchedule(1L, request)
        }
        assertTrue(exception.message!!.contains("End time must be after start time"))
    }

    @Test
    fun `updateSchedule validates mode`() {
        // Given
        whenever(seasonRepository.existsById(1L)).thenReturn(true)
        whenever(zoneRepository.findAll()).thenReturn(listOf(Zone(id = 1, name = "Living", myAirZoneId = "z01")))

        val request = FullScheduleUpdateRequest(
            entries = listOf(
                ScheduleEntryRequest(
                    dayOfWeek = 1,
                    startTime = "08:00",
                    endTime = "17:00",
                    mode = "invalid",
                    zoneSettings = emptyList()
                )
            )
        )

        // When/Then
        val exception = assertThrows<IllegalArgumentException> {
            seasonService.updateSchedule(1L, request)
        }
        assertTrue(exception.message!!.contains("Invalid mode"))
    }

    @Test
    fun `updateSchedule validates day of week`() {
        // Given
        whenever(seasonRepository.existsById(1L)).thenReturn(true)
        whenever(zoneRepository.findAll()).thenReturn(listOf(Zone(id = 1, name = "Living", myAirZoneId = "z01")))

        val request = FullScheduleUpdateRequest(
            entries = listOf(
                ScheduleEntryRequest(
                    dayOfWeek = 8, // Invalid
                    startTime = "08:00",
                    endTime = "17:00",
                    mode = "cool",
                    zoneSettings = emptyList()
                )
            )
        )

        // When/Then
        val exception = assertThrows<IllegalArgumentException> {
            seasonService.updateSchedule(1L, request)
        }
        assertTrue(exception.message!!.contains("Day of week"))
    }

    @Test
    fun `updateSchedule validates zone temperature range`() {
        // Given
        whenever(seasonRepository.existsById(1L)).thenReturn(true)
        whenever(zoneRepository.findAll()).thenReturn(listOf(Zone(id = 1, name = "Living", myAirZoneId = "z01")))

        val request = FullScheduleUpdateRequest(
            entries = listOf(
                ScheduleEntryRequest(
                    dayOfWeek = 1,
                    startTime = "08:00",
                    endTime = "17:00",
                    mode = "cool",
                    zoneSettings = listOf(ZoneScheduleRequest(zoneId = 1, targetTemp = 35)) // Too high
                )
            )
        )

        // When/Then
        val exception = assertThrows<IllegalArgumentException> {
            seasonService.updateSchedule(1L, request)
        }
        assertTrue(exception.message!!.contains("Temperature must be between"))
    }

    @Test
    fun `updateSchedule validates zone ID exists`() {
        // Given
        whenever(seasonRepository.existsById(1L)).thenReturn(true)
        whenever(zoneRepository.findAll()).thenReturn(listOf(Zone(id = 1, name = "Living", myAirZoneId = "z01")))

        val request = FullScheduleUpdateRequest(
            entries = listOf(
                ScheduleEntryRequest(
                    dayOfWeek = 1,
                    startTime = "08:00",
                    endTime = "17:00",
                    mode = "cool",
                    zoneSettings = listOf(ZoneScheduleRequest(zoneId = 999, targetTemp = 22)) // Invalid zone
                )
            )
        )

        // When/Then
        val exception = assertThrows<IllegalArgumentException> {
            seasonService.updateSchedule(1L, request)
        }
        assertTrue(exception.message!!.contains("Invalid zone ID"))
    }

    @Test
    fun `updateSchedule detects overlapping time periods`() {
        // Given
        whenever(seasonRepository.existsById(1L)).thenReturn(true)
        whenever(zoneRepository.findAll()).thenReturn(listOf(Zone(id = 1, name = "Living", myAirZoneId = "z01")))

        val request = FullScheduleUpdateRequest(
            entries = listOf(
                ScheduleEntryRequest(
                    dayOfWeek = 1,
                    startTime = "08:00",
                    endTime = "12:00",
                    mode = "cool",
                    zoneSettings = emptyList()
                ),
                ScheduleEntryRequest(
                    dayOfWeek = 1,
                    startTime = "10:00", // Overlaps with previous
                    endTime = "15:00",
                    mode = "heat",
                    zoneSettings = emptyList()
                )
            )
        )

        // When/Then
        val exception = assertThrows<IllegalArgumentException> {
            seasonService.updateSchedule(1L, request)
        }
        assertTrue(exception.message!!.contains("overlap"))
    }

    @Test
    fun `updateSchedule creates schedule entries successfully`() {
        // Given
        val zones = listOf(
            Zone(id = 1, name = "Living", myAirZoneId = "z01"),
            Zone(id = 2, name = "Guest", myAirZoneId = "z02")
        )
        whenever(seasonRepository.existsById(1L)).thenReturn(true)
        whenever(zoneRepository.findAll()).thenReturn(zones)
        whenever(scheduleEntryRepository.findBySeasonIdOrderByDayOfWeekAscStartTimeAsc(1L)).thenReturn(emptyList())
        whenever(scheduleEntryRepository.save(any<ScheduleEntry>())).thenAnswer { invocation ->
            val entry = invocation.getArgument<ScheduleEntry>(0)
            entry.copy(id = 1)
        }
        whenever(zoneScheduleRepository.save(any<ZoneSchedule>())).thenAnswer { invocation ->
            val zs = invocation.getArgument<ZoneSchedule>(0)
            zs.copy(id = 1)
        }

        val request = FullScheduleUpdateRequest(
            entries = listOf(
                ScheduleEntryRequest(
                    dayOfWeek = 1,
                    startTime = "08:00",
                    endTime = "17:00",
                    mode = "cool",
                    zoneSettings = listOf(
                        ZoneScheduleRequest(zoneId = 1, targetTemp = 22, enabled = true),
                        ZoneScheduleRequest(zoneId = 2, targetTemp = 24, enabled = false)
                    )
                )
            )
        )

        // When
        val result = seasonService.updateSchedule(1L, request)

        // Then
        assertNotNull(result)
        assertEquals(1, result!!.size)
        assertEquals(1, result[0].dayOfWeek)
        assertEquals("08:00", result[0].startTime)
        assertEquals("17:00", result[0].endTime)
        assertEquals("cool", result[0].mode)
        assertEquals(2, result[0].zoneSettings.size)
    }
}
