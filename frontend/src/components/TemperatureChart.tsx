import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  ReferenceLine,
} from "recharts"
import type { TemperatureLogEntry, SystemLogEntry } from "@/types"
import { useMemo } from "react"

interface ChartDataPoint {
  timestamp: string
  displayTime: string
  currentTemp: number
  targetTemp: number
  zoneEnabled: boolean
  outdoorTemp?: number | null
  systemOn?: boolean
}

interface TemperatureChartProps {
  data: TemperatureLogEntry[]
  systemData?: SystemLogEntry[]
  showOutdoorTemp?: boolean
  zoneName?: string
}

/**
 * Formats an ISO timestamp for display on the chart axis.
 * Adapts format based on data density.
 */
function formatTimestamp(timestamp: string, showDate: boolean): string {
  const date = new Date(timestamp)
  if (showDate) {
    return date.toLocaleDateString(undefined, {
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    })
  }
  return date.toLocaleTimeString(undefined, {
    hour: "2-digit",
    minute: "2-digit",
  })
}

/**
 * Temperature chart for displaying zone history.
 * Shows current temperature (solid line) and target temperature (dashed line).
 * Optionally overlays outdoor temperature from system history.
 */
export function TemperatureChart({
  data,
  systemData,
  showOutdoorTemp = false,
  zoneName = "Zone",
}: TemperatureChartProps) {
  // Merge zone and system data, determining if we should show dates on X-axis
  const chartData = useMemo(() => {
    if (data.length === 0) {
      return []
    }

    // Determine time span to decide on axis formatting
    const firstTime = new Date(data[0].timestamp).getTime()
    const lastTime = new Date(data[data.length - 1].timestamp).getTime()
    const hoursDiff = (lastTime - firstTime) / (1000 * 60 * 60)
    const showDates = hoursDiff > 24

    // Create a map of system data by timestamp for quick lookup
    const systemMap = new Map<string, SystemLogEntry>()
    if (systemData) {
      systemData.forEach((entry) => {
        // Round to nearest 5 minutes for matching
        const date = new Date(entry.timestamp)
        date.setMinutes(Math.round(date.getMinutes() / 5) * 5, 0, 0)
        systemMap.set(date.toISOString(), entry)
      })
    }

    const chartData: ChartDataPoint[] = data.map((entry) => {
      // Try to find matching system entry
      const entryDate = new Date(entry.timestamp)
      entryDate.setMinutes(Math.round(entryDate.getMinutes() / 5) * 5, 0, 0)
      const systemEntry = systemMap.get(entryDate.toISOString())

      return {
        timestamp: entry.timestamp,
        displayTime: formatTimestamp(entry.timestamp, showDates),
        currentTemp: entry.currentTemp,
        targetTemp: entry.targetTemp,
        zoneEnabled: entry.zoneEnabled,
        outdoorTemp: systemEntry?.outdoorTemp,
        systemOn: systemEntry?.systemOn,
      }
    })

    return chartData
  }, [data, systemData])

  if (chartData.length === 0) {
    return (
      <div className="flex items-center justify-center h-64 text-muted-foreground">
        No temperature data available for this time range
      </div>
    )
  }

  return (
    <ResponsiveContainer width="100%" height={350}>
      <LineChart
        data={chartData}
        margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
      >
        <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
        <XAxis
          dataKey="displayTime"
          tick={{ fontSize: 12 }}
          interval="preserveStartEnd"
          minTickGap={50}
        />
        <YAxis
          domain={["auto", "auto"]}
          tick={{ fontSize: 12 }}
          tickFormatter={(value) => `${value}°`}
          label={{
            value: "Temperature (°C)",
            angle: -90,
            position: "insideLeft",
            style: { textAnchor: "middle", fontSize: 12 },
          }}
        />
        <Tooltip
          formatter={(value, name) => {
            if (value === undefined || value === null) return ["-", name]
            const labels: Record<string, string> = {
              currentTemp: "Current",
              targetTemp: "Target",
              outdoorTemp: "Outdoor",
            }
            const numValue = typeof value === "number" ? value : Number(value)
            return [`${numValue.toFixed(1)}°C`, labels[String(name)] || String(name)]
          }}
          labelFormatter={(label) => `Time: ${label}`}
          contentStyle={{
            backgroundColor: "hsl(var(--card))",
            borderColor: "hsl(var(--border))",
            borderRadius: "var(--radius)",
          }}
        />
        <Legend
          formatter={(value) => {
            const labels: Record<string, string> = {
              currentTemp: `${zoneName} Current`,
              targetTemp: `${zoneName} Target`,
              outdoorTemp: "Outdoor",
            }
            return labels[value] || value
          }}
        />
        {/* Reference lines for comfortable temperature range */}
        <ReferenceLine
          y={20}
          stroke="hsl(var(--muted-foreground))"
          strokeDasharray="3 3"
          strokeOpacity={0.5}
        />
        <ReferenceLine
          y={24}
          stroke="hsl(var(--muted-foreground))"
          strokeDasharray="3 3"
          strokeOpacity={0.5}
        />
        {/* Current temperature - solid line */}
        <Line
          type="monotone"
          dataKey="currentTemp"
          stroke="hsl(var(--primary))"
          strokeWidth={2}
          dot={false}
          activeDot={{ r: 4 }}
        />
        {/* Target temperature - dashed line */}
        <Line
          type="stepAfter"
          dataKey="targetTemp"
          stroke="hsl(var(--destructive))"
          strokeWidth={2}
          strokeDasharray="5 5"
          dot={false}
          activeDot={{ r: 4 }}
        />
        {/* Outdoor temperature - optional overlay */}
        {showOutdoorTemp && (
          <Line
            type="monotone"
            dataKey="outdoorTemp"
            stroke="hsl(var(--warning))"
            strokeWidth={1.5}
            dot={false}
            activeDot={{ r: 3 }}
            connectNulls
          />
        )}
      </LineChart>
    </ResponsiveContainer>
  )
}
