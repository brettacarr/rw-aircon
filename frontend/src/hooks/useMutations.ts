import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  setSystemPower,
  setSystemMode,
  setFanSpeed,
  setSystemTemperature,
  setMyZone,
} from '@/api/system'
import { setZoneTemperature, setZonePower } from '@/api/zones'
import { SYSTEM_STATUS_QUERY_KEY } from './useSystemStatus'
import type { PowerState, AcMode, FanSpeed, ZoneState, SystemStatus } from '@/types'

// Delay before refetching to allow the MyAir device to apply changes
const REFETCH_DELAY_MS = 750

export function useSetSystemPower() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (state: PowerState) => setSystemPower(state),
    onMutate: async (state) => {
      await queryClient.cancelQueries({ queryKey: SYSTEM_STATUS_QUERY_KEY })
      const previousData = queryClient.getQueryData<SystemStatus>(SYSTEM_STATUS_QUERY_KEY)

      queryClient.setQueryData<SystemStatus>(SYSTEM_STATUS_QUERY_KEY, (old) => {
        if (!old) return old
        return { ...old, state }
      })

      return { previousData }
    },
    onError: (_err, _variables, context) => {
      if (context?.previousData) {
        queryClient.setQueryData(SYSTEM_STATUS_QUERY_KEY, context.previousData)
      }
    },
    onSettled: () => {
      setTimeout(() => {
        queryClient.invalidateQueries({ queryKey: SYSTEM_STATUS_QUERY_KEY })
      }, REFETCH_DELAY_MS)
    },
  })
}

export function useSetSystemMode() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (mode: AcMode) => setSystemMode(mode),
    onMutate: async (mode) => {
      await queryClient.cancelQueries({ queryKey: SYSTEM_STATUS_QUERY_KEY })
      const previousData = queryClient.getQueryData<SystemStatus>(SYSTEM_STATUS_QUERY_KEY)

      queryClient.setQueryData<SystemStatus>(SYSTEM_STATUS_QUERY_KEY, (old) => {
        if (!old) return old
        return { ...old, mode }
      })

      return { previousData }
    },
    onError: (_err, _variables, context) => {
      if (context?.previousData) {
        queryClient.setQueryData(SYSTEM_STATUS_QUERY_KEY, context.previousData)
      }
    },
    onSettled: () => {
      setTimeout(() => {
        queryClient.invalidateQueries({ queryKey: SYSTEM_STATUS_QUERY_KEY })
      }, REFETCH_DELAY_MS)
    },
  })
}

export function useSetFanSpeed() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (fan: FanSpeed) => setFanSpeed(fan),
    onMutate: async (fan) => {
      await queryClient.cancelQueries({ queryKey: SYSTEM_STATUS_QUERY_KEY })
      const previousData = queryClient.getQueryData<SystemStatus>(SYSTEM_STATUS_QUERY_KEY)

      queryClient.setQueryData<SystemStatus>(SYSTEM_STATUS_QUERY_KEY, (old) => {
        if (!old) return old
        return { ...old, fan }
      })

      return { previousData }
    },
    onError: (_err, _variables, context) => {
      if (context?.previousData) {
        queryClient.setQueryData(SYSTEM_STATUS_QUERY_KEY, context.previousData)
      }
    },
    onSettled: () => {
      setTimeout(() => {
        queryClient.invalidateQueries({ queryKey: SYSTEM_STATUS_QUERY_KEY })
      }, REFETCH_DELAY_MS)
    },
  })
}

export function useSetSystemTemperature() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (temperature: number) => setSystemTemperature(temperature),
    onMutate: async (temperature) => {
      await queryClient.cancelQueries({ queryKey: SYSTEM_STATUS_QUERY_KEY })
      const previousData = queryClient.getQueryData<SystemStatus>(SYSTEM_STATUS_QUERY_KEY)

      queryClient.setQueryData<SystemStatus>(SYSTEM_STATUS_QUERY_KEY, (old) => {
        if (!old) return old
        return { ...old, setTemp: temperature }
      })

      return { previousData }
    },
    onError: (_err, _variables, context) => {
      if (context?.previousData) {
        queryClient.setQueryData(SYSTEM_STATUS_QUERY_KEY, context.previousData)
      }
    },
    onSettled: () => {
      setTimeout(() => {
        queryClient.invalidateQueries({ queryKey: SYSTEM_STATUS_QUERY_KEY })
      }, REFETCH_DELAY_MS)
    },
  })
}

export function useSetMyZone() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (zone: number) => setMyZone(zone),
    onMutate: async (zone) => {
      await queryClient.cancelQueries({ queryKey: SYSTEM_STATUS_QUERY_KEY })
      const previousData = queryClient.getQueryData<SystemStatus>(SYSTEM_STATUS_QUERY_KEY)

      queryClient.setQueryData<SystemStatus>(SYSTEM_STATUS_QUERY_KEY, (old) => {
        if (!old) return old
        return {
          ...old,
          myZone: zone,
          zones: old.zones.map((z) => ({
            ...z,
            isMyZone: z.id === zone,
          })),
        }
      })

      return { previousData }
    },
    onError: (_err, _variables, context) => {
      if (context?.previousData) {
        queryClient.setQueryData(SYSTEM_STATUS_QUERY_KEY, context.previousData)
      }
    },
    onSettled: () => {
      setTimeout(() => {
        queryClient.invalidateQueries({ queryKey: SYSTEM_STATUS_QUERY_KEY })
      }, REFETCH_DELAY_MS)
    },
  })
}

export function useSetZoneTemperature() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, temperature }: { id: number; temperature: number }) =>
      setZoneTemperature(id, temperature),
    onMutate: async ({ id, temperature }) => {
      await queryClient.cancelQueries({ queryKey: SYSTEM_STATUS_QUERY_KEY })
      const previousData = queryClient.getQueryData<SystemStatus>(SYSTEM_STATUS_QUERY_KEY)

      queryClient.setQueryData<SystemStatus>(SYSTEM_STATUS_QUERY_KEY, (old) => {
        if (!old) return old
        return {
          ...old,
          zones: old.zones.map((zone) =>
            zone.id === id ? { ...zone, setTemp: temperature } : zone
          ),
        }
      })

      return { previousData }
    },
    onError: (_err, _variables, context) => {
      if (context?.previousData) {
        queryClient.setQueryData(SYSTEM_STATUS_QUERY_KEY, context.previousData)
      }
    },
    onSettled: () => {
      setTimeout(() => {
        queryClient.invalidateQueries({ queryKey: SYSTEM_STATUS_QUERY_KEY })
      }, REFETCH_DELAY_MS)
    },
  })
}

export function useSetZonePower() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, state }: { id: number; state: ZoneState }) =>
      setZonePower(id, state),
    onMutate: async ({ id, state }) => {
      await queryClient.cancelQueries({ queryKey: SYSTEM_STATUS_QUERY_KEY })
      const previousData = queryClient.getQueryData<SystemStatus>(SYSTEM_STATUS_QUERY_KEY)

      queryClient.setQueryData<SystemStatus>(SYSTEM_STATUS_QUERY_KEY, (old) => {
        if (!old) return old
        return {
          ...old,
          zones: old.zones.map((zone) =>
            zone.id === id ? { ...zone, state } : zone
          ),
        }
      })

      return { previousData }
    },
    onError: (_err, _variables, context) => {
      if (context?.previousData) {
        queryClient.setQueryData(SYSTEM_STATUS_QUERY_KEY, context.previousData)
      }
    },
    onSettled: () => {
      setTimeout(() => {
        queryClient.invalidateQueries({ queryKey: SYSTEM_STATUS_QUERY_KEY })
      }, REFETCH_DELAY_MS)
    },
  })
}
