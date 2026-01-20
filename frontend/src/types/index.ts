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
