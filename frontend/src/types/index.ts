// AC Mode types
export type AcMode = "cool" | "heat" | "vent" | "dry"

// Fan speed types
export type FanSpeed = "low" | "medium" | "high" | "auto" | "autoAA"

// Zone state types
export type ZoneState = "open" | "close"

// Power state types
export type PowerState = "on" | "off"

// Zone response from API
export interface Zone {
  id: number
  name: string
  myAirZoneId: string
  state: ZoneState
  type: number
  value: number
  setTemp: number
  measuredTemp: number
  rssi?: number
  error?: number
  isMyZone: boolean
}

// System status response from API
export interface SystemStatus {
  state: PowerState
  mode: AcMode
  fan: FanSpeed
  setTemp: number
  myZone: number
  outdoorTemp?: number
  isValidOutdoorTemp?: boolean
  filterCleanStatus?: number
  airconErrorCode?: string
  zones: Zone[]
}

// Health check response
export interface HealthStatus {
  status: "ok" | "degraded"
  myairConnected: boolean
  lastSuccessfulPoll?: string
}

// API error response
export interface ApiError {
  code: string
  message: string
}

// Request types
export interface SystemPowerRequest {
  state: PowerState
}

export interface SystemModeRequest {
  mode: AcMode
}

export interface FanSpeedRequest {
  fan: FanSpeed
}

export interface SystemTemperatureRequest {
  temperature: number
}

export interface MyZoneRequest {
  zone: number
}

export interface ZoneTargetRequest {
  temperature: number
}

export interface ZonePowerRequest {
  state: ZoneState
}

// History types
export interface TemperatureLogEntry {
  timestamp: string
  currentTemp: number
  targetTemp: number
  zoneEnabled: boolean
}

export interface SystemLogEntry {
  timestamp: string
  mode: AcMode
  outdoorTemp: number | null
  systemOn: boolean
}

export interface ZoneHistoryResponse {
  zoneId: number
  zoneName: string
  from: string
  to: string
  aggregated: boolean
  data: TemperatureLogEntry[]
}

export interface SystemHistoryResponse {
  from: string
  to: string
  data: SystemLogEntry[]
}

export type TimeRange = "24h" | "7d" | "30d" | "custom"

// ============ Schedule Types ============

// Season response from API
export interface Season {
  id: number
  name: string
  startMonth: number
  startDay: number
  endMonth: number
  endDay: number
  priority: number
  active: boolean
}

// Season with full schedule
export interface SeasonWithSchedule {
  season: Season
  schedule: ScheduleEntry[]
}

// Schedule entry (time period within a day)
export interface ScheduleEntry {
  id: number
  seasonId: number
  dayOfWeek: number // 1=Monday, 7=Sunday
  startTime: string // HH:MM
  endTime: string // HH:MM
  mode: AcMode
  zoneSettings: ZoneScheduleSetting[]
}

// Per-zone settings for a schedule entry
export interface ZoneScheduleSetting {
  id: number
  zoneId: number
  zoneName: string
  targetTemp: number
  enabled: boolean
}

// Request types for creating/updating seasons
export interface SeasonCreateRequest {
  name: string
  startMonth: number
  startDay: number
  endMonth: number
  endDay: number
  priority?: number
  active?: boolean
}

export interface SeasonUpdateRequest {
  name?: string
  startMonth?: number
  startDay?: number
  endMonth?: number
  endDay?: number
  priority?: number
  active?: boolean
}

// Request types for schedule entries
export interface ScheduleEntryRequest {
  dayOfWeek: number
  startTime: string
  endTime: string
  mode: AcMode
  zoneSettings: ZoneScheduleRequest[]
}

export interface ZoneScheduleRequest {
  zoneId: number
  targetTemp: number
  enabled: boolean
}

export interface FullScheduleUpdateRequest {
  entries: ScheduleEntryRequest[]
}

// Day of week helper
export const DAY_NAMES = [
  "Monday",
  "Tuesday",
  "Wednesday",
  "Thursday",
  "Friday",
  "Saturday",
  "Sunday",
] as const

export const MONTH_NAMES = [
  "January",
  "February",
  "March",
  "April",
  "May",
  "June",
  "July",
  "August",
  "September",
  "October",
  "November",
  "December",
] as const

// ============ Override Types ============

// Override duration options
export type OverrideDuration = "1h" | "2h" | "4h" | "until_next"

// Override response from API
export interface Override {
  id: number
  createdAt: string // ISO timestamp
  expiresAt: string // ISO timestamp
  mode: AcMode | null
  systemTemp: number | null
  zoneOverrides: ZoneOverride[]
  remainingMinutes: number
}

// Per-zone override settings
export interface ZoneOverride {
  zoneId: number
  temp: number | null
  enabled: boolean | null
}

// Request to create a new override
export interface OverrideCreateRequest {
  duration: OverrideDuration
  mode?: AcMode | null
  systemTemp?: number | null
  zoneOverrides?: ZoneOverrideRequest[] | null
}

// Per-zone override request
export interface ZoneOverrideRequest {
  zoneId: number
  temp?: number | null
  enabled?: boolean | null
}
