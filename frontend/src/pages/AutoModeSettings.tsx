import { AutoModeConfigPanel } from "@/components/AutoModeConfigPanel"
import { useControlMode } from "@/hooks/useControlMode"
import { useActivateAutoMode, useDeactivateAutoMode, useAutoModeStatus } from "@/hooks/useAutoMode"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { ArrowLeft, Play, Square, Thermometer, TrendingUp, TrendingDown, Check } from "lucide-react"

interface AutoModeSettingsProps {
  onBack?: () => void
}

/**
 * Auto Mode settings page
 * Allows configuring and activating/deactivating Auto Mode
 */
export function AutoModeSettings({ onBack }: AutoModeSettingsProps) {
  const { data: controlMode } = useControlMode()
  const { data: status } = useAutoModeStatus(controlMode?.mode === "auto")
  const activateAutoMode = useActivateAutoMode()
  const deactivateAutoMode = useDeactivateAutoMode()

  const isAutoModeActive = controlMode?.mode === "auto"

  // Get status display info
  const getStatusInfo = () => {
    if (!isAutoModeActive || !status) return null

    switch (status.systemState) {
      case "heating":
        return {
          icon: TrendingUp,
          label: `Heating to ${status.targetTemp}°C`,
          color: "text-orange-600",
          bgColor: "bg-orange-50",
        }
      case "cooling":
        return {
          icon: TrendingDown,
          label: `Cooling to ${status.targetTemp}°C`,
          color: "text-blue-600",
          bgColor: "bg-blue-50",
        }
      default:
        return {
          icon: Check,
          label: "All zones in range - System off",
          color: "text-green-600",
          bgColor: "bg-green-50",
        }
    }
  }

  const statusInfo = getStatusInfo()

  return (
    <div className="container mx-auto p-4 space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        {onBack && (
          <Button variant="ghost" size="icon" onClick={onBack} aria-label="Back to Dashboard">
            <ArrowLeft className="h-5 w-5" />
          </Button>
        )}
        <div className="flex-1">
          <h1 className="text-2xl font-bold flex items-center gap-2">
            <Thermometer className="h-6 w-6" />
            Auto Mode Settings
          </h1>
          <p className="text-muted-foreground">
            Configure automatic temperature control for your zones
          </p>
        </div>
      </div>

      {/* Status Card - shows when Auto Mode is active */}
      {isAutoModeActive && statusInfo && (
        <Card className={statusInfo.bgColor}>
          <CardContent className="pt-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <Badge variant="default" className="bg-green-600">
                  Active
                </Badge>
                <div className="flex items-center gap-2">
                  <statusInfo.icon className={`h-5 w-5 ${statusInfo.color}`} />
                  <span className={`font-medium ${statusInfo.color}`}>
                    {statusInfo.label}
                  </span>
                </div>
                {status?.triggeringZone && status.systemState !== "off" && (
                  <span className="text-sm text-muted-foreground">
                    ({status.triggeringZone.zoneName}: {status.triggeringZone.currentTemp.toFixed(1)}°C)
                  </span>
                )}
              </div>
              <Button
                variant="outline"
                onClick={() => deactivateAutoMode.mutate()}
                disabled={deactivateAutoMode.isPending}
              >
                <Square className="h-4 w-4 mr-2" />
                {deactivateAutoMode.isPending ? "Stopping..." : "Stop Auto Mode"}
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Activation Card - shows when Auto Mode is not active */}
      {!isAutoModeActive && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Start Auto Mode</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-center justify-between">
              <p className="text-muted-foreground">
                Auto Mode will automatically maintain your zones within the configured temperature ranges.
              </p>
              <Button
                onClick={() => activateAutoMode.mutate()}
                disabled={activateAutoMode.isPending}
              >
                <Play className="h-4 w-4 mr-2" />
                {activateAutoMode.isPending ? "Starting..." : "Start Auto Mode"}
              </Button>
            </div>
            {activateAutoMode.isError && (
              <p className="text-sm text-red-600 mt-2">
                Failed to start Auto Mode. Make sure at least one non-Guest zone is configured.
              </p>
            )}
          </CardContent>
        </Card>
      )}

      {/* Zone Status - shows when Auto Mode is active */}
      {isAutoModeActive && status && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Zone Status</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid gap-3">
              {status.zoneStatuses.map((zone) => (
                <div
                  key={zone.zoneId}
                  className={`flex items-center justify-between p-3 rounded-lg border ${
                    !zone.enabled
                      ? "bg-muted/50"
                      : zone.status === "in_range"
                      ? "bg-green-50 border-green-200"
                      : zone.status === "below_min"
                      ? "bg-blue-50 border-blue-200"
                      : "bg-orange-50 border-orange-200"
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <span className="font-medium">{zone.zoneName}</span>
                    {!zone.enabled && (
                      <Badge variant="secondary" className="text-xs">
                        Disabled
                      </Badge>
                    )}
                  </div>
                  {zone.enabled && (
                    <div className="flex items-center gap-4 text-sm">
                      <span>
                        Current: <strong>{zone.currentTemp.toFixed(1)}°C</strong>
                      </span>
                      <span className="text-muted-foreground">
                        Range: {zone.minTemp}°C - {zone.maxTemp}°C
                      </span>
                      <Badge
                        variant={
                          zone.status === "in_range"
                            ? "default"
                            : zone.status === "below_min"
                            ? "secondary"
                            : "warning"
                        }
                        className={
                          zone.status === "in_range"
                            ? "bg-green-600"
                            : zone.status === "below_min"
                            ? "bg-blue-600 text-white"
                            : ""
                        }
                      >
                        {zone.status === "in_range" && "In Range"}
                        {zone.status === "below_min" && "Below Min"}
                        {zone.status === "above_max" && "Above Max"}
                      </Badge>
                    </div>
                  )}
                </div>
              ))}
            </div>
            <p className="text-xs text-muted-foreground mt-3">
              Last checked: {new Date(status.lastChecked).toLocaleTimeString()}
            </p>
          </CardContent>
        </Card>
      )}

      {/* Configuration Panel */}
      <AutoModeConfigPanel />
    </div>
  )
}
