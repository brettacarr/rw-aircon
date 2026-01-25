import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  getAutoModeConfig,
  updateAutoModeConfig,
  activateAutoMode,
  deactivateAutoMode,
  getAutoModeStatus,
} from '@/api/autoMode'
import type { AutoModeConfigRequest } from '@/types'
import { SYSTEM_STATUS_QUERY_KEY } from './useSystemStatus'
import { CONTROL_MODE_QUERY_KEY } from './useControlMode'

export const AUTO_MODE_CONFIG_QUERY_KEY = ['autoModeConfig'] as const
export const AUTO_MODE_STATUS_QUERY_KEY = ['autoModeStatus'] as const

/**
 * Hook to fetch Auto Mode configuration
 * Includes zone enable/disable states and min/max temperatures
 */
export function useAutoModeConfig() {
  return useQuery({
    queryKey: AUTO_MODE_CONFIG_QUERY_KEY,
    queryFn: getAutoModeConfig,
    staleTime: 30000, // Consider stale after 30 seconds
  })
}

/**
 * Hook to fetch Auto Mode execution status
 * Shows what the system is doing (heating/cooling/off) and why
 * Only polls when Auto Mode is active
 */
export function useAutoModeStatus(enabled: boolean = true) {
  return useQuery({
    queryKey: AUTO_MODE_STATUS_QUERY_KEY,
    queryFn: getAutoModeStatus,
    refetchInterval: enabled ? 10000 : false, // Poll every 10 seconds when enabled
    staleTime: 5000,
    enabled,
  })
}

/**
 * Hook to update Auto Mode configuration
 */
export function useUpdateAutoModeConfig() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: AutoModeConfigRequest) => updateAutoModeConfig(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: AUTO_MODE_CONFIG_QUERY_KEY })
      queryClient.invalidateQueries({ queryKey: AUTO_MODE_STATUS_QUERY_KEY })
    },
  })
}

/**
 * Hook to activate Auto Mode
 * Switches control mode to AUTO
 */
export function useActivateAutoMode() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => activateAutoMode(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: AUTO_MODE_CONFIG_QUERY_KEY })
      queryClient.invalidateQueries({ queryKey: AUTO_MODE_STATUS_QUERY_KEY })
      queryClient.invalidateQueries({ queryKey: CONTROL_MODE_QUERY_KEY })
      queryClient.invalidateQueries({ queryKey: SYSTEM_STATUS_QUERY_KEY })
    },
  })
}

/**
 * Hook to deactivate Auto Mode
 * Switches control mode back to MANUAL
 */
export function useDeactivateAutoMode() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => deactivateAutoMode(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: AUTO_MODE_CONFIG_QUERY_KEY })
      queryClient.invalidateQueries({ queryKey: AUTO_MODE_STATUS_QUERY_KEY })
      queryClient.invalidateQueries({ queryKey: CONTROL_MODE_QUERY_KEY })
      queryClient.invalidateQueries({ queryKey: SYSTEM_STATUS_QUERY_KEY })
    },
  })
}
