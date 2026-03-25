import { useAutoModeStatus, useDeactivateAutoMode } from "@/hooks/useAutoMode"
import { useControlMode } from "@/hooks/useControlMode"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Thermometer, TrendingUp, TrendingDown, Check, X } from "lucide-react"

/**
 * Banner component that displays when Auto Mode is active.
 * Shows current system action: heating, cooling, or holding (all zones in range).
 * The system never turns off in auto mode — it holds at the boundary.
 */
export function AutoModeBanner() {
  const { data: controlMode, isLoading: isModeLoading } = useControlMode()
  const { data: status, isLoading: isStatusLoading } = useAutoModeStatus(
    controlMode?.mode === "auto"
  )
  const deactivateAutoMode = useDeactivateAutoMode()

  // Only show when Auto Mode is active
  if (isModeLoading || controlMode?.mode !== "auto") {
    return null
  }

  if (isStatusLoading || !status) {
    return (
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 mb-4 animate-pulse">
        <div className="h-5 bg-blue-100 rounded w-48"></div>
      </div>
    )
  }

  // Determine if the system is holding (all zones in range)
  const isHolding = status.reason?.includes("holding") || (
    status.systemState !== "heating" && status.systemState !== "cooling" && !status.targetTemp
  )

  // Get icon and color based on system state
  const getStateDisplay = () => {
    switch (status.systemState) {
      case "heating":
        return {
          icon: TrendingUp,
          color: "text-orange-600",
          bgColor: "bg-orange-50 border-orange-200",
          badgeColor: "bg-orange-100 text-orange-800 border-orange-300",
          label: `Heating to ${status.targetTemp}°C`,
        }
      case "cooling":
        return {
          icon: TrendingDown,
          color: "text-blue-600",
          bgColor: "bg-blue-50 border-blue-200",
          badgeColor: "bg-blue-100 text-blue-800 border-blue-300",
          label: `Cooling to ${status.targetTemp}°C`,
        }
      default:
        if (isHolding) {
          return {
            icon: Check,
            color: "text-green-600",
            bgColor: "bg-green-50 border-green-200",
            badgeColor: "bg-green-100 text-green-800 border-green-300",
            label: "All zones in range - Holding",
          }
        }
        return null
    }
  }

  const stateDisplay = getStateDisplay()

  const bgColor = stateDisplay?.bgColor ?? "bg-green-50 border-green-200"
  const badgeColor = stateDisplay?.badgeColor ?? "bg-green-100 text-green-800 border-green-300"

  return (
    <div
      className={`${bgColor} border rounded-lg p-3 mb-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3`}
    >
      <div className="flex items-center gap-3">
        <Badge variant="outline" className={badgeColor}>
          <Thermometer className="h-3.5 w-3.5 mr-1" />
          Auto Mode
        </Badge>
        {stateDisplay && (
          <>
            <div className="flex items-center gap-2">
              <stateDisplay.icon className={`h-4 w-4 ${stateDisplay.color}`} />
              <span className={`text-sm font-medium ${stateDisplay.color}`}>
                {stateDisplay.label}
              </span>
            </div>
            {status.triggeringZone && (
              <span className="text-sm text-muted-foreground">
                ({status.triggeringZone.zoneName}: {status.triggeringZone.currentTemp.toFixed(1)}°C)
              </span>
            )}
          </>
        )}
      </div>
      <Button
        variant="outline"
        size="sm"
        onClick={() => deactivateAutoMode.mutate()}
        disabled={deactivateAutoMode.isPending}
        className="border-gray-300 hover:bg-gray-100"
      >
        <X className="h-4 w-4 mr-1" />
        {deactivateAutoMode.isPending ? "Stopping..." : "Stop Auto"}
      </Button>
    </div>
  )
}
