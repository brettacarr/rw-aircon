import { useState } from "react"
import {
  useSeasons,
  useSeason,
  useCreateSeason,
  useUpdateSeason,
  useDeleteSeason,
  useUpdateSchedule,
} from "@/hooks/useSeasons"
import { useSystemStatus } from "@/hooks/useSystemStatus"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Select } from "@/components/ui/select"
import { Switch } from "@/components/ui/switch"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import {
  ArrowLeft,
  Plus,
  Pencil,
  Trash2,
  Calendar,
  Clock,
  Save,
  X,
  ChevronRight,
  AlertTriangle,
} from "lucide-react"
import type {
  Season,
  ScheduleEntry,
  ScheduleEntryRequest,
  ZoneScheduleRequest,
  AcMode,
  Zone,
} from "@/types"
import { DAY_NAMES, MONTH_NAMES } from "@/types"

interface SchedulesProps {
  onBack: () => void
}

const MODE_OPTIONS: { value: AcMode; label: string }[] = [
  { value: "cool", label: "Cool" },
  { value: "heat", label: "Heat" },
  { value: "vent", label: "Vent" },
  { value: "dry", label: "Dry" },
]

export function Schedules({ onBack }: SchedulesProps) {
  const [selectedSeasonId, setSelectedSeasonId] = useState<number | null>(null)
  const [isCreatingNew, setIsCreatingNew] = useState(false)
  const [editingSeason, setEditingSeason] = useState<Season | null>(null)

  const { data: seasons, isLoading: seasonsLoading } = useSeasons()
  const { data: seasonData, isLoading: seasonLoading } = useSeason(selectedSeasonId)
  const { data: systemStatus } = useSystemStatus()

  const createSeason = useCreateSeason()
  const updateSeason = useUpdateSeason()
  const deleteSeason = useDeleteSeason()
  const updateSchedule = useUpdateSchedule()

  const zones = systemStatus?.zones ?? []

  if (seasonsLoading) {
    return <SchedulesSkeleton onBack={onBack} />
  }

  // Season list view
  if (selectedSeasonId === null && !isCreatingNew) {
    return (
      <div className="container mx-auto p-4 space-y-6">
        {/* Header */}
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" onClick={onBack} aria-label="Back">
            <ArrowLeft className="h-5 w-5" />
          </Button>
          <h1 className="text-2xl font-bold">Schedules</h1>
        </div>

        {/* Season List */}
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold">Seasons</h2>
            <Button onClick={() => setIsCreatingNew(true)}>
              <Plus className="h-4 w-4 mr-2" />
              New Season
            </Button>
          </div>

          {seasons && seasons.length > 0 ? (
            <div className="space-y-3">
              {seasons.map((season) => (
                <SeasonCard
                  key={season.id}
                  season={season}
                  onSelect={() => setSelectedSeasonId(season.id)}
                  onEdit={() => setEditingSeason(season)}
                  onDelete={() => deleteSeason.mutate(season.id)}
                  isDeleting={deleteSeason.isPending}
                />
              ))}
            </div>
          ) : (
            <Card>
              <CardContent className="py-8">
                <div className="text-center text-muted-foreground">
                  <Calendar className="h-12 w-12 mx-auto mb-4 opacity-50" />
                  <p>No seasons configured yet.</p>
                  <p className="text-sm mt-2">
                    Create a season to define schedules for different times of year.
                  </p>
                </div>
              </CardContent>
            </Card>
          )}
        </div>

        {/* Edit Season Modal */}
        {editingSeason && (
          <SeasonForm
            season={editingSeason}
            onSave={(data) => {
              updateSeason.mutate(
                { id: editingSeason.id, data },
                { onSuccess: () => setEditingSeason(null) }
              )
            }}
            onCancel={() => setEditingSeason(null)}
            isSaving={updateSeason.isPending}
          />
        )}
      </div>
    )
  }

  // Create new season form
  if (isCreatingNew) {
    return (
      <div className="container mx-auto p-4 space-y-6">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" onClick={() => setIsCreatingNew(false)} aria-label="Back">
            <ArrowLeft className="h-5 w-5" />
          </Button>
          <h1 className="text-2xl font-bold">New Season</h1>
        </div>

        <SeasonForm
          onSave={(data) => {
            createSeason.mutate(
              {
                name: data.name!,
                startMonth: data.startMonth!,
                startDay: data.startDay!,
                endMonth: data.endMonth!,
                endDay: data.endDay!,
                priority: data.priority ?? 0,
                active: data.active ?? true,
              },
              { onSuccess: () => setIsCreatingNew(false) }
            )
          }}
          onCancel={() => setIsCreatingNew(false)}
          isSaving={createSeason.isPending}
        />
      </div>
    )
  }

  // Season detail view with schedule
  return (
    <div className="container mx-auto p-4 space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Button
          variant="ghost"
          size="icon"
          onClick={() => setSelectedSeasonId(null)}
          aria-label="Back"
        >
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <div>
          <h1 className="text-2xl font-bold">
            {seasonData?.season.name ?? "Loading..."}
          </h1>
          {seasonData?.season && (
            <p className="text-muted-foreground text-sm">
              {formatDateRange(seasonData.season)}
            </p>
          )}
        </div>
        {seasonData?.season && (
          <div className="ml-auto flex items-center gap-2">
            <Badge variant={seasonData.season.active ? "default" : "secondary"}>
              {seasonData.season.active ? "Active" : "Inactive"}
            </Badge>
            <Badge variant="outline">Priority: {seasonData.season.priority}</Badge>
          </div>
        )}
      </div>

      {/* Schedule Editor */}
      {seasonLoading ? (
        <ScheduleEditorSkeleton />
      ) : seasonData && selectedSeasonId !== null ? (
        <ScheduleEditor
          schedule={seasonData.schedule}
          zones={zones}
          onSave={(entries) => {
            updateSchedule.mutate({
              seasonId: selectedSeasonId,
              data: { entries },
            })
          }}
          isSaving={updateSchedule.isPending}
        />
      ) : (
        <Card>
          <CardContent className="py-8">
            <div className="text-center text-muted-foreground">
              <AlertTriangle className="h-12 w-12 mx-auto mb-4 opacity-50" />
              <p>Failed to load season data.</p>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}

// ============ Season Card ============

interface SeasonCardProps {
  season: Season
  onSelect: () => void
  onEdit: () => void
  onDelete: () => void
  isDeleting: boolean
}

function SeasonCard({ season, onSelect, onEdit, onDelete, isDeleting }: SeasonCardProps) {
  return (
    <Card className="hover:bg-accent/50 transition-colors cursor-pointer" onClick={onSelect}>
      <CardContent className="p-4">
        <div className="flex items-center justify-between">
          <div className="flex-1">
            <div className="flex items-center gap-2">
              <h3 className="font-semibold">{season.name}</h3>
              <Badge variant={season.active ? "default" : "secondary"} className="text-xs">
                {season.active ? "Active" : "Inactive"}
              </Badge>
            </div>
            <p className="text-sm text-muted-foreground mt-1">
              {formatDateRange(season)}
            </p>
            <p className="text-xs text-muted-foreground mt-1">
              Priority: {season.priority}
            </p>
          </div>
          <div className="flex items-center gap-2" onClick={(e) => e.stopPropagation()}>
            <Button
              variant="ghost"
              size="icon"
              onClick={onEdit}
              aria-label="Edit season"
            >
              <Pencil className="h-4 w-4" />
            </Button>
            <Button
              variant="ghost"
              size="icon"
              onClick={onDelete}
              disabled={isDeleting}
              aria-label="Delete season"
            >
              <Trash2 className="h-4 w-4" />
            </Button>
            <ChevronRight className="h-5 w-5 text-muted-foreground" />
          </div>
        </div>
      </CardContent>
    </Card>
  )
}

// ============ Season Form ============

interface SeasonFormProps {
  season?: Season
  onSave: (data: Partial<Season>) => void
  onCancel: () => void
  isSaving: boolean
}

function SeasonForm({ season, onSave, onCancel, isSaving }: SeasonFormProps) {
  const [name, setName] = useState(season?.name ?? "")
  const [startMonth, setStartMonth] = useState(season?.startMonth ?? 1)
  const [startDay, setStartDay] = useState(season?.startDay ?? 1)
  const [endMonth, setEndMonth] = useState(season?.endMonth ?? 12)
  const [endDay, setEndDay] = useState(season?.endDay ?? 31)
  const [priority, setPriority] = useState(season?.priority ?? 0)
  const [active, setActive] = useState(season?.active ?? true)

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    onSave({
      name,
      startMonth,
      startDay,
      endMonth,
      endDay,
      priority,
      active,
    })
  }

  return (
    <Card>
      <CardContent className="pt-6">
        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Name */}
          <div className="space-y-2">
            <label className="text-sm font-medium">Season Name</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g., Summer, Winter"
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
              required
            />
          </div>

          {/* Date Range */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <label className="text-sm font-medium">Start Date</label>
              <div className="flex gap-2">
                <Select
                  value={String(startMonth)}
                  onValueChange={(v) => setStartMonth(Number(v))}
                  className="flex-1"
                >
                  {MONTH_NAMES.map((month, i) => (
                    <option key={i + 1} value={i + 1}>
                      {month}
                    </option>
                  ))}
                </Select>
                <input
                  type="number"
                  min={1}
                  max={31}
                  value={startDay}
                  onChange={(e) => setStartDay(Number(e.target.value))}
                  className="w-20 rounded-md border border-input bg-background px-3 py-2 text-sm"
                />
              </div>
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">End Date</label>
              <div className="flex gap-2">
                <Select
                  value={String(endMonth)}
                  onValueChange={(v) => setEndMonth(Number(v))}
                  className="flex-1"
                >
                  {MONTH_NAMES.map((month, i) => (
                    <option key={i + 1} value={i + 1}>
                      {month}
                    </option>
                  ))}
                </Select>
                <input
                  type="number"
                  min={1}
                  max={31}
                  value={endDay}
                  onChange={(e) => setEndDay(Number(e.target.value))}
                  className="w-20 rounded-md border border-input bg-background px-3 py-2 text-sm"
                />
              </div>
            </div>
          </div>

          {/* Priority and Active */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <label className="text-sm font-medium">Priority</label>
              <input
                type="number"
                value={priority}
                onChange={(e) => setPriority(Number(e.target.value))}
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
              />
              <p className="text-xs text-muted-foreground">
                Higher priority seasons override lower ones when dates overlap.
              </p>
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Active</label>
              <div className="flex items-center gap-2 h-10">
                <Switch checked={active} onCheckedChange={setActive} />
                <span className="text-sm">{active ? "Enabled" : "Disabled"}</span>
              </div>
            </div>
          </div>

          {/* Actions */}
          <div className="flex justify-end gap-2 pt-4">
            <Button type="button" variant="outline" onClick={onCancel}>
              <X className="h-4 w-4 mr-2" />
              Cancel
            </Button>
            <Button type="submit" disabled={isSaving}>
              <Save className="h-4 w-4 mr-2" />
              {isSaving ? "Saving..." : "Save"}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  )
}

// ============ Schedule Editor ============

interface ScheduleEditorProps {
  schedule: ScheduleEntry[]
  zones: Zone[]
  onSave: (entries: ScheduleEntryRequest[]) => void
  isSaving: boolean
}

function ScheduleEditor({ schedule, zones, onSave, isSaving }: ScheduleEditorProps) {
  const [entries, setEntries] = useState<ScheduleEntryRequest[]>(() =>
    schedule.map((e) => ({
      dayOfWeek: e.dayOfWeek,
      startTime: e.startTime,
      endTime: e.endTime,
      mode: e.mode,
      zoneSettings: e.zoneSettings.map((z) => ({
        zoneId: z.zoneId,
        targetTemp: z.targetTemp,
        enabled: z.enabled,
      })),
    }))
  )
  const [selectedDay, setSelectedDay] = useState(1)
  const [hasChanges, setHasChanges] = useState(false)

  const dayEntries = entries.filter((e) => e.dayOfWeek === selectedDay)

  const addEntry = () => {
    const newEntry: ScheduleEntryRequest = {
      dayOfWeek: selectedDay,
      startTime: "08:00",
      endTime: "17:00",
      mode: "cool",
      zoneSettings: zones.map((z) => ({
        zoneId: z.id,
        targetTemp: 22,
        enabled: true,
      })),
    }
    setEntries([...entries, newEntry])
    setHasChanges(true)
  }

  const updateEntry = (index: number, updates: Partial<ScheduleEntryRequest>) => {
    // Find the actual global index for the nth entry of the selected day
    let count = 0
    let globalIndex = -1
    for (let idx = 0; idx < entries.length; idx++) {
      if (entries[idx].dayOfWeek === selectedDay) {
        if (count === index) {
          globalIndex = idx
          break
        }
        count++
      }
    }

    if (globalIndex >= 0) {
      const newEntries = [...entries]
      newEntries[globalIndex] = { ...newEntries[globalIndex], ...updates }
      setEntries(newEntries)
      setHasChanges(true)
    }
  }

  const removeEntry = (index: number) => {
    let count = 0
    let globalIndex = -1
    for (let i = 0; i < entries.length; i++) {
      if (entries[i].dayOfWeek === selectedDay) {
        if (count === index) {
          globalIndex = i
          break
        }
        count++
      }
    }

    if (globalIndex >= 0) {
      const newEntries = entries.filter((_, i) => i !== globalIndex)
      setEntries(newEntries)
      setHasChanges(true)
    }
  }

  const handleSave = () => {
    onSave(entries)
    setHasChanges(false)
  }

  return (
    <div className="space-y-4">
      {/* Day Selector */}
      <Card>
        <CardContent className="p-4">
          <div className="flex flex-wrap gap-2">
            {DAY_NAMES.map((day, i) => {
              const dayNum = i + 1
              const hasEntries = entries.some((e) => e.dayOfWeek === dayNum)
              return (
                <Button
                  key={day}
                  variant={selectedDay === dayNum ? "default" : "outline"}
                  size="sm"
                  onClick={() => setSelectedDay(dayNum)}
                  className="relative"
                >
                  {day.slice(0, 3)}
                  {hasEntries && (
                    <span className="absolute -top-1 -right-1 h-2 w-2 bg-primary rounded-full" />
                  )}
                </Button>
              )
            })}
          </div>
        </CardContent>
      </Card>

      {/* Time Periods for Selected Day */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="text-lg">
              <Clock className="h-5 w-5 inline-block mr-2" />
              {DAY_NAMES[selectedDay - 1]} Schedule
            </CardTitle>
            <Button size="sm" onClick={addEntry}>
              <Plus className="h-4 w-4 mr-2" />
              Add Period
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          {dayEntries.length === 0 ? (
            <div className="text-center text-muted-foreground py-8">
              <Clock className="h-12 w-12 mx-auto mb-4 opacity-50" />
              <p>No time periods configured for {DAY_NAMES[selectedDay - 1]}.</p>
              <p className="text-sm mt-2">Add a period to define temperature settings for this day.</p>
            </div>
          ) : (
            <div className="space-y-4">
              {dayEntries.map((entry, index) => (
                <TimePeriodEditor
                  key={index}
                  entry={entry}
                  zones={zones}
                  onChange={(updates) => updateEntry(index, updates)}
                  onRemove={() => removeEntry(index)}
                />
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Save Button */}
      <div className="flex justify-end">
        <Button onClick={handleSave} disabled={!hasChanges || isSaving}>
          <Save className="h-4 w-4 mr-2" />
          {isSaving ? "Saving..." : "Save Schedule"}
        </Button>
      </div>
    </div>
  )
}

// ============ Time Period Editor ============

interface TimePeriodEditorProps {
  entry: ScheduleEntryRequest
  zones: Zone[]
  onChange: (updates: Partial<ScheduleEntryRequest>) => void
  onRemove: () => void
}

function TimePeriodEditor({ entry, zones, onChange, onRemove }: TimePeriodEditorProps) {
  const updateZoneSetting = (zoneId: number, updates: Partial<ZoneScheduleRequest>) => {
    const newSettings = entry.zoneSettings.map((z) =>
      z.zoneId === zoneId ? { ...z, ...updates } : z
    )
    onChange({ zoneSettings: newSettings })
  }

  return (
    <Card className="bg-muted/50">
      <CardContent className="p-4 space-y-4">
        {/* Time Range and Mode */}
        <div className="flex flex-wrap items-center gap-4">
          <div className="flex items-center gap-2">
            <input
              type="time"
              value={entry.startTime}
              onChange={(e) => onChange({ startTime: e.target.value })}
              className="rounded-md border border-input bg-background px-3 py-2 text-sm"
            />
            <span className="text-muted-foreground">to</span>
            <input
              type="time"
              value={entry.endTime}
              onChange={(e) => onChange({ endTime: e.target.value })}
              className="rounded-md border border-input bg-background px-3 py-2 text-sm"
            />
          </div>
          <Select
            value={entry.mode}
            onValueChange={(v) => onChange({ mode: v as AcMode })}
            className="w-32"
          >
            {MODE_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </Select>
          <Button variant="ghost" size="icon" onClick={onRemove} className="ml-auto">
            <Trash2 className="h-4 w-4" />
          </Button>
        </div>

        {/* Zone Settings */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {zones.map((zone) => {
            const setting = entry.zoneSettings.find((z) => z.zoneId === zone.id)
            if (!setting) return null

            return (
              <div
                key={zone.id}
                className="flex items-center justify-between p-3 rounded-md bg-background border"
              >
                <div className="flex items-center gap-3">
                  <Switch
                    checked={setting.enabled}
                    onCheckedChange={(enabled) => updateZoneSetting(zone.id, { enabled })}
                  />
                  <span className="font-medium">{zone.name}</span>
                </div>
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="icon"
                    className="h-8 w-8"
                    disabled={!setting.enabled || setting.targetTemp <= 16}
                    onClick={() =>
                      updateZoneSetting(zone.id, { targetTemp: setting.targetTemp - 1 })
                    }
                  >
                    -
                  </Button>
                  <span className="w-12 text-center font-mono">
                    {setting.enabled ? `${setting.targetTemp}°` : "-"}
                  </span>
                  <Button
                    variant="outline"
                    size="icon"
                    className="h-8 w-8"
                    disabled={!setting.enabled || setting.targetTemp >= 32}
                    onClick={() =>
                      updateZoneSetting(zone.id, { targetTemp: setting.targetTemp + 1 })
                    }
                  >
                    +
                  </Button>
                </div>
              </div>
            )
          })}
        </div>
      </CardContent>
    </Card>
  )
}

// ============ Helper Functions ============

function formatDateRange(season: Season): string {
  const startMonth = MONTH_NAMES[season.startMonth - 1]
  const endMonth = MONTH_NAMES[season.endMonth - 1]
  return `${startMonth} ${season.startDay} - ${endMonth} ${season.endDay}`
}

// ============ Skeletons ============

function SchedulesSkeleton({ onBack }: { onBack: () => void }) {
  return (
    <div className="container mx-auto p-4 space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={onBack}>
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <Skeleton className="h-8 w-32" />
      </div>
      <div className="space-y-3">
        <Skeleton className="h-24" />
        <Skeleton className="h-24" />
        <Skeleton className="h-24" />
      </div>
    </div>
  )
}

function ScheduleEditorSkeleton() {
  return (
    <div className="space-y-4">
      <Skeleton className="h-16" />
      <Skeleton className="h-64" />
    </div>
  )
}
