import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Switch } from "@/components/ui/switch"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import { Minus, Plus, Wifi, AlertTriangle } from "lucide-react"
import type { Zone } from "@/types"

interface ZoneCardProps {
  zone: Zone
  isDisabled?: boolean
  onTemperatureChange: (id: number, temperature: number) => void
  onPowerChange: (id: number, state: "open" | "close") => void
  isPending?: boolean
}

export function ZoneCard({
  zone,
  isDisabled = false,
  onTemperatureChange,
  onPowerChange,
  isPending = false,
}: ZoneCardProps) {
  const isOpen = zone.state === "open"
  const canClose = !zone.isMyZone // Cannot close myZone

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

  return (
    <Card
      className={cn(
        "transition-all",
        !isOpen && "opacity-60",
        isPending && "animate-pulse"
      )}
    >
      <CardHeader className="pb-2">
        <div className="flex items-center justify-between">
          <CardTitle className="text-lg">{zone.name}</CardTitle>
          <div className="flex items-center gap-2">
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
          <span className="text-4xl font-bold">
            {zone.measuredTemp.toFixed(1)}
          </span>
          <span className="text-xl text-muted-foreground">°C</span>
        </div>

        {/* Target Temperature Controls */}
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
