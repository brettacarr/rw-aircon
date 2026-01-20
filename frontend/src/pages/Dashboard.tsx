import { useSystemStatus } from "@/hooks/useSystemStatus"
import {
  useSetSystemPower,
  useSetSystemMode,
  useSetFanSpeed,
  useSetSystemTemperature,
  useSetZoneTemperature,
  useSetZonePower,
} from "@/hooks/useMutations"
import { ZoneCard } from "@/components/ZoneCard"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Switch } from "@/components/ui/switch"
import { Select } from "@/components/ui/select"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Power,
  Thermometer,
  Wind,
  AlertTriangle,
  RefreshCw,
  Minus,
  Plus,
} from "lucide-react"
import type { AcMode, FanSpeed } from "@/types"

const MODE_OPTIONS: { value: AcMode; label: string }[] = [
  { value: "cool", label: "Cool" },
  { value: "heat", label: "Heat" },
  { value: "vent", label: "Vent" },
  { value: "dry", label: "Dry" },
]

const FAN_OPTIONS: { value: FanSpeed; label: string }[] = [
  { value: "low", label: "Low" },
  { value: "medium", label: "Medium" },
  { value: "high", label: "High" },
  { value: "auto", label: "Auto" },
  { value: "autoAA", label: "Auto AA" },
]

export function Dashboard() {
  const { data, isLoading, isError, error, refetch } = useSystemStatus()

  const setSystemPower = useSetSystemPower()
  const setSystemMode = useSetSystemMode()
  const setFanSpeed = useSetFanSpeed()
  const setSystemTemperature = useSetSystemTemperature()
  const setZoneTemperature = useSetZoneTemperature()
  const setZonePower = useSetZonePower()

  const isSystemOn = data?.state === "on"
  const isAnyMutationPending =
    setSystemPower.isPending ||
    setSystemMode.isPending ||
    setFanSpeed.isPending ||
    setSystemTemperature.isPending

  if (isLoading) {
    return <DashboardSkeleton />
  }

  if (isError) {
    return (
      <div className="container mx-auto p-4">
        <Card className="border-destructive">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-destructive">
              <AlertTriangle className="h-5 w-5" />
              Connection Error
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-muted-foreground mb-4">
              {error instanceof Error
                ? error.message
                : "Failed to connect to the AC system"}
            </p>
            <Button onClick={() => refetch()} variant="outline">
              <RefreshCw className="h-4 w-4 mr-2" />
              Retry
            </Button>
          </CardContent>
        </Card>
      </div>
    )
  }

  if (!data) {
    return null
  }

  return (
    <div className="container mx-auto p-4 space-y-6">
      {/* Header with System Status */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <h1 className="text-2xl font-bold">Air Conditioning</h1>
        <div className="flex items-center gap-4">
          {/* Outdoor Temperature */}
          {data.isValidOutdoorTemp && data.outdoorTemp !== undefined && (
            <Badge variant="secondary" className="text-sm">
              <Thermometer className="h-4 w-4 mr-1" />
              Outside: {data.outdoorTemp.toFixed(1)}°C
            </Badge>
          )}

          {/* Filter Warning */}
          {data.filterCleanStatus && data.filterCleanStatus > 0 && (
            <Badge variant="warning">
              <AlertTriangle className="h-4 w-4 mr-1" />
              Filter needs cleaning
            </Badge>
          )}

          {/* Error Status */}
          {data.airconErrorCode && (
            <Badge variant="destructive">
              <AlertTriangle className="h-4 w-4 mr-1" />
              Error: {data.airconErrorCode}
            </Badge>
          )}
        </div>
      </div>

      {/* System Controls Card */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center justify-between">
            <span>System Controls</span>
            <div className="flex items-center gap-2">
              <span className="text-sm font-normal text-muted-foreground">
                {isSystemOn ? "On" : "Off"}
              </span>
              <Switch
                checked={isSystemOn}
                onCheckedChange={(checked) =>
                  setSystemPower.mutate(checked ? "on" : "off")
                }
                disabled={setSystemPower.isPending}
                aria-label="System power"
              />
            </div>
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {/* Mode Selector */}
            <div className="space-y-2">
              <label className="text-sm font-medium flex items-center gap-2">
                <Power className="h-4 w-4" />
                Mode
              </label>
              <Select
                value={data.mode}
                onValueChange={(value) =>
                  setSystemMode.mutate(value as AcMode)
                }
                disabled={!isSystemOn || setSystemMode.isPending}
              >
                {MODE_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </Select>
            </div>

            {/* Fan Speed Selector */}
            <div className="space-y-2">
              <label className="text-sm font-medium flex items-center gap-2">
                <Wind className="h-4 w-4" />
                Fan Speed
              </label>
              <Select
                value={data.fan}
                onValueChange={(value) =>
                  setFanSpeed.mutate(value as FanSpeed)
                }
                disabled={!isSystemOn || setFanSpeed.isPending}
              >
                {FAN_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </Select>
            </div>

            {/* System Temperature (when myZone=0) */}
            {data.myZone === 0 && (
              <div className="space-y-2 sm:col-span-2">
                <label className="text-sm font-medium flex items-center gap-2">
                  <Thermometer className="h-4 w-4" />
                  System Temperature
                </label>
                <div className="flex items-center gap-3">
                  <Button
                    variant="outline"
                    size="icon"
                    onClick={() =>
                      setSystemTemperature.mutate(data.setTemp - 1)
                    }
                    disabled={
                      !isSystemOn ||
                      data.setTemp <= 16 ||
                      setSystemTemperature.isPending
                    }
                    aria-label="Decrease system temperature"
                  >
                    <Minus className="h-4 w-4" />
                  </Button>
                  <span className="text-2xl font-bold min-w-[60px] text-center">
                    {data.setTemp}°C
                  </span>
                  <Button
                    variant="outline"
                    size="icon"
                    onClick={() =>
                      setSystemTemperature.mutate(data.setTemp + 1)
                    }
                    disabled={
                      !isSystemOn ||
                      data.setTemp >= 32 ||
                      setSystemTemperature.isPending
                    }
                    aria-label="Increase system temperature"
                  >
                    <Plus className="h-4 w-4" />
                  </Button>
                </div>
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Zone Cards Grid */}
      <div>
        <h2 className="text-xl font-semibold mb-4">Zones</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {data.zones.map((zone) => (
            <ZoneCard
              key={zone.id}
              zone={zone}
              isDisabled={!isSystemOn || isAnyMutationPending}
              onTemperatureChange={(id, temperature) =>
                setZoneTemperature.mutate({ id, temperature })
              }
              onPowerChange={(id, state) => setZonePower.mutate({ id, state })}
              isPending={
                setZoneTemperature.isPending || setZonePower.isPending
              }
            />
          ))}
        </div>
      </div>
    </div>
  )
}

function DashboardSkeleton() {
  return (
    <div className="container mx-auto p-4 space-y-6">
      <div className="flex items-center justify-between">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-6 w-32" />
      </div>

      <Card>
        <CardHeader>
          <Skeleton className="h-6 w-40" />
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <Skeleton className="h-20" />
            <Skeleton className="h-20" />
            <Skeleton className="h-20" />
            <Skeleton className="h-20" />
          </div>
        </CardContent>
      </Card>

      <div>
        <Skeleton className="h-6 w-24 mb-4" />
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <Skeleton className="h-48" />
          <Skeleton className="h-48" />
          <Skeleton className="h-48" />
        </div>
      </div>
    </div>
  )
}
