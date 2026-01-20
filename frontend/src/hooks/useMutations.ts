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
import type { PowerState, AcMode, FanSpeed, ZoneState } from '@/types'

export function useSetSystemPower() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (state: PowerState) => setSystemPower(state),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SYSTEM_STATUS_QUERY_KEY })
    },
  })
}

export function useSetSystemMode() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (mode: AcMode) => setSystemMode(mode),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SYSTEM_STATUS_QUERY_KEY })
    },
  })
}

export function useSetFanSpeed() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (fan: FanSpeed) => setFanSpeed(fan),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SYSTEM_STATUS_QUERY_KEY })
    },
  })
}

export function useSetSystemTemperature() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (temperature: number) => setSystemTemperature(temperature),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SYSTEM_STATUS_QUERY_KEY })
    },
  })
}

export function useSetMyZone() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (zone: number) => setMyZone(zone),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SYSTEM_STATUS_QUERY_KEY })
    },
  })
}

export function useSetZoneTemperature() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, temperature }: { id: number; temperature: number }) =>
      setZoneTemperature(id, temperature),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SYSTEM_STATUS_QUERY_KEY })
    },
  })
}

export function useSetZonePower() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, state }: { id: number; state: ZoneState }) =>
      setZonePower(id, state),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: SYSTEM_STATUS_QUERY_KEY })
    },
  })
}
