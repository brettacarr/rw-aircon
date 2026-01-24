# Auto Mode Specification

## Overview

Auto Mode is an intelligent climate control feature that automatically maintains zone temperatures within user-defined ranges. Rather than setting fixed temperatures, users specify minimum and maximum bounds for each zone. The system automatically switches between heating, cooling, and off states to keep all zones comfortable.

## Control Modes

The application now supports three mutually exclusive control modes:

1. **Manual Mode**: Direct user control of all settings (existing behavior)
2. **Auto Mode**: System automatically adjusts based on min/max temperature ranges (this feature)
3. **Schedule Mode**: Settings follow the configured season/schedule (existing behavior)

Only one mode can be active at a time. Switching modes preserves the configuration of other modes (e.g., switching from Auto to Manual doesn't delete the Auto mode ranges).

## Core Behavior

### Temperature Ranges

- Each **active** zone must have a minimum and maximum temperature configured
- **Valid range**: 16°C to 32°C (matching MyAir limits)
- **Minimum gap**: 2°C between min and max (e.g., min=22°C, max=24°C is valid; min=22°C, max=23°C is invalid)
- Zones that are turned off do not require ranges

### Polling & Execution

- System checks zone temperatures **every 1 minute**
- On each poll:
  1. Read current temperature for each active zone
  2. Compare against configured min/max ranges
  3. Determine required action based on myZone priority
  4. Apply changes via MyAir API

### Decision Logic

For each poll cycle:

1. **Check myZone first** (the controlling zone):
   - If myZone temp < min → Set system to **Heat**, target = min + 0.5°C
   - If myZone temp > max → Set system to **Cool**, target = max - 0.5°C
   - If myZone within range → Continue to step 2

2. **If myZone is within range**, check other active zones:
   - If any zone temp < min → Set system to **Heat**, use that zone's min + 0.5°C as target
   - If any zone temp > max → Set system to **Cool**, use that zone's max - 0.5°C as target
   - If multiple zones need adjustment, pick the one furthest outside its range
   - If zones have conflicting needs (one needs heat, another needs cool) → **Ignore conflicts**, only act on zones that align with current mode or the first zone evaluated

3. **If all zones within range** → Turn system **OFF**

4. **If system is OFF and any zone goes outside bounds** → Turn system **ON** and apply appropriate mode

### Conflict Resolution

When zones have conflicting temperature needs:

- **myZone always takes priority** - if myZone needs heating, the system heats regardless of other zones needing cooling
- **Conflicting zones are ignored** - they will be addressed once the priority zone is satisfied
- The system cannot heat and cool simultaneously; it must choose one mode

### Target Temperature Calculation

When adjusting temperature:
- **Heating**: Set target to `min + 0.5°C` (heat slightly beyond minimum to prevent immediate re-triggering)
- **Cooling**: Set target to `max - 0.5°C` (cool slightly beyond maximum to prevent immediate re-triggering)

This 0.5°C overshoot acts as hysteresis to prevent rapid cycling.

## Guest Zone Restrictions

The **Guest zone** (z02, named "Guest"):
- **Cannot be set as myZone** when Auto Mode is active
- Must be used in conjunction with other zones (single vent causes pressure issues)
- If Guest is the only active zone, Auto Mode should display a validation error
- Participates in auto adjustments like other zones, but cannot be the priority/controlling zone

## Zone Configuration

When enabling Auto Mode, the user must:

1. **Select which zones are active** (on/off toggle per zone)
2. **Set min/max range for each active zone**
3. **Ensure at least one non-Guest zone is active**

Validation rules:
- At least one zone must be active
- Active zones must have valid min/max ranges
- Guest zone cannot be the only active zone
- Min must be at least 2°C less than max
- All temperatures must be between 16°C and 32°C

## myZone Selection

In Auto Mode:
- The system automatically selects an appropriate myZone from active zones
- **Guest zone is excluded** from myZone selection
- If user has a preferred myZone, they can set it during Auto Mode configuration
- Default: Use the zone with the largest deviation from its range, excluding Guest

## User Interface

### Mode Selector

Add a prominent mode selector to the Dashboard, showing:
- **Manual** (icon: hand/slider)
- **Auto** (icon: thermometer with range indicator)
- **Schedule** (icon: calendar/clock)

Current active mode is highlighted. Tapping another mode switches to it.

### Auto Mode Configuration Panel

When Auto Mode is selected or configured:

```
┌─────────────────────────────────────────┐
│  AUTO MODE SETTINGS                     │
├─────────────────────────────────────────┤
│  ☑ Living                               │
│     Min: [20°C]  Max: [24°C]           │
│                                         │
│  ☐ Guest                                │
│     (disabled - cannot be only zone)    │
│                                         │
│  ☑ Upstairs                             │
│     Min: [18°C]  Max: [22°C]           │
│                                         │
├─────────────────────────────────────────┤
│  Priority Zone: [Upstairs ▼]            │
│  (Guest cannot be priority)             │
└─────────────────────────────────────────┘
```

### Dashboard Integration

When Auto Mode is active, the Dashboard shows:
- **Mode indicator**: "Auto Mode Active" badge/banner
- **Current action**: "Heating to 22.5°C" or "Cooling to 23.5°C" or "All zones in range - System off"
- **Per-zone status**: Show min/max range and whether zone is within range
  - Green indicator: within range
  - Blue indicator: below min (needs heating)
  - Red/Orange indicator: above max (needs cooling)

### Zone Cards in Auto Mode

Each zone card displays:
```
┌─────────────────────────┐
│ Living           [AUTO] │
│ Current: 23.5°C    ✓    │
│ Range: 20°C - 24°C      │
└─────────────────────────┘
```

The checkmark (✓) indicates within range. Show ↑ for "needs heating" or ↓ for "needs cooling".

## Data Model

### New Entity: AutoModeConfig

```
auto_mode_config
├── id (PK)
├── active (boolean) - whether auto mode is the current control mode
├── created_at (timestamp)
├── updated_at (timestamp)
└── priority_zone_id (FK to zone, nullable) - preferred myZone for auto mode
```

### New Entity: AutoModeZone

```
auto_mode_zone
├── id (PK)
├── zone_id (FK to zone)
├── enabled (boolean)
├── min_temp (decimal, 16.0-32.0)
├── max_temp (decimal, 16.0-32.0)
└── UNIQUE(zone_id)
```

### Control Mode Tracking

Add to system state tracking:
```
control_mode ENUM('manual', 'auto', 'schedule')
```

This could be stored in:
- A new `system_config` table, or
- As part of `auto_mode_config` with the `active` flag

## API Endpoints

### Auto Mode Configuration

```
GET /api/auto-mode
```
Returns current Auto Mode configuration including all zone ranges.

Response:
```json
{
  "active": true,
  "priorityZoneId": 3,
  "zones": [
    { "zoneId": 1, "zoneName": "Living", "enabled": true, "minTemp": 20.0, "maxTemp": 24.0 },
    { "zoneId": 2, "zoneName": "Guest", "enabled": false, "minTemp": null, "maxTemp": null },
    { "zoneId": 3, "zoneName": "Upstairs", "enabled": true, "minTemp": 18.0, "maxTemp": 22.0 }
  ]
}
```

```
PUT /api/auto-mode
```
Update Auto Mode configuration.

Request:
```json
{
  "priorityZoneId": 3,
  "zones": [
    { "zoneId": 1, "enabled": true, "minTemp": 20.0, "maxTemp": 24.0 },
    { "zoneId": 2, "enabled": false },
    { "zoneId": 3, "enabled": true, "minTemp": 18.0, "maxTemp": 22.0 }
  ]
}
```

```
POST /api/auto-mode/activate
```
Activate Auto Mode (deactivates Manual/Schedule modes).

```
DELETE /api/auto-mode/activate
```
Deactivate Auto Mode (returns to Manual mode).

### Control Mode

```
GET /api/control-mode
```
Returns current control mode.

Response:
```json
{
  "mode": "auto",
  "activeSince": "2024-01-15T10:30:00Z"
}
```

```
PUT /api/control-mode
```
Switch control mode.

Request:
```json
{
  "mode": "schedule"
}
```

### Auto Mode Status

```
GET /api/auto-mode/status
```
Returns current Auto Mode execution status (for "why is it heating/cooling" view).

Response:
```json
{
  "systemState": "heating",
  "targetTemp": 20.5,
  "reason": "Living zone below minimum (19.2°C < 20.0°C)",
  "triggeringZone": {
    "zoneId": 1,
    "zoneName": "Living",
    "currentTemp": 19.2,
    "minTemp": 20.0,
    "maxTemp": 24.0
  },
  "zoneStatuses": [
    { "zoneId": 1, "status": "below_min", "currentTemp": 19.2, "deviation": -0.8 },
    { "zoneId": 3, "status": "in_range", "currentTemp": 20.1, "deviation": 0 }
  ],
  "lastChecked": "2024-01-15T10:35:00Z"
}
```

## Backend Services

### AutoModeService

Responsibilities:
- Validate and store Auto Mode configuration
- Execute the polling/decision logic every minute
- Apply changes via MyAirClient
- Track execution status for the status endpoint

### AutoModeExecutionService

Scheduled service (similar to ScheduleExecutionService):
- Runs every minute: `@Scheduled(cron = "0 * * * * *")`
- Only executes when Auto Mode is active
- Implements the decision logic described above
- Logs actions for debugging/history

### Integration with Existing Services

- **OverrideService**: Manual overrides should temporarily pause Auto Mode (existing override behavior)
- **ScheduleExecutionService**: Only runs when Schedule mode is active
- **SystemService/ZoneService**: Used by AutoModeExecutionService to apply changes

## Execution Priority

When determining what controls the system:

1. **Active Override** (highest priority) - Manual override always wins
2. **Active Control Mode**:
   - If Manual: No automatic adjustments
   - If Auto: AutoModeExecutionService controls
   - If Schedule: ScheduleExecutionService controls

## Logging (Lower Priority)

### Auto Mode Action Log

Track automatic adjustments for user visibility:

```
auto_mode_log
├── id (PK)
├── timestamp
├── action ('heat_on', 'cool_on', 'system_off', 'mode_change')
├── reason (text description)
├── triggering_zone_id (FK to zone)
├── system_mode (before action)
├── new_system_mode (after action)
└── zone_temps (JSON snapshot of all zone temps at time of action)
```

API endpoint:
```
GET /api/auto-mode/log?limit=50
```

## Future Enhancements (Out of Scope for Initial Implementation)

### Schedule Integration

In a future phase, Auto Mode could be integrated with the scheduling system:
- Schedule entries could specify "auto" as the mode
- Each schedule entry with "auto" mode would have its own min/max ranges
- This allows different comfort ranges for different times of day

Example:
```
Season: Winter
Day: Monday
Period: 06:00 - 09:00 (Morning)
  Mode: Auto
  Zone 1: Min 20°C, Max 24°C
  Zone 2: Min 18°C, Max 22°C

Period: 09:00 - 17:00 (Away)
  Mode: Auto
  Zone 1: Min 16°C, Max 28°C  (wider range = energy saving)
  Zone 2: Min 16°C, Max 28°C

Period: 17:00 - 22:00 (Evening)
  Mode: Auto
  Zone 1: Min 21°C, Max 23°C  (tighter range = more comfort)
  Zone 2: Min 19°C, Max 21°C
```

## Implementation Phases

### Phase 5A: Core Auto Mode
- Database schema for auto_mode_config and auto_mode_zone
- AutoModeService and AutoModeExecutionService
- API endpoints for configuration
- Basic UI for mode selection and zone range configuration
- Integration with existing override system

### Phase 5B: UI Enhancements
- Dashboard integration showing auto mode status
- Zone cards with range display
- "Why heating/cooling" status view
- Mode indicator and current action display

### Phase 5C: Logging (Lower Priority)
- Auto mode action logging
- Log viewing UI
- Historical action review

## Validation Summary

| Rule | Validation |
|------|------------|
| Temperature range | 16°C - 32°C |
| Min/Max gap | At least 2°C |
| Active zones | At least 1 non-Guest zone |
| Guest zone | Cannot be priority/myZone |
| Guest only | Invalid configuration |
| Ranges required | All active zones must have min/max set |
