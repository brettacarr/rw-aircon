import apiClient from './client'
import type { ControlMode, ControlModeState } from '@/types'

/**
 * Get the current control mode
 * Returns: manual, auto, or schedule
 */
export async function getControlMode(): Promise<ControlModeState> {
  const response = await apiClient.get<ControlModeState>('/control-mode')
  return response.data
}

/**
 * Set the control mode
 * Switching modes preserves the configuration of other modes
 */
export async function setControlMode(mode: ControlMode): Promise<ControlModeState> {
  const response = await apiClient.put<ControlModeState>('/control-mode', { mode })
  return response.data
}
