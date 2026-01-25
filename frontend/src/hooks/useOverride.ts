import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getOverride, createOverride, cancelOverride } from '@/api/override'
import type { OverrideCreateRequest } from '@/types'
import { SYSTEM_STATUS_QUERY_KEY } from './useSystemStatus'

export const OVERRIDE_QUERY_KEY = ['override'] as const

/**
 * Hook to fetch the current active override
 * Refetches every 30 seconds to keep remaining time updated
 */
export function useOverride() {
  return useQuery({
    queryKey: OVERRIDE_QUERY_KEY,
    queryFn: getOverride,
    refetchInterval: 30000, // Refresh every 30 seconds
    staleTime: 15000, // Consider stale after 15 seconds
  })
}

/**
 * Hook to create a new override
 */
export function useCreateOverride() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: OverrideCreateRequest) => createOverride(request),
    onSuccess: () => {
      // Invalidate override query to fetch the new override
      queryClient.invalidateQueries({ queryKey: OVERRIDE_QUERY_KEY })
      // Also invalidate system status as override may change settings
      queryClient.invalidateQueries({ queryKey: SYSTEM_STATUS_QUERY_KEY })
    },
  })
}

/**
 * Hook to cancel the current override
 */
export function useCancelOverride() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => cancelOverride(),
    onSuccess: () => {
      // Invalidate override query to clear the cached override
      queryClient.invalidateQueries({ queryKey: OVERRIDE_QUERY_KEY })
      // Also invalidate system status as schedule may resume
      queryClient.invalidateQueries({ queryKey: SYSTEM_STATUS_QUERY_KEY })
    },
  })
}
