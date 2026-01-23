import apiClient from './client'
import type {
  Season,
  SeasonWithSchedule,
  SeasonCreateRequest,
  SeasonUpdateRequest,
  ScheduleEntry,
  FullScheduleUpdateRequest,
} from '@/types'

/**
 * Get all seasons ordered by priority
 */
export async function getSeasons(): Promise<Season[]> {
  const response = await apiClient.get<Season[]>('/seasons')
  return response.data
}

/**
 * Get a single season with its schedule
 */
export async function getSeason(id: number): Promise<SeasonWithSchedule> {
  const response = await apiClient.get<SeasonWithSchedule>(`/seasons/${id}`)
  return response.data
}

/**
 * Create a new season
 */
export async function createSeason(data: SeasonCreateRequest): Promise<Season> {
  const response = await apiClient.post<Season>('/seasons', data)
  return response.data
}

/**
 * Update an existing season
 */
export async function updateSeason(id: number, data: SeasonUpdateRequest): Promise<Season> {
  const response = await apiClient.put<Season>(`/seasons/${id}`, data)
  return response.data
}

/**
 * Delete a season
 */
export async function deleteSeason(id: number): Promise<void> {
  await apiClient.delete(`/seasons/${id}`)
}

/**
 * Get the schedule for a season
 */
export async function getSchedule(seasonId: number): Promise<ScheduleEntry[]> {
  const response = await apiClient.get<ScheduleEntry[]>(`/seasons/${seasonId}/schedule`)
  return response.data
}

/**
 * Update the entire schedule for a season
 */
export async function updateSchedule(
  seasonId: number,
  data: FullScheduleUpdateRequest
): Promise<ScheduleEntry[]> {
  const response = await apiClient.put<ScheduleEntry[]>(`/seasons/${seasonId}/schedule`, data)
  return response.data
}
