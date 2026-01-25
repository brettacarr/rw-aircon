import { useOverride, useCancelOverride } from "@/hooks/useOverride"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Clock, X } from "lucide-react"

/**
 * Banner component that displays when an override is active
 * Shows remaining time and provides a cancel button
 */
export function OverrideBanner() {
  const { data: override, isLoading } = useOverride()
  const cancelOverride = useCancelOverride()

  // Don't render anything if no override or still loading
  if (isLoading || !override) {
    return null
  }

  // Format remaining time
  const formatRemainingTime = (minutes: number): string => {
    if (minutes < 1) {
      return "less than a minute"
    }
    if (minutes < 60) {
      return `${minutes} minute${minutes === 1 ? "" : "s"}`
    }
    const hours = Math.floor(minutes / 60)
    const remainingMins = minutes % 60
    if (remainingMins === 0) {
      return `${hours} hour${hours === 1 ? "" : "s"}`
    }
    return `${hours}h ${remainingMins}m`
  }

  // Build description of what's being overridden
  const getOverrideDescription = (): string => {
    const parts: string[] = []
    if (override.mode) {
      parts.push(`Mode: ${override.mode}`)
    }
    if (override.systemTemp !== null) {
      parts.push(`Temp: ${override.systemTemp}°C`)
    }
    if (override.zoneOverrides && override.zoneOverrides.length > 0) {
      parts.push(`${override.zoneOverrides.length} zone${override.zoneOverrides.length === 1 ? "" : "s"}`)
    }
    return parts.length > 0 ? parts.join(" • ") : "Manual settings active"
  }

  return (
    <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 mb-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
      <div className="flex items-center gap-3">
        <Badge variant="warning" className="bg-amber-100 text-amber-800 border-amber-300">
          <Clock className="h-3.5 w-3.5 mr-1" />
          Override Active
        </Badge>
        <span className="text-sm text-amber-800">
          {getOverrideDescription()} — expires in {formatRemainingTime(override.remainingMinutes)}
        </span>
      </div>
      <Button
        variant="outline"
        size="sm"
        onClick={() => cancelOverride.mutate()}
        disabled={cancelOverride.isPending}
        className="border-amber-300 text-amber-800 hover:bg-amber-100"
      >
        <X className="h-4 w-4 mr-1" />
        {cancelOverride.isPending ? "Cancelling..." : "Resume Schedule"}
      </Button>
    </div>
  )
}
