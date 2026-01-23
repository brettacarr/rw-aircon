package com.rw.aircon.service

import com.rw.aircon.client.MyAirClient
import com.rw.aircon.dto.*
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
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.*
import org.mockito.quality.Strictness
import java.time.LocalDate
import java.time.LocalTime
import java.time.MonthDay

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScheduleExecutionServiceTest {

    @Mock
    private lateinit var seasonRepository: SeasonRepository

    @Mock
    private lateinit var scheduleEntryRepository: ScheduleEntryRepository

    @Mock
    private lateinit var zoneScheduleRepository: ZoneScheduleRepository

    @Mock
    private lateinit var zoneRepository: ZoneRepository

    @Mock
    private lateinit var myAirClient: MyAirClient

    @Mock
    private lateinit var myAirCacheService: MyAirCacheService

    private lateinit var scheduleExecutionService: ScheduleExecutionService

    @BeforeEach
    fun setUp() {
        scheduleExecutionService = ScheduleExecutionService(
            seasonRepository,
            scheduleEntryRepository,
            zoneScheduleRepository,
            zoneRepository,
            myAirClient,
            myAirCacheService
        )
        scheduleExecutionService.resetAppliedState()
    }

    // ============ determineActiveSeason tests ============

    @Test
    fun `determineActiveSeason returns null when no active seasons`() {
        // Given
        whenever(seasonRepository.findByActiveTrueOrderByPriorityDesc()).thenReturn(emptyList())

        // When
        val result = scheduleExecutionService.determineActiveSeason(LocalDate.of(2026, 6, 15))

        // Then
        assertNull(result)
    }

    @Test
    fun `determineActiveSeason returns matching season for normal date range`() {
        // Given - Winter from Jun 1 to Aug 31
        val winterSeason = Season(
            id = 1,
            name = "Winter",
            startMonth = 6,
            startDay = 1,
            endMonth = 8,
            endDay = 31,
            priority = 10,
            active = true
        )
        whenever(seasonRepository.findByActiveTrueOrderByPriorityDesc()).thenReturn(listOf(winterSeason))

        // When - June 15 is within Winter
        val result = scheduleExecutionService.determineActiveSeason(LocalDate.of(2026, 6, 15))

        // Then
        assertNotNull(result)
        assertEquals("Winter", result!!.name)
    }

    @Test
    fun `determineActiveSeason returns null when date is outside range`() {
        // Given - Winter from Jun 1 to Aug 31
        val winterSeason = Season(
            id = 1,
            name = "Winter",
            startMonth = 6,
            startDay = 1,
            endMonth = 8,
            endDay = 31,
            priority = 10,
            active = true
        )
        whenever(seasonRepository.findByActiveTrueOrderByPriorityDesc()).thenReturn(listOf(winterSeason))

        // When - October 15 is outside Winter
        val result = scheduleExecutionService.determineActiveSeason(LocalDate.of(2026, 10, 15))

        // Then
        assertNull(result)
    }

    @Test
    fun `determineActiveSeason handles year-wrapping season correctly`() {
        // Given - Summer from Dec 1 to Feb 28 (Australian summer)
        val summerSeason = Season(
            id = 1,
            name = "Summer",
            startMonth = 12,
            startDay = 1,
            endMonth = 2,
            endDay = 28,
            priority = 10,
            active = true
        )
        whenever(seasonRepository.findByActiveTrueOrderByPriorityDesc()).thenReturn(listOf(summerSeason))

        // When - January 15 is within Summer (year-wrapping)
        val jan15 = scheduleExecutionService.determineActiveSeason(LocalDate.of(2026, 1, 15))

        // Then
        assertNotNull(jan15)
        assertEquals("Summer", jan15!!.name)

        // And December 20 is also within Summer
        val dec20 = scheduleExecutionService.determineActiveSeason(LocalDate.of(2026, 12, 20))
        assertNotNull(dec20)
        assertEquals("Summer", dec20!!.name)

        // But March 15 is outside
        val mar15 = scheduleExecutionService.determineActiveSeason(LocalDate.of(2026, 3, 15))
        assertNull(mar15)
    }

    @Test
    fun `determineActiveSeason returns highest priority when multiple seasons match`() {
        // Given - Two overlapping seasons
        val seasons = listOf(
            Season(id = 1, name = "HighPriority", startMonth = 6, startDay = 1, endMonth = 8, endDay = 31, priority = 20, active = true),
            Season(id = 2, name = "LowPriority", startMonth = 5, startDay = 1, endMonth = 9, endDay = 30, priority = 10, active = true)
        )
        whenever(seasonRepository.findByActiveTrueOrderByPriorityDesc()).thenReturn(seasons)

        // When - July 15 matches both
        val result = scheduleExecutionService.determineActiveSeason(LocalDate.of(2026, 7, 15))

        // Then - Returns highest priority
        assertNotNull(result)
        assertEquals("HighPriority", result!!.name)
    }

    @Test
    fun `determineActiveSeason matches exact boundary dates`() {
        // Given
        val season = Season(
            id = 1,
            name = "Winter",
            startMonth = 6,
            startDay = 1,
            endMonth = 8,
            endDay = 31,
            priority = 10,
            active = true
        )
        whenever(seasonRepository.findByActiveTrueOrderByPriorityDesc()).thenReturn(listOf(season))

        // When/Then - Exact start date
        val startDate = scheduleExecutionService.determineActiveSeason(LocalDate.of(2026, 6, 1))
        assertNotNull(startDate)

        // When/Then - Exact end date
        val endDate = scheduleExecutionService.determineActiveSeason(LocalDate.of(2026, 8, 31))
        assertNotNull(endDate)

        // When/Then - Day before start
        val beforeStart = scheduleExecutionService.determineActiveSeason(LocalDate.of(2026, 5, 31))
        assertNull(beforeStart)

        // When/Then - Day after end
        val afterEnd = scheduleExecutionService.determineActiveSeason(LocalDate.of(2026, 9, 1))
        assertNull(afterEnd)
    }

    // ============ findCurrentPeriod tests ============

    @Test
    fun `findCurrentPeriod returns null when no entries for day`() {
        // Given
        whenever(scheduleEntryRepository.findBySeasonIdAndDayOfWeekOrderByStartTimeAsc(1L, 1)).thenReturn(emptyList())

        // When
        val result = scheduleExecutionService.findCurrentPeriod(1L, 1, LocalTime.of(10, 0))

        // Then
        assertNull(result)
    }

    @Test
    fun `findCurrentPeriod returns matching entry when time falls within period`() {
        // Given
        val entries = listOf(
            ScheduleEntry(id = 1, seasonId = 1, dayOfWeek = 1, startTime = LocalTime.of(8, 0), endTime = LocalTime.of(12, 0), mode = "cool"),
            ScheduleEntry(id = 2, seasonId = 1, dayOfWeek = 1, startTime = LocalTime.of(14, 0), endTime = LocalTime.of(18, 0), mode = "heat")
        )
        whenever(scheduleEntryRepository.findBySeasonIdAndDayOfWeekOrderByStartTimeAsc(1L, 1)).thenReturn(entries)

        // When - 10:00 falls within first entry (8:00-12:00)
        val result = scheduleExecutionService.findCurrentPeriod(1L, 1, LocalTime.of(10, 0))

        // Then
        assertNotNull(result)
        assertEquals(1L, result!!.id)
        assertEquals("cool", result.mode)
    }

    @Test
    fun `findCurrentPeriod returns null when time is between periods`() {
        // Given
        val entries = listOf(
            ScheduleEntry(id = 1, seasonId = 1, dayOfWeek = 1, startTime = LocalTime.of(8, 0), endTime = LocalTime.of(12, 0), mode = "cool"),
            ScheduleEntry(id = 2, seasonId = 1, dayOfWeek = 1, startTime = LocalTime.of(14, 0), endTime = LocalTime.of(18, 0), mode = "heat")
        )
        whenever(scheduleEntryRepository.findBySeasonIdAndDayOfWeekOrderByStartTimeAsc(1L, 1)).thenReturn(entries)

        // When - 13:00 is between periods
        val result = scheduleExecutionService.findCurrentPeriod(1L, 1, LocalTime.of(13, 0))

        // Then
        assertNull(result)
    }

    @Test
    fun `findCurrentPeriod uses start time inclusive and end time exclusive`() {
        // Given
        val entry = ScheduleEntry(id = 1, seasonId = 1, dayOfWeek = 1, startTime = LocalTime.of(8, 0), endTime = LocalTime.of(12, 0), mode = "cool")
        whenever(scheduleEntryRepository.findBySeasonIdAndDayOfWeekOrderByStartTimeAsc(1L, 1)).thenReturn(listOf(entry))

        // When/Then - Exactly at start time (inclusive)
        val atStart = scheduleExecutionService.findCurrentPeriod(1L, 1, LocalTime.of(8, 0))
        assertNotNull(atStart)

        // When/Then - Exactly at end time (exclusive)
        val atEnd = scheduleExecutionService.findCurrentPeriod(1L, 1, LocalTime.of(12, 0))
        assertNull(atEnd)
    }

    // ============ applyScheduleSettings tests ============

    @Test
    fun `applyScheduleSettings sets system mode`() {
        // Given
        val entry = ScheduleEntry(id = 1, seasonId = 1, dayOfWeek = 1, startTime = LocalTime.of(8, 0), endTime = LocalTime.of(17, 0), mode = "cool")
        whenever(zoneScheduleRepository.findByScheduleEntryId(1L)).thenReturn(emptyList())
        whenever(zoneRepository.findAll()).thenReturn(emptyList())
        whenever(myAirClient.setSystemInfo(any())).thenReturn(true)

        // When
        scheduleExecutionService.applyScheduleSettings(entry)

        // Then
        verify(myAirClient).setSystemInfo(mapOf("mode" to "cool"))
    }

    @Test
    fun `applyScheduleSettings sets zone temperatures and states`() {
        // Given
        val entry = ScheduleEntry(id = 1, seasonId = 1, dayOfWeek = 1, startTime = LocalTime.of(8, 0), endTime = LocalTime.of(17, 0), mode = "cool")
        val zones = listOf(
            Zone(id = 1, name = "Living", myAirZoneId = "z01"),
            Zone(id = 2, name = "Guest", myAirZoneId = "z02")
        )
        val zoneSchedules = listOf(
            ZoneSchedule(id = 1, scheduleEntryId = 1, zoneId = 1, targetTemp = 22, enabled = true),
            ZoneSchedule(id = 2, scheduleEntryId = 1, zoneId = 2, targetTemp = 24, enabled = true)
        )

        whenever(zoneScheduleRepository.findByScheduleEntryId(1L)).thenReturn(zoneSchedules)
        whenever(zoneRepository.findAll()).thenReturn(zones)
        whenever(myAirClient.setSystemInfo(any())).thenReturn(true)
        whenever(myAirClient.setZone(any(), any())).thenReturn(true)
        whenever(myAirCacheService.getSystemData()).thenReturn(createMockResponse() to false)

        // When
        scheduleExecutionService.applyScheduleSettings(entry)

        // Then
        verify(myAirClient).setZone("z01", mapOf("setTemp" to "22"))
        verify(myAirClient).setZone("z01", mapOf("state" to "open"))
        verify(myAirClient).setZone("z02", mapOf("setTemp" to "24"))
        verify(myAirClient).setZone("z02", mapOf("state" to "open"))
    }

    @Test
    fun `applyScheduleSettings does not close myZone`() {
        // Given
        val entry = ScheduleEntry(id = 1, seasonId = 1, dayOfWeek = 1, startTime = LocalTime.of(8, 0), endTime = LocalTime.of(17, 0), mode = "cool")
        val zones = listOf(Zone(id = 3, name = "Upstairs", myAirZoneId = "z03"))
        val zoneSchedules = listOf(
            ZoneSchedule(id = 1, scheduleEntryId = 1, zoneId = 3, targetTemp = 22, enabled = false) // Trying to close
        )

        whenever(zoneScheduleRepository.findByScheduleEntryId(1L)).thenReturn(zoneSchedules)
        whenever(zoneRepository.findAll()).thenReturn(zones)
        whenever(myAirClient.setSystemInfo(any())).thenReturn(true)
        whenever(myAirClient.setZone(any(), any())).thenReturn(true)

        // Mock that z03 (zone number 3) is the current myZone
        val myAirResponse = createMockResponse()
        whenever(myAirCacheService.getSystemData()).thenReturn(myAirResponse to false)

        // When
        scheduleExecutionService.applyScheduleSettings(entry)

        // Then - Should set temperature but NOT close the zone (because it's myZone)
        verify(myAirClient).setZone("z03", mapOf("setTemp" to "22"))
        verify(myAirClient, never()).setZone("z03", mapOf("state" to "close"))
    }

    @Test
    fun `applyScheduleSettings handles missing zone gracefully`() {
        // Given
        val entry = ScheduleEntry(id = 1, seasonId = 1, dayOfWeek = 1, startTime = LocalTime.of(8, 0), endTime = LocalTime.of(17, 0), mode = "cool")
        val zones = listOf(Zone(id = 1, name = "Living", myAirZoneId = "z01"))
        val zoneSchedules = listOf(
            ZoneSchedule(id = 1, scheduleEntryId = 1, zoneId = 1, targetTemp = 22, enabled = true),
            ZoneSchedule(id = 2, scheduleEntryId = 1, zoneId = 999, targetTemp = 24, enabled = true) // Non-existent zone
        )

        whenever(zoneScheduleRepository.findByScheduleEntryId(1L)).thenReturn(zoneSchedules)
        whenever(zoneRepository.findAll()).thenReturn(zones)
        whenever(myAirClient.setSystemInfo(any())).thenReturn(true)
        whenever(myAirClient.setZone(any(), any())).thenReturn(true)
        whenever(myAirCacheService.getSystemData()).thenReturn(createMockResponse() to false)

        // When - Should not throw, should skip missing zone
        scheduleExecutionService.applyScheduleSettings(entry)

        // Then - Only zone 1 commands should be issued
        verify(myAirClient).setZone("z01", mapOf("setTemp" to "22"))
        verify(myAirClient).setZone("z01", mapOf("state" to "open"))
        verify(myAirClient, never()).setZone(eq("z999"), any())
    }

    // ============ Integration tests ============

    @Test
    fun `resetAppliedState clears tracking`() {
        // Given - Set some state
        val entry = ScheduleEntry(id = 1, seasonId = 1, dayOfWeek = 1, startTime = LocalTime.of(8, 0), endTime = LocalTime.of(17, 0), mode = "cool")
        whenever(zoneScheduleRepository.findByScheduleEntryId(1L)).thenReturn(emptyList())
        whenever(zoneRepository.findAll()).thenReturn(emptyList())
        whenever(myAirClient.setSystemInfo(any())).thenReturn(true)

        scheduleExecutionService.applyScheduleSettings(entry)

        // When
        scheduleExecutionService.resetAppliedState()

        // Then
        assertNull(scheduleExecutionService.getLastAppliedEntryId())
    }

    private fun createMockResponse(): MyAirResponse {
        val zones = mapOf(
            "z01" to ZoneInfo(name = "Living", state = "open", setTemp = 22.0, measuredTemp = 23.0, type = 1, number = 1),
            "z02" to ZoneInfo(name = "Guest", state = "open", setTemp = 24.0, measuredTemp = 24.5, type = 1, number = 2),
            "z03" to ZoneInfo(name = "Upstairs", state = "open", setTemp = 22.0, measuredTemp = 21.9, type = 1, number = 3)
        )

        val info = AirconInfo(
            state = "on",
            mode = "cool",
            fan = "auto",
            setTemp = 22.0,
            myZone = 3, // z03 is the controlling zone
            noOfZones = 3
        )

        return MyAirResponse(
            aircons = AirconsWrapper(ac1 = Aircon(info = info, zones = zones)),
            system = SystemInfo(suburbTemp = 18.4, isValidSuburbTemp = true)
        )
    }
}
