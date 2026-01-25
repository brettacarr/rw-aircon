import apiClient from './client'
import type { AutoModeConfig, AutoModeConfigRequest, AutoModeStatus } from '@/types'

/**
 * Get the current Auto Mode configuration
 */
export async function getAutoModeConfig(): Promise<AutoModeConfig> {
  const response = await apiClient.get<AutoModeConfig>('/auto-mode')
  return response.data
}

/**
 * Update the Auto Mode configuration
 * Validates:
 * - Temperature range: 16-32°C
 * - Min/Max gap: at least 2°C
 * - At least one non-Guest zone must be enabled
 * - Guest zone cannot be priority zone
 */
export async function updateAutoModeConfig(request: AutoModeConfigRequest): Promise<AutoModeConfig> {
  const response = await apiClient.put<AutoModeConfig>('/auto-mode', request)
  return response.data
}

/**
 * Activate Auto Mode
 * Switches control mode to AUTO and begins automatic temperature control
 */
export async function activateAutoMode(): Promise<AutoModeConfig> {
  const response = await apiClient.post<AutoModeConfig>('/auto-mode/activate')
  return response.data
}

/**
 * Deactivate Auto Mode
 * Switches control mode back to MANUAL
 */
export async function deactivateAutoMode(): Promise<AutoModeConfig> {
  const response = await apiClient.delete<AutoModeConfig>('/auto-mode/activate')
  return response.data
}

/**
 * Get Auto Mode execution status
 * Returns current system state and reason for heating/cooling/off
 */
export async function getAutoModeStatus(): Promise<AutoModeStatus> {
  const response = await apiClient.get<AutoModeStatus>('/auto-mode/status')
  return response.data
}
