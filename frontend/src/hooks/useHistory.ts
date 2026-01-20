import { useQuery } from "@tanstack/react-query"
import { getZoneHistory, getSystemHistory } from "@/api/history"
import type { ZoneHistoryResponse, SystemHistoryResponse } from "@/types"

/**
 * Hook for fetching zone temperature history.
 */
export function useZoneHistory(
  zoneId: number,
  from: string,
  to: string,
  enabled = true
) {
  return useQuery<ZoneHistoryResponse, Error>({
    queryKey: ["zoneHistory", zoneId, from, to],
    queryFn: () => getZoneHistory(zoneId, from, to),
    enabled: enabled && !!zoneId && !!from && !!to,
    staleTime: 60 * 1000, // History data is stable, only refetch after 1 minute
    refetchOnWindowFocus: false,
  })
}

/**
 * Hook for fetching system state history.
 */
export function useSystemHistory(from: string, to: string, enabled = true) {
  return useQuery<SystemHistoryResponse, Error>({
    queryKey: ["systemHistory", from, to],
    queryFn: () => getSystemHistory(from, to),
    enabled: enabled && !!from && !!to,
    staleTime: 60 * 1000,
    refetchOnWindowFocus: false,
  })
}
