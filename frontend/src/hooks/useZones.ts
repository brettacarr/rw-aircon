import { useQuery } from '@tanstack/react-query'
import { getZones } from '@/api/zones'

export const ZONES_QUERY_KEY = ['zones']

export function useZones() {
  return useQuery({
    queryKey: ZONES_QUERY_KEY,
    queryFn: getZones,
    refetchInterval: 10000, // Refetch every 10 seconds
    staleTime: 5000, // Consider data stale after 5 seconds
  })
}
