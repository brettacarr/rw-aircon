import apiClient from "./client"
import type { ZoneHistoryResponse, SystemHistoryResponse } from "@/types"

/**
 * Fetches temperature history for a specific zone.
 * For ranges > 7 days, returns hourly averages instead of raw data.
 */
export async function getZoneHistory(
  zoneId: number,
  from: string,
  to: string
): Promise<ZoneHistoryResponse> {
  const params = new URLSearchParams({ from, to })
  const response = await apiClient.get<ZoneHistoryResponse>(
    `/history/zones/${zoneId}?${params.toString()}`
  )
  return response.data
}

/**
 * Fetches system state history.
 */
export async function getSystemHistory(
  from: string,
  to: string
): Promise<SystemHistoryResponse> {
  const params = new URLSearchParams({ from, to })
  const response = await apiClient.get<SystemHistoryResponse>(
    `/history/system?${params.toString()}`
  )
  return response.data
}
