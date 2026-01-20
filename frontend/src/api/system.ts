import apiClient from './client'
import type {
  SystemStatus,
  HealthStatus,
  PowerState,
  AcMode,
  FanSpeed
} from '@/types'

/**
 * Get current system status including all zones
 */
export async function getSystemStatus(): Promise<SystemStatus> {
  const response = await apiClient.get<SystemStatus>('/system/status')
  return response.data
}

/**
 * Get health check status
 */
export async function getHealthStatus(): Promise<HealthStatus> {
  const response = await apiClient.get<HealthStatus>('/health')
  return response.data
}

/**
 * Set system power state (on/off)
 */
export async function setSystemPower(state: PowerState): Promise<void> {
  await apiClient.post('/system/power', { state })
}

/**
 * Set AC mode (cool, heat, vent, dry)
 */
export async function setSystemMode(mode: AcMode): Promise<void> {
  await apiClient.post('/system/mode', { mode })
}

/**
 * Set fan speed
 */
export async function setFanSpeed(fan: FanSpeed): Promise<void> {
  await apiClient.post('/system/fan', { fan })
}

/**
 * Set system target temperature (when myZone=0)
 */
export async function setSystemTemperature(temperature: number): Promise<void> {
  await apiClient.post('/system/temperature', { temperature })
}

/**
 * Set the controlling zone (myZone)
 */
export async function setMyZone(zone: number): Promise<void> {
  await apiClient.post('/system/myzone', { zone })
}
