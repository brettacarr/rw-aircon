# MyAir API Integration Specification

## Connection Details

- **Base URL:** `http://192.168.0.10:2025`
- **Protocol:** HTTP GET with JSON
- **Authentication:** None
- **Network:** Local network only

## Endpoints

### Get System Data

```
GET /getSystemData
```

Returns complete system state as JSON.

### Set Aircon

```
GET /setAircon?json={...}
```

Sends commands via URL-encoded JSON parameter. After a valid command, the API returns empty `{}` until the state change is confirmed by hardware (up to 4 seconds).

## Response Structure

```json
{
  "aircons": {
    "ac1": {
      "info": {
        "state": "on",           // "on" or "off"
        "mode": "cool",          // "heat", "cool", "vent", "dry"
        "fan": "autoAA",         // "low", "medium", "high", "auto", "autoAA"
        "setTemp": 24.0,         // 16-32
        "myZone": 3,             // 0 = disabled, 1-10 = zone number
        "name": "AC",
        "noOfZones": 3,
        "countDownToOff": 0,     // minutes until auto-off (0 = disabled, max 720)
        "countDownToOn": 0,      // minutes until auto-on (0 = disabled, max 720)
        "freshAirStatus": "none", // "on", "off", or "none"
        "constant1": 0,          // readonly - constant zone settings
        "constant2": 0,
        "constant3": 0
      },
      "zones": {
        "z01": {
          "name": "Living",
          "state": "close",      // "open" or "close"
          "type": 1,             // 0 = percentage control, >0 = temperature control
          "value": 100,          // 5-100 in increments of 5 (when type = 0)
          "setTemp": 19.0,       // 16-32 (when type > 0)
          "measuredTemp": 25.0,  // current temperature reading from zone sensor
          "number": 1,
          "rssi": 47,            // signal strength
          "maxDamper": 95,
          "minDamper": 0
        },
        "z02": { ... },
        "z03": { ... }
      }
    }
  },
  "system": {
    "name": "MyPlace",
    "needsUpdate": false,
    "noOfAircons": 1,            // 0-4 aircon units supported
    "sysType": "MyAir5",
    "tspIp": "192.168.0.10"
  }
}
```

## Zone Control Types

The `type` field determines how a zone is controlled:

- **type = 0:** Percentage-based control. Use `value` field (5-100, increments of 5).
- **type > 0:** Temperature-based control. Use `setTemp` field (16-32).

The zone `state` field controls whether the zone is active:
- `"open"` - Zone is active/enabled
- `"close"` - Zone is closed/disabled

## Command Examples

### System Commands

**Turn system on:**
```
/setAircon?json={"ac1":{"info":{"state":"on"}}}
```

**Turn system off:**
```
/setAircon?json={"ac1":{"info":{"state":"off"}}}
```

**Set mode to cool:**
```
/setAircon?json={"ac1":{"info":{"mode":"cool"}}}
```

**Set mode to heat:**
```
/setAircon?json={"ac1":{"info":{"mode":"heat"}}}
```

**Set fan speed:**
```
/setAircon?json={"ac1":{"info":{"fan":"high"}}}
```

**Combined - turn on and set to cool:**
```
/setAircon?json={"ac1":{"info":{"state":"on","mode":"cool"}}}
```

### Zone Commands

**Open zone 2:**
```
/setAircon?json={"ac1":{"zones":{"z02":{"state":"open"}}}}
```

**Close zone 2:**
```
/setAircon?json={"ac1":{"zones":{"z02":{"state":"close"}}}}
```

**Set zone temperature (for type > 0):**
```
/setAircon?json={"ac1":{"zones":{"z02":{"setTemp":24}}}}
```

**Set zone percentage (for type = 0):**
```
/setAircon?json={"ac1":{"zones":{"z02":{"value":80}}}}
```

### Combined Commands

**System and zone in one request:**
```
/setAircon?json={"ac1":{"info":{"state":"on","mode":"cool"},"zones":{"z01":{"state":"open"}}}}
```

## Zone Mapping (Confirmed from Live System)

| Zone ID | Name     | Type | Control Method     |
|---------|----------|------|--------------------|
| z01     | Living   | 1    | Temperature (16-32°C) |
| z02     | Guest    | 1    | Temperature (16-32°C) |
| z03     | Upstairs | 1    | Temperature (16-32°C) |

All zones use temperature-based control (type=1), not percentage.

## Temperature Constraints

- **Minimum:** 16°C
- **Maximum:** 32°C
- **Unit:** Celsius

## Implementation Notes

1. **Polling:** The API is read-only for state; poll `/getSystemData` periodically to get current temperatures and state.

2. **Command Confirmation:** After sending a command, the API returns `{}` until hardware confirms the change (up to 4 seconds). Re-poll `/getSystemData` to confirm the new state.

3. **Single Aircon:** This system has 1 aircon unit (`ac1`). The API supports up to 4 (`ac1`-`ac4`).

4. **MyZone Feature:** If `myZone` is set to a zone number (1-10), that zone controls the system temperature and cannot be closed. When `myZone = 0`, the system uses return air temperature.

5. **URL Encoding:** The JSON in the `setAircon` endpoint should be URL-encoded when sent via HTTP.
