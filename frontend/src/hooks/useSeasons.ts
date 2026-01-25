import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  getSeasons,
  getSeason,
  createSeason,
  updateSeason,
  deleteSeason,
  getSchedule,
  updateSchedule,
} from '@/api/seasons'
import type {
  SeasonCreateRequest,
  SeasonUpdateRequest,
  FullScheduleUpdateRequest,
} from '@/types'

export const SEASONS_QUERY_KEY = ['seasons']
export const SEASON_QUERY_KEY = (id: number) => ['seasons', id]
export const SCHEDULE_QUERY_KEY = (seasonId: number) => ['seasons', seasonId, 'schedule']

/**
 * Hook to fetch all seasons
 */
export function useSeasons() {
  return useQuery({
    queryKey: SEASONS_QUERY_KEY,
    queryFn: getSeasons,
    staleTime: 60000, // Consider data stale after 1 minute
  })
}

/**
 * Hook to fetch a single season with its schedule
 */
export function useSeason(id: number | null) {
  return useQuery({
    queryKey: SEASON_QUERY_KEY(id ?? 0),
    queryFn: () => getSeason(id!),
    enabled: id !== null,
    staleTime: 30000, // Consider data stale after 30 seconds
  })
}

/**
 * Hook to fetch schedule entries for a season
 */
export function useSchedule(seasonId: number | null) {
  return useQuery({
    queryKey: SCHEDULE_QUERY_KEY(seasonId ?? 0),
    queryFn: () => getSchedule(seasonId!),
    enabled: seasonId !== null,
    staleTime: 30000,
  })
}

/**
 * Hook to create a new season
 */
export function useCreateSeason() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: SeasonCreateRequest) => createSeason(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SEASONS_QUERY_KEY })
    },
  })
}

/**
 * Hook to update an existing season
 */
export function useUpdateSeason() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: SeasonUpdateRequest }) =>
      updateSeason(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: SEASONS_QUERY_KEY })
      queryClient.invalidateQueries({ queryKey: SEASON_QUERY_KEY(variables.id) })
    },
  })
}

/**
 * Hook to delete a season
 */
export function useDeleteSeason() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: number) => deleteSeason(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SEASONS_QUERY_KEY })
    },
  })
}

/**
 * Hook to update a season's schedule
 */
export function useUpdateSchedule() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      seasonId,
      data,
    }: {
      seasonId: number
      data: FullScheduleUpdateRequest
    }) => updateSchedule(seasonId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({
        queryKey: SEASON_QUERY_KEY(variables.seasonId),
      })
      queryClient.invalidateQueries({
        queryKey: SCHEDULE_QUERY_KEY(variables.seasonId),
      })
    },
  })
}
