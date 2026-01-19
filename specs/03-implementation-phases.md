# Implementation Phases

## Phase 1: Dashboard MVP

Minimal full-stack application with core monitoring and control. No scheduling features.

### Scope

**Backend:**
- Spring Boot application with REST API
- SQLite database setup
- MyAir API integration (http://192.168.0.10:2025)
- Endpoints:
  - `GET /api/system/status` - System state
  - `POST /api/system/mode` - Set AC mode (cool/heat/vent/dry)
  - `POST /api/system/fan` - Set fan speed (low/medium/high/auto)
  - `POST /api/system/power` - System on/off
  - `GET /api/zones` - List zones with current state
  - `POST /api/zones/{id}/target` - Set target temperature (16-32°C)
  - `POST /api/zones/{id}/power` - Zone open/close

**Frontend:**
- React SPA with Vite
- ShadCN component library setup
- Tailwind CSS
- Dashboard page:
  - System power toggle
  - System mode selector (Cool/Heat/Vent/Dry)
  - Fan speed selector (Low/Medium/High/Auto/AutoAA)
  - Zone cards showing:
    - Zone name (Living, Guest, Upstairs - from API)
    - Current temperature (from zone sensor)
    - Target temperature control (16-32°C)
    - Zone open/close toggle

**Database:**
- Zone configuration table (id, name, myAirZoneId)
- Seed data for 3 zones

### Out of Scope for Phase 1
- Scheduling system (seasons, weekly schedules)
- Manual override with hold duration
- Temperature history logging
- History graphs
- Zone name editing UI

---

## Phase 2: Temperature History

- Periodic temperature logging (every 5 minutes)
- History API endpoints
- Temperature graphs per zone
- Time range selection (24h, 7d, 30d)

---

## Phase 3: Scheduling

- Season management (CRUD)
- Weekly schedule per season
- Per-zone time period configuration
- Schedule execution engine

---

## Phase 4: Manual Override

- Override creation with hold duration
- Override indicator in UI
- Override cancellation
- Resume schedule after override expires
