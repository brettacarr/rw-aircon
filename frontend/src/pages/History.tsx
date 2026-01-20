import { useState, useMemo } from "react"
import { useZoneHistory, useSystemHistory } from "@/hooks/useHistory"
import { useSystemStatus } from "@/hooks/useSystemStatus"
import { TemperatureChart } from "@/components/TemperatureChart"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Select } from "@/components/ui/select"
import { Switch } from "@/components/ui/switch"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import {
  ArrowLeft,
  RefreshCw,
  AlertTriangle,
  ChartLine,
  Thermometer,
} from "lucide-react"
import type { TimeRange } from "@/types"

const TIME_RANGE_OPTIONS: { value: TimeRange; label: string }[] = [
  { value: "24h", label: "Last 24 Hours" },
  { value: "7d", label: "Last 7 Days" },
  { value: "30d", label: "Last 30 Days" },
]

/**
 * Calculates the from/to timestamps based on the selected time range.
 */
function getTimeRangeBounds(range: TimeRange): { from: string; to: string } {
  const now = new Date()
  const to = now.toISOString()

  let from: Date
  switch (range) {
    case "24h":
      from = new Date(now.getTime() - 24 * 60 * 60 * 1000)
      break
    case "7d":
      from = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
      break
    case "30d":
      from = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000)
      break
    default:
      from = new Date(now.getTime() - 24 * 60 * 60 * 1000)
  }

  return { from: from.toISOString(), to }
}

interface HistoryPageProps {
  onBack: () => void
}

export function History({ onBack }: HistoryPageProps) {
  const { data: systemStatus, isLoading: isSystemLoading } = useSystemStatus()

  // State for controls
  const [selectedZoneId, setSelectedZoneId] = useState<number>(1)
  const [timeRange, setTimeRange] = useState<TimeRange>("24h")
  const [showOutdoorTemp, setShowOutdoorTemp] = useState(false)

  // Calculate time bounds
  const { from, to } = useMemo(() => getTimeRangeBounds(timeRange), [timeRange])

  // Fetch history data
  const {
    data: zoneHistory,
    isLoading: isZoneHistoryLoading,
    isError: isZoneHistoryError,
    error: zoneHistoryError,
    refetch: refetchZoneHistory,
  } = useZoneHistory(selectedZoneId, from, to)

  const { data: systemHistory, isLoading: isSystemHistoryLoading } =
    useSystemHistory(from, to, showOutdoorTemp)

  // Get zone options from system status
  const zoneOptions = useMemo(() => {
    if (!systemStatus?.zones) return []
    return systemStatus.zones.map((zone) => ({
      value: zone.id,
      label: zone.name,
    }))
  }, [systemStatus?.zones])

  // Selected zone name for chart
  const selectedZoneName = useMemo(() => {
    const zone = systemStatus?.zones.find((z) => z.id === selectedZoneId)
    return zone?.name ?? "Zone"
  }, [systemStatus?.zones, selectedZoneId])

  const isLoading =
    isSystemLoading || isZoneHistoryLoading || (showOutdoorTemp && isSystemHistoryLoading)

  if (isSystemLoading) {
    return <HistorySkeleton onBack={onBack} />
  }

  return (
    <div className="container mx-auto p-4 space-y-6">
      {/* Header with back navigation */}
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={onBack} aria-label="Back to dashboard">
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <h1 className="text-2xl font-bold flex items-center gap-2">
          <ChartLine className="h-6 w-6" />
          Temperature History
        </h1>
      </div>

      {/* Controls Card */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Chart Options</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {/* Zone Selector */}
            <div className="space-y-2">
              <label className="text-sm font-medium flex items-center gap-2">
                <Thermometer className="h-4 w-4" />
                Zone
              </label>
              <Select
                value={String(selectedZoneId)}
                onValueChange={(value) => setSelectedZoneId(Number(value))}
              >
                {zoneOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </Select>
            </div>

            {/* Time Range Selector */}
            <div className="space-y-2">
              <label className="text-sm font-medium">Time Range</label>
              <Select
                value={timeRange}
                onValueChange={(value) => setTimeRange(value as TimeRange)}
              >
                {TIME_RANGE_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </Select>
            </div>

            {/* Outdoor Temperature Toggle */}
            <div className="space-y-2">
              <label className="text-sm font-medium">Show Outdoor Temp</label>
              <div className="flex items-center gap-2 h-10">
                <Switch
                  checked={showOutdoorTemp}
                  onCheckedChange={setShowOutdoorTemp}
                  aria-label="Show outdoor temperature"
                />
                <span className="text-sm text-muted-foreground">
                  {showOutdoorTemp ? "On" : "Off"}
                </span>
              </div>
            </div>

            {/* Refresh Button */}
            <div className="space-y-2">
              <label className="text-sm font-medium">&nbsp;</label>
              <Button
                variant="outline"
                onClick={() => refetchZoneHistory()}
                disabled={isLoading}
                className="w-full"
              >
                <RefreshCw className={`h-4 w-4 mr-2 ${isLoading ? "animate-spin" : ""}`} />
                Refresh
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Chart Card */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="text-lg">
              {selectedZoneName} Temperature
            </CardTitle>
            <div className="flex items-center gap-2">
              {zoneHistory?.aggregated && (
                <Badge variant="secondary">Hourly Averages</Badge>
              )}
              {isLoading && <Badge variant="outline">Loading...</Badge>}
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {isZoneHistoryError ? (
            <div className="flex flex-col items-center justify-center h-64 gap-4">
              <AlertTriangle className="h-8 w-8 text-destructive" />
              <p className="text-muted-foreground">
                {zoneHistoryError?.message || "Failed to load temperature history"}
              </p>
              <Button variant="outline" onClick={() => refetchZoneHistory()}>
                <RefreshCw className="h-4 w-4 mr-2" />
                Retry
              </Button>
            </div>
          ) : isZoneHistoryLoading ? (
            <div className="h-[350px] flex items-center justify-center">
              <Skeleton className="h-full w-full" />
            </div>
          ) : (
            <TemperatureChart
              data={zoneHistory?.data ?? []}
              systemData={showOutdoorTemp ? systemHistory?.data : undefined}
              showOutdoorTemp={showOutdoorTemp}
              zoneName={selectedZoneName}
            />
          )}
        </CardContent>
      </Card>

      {/* Data Info */}
      {zoneHistory && (
        <div className="text-sm text-muted-foreground text-center">
          Showing {zoneHistory.data.length} data points from{" "}
          {new Date(zoneHistory.from).toLocaleString()} to{" "}
          {new Date(zoneHistory.to).toLocaleString()}
          {zoneHistory.aggregated && " (hourly averages)"}
        </div>
      )}
    </div>
  )
}

function HistorySkeleton({ onBack }: { onBack: () => void }) {
  return (
    <div className="container mx-auto p-4 space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={onBack} aria-label="Back to dashboard">
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <Skeleton className="h-8 w-48" />
      </div>

      <Card>
        <CardHeader>
          <Skeleton className="h-6 w-32" />
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <Skeleton className="h-16" />
            <Skeleton className="h-16" />
            <Skeleton className="h-16" />
            <Skeleton className="h-16" />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <Skeleton className="h-6 w-40" />
        </CardHeader>
        <CardContent>
          <Skeleton className="h-[350px]" />
        </CardContent>
      </Card>
    </div>
  )
}
