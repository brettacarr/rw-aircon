import apiClient from './client'
import type { Zone, ZoneState } from '@/types'

/**
 * Get all zones with their current state
 */
export async function getZones(): Promise<Zone[]> {
  const response = await apiClient.get<Zone[]>('/zones')
  return response.data
}

/**
 * Get a single zone by ID
 */
export async function getZone(id: number): Promise<Zone> {
  const response = await apiClient.get<Zone>(`/zones/${id}`)
  return response.data
}

/**
 * Set zone target temperature
 */
export async function setZoneTemperature(id: number, temperature: number): Promise<void> {
  await apiClient.post(`/zones/${id}/target`, { temperature })
}

/**
 * Set zone power state (open/close)
 */
export async function setZonePower(id: number, state: ZoneState): Promise<void> {
  await apiClient.post(`/zones/${id}/power`, { state })
}
