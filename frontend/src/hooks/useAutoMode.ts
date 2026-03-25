import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  getAutoModeConfig,
  updateAutoModeConfig,
  activateAutoMode,
  deactivateAutoMode,
  getAutoModeStatus,
  getAutoModeLogs,
} from '@/api/autoMode'
import type { AutoModeConfigRequest } from '@/types'
import { SYSTEM_STATUS_QUERY_KEY } from './useSystemStatus'
import { CONTROL_MODE_QUERY_KEY } from './useControlMode'

export const AUTO_MODE_CONFIG_QUERY_KEY = ['autoModeConfig'] as const
export const AUTO_MODE_STATUS_QUERY_KEY = ['autoModeStatus'] as const
export const AUTO_MODE_LOG_QUERY_KEY = ['autoModeLog'] as const

/**
 * Hook to fetch Auto Mode configuration
 * Includes zone enable/disable states and min/max temperatures
 */
export function useAutoModeConfig() {
  return useQuery({
    queryKey: AUTO_MODE_CONFIG_QUERY_KEY,
    queryFn: getAutoModeConfig,
    staleTime: 5000, // Consider stale after 5 seconds for more responsive updates
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
    onSuccess: (data) => {
      // Use returned data to update cache immediately (no extra network request)
      queryClient.setQueryData(AUTO_MODE_CONFIG_QUERY_KEY, data)
      // Status needs refetch as backend recalculates zone statuses
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
    onSuccess: (data) => {
      // Use returned data to update config cache immediately
      queryClient.setQueryData(AUTO_MODE_CONFIG_QUERY_KEY, data)
      // Control mode needs refetch to get proper changedAt timestamp
      queryClient.invalidateQueries({ queryKey: CONTROL_MODE_QUERY_KEY })
      // Status and system status need refetch as backend state changed
      queryClient.invalidateQueries({ queryKey: AUTO_MODE_STATUS_QUERY_KEY })
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
    onSuccess: (data) => {
      // Use returned data to update config cache immediately
      queryClient.setQueryData(AUTO_MODE_CONFIG_QUERY_KEY, data)
      // Control mode needs refetch to get proper changedAt timestamp
      queryClient.invalidateQueries({ queryKey: CONTROL_MODE_QUERY_KEY })
      // Invalidate status queries as they're no longer relevant
      queryClient.invalidateQueries({ queryKey: AUTO_MODE_STATUS_QUERY_KEY })
      queryClient.invalidateQueries({ queryKey: SYSTEM_STATUS_QUERY_KEY })
    },
  })
}

/**
 * Hook to fetch Auto Mode action logs
 * Returns history of automatic adjustments made by the system
 *
 * @param limit Maximum number of entries to return
 * @param actionFilter Optional filter by action type
 */
export function useAutoModeLogs(limit: number = 50, actionFilter?: string) {
  return useQuery({
    queryKey: [...AUTO_MODE_LOG_QUERY_KEY, limit, actionFilter] as const,
    queryFn: () => getAutoModeLogs(limit, actionFilter),
    staleTime: 30000, // Consider stale after 30 seconds
  })
}
