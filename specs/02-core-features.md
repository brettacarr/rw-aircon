# Core Features Specification

## Temperature Units

All temperatures are displayed and stored in **Celsius**.

## Zones

The system manages **3 zones** (as configured in MyAir):
1. Living (z01)
2. Guest (z02)
3. Upstairs (z03)

Zone names are read from the MyAir API.

Each zone has:
- Name (from MyAir API)
- Current temperature (`measuredTemp` from zone sensor)
- Target temperature (`setTemp`, 16-32°C)
- State (`open` or `close`)

## AC Modes

The application controls the following AC modes:
- Cool
- Heat
- Vent (fan only)
- Dry (if supported by unit)

Mode is set system-wide (not per-zone).

## Fan Speed

System-wide fan speed control:
- Low
- Medium
- High
- Auto
- AutoAA (Advantage Air intelligent auto mode)

## Zone Control Types

Zones can be configured in two ways (determined by installer, read from API):

1. **Percentage Control** (`type = 0`): Airflow controlled by percentage (5-100%, increments of 5)
2. **Temperature Control** (`type > 0`): Zone has temperature sensor, controlled by target temperature (16-32°C)

**This system:** All 3 zones use temperature control (type=1) with temperature sensors.

The UI should detect the zone type and show appropriate controls.

## Dashboard View

Primary interface showing:
- System on/off state
- System mode (cool/heat/vent/dry)
- Fan speed (low/medium/high/auto)
- Per-zone display:
  - Zone name
  - Current temperature (if zone has sensor)
  - Target temperature or percentage (based on zone type)
  - Zone open/close toggle
- Quick controls for mode, fan speed, and master on/off

## Manual Override

When a user manually adjusts temperature or mode outside the schedule:
- Override holds the setting for a user-specified duration
- Default hold options: 1 hour, 2 hours, 4 hours, until next scheduled change
- Override indicator shown in UI
- Override can be cancelled to resume schedule

## Scheduling System

### Seasons

- User-defined seasons (not limited to 4)
- Each season has:
  - Name (e.g., "Summer", "Winter", "Shoulder")
  - Start date (month/day)
  - End date (month/day)
  - Active flag
- System uses the currently active season's schedule
- Seasons can overlap; first matching season by priority wins

### Weekly Schedule

Each season contains a weekly schedule:
- 7 days (Monday through Sunday)
- Each day has multiple time periods
- Time periods do not overlap within a day

### Time Periods

Each time period defines:
- Start time (HH:MM)
- End time (HH:MM)
- System mode (cool/heat/fan/auto/off)
- Per-zone settings:
  - Target temperature
  - Zone on/off

Example schedule entry:
```
Season: Summer
Day: Monday
Period: 06:00 - 08:00
  Mode: Cool
  Zone 1: 22°C, On
  Zone 2: 24°C, On
  Zone 3: Off
```

## Temperature History

- Log temperature readings periodically (e.g., every 5 minutes)
- Store per-zone: timestamp, current temp, target temp, zone state
- Store system-wide: mode, outdoor temp (if available)
- Retention: configurable (default 90 days)
- Graphs:
  - Per-zone temperature over time
  - Selectable time range (24h, 7d, 30d, custom)
  - Overlay target temp on graph

## Data Model Summary

### Entities

- **Zone**: id, name, myAirZoneId
- **Season**: id, name, startDate, endDate, priority, active
- **ScheduleEntry**: id, seasonId, dayOfWeek, startTime, endTime, mode
- **ZoneSchedule**: id, scheduleEntryId, zoneId, targetTemp, enabled
- **TemperatureLog**: id, timestamp, zoneId, currentTemp, targetTemp, zoneEnabled
- **SystemLog**: id, timestamp, mode, outdoorTemp, systemOn
- **Override**: id, createdAt, expiresAt, mode, zoneOverrides (JSON)

## API Endpoints (Backend)

### System
- `GET /api/system/status` - Current system state from MyAir
- `POST /api/system/mode` - Set AC mode
- `POST /api/system/fan` - Set fan speed
- `POST /api/system/power` - Turn system on/off

### Zones
- `GET /api/zones` - List zones with current state
- `POST /api/zones/{id}/target` - Set target temperature
- `POST /api/zones/{id}/power` - Turn zone on/off

### Schedules
- `GET /api/seasons` - List all seasons
- `POST /api/seasons` - Create season
- `PUT /api/seasons/{id}` - Update season
- `DELETE /api/seasons/{id}` - Delete season
- `GET /api/seasons/{id}/schedule` - Get schedule for season
- `PUT /api/seasons/{id}/schedule` - Update schedule for season

### Override
- `GET /api/override` - Get current override (if any)
- `POST /api/override` - Create override
- `DELETE /api/override` - Cancel override

### History
- `GET /api/history/zones/{id}?from=&to=` - Temperature history for zone
- `GET /api/history/system?from=&to=` - System history
