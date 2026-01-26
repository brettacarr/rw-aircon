import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getControlMode, setControlMode } from '@/api/controlMode'
import type { ControlMode } from '@/types'
import { SYSTEM_STATUS_QUERY_KEY } from './useSystemStatus'

export const CONTROL_MODE_QUERY_KEY = ['controlMode'] as const

/**
 * Hook to fetch the current control mode
 * Returns: manual, auto, or schedule
 */
export function useControlMode() {
  return useQuery({
    queryKey: CONTROL_MODE_QUERY_KEY,
    queryFn: getControlMode,
    staleTime: 5000, // Consider stale after 5 seconds for responsive mode switching
  })
}

/**
 * Hook to change the control mode
 * Switching modes preserves the configuration of other modes
 */
export function useSetControlMode() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (mode: ControlMode) => setControlMode(mode),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CONTROL_MODE_QUERY_KEY })
      // Invalidate system status as mode change may affect system behavior
      queryClient.invalidateQueries({ queryKey: SYSTEM_STATUS_QUERY_KEY })
    },
  })
}
