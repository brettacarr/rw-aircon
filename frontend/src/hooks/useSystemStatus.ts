import { useQuery } from '@tanstack/react-query'
import { getSystemStatus } from '@/api/system'

export const SYSTEM_STATUS_QUERY_KEY = ['systemStatus'] as const

export function useSystemStatus() {
  return useQuery({
    queryKey: SYSTEM_STATUS_QUERY_KEY,
    queryFn: getSystemStatus,
    refetchInterval: 10000, // Refetch every 10 seconds
    staleTime: 5000, // Consider data stale after 5 seconds
  })
}
