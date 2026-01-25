import { useState } from "react"
import { useAutoModeLogs } from "@/hooks/useAutoMode"
import type { AutoModeAction, AutoModeLogEntry } from "@/types"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Select } from "@/components/ui/select"
import { RefreshCw, Flame, Snowflake, Power, Settings } from "lucide-react"

/**
 * AutoModeLogViewer displays a history of automatic actions taken by Auto Mode.
 * Shows when the system switched between heating, cooling, and off states,
 * including the reason for each action and zone temperature snapshots.
 */
export function AutoModeLogViewer() {
  const [limit, setLimit] = useState(25)
  const [actionFilter, setActionFilter] = useState<string>("all")

  const { data, isLoading, error, refetch, isFetching } = useAutoModeLogs(
    limit,
    actionFilter === "all" ? undefined : actionFilter
  )

  // Format timestamp for display
  const formatTime = (timestamp: string): string => {
    const date = new Date(timestamp)
    return date.toLocaleString()
  }

  // Get icon for action type
  const getActionIcon = (action: AutoModeAction) => {
    switch (action) {
      case "heat_on":
        return <Flame className="h-4 w-4 text-orange-500" />
      case "cool_on":
        return <Snowflake className="h-4 w-4 text-blue-500" />
      case "system_off":
        return <Power className="h-4 w-4 text-gray-500" />
      case "mode_change":
        return <Settings className="h-4 w-4 text-purple-500" />
    }
  }

  // Get human-readable action label
  const getActionLabel = (action: AutoModeAction): string => {
    switch (action) {
      case "heat_on":
        return "Heating Started"
      case "cool_on":
        return "Cooling Started"
      case "system_off":
        return "System Off"
      case "mode_change":
        return "Mode Changed"
    }
  }

  // Get background color class for action type
  const getActionBgClass = (action: AutoModeAction): string => {
    switch (action) {
      case "heat_on":
        return "bg-orange-50 border-orange-200"
      case "cool_on":
        return "bg-blue-50 border-blue-200"
      case "system_off":
        return "bg-gray-50 border-gray-200"
      case "mode_change":
        return "bg-purple-50 border-purple-200"
    }
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle className="text-lg">Auto Mode Activity Log</CardTitle>
          <div className="flex items-center gap-2">
            <Select
              value={actionFilter}
              onValueChange={setActionFilter}
              className="w-[140px]"
            >
              <option value="all">All Actions</option>
              <option value="heat_on">Heating</option>
              <option value="cool_on">Cooling</option>
              <option value="system_off">System Off</option>
              <option value="mode_change">Mode Change</option>
            </Select>
            <Select
              value={limit.toString()}
              onValueChange={(value) => setLimit(parseInt(value))}
              className="w-[80px]"
            >
              <option value="10">10</option>
              <option value="25">25</option>
              <option value="50">50</option>
              <option value="100">100</option>
            </Select>
            <Button
              variant="outline"
              size="icon"
              onClick={() => refetch()}
              disabled={isFetching}
            >
              <RefreshCw
                className={`h-4 w-4 ${isFetching ? "animate-spin" : ""}`}
              />
            </Button>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        {isLoading && (
          <div className="text-center py-8 text-muted-foreground">
            Loading activity log...
          </div>
        )}

        {error && (
          <div className="text-center py-8 text-destructive">
            Failed to load activity log
          </div>
        )}

        {data && data.logs.length === 0 && (
          <div className="text-center py-8 text-muted-foreground">
            No activity recorded yet. Auto Mode will log actions here when it
            automatically adjusts the system.
          </div>
        )}

        {data && data.logs.length > 0 && (
          <div className="space-y-3">
            {data.logs.map((entry: AutoModeLogEntry) => (
              <LogEntry key={entry.id} entry={entry} />
            ))}
            {data.total > limit && (
              <div className="text-center text-sm text-muted-foreground pt-2">
                Showing {data.logs.length} of {data.total} entries
              </div>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  )

  function LogEntry({ entry }: { entry: AutoModeLogEntry }) {
    const [expanded, setExpanded] = useState(false)

    return (
      <div
        className={`border rounded-lg p-3 ${getActionBgClass(entry.action)} cursor-pointer`}
        onClick={() => setExpanded(!expanded)}
      >
        <div className="flex items-start gap-3">
          <div className="mt-1">{getActionIcon(entry.action)}</div>
          <div className="flex-1 min-w-0">
            <div className="flex items-center justify-between gap-2">
              <span className="font-medium text-sm">
                {getActionLabel(entry.action)}
              </span>
              <span className="text-xs text-muted-foreground whitespace-nowrap">
                {formatTime(entry.timestamp)}
              </span>
            </div>
            <p className="text-sm text-muted-foreground mt-1">{entry.reason}</p>
            {entry.triggeringZoneName && (
              <p className="text-xs text-muted-foreground mt-1">
                Triggered by: {entry.triggeringZoneName}
              </p>
            )}
            {expanded && entry.zoneTemps && entry.zoneTemps.length > 0 && (
              <div className="mt-3 pt-2 border-t border-current/10">
                <p className="text-xs font-medium mb-2">
                  Zone temperatures at time of action:
                </p>
                <div className="grid gap-1">
                  {entry.zoneTemps.map((zone) => (
                    <div
                      key={zone.zoneId}
                      className="text-xs flex justify-between"
                    >
                      <span>{zone.zoneName}</span>
                      <span className="text-muted-foreground">
                        {zone.currentTemp.toFixed(1)}°C (range:{" "}
                        {zone.minTemp.toFixed(0)}-{zone.maxTemp.toFixed(0)}°C)
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
        {entry.zoneTemps && entry.zoneTemps.length > 0 && (
          <div className="text-xs text-center mt-2 text-muted-foreground">
            {expanded ? "Click to collapse" : "Click for details"}
          </div>
        )}
      </div>
    )
  }
}
