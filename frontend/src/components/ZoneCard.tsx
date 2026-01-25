import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Switch } from "@/components/ui/switch"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import { Minus, Plus, Wifi, AlertTriangle, Check, TrendingUp, TrendingDown } from "lucide-react"
import type { Zone, ZoneStatusInfo } from "@/types"

interface ZoneCardProps {
  zone: Zone
  isDisabled?: boolean
  onTemperatureChange: (id: number, temperature: number) => void
  onPowerChange: (id: number, state: "open" | "close") => void
  isPending?: boolean
  autoModeStatus?: ZoneStatusInfo | null
}

export function ZoneCard({
  zone,
  isDisabled = false,
  onTemperatureChange,
  onPowerChange,
  isPending = false,
  autoModeStatus = null,
}: ZoneCardProps) {
  const isOpen = zone.state === "open"
  const canClose = !zone.isMyZone // Cannot close myZone
  const isAutoMode = autoModeStatus !== null

  const handleTemperatureDecrease = () => {
    if (zone.setTemp > 16) {
      onTemperatureChange(zone.id, zone.setTemp - 1)
    }
  }

  const handleTemperatureIncrease = () => {
    if (zone.setTemp < 32) {
      onTemperatureChange(zone.id, zone.setTemp + 1)
    }
  }

  const handlePowerToggle = (checked: boolean) => {
    onPowerChange(zone.id, checked ? "open" : "close")
  }

  // Get status display for Auto Mode
  const getAutoModeStatusDisplay = () => {
    if (!autoModeStatus || !autoModeStatus.enabled) return null

    switch (autoModeStatus.status) {
      case "in_range":
        return {
          icon: Check,
          color: "text-green-600",
          bgColor: "bg-green-50 border-green-200",
          label: "In Range",
        }
      case "below_min":
        return {
          icon: TrendingUp,
          color: "text-blue-600",
          bgColor: "bg-blue-50 border-blue-200",
          label: "Below Min",
        }
      case "above_max":
        return {
          icon: TrendingDown,
          color: "text-orange-600",
          bgColor: "bg-orange-50 border-orange-200",
          label: "Above Max",
        }
      default:
        return null
    }
  }

  const statusDisplay = getAutoModeStatusDisplay()

  return (
    <Card
      className={cn(
        "transition-all",
        !isOpen && "opacity-60",
        isPending && "animate-pulse",
        isAutoMode && statusDisplay && statusDisplay.bgColor
      )}
    >
      <CardHeader className="pb-2">
        <div className="flex items-center justify-between">
          <CardTitle className="text-lg">{zone.name}</CardTitle>
          <div className="flex items-center gap-2">
            {/* Auto Mode Status Indicator */}
            {isAutoMode && autoModeStatus?.enabled && statusDisplay && (
              <Badge
                variant="outline"
                className={cn(
                  "text-xs",
                  autoModeStatus.status === "in_range" && "bg-green-100 text-green-800 border-green-300",
                  autoModeStatus.status === "below_min" && "bg-blue-100 text-blue-800 border-blue-300",
                  autoModeStatus.status === "above_max" && "bg-orange-100 text-orange-800 border-orange-300"
                )}
              >
                <statusDisplay.icon className="h-3 w-3 mr-1" />
                {statusDisplay.label}
              </Badge>
            )}
            {isAutoMode && autoModeStatus && !autoModeStatus.enabled && (
              <Badge variant="secondary" className="text-xs">
                Auto: Off
              </Badge>
            )}
            {zone.isMyZone && (
              <Badge variant="default" className="text-xs">
                MyZone
              </Badge>
            )}
            {zone.error && zone.error > 0 && (
              <Badge variant="destructive" className="text-xs">
                <AlertTriangle className="h-3 w-3 mr-1" />
                Error
              </Badge>
            )}
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* Current Temperature */}
        <div className="text-center">
          <span
            className={cn(
              "text-4xl font-bold",
              isAutoMode && statusDisplay && statusDisplay.color
            )}
          >
            {zone.measuredTemp.toFixed(1)}
          </span>
          <span className="text-xl text-muted-foreground">°C</span>
        </div>

        {/* Auto Mode: Show Range instead of controls */}
        {isAutoMode && autoModeStatus?.enabled ? (
          <div className="text-center p-3 bg-muted/30 rounded-lg">
            <div className="text-sm text-muted-foreground mb-1">Target Range</div>
            <div className="flex items-center justify-center gap-2">
              <span className="text-lg font-semibold text-blue-600">
                {autoModeStatus.minTemp}°C
              </span>
              <span className="text-muted-foreground">-</span>
              <span className="text-lg font-semibold text-orange-600">
                {autoModeStatus.maxTemp}°C
              </span>
            </div>
          </div>
        ) : (
          /* Manual Mode: Target Temperature Controls */
          <div className="flex items-center justify-center gap-4">
            <Button
              variant="outline"
              size="icon"
              onClick={handleTemperatureDecrease}
              disabled={isDisabled || !isOpen || zone.setTemp <= 16}
              aria-label="Decrease temperature"
            >
              <Minus className="h-4 w-4" />
            </Button>
            <div className="text-center min-w-[80px]">
              <span className="text-2xl font-semibold">{zone.setTemp}</span>
              <span className="text-sm text-muted-foreground">°C target</span>
            </div>
            <Button
              variant="outline"
              size="icon"
              onClick={handleTemperatureIncrease}
              disabled={isDisabled || !isOpen || zone.setTemp >= 32}
              aria-label="Increase temperature"
            >
              <Plus className="h-4 w-4" />
            </Button>
          </div>
        )}

        {/* Zone Power Toggle */}
        <div className="flex items-center justify-between pt-2 border-t">
          <span className="text-sm text-muted-foreground">
            {isOpen ? "Open" : "Closed"}
          </span>
          <div
            className="flex items-center gap-2"
            title={
              zone.isMyZone
                ? "Cannot close the controlling zone (MyZone)"
                : undefined
            }
          >
            <Switch
              checked={isOpen}
              onCheckedChange={handlePowerToggle}
              disabled={isDisabled || !canClose}
              aria-label={`Toggle ${zone.name} zone`}
            />
          </div>
        </div>

        {/* Signal Strength (optional) */}
        {zone.rssi !== undefined && (
          <div className="flex items-center justify-end gap-1 text-xs text-muted-foreground">
            <Wifi className="h-3 w-3" />
            <span>{zone.rssi}%</span>
          </div>
        )}
      </CardContent>
    </Card>
  )
}
