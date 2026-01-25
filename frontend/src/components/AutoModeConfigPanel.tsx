import { useState, useMemo, useCallback } from "react"
import { useAutoModeConfig, useUpdateAutoModeConfig } from "@/hooks/useAutoMode"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Switch } from "@/components/ui/switch"
import { Select } from "@/components/ui/select"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { AlertTriangle, Save, Thermometer, Minus, Plus, RotateCcw } from "lucide-react"
import type { AutoModeZoneRequest } from "@/types"

const MIN_TEMP = 16
const MAX_TEMP = 32
const MIN_GAP = 2
const GUEST_ZONE_ID = 2

interface ZoneEdit {
  enabled?: boolean
  minTemp?: number
  maxTemp?: number
}

interface ConfigEdits {
  priorityZoneId?: number | null
  zones: Record<number, ZoneEdit>
}

/**
 * Configuration panel for Auto Mode settings
 * Allows setting min/max temperatures for each zone and selecting priority zone
 */
export function AutoModeConfigPanel() {
  const { data: config, isLoading } = useAutoModeConfig()
  const updateConfig = useUpdateAutoModeConfig()

  // Track edits as deltas from the original config
  const [edits, setEdits] = useState<ConfigEdits>({ zones: {} })

  // Compute effective values by merging config with edits
  const effectiveConfig = useMemo(() => {
    if (!config) return null

    return {
      priorityZoneId: edits.priorityZoneId !== undefined
        ? edits.priorityZoneId
        : config.priorityZoneId,
      zones: config.zones.map((z) => ({
        zoneId: z.zoneId,
        zoneName: z.zoneName,
        enabled: edits.zones[z.zoneId]?.enabled ?? z.enabled,
        minTemp: edits.zones[z.zoneId]?.minTemp ?? z.minTemp,
        maxTemp: edits.zones[z.zoneId]?.maxTemp ?? z.maxTemp,
      })),
    }
  }, [config, edits])

  const isDirty = useMemo(() => {
    if (edits.priorityZoneId !== undefined) return true
    return Object.keys(edits.zones).length > 0
  }, [edits])

  const resetEdits = useCallback(() => {
    setEdits({ zones: {} })
  }, [])

  if (isLoading || !config || !effectiveConfig) {
    return (
      <Card>
        <CardHeader>
          <Skeleton className="h-6 w-48" />
        </CardHeader>
        <CardContent className="space-y-4">
          <Skeleton className="h-24" />
          <Skeleton className="h-24" />
          <Skeleton className="h-24" />
        </CardContent>
      </Card>
    )
  }

  // Validation
  const getValidationErrors = (): string[] => {
    const errors: string[] = []
    const zones = effectiveConfig.zones
    const priorityZoneId = effectiveConfig.priorityZoneId

    // Check at least one non-Guest zone is enabled
    const enabledNonGuestZones = zones.filter(
      (z) => z.enabled && z.zoneId !== GUEST_ZONE_ID
    )
    if (enabledNonGuestZones.length === 0) {
      errors.push("At least one non-Guest zone must be enabled")
    }

    // Check Guest is not the only enabled zone
    const enabledZones = zones.filter((z) => z.enabled)
    if (
      enabledZones.length === 1 &&
      enabledZones[0].zoneId === GUEST_ZONE_ID
    ) {
      errors.push("Guest zone cannot be the only enabled zone")
    }

    // Check priority zone is not Guest
    if (priorityZoneId === GUEST_ZONE_ID) {
      errors.push("Guest zone cannot be set as priority zone")
    }

    // Check priority zone is enabled if set
    if (priorityZoneId !== null) {
      const priorityZone = zones.find((z) => z.zoneId === priorityZoneId)
      if (priorityZone && !priorityZone.enabled) {
        errors.push("Priority zone must be enabled")
      }
    }

    // Check temperature ranges
    for (const zone of zones) {
      if (zone.enabled) {
        if (zone.minTemp < MIN_TEMP || zone.maxTemp > MAX_TEMP) {
          errors.push(`${zone.zoneName}: Temperature must be ${MIN_TEMP}-${MAX_TEMP}°C`)
        }
        if (zone.maxTemp - zone.minTemp < MIN_GAP) {
          errors.push(`${zone.zoneName}: Min/Max gap must be at least ${MIN_GAP}°C`)
        }
      }
    }

    return errors
  }

  const validationErrors = getValidationErrors()
  const isValid = validationErrors.length === 0

  // Handlers
  const handleZoneToggle = (zoneId: number, enabled: boolean) => {
    setEdits((prev) => ({
      ...prev,
      zones: {
        ...prev.zones,
        [zoneId]: { ...prev.zones[zoneId], enabled },
      },
    }))
  }

  const handleMinTempChange = (zoneId: number, delta: number) => {
    const zone = effectiveConfig.zones.find((z) => z.zoneId === zoneId)
    if (!zone) return
    const newMin = Math.max(MIN_TEMP, Math.min(zone.maxTemp - MIN_GAP, zone.minTemp + delta))
    setEdits((prev) => ({
      ...prev,
      zones: {
        ...prev.zones,
        [zoneId]: { ...prev.zones[zoneId], minTemp: newMin },
      },
    }))
  }

  const handleMaxTempChange = (zoneId: number, delta: number) => {
    const zone = effectiveConfig.zones.find((z) => z.zoneId === zoneId)
    if (!zone) return
    const newMax = Math.min(MAX_TEMP, Math.max(zone.minTemp + MIN_GAP, zone.maxTemp + delta))
    setEdits((prev) => ({
      ...prev,
      zones: {
        ...prev.zones,
        [zoneId]: { ...prev.zones[zoneId], maxTemp: newMax },
      },
    }))
  }

  const handlePriorityChange = (value: string) => {
    const newPriority = value === "auto" ? null : parseInt(value, 10)
    setEdits((prev) => ({
      ...prev,
      priorityZoneId: newPriority,
    }))
  }

  const handleSave = () => {
    const request: { priorityZoneId?: number | null; zones: AutoModeZoneRequest[] } = {
      priorityZoneId: effectiveConfig.priorityZoneId,
      zones: effectiveConfig.zones.map((z) => ({
        zoneId: z.zoneId,
        enabled: z.enabled,
        minTemp: z.minTemp,
        maxTemp: z.maxTemp,
      })),
    }
    updateConfig.mutate(request, {
      onSuccess: () => resetEdits(),
    })
  }

  // Get eligible priority zones (enabled, non-Guest)
  const eligiblePriorityZones = effectiveConfig.zones.filter(
    (z) => z.enabled && z.zoneId !== GUEST_ZONE_ID
  )

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Thermometer className="h-5 w-5" />
          Auto Mode Configuration
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* Validation Errors */}
        {validationErrors.length > 0 && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-3">
            <div className="flex items-center gap-2 text-red-800 font-medium mb-2">
              <AlertTriangle className="h-4 w-4" />
              Configuration Issues
            </div>
            <ul className="list-disc list-inside text-sm text-red-700 space-y-1">
              {validationErrors.map((err, idx) => (
                <li key={idx}>{err}</li>
              ))}
            </ul>
          </div>
        )}

        {/* Zone Configuration */}
        <div className="space-y-4">
          <h3 className="font-medium text-sm text-muted-foreground">Zone Temperature Ranges</h3>
          {effectiveConfig.zones.map((zone) => (
            <div
              key={zone.zoneId}
              className={`border rounded-lg p-4 ${
                zone.enabled ? "bg-background" : "bg-muted/50"
              }`}
            >
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-3">
                  <Switch
                    checked={zone.enabled}
                    onCheckedChange={(checked) =>
                      handleZoneToggle(zone.zoneId, checked)
                    }
                    aria-label={`Enable ${zone.zoneName} for Auto Mode`}
                  />
                  <span className="font-medium">{zone.zoneName}</span>
                  {zone.zoneId === GUEST_ZONE_ID && (
                    <Badge variant="secondary" className="text-xs">
                      Guest
                    </Badge>
                  )}
                  {zone.zoneId === effectiveConfig.priorityZoneId && (
                    <Badge variant="default" className="text-xs">
                      Priority
                    </Badge>
                  )}
                </div>
              </div>

              {zone.enabled && (
                <div className="grid grid-cols-2 gap-4 mt-3">
                  {/* Min Temperature */}
                  <div className="space-y-1">
                    <label className="text-sm text-muted-foreground">Minimum</label>
                    <div className="flex items-center gap-2">
                      <Button
                        variant="outline"
                        size="icon"
                        className="h-8 w-8"
                        onClick={() => handleMinTempChange(zone.zoneId, -1)}
                        disabled={zone.minTemp <= MIN_TEMP}
                        aria-label="Decrease minimum temperature"
                      >
                        <Minus className="h-3 w-3" />
                      </Button>
                      <span className="font-medium w-12 text-center">
                        {zone.minTemp}°C
                      </span>
                      <Button
                        variant="outline"
                        size="icon"
                        className="h-8 w-8"
                        onClick={() => handleMinTempChange(zone.zoneId, 1)}
                        disabled={zone.minTemp >= zone.maxTemp - MIN_GAP}
                        aria-label="Increase minimum temperature"
                      >
                        <Plus className="h-3 w-3" />
                      </Button>
                    </div>
                  </div>

                  {/* Max Temperature */}
                  <div className="space-y-1">
                    <label className="text-sm text-muted-foreground">Maximum</label>
                    <div className="flex items-center gap-2">
                      <Button
                        variant="outline"
                        size="icon"
                        className="h-8 w-8"
                        onClick={() => handleMaxTempChange(zone.zoneId, -1)}
                        disabled={zone.maxTemp <= zone.minTemp + MIN_GAP}
                        aria-label="Decrease maximum temperature"
                      >
                        <Minus className="h-3 w-3" />
                      </Button>
                      <span className="font-medium w-12 text-center">
                        {zone.maxTemp}°C
                      </span>
                      <Button
                        variant="outline"
                        size="icon"
                        className="h-8 w-8"
                        onClick={() => handleMaxTempChange(zone.zoneId, 1)}
                        disabled={zone.maxTemp >= MAX_TEMP}
                        aria-label="Increase maximum temperature"
                      >
                        <Plus className="h-3 w-3" />
                      </Button>
                    </div>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>

        {/* Priority Zone Selector */}
        <div className="space-y-2">
          <label className="font-medium text-sm">Priority Zone</label>
          <p className="text-sm text-muted-foreground">
            The priority zone controls system heating/cooling decisions. Guest zone cannot be priority.
          </p>
          <Select
            value={effectiveConfig.priorityZoneId?.toString() ?? "auto"}
            onValueChange={handlePriorityChange}
            disabled={eligiblePriorityZones.length === 0}
          >
            <option value="auto">Automatic (system chooses)</option>
            {eligiblePriorityZones.map((zone) => (
              <option key={zone.zoneId} value={zone.zoneId.toString()}>
                {zone.zoneName}
              </option>
            ))}
          </Select>
        </div>

        {/* Action Buttons */}
        <div className="flex justify-end gap-2">
          {isDirty && (
            <Button variant="outline" onClick={resetEdits}>
              <RotateCcw className="h-4 w-4 mr-2" />
              Reset
            </Button>
          )}
          <Button
            onClick={handleSave}
            disabled={!isDirty || !isValid || updateConfig.isPending}
          >
            <Save className="h-4 w-4 mr-2" />
            {updateConfig.isPending ? "Saving..." : "Save Configuration"}
          </Button>
        </div>

        {/* Success/Error Messages */}
        {updateConfig.isSuccess && !isDirty && (
          <p className="text-sm text-green-600">Configuration saved successfully.</p>
        )}
        {updateConfig.isError && (
          <p className="text-sm text-red-600">
            Failed to save configuration. Please try again.
          </p>
        )}
      </CardContent>
    </Card>
  )
}
