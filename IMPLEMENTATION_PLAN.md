# Implementation Plan

## Quick Start

This is a greenfield project - no source code exists yet. Implementation proceeds in order:

1. **Phase 1.1-1.5** - Backend (Kotlin/Spring Boot, SQLite, MyAir API client)
2. **Phase 1.6-1.10** - Frontend (React/TypeScript, Vite, ShadCN, Tailwind)
3. **Phase 1.11-1.12** - Testing & Polish
4. **Phase 2** - Temperature History (logging, graphs)
5. **Phase 3** - Scheduling (seasons, weekly schedules)
6. **Phase 4** - Manual Override (hold duration, resume schedule)

**Start here:** Phase 1.1 - Create `backend/` directory structure and Gradle build files.

---

## Project Status: Phase 1.1-1.5 Complete (Backend)

**Last Updated:** 2026-01-20
**Status:** Backend implementation complete. Ready for frontend development.
**Next Action:** Begin Phase 1.6 - Frontend Project Setup

Backend foundation is complete with Kotlin/Spring Boot REST API, MyAir client integration, and SQLite database.

### Verification Performed
- Backend builds successfully with `./gradlew build`
- Specs verified: 01-technical-stack.md, 02-core-features.md, 03-implementation-phases.md, 04-myair-api.md
- API sample data verified: docs/myapi-response.json matches expected structure
- Note: `git push` requires SSH key setup (user to configure)

### Key Directories
- `backend/` - Kotlin + Spring Boot REST API (created)
- `frontend/` - React + TypeScript SPA (to be created)
- `backend/data/` - SQLite database storage (created at runtime)

---

## Specification Notes & Clarifications

### Endpoint Naming (Resolved)
The `specs/02-core-features.md` defines the authoritative endpoint names:
- `POST /api/zones/{id}/target` - Set target temperature
- `POST /api/zones/{id}/power` - Turn zone on/off

~~Note: `specs/03-implementation-phases.md` incorrectly references `/temperature` and `/state`.~~ **FIXED:** specs/03 has been updated to use correct naming.

### Additional API Fields Discovered
The actual MyAir API response (`docs/myapi-response.json`) includes valuable fields beyond the spec:

**System-level (`aircons.ac1.info`):**
- `filterCleanStatus` (0=clean, >0=needs cleaning) - filter maintenance indicator
- `airconErrorCode` (string) - error reporting
- `aaAutoFanModeEnabled`, `climateControlModeEnabled`, `climateControlModeIsRunning`
- `countDownToOff` / `countDownToOn` - timer support (0-720 minutes)

**System-level (`system`):**
- `suburbTemp` (outdoor temperature, e.g., 18.4)
- `isValidSuburbTemp` (boolean) - indicates if outdoor temp reading is valid

**Per-zone (`aircons.ac1.zones.z0X`):**
- `error` (0=ok, >0=sensor error) - sensor health monitoring
- `rssi` (signal strength, e.g., 47-63) - for diagnostics
- `tempSensorClash` (boolean) - sensor conflict indicator
- `motion` / `motionConfig` - motion detection support
- `maxDamper` / `minDamper` - damper limits

**Native Scheduling (`myScenes`):**
- The API supports native scenes with time-based triggers
- Scenes include `startTime` (minutes from midnight), `airconStopTime`, `activeDays` (bitmask)
- Evaluate for Phase 3 as alternative to custom scheduling

---

## Priority Summary

| Priority | Phase | Description | Status |
|----------|-------|-------------|--------|
| 1 | 1.1-1.5 | Backend setup, MyAir client, REST API | Complete |
| 2 | 1.6-1.10 | Frontend setup, Dashboard, Zone cards | Not Started |
| 3 | 1.11-1.12 | Testing & polish | Not Started |
| 4 | 2.1-2.4 | Temperature history logging & graphs | Not Started |
| 5 | 3.1-3.5 | Season-based scheduling system | Not Started |
| 6 | 4.1-4.4 | Manual override with hold duration | Not Started |

---

## Phase 1: Dashboard MVP (Priority: HIGH)

### 1.1 Backend Project Setup
- [x] Create `backend/` directory structure:
  ```
  backend/
  ├── build.gradle.kts
  ├── settings.gradle.kts
  └── src/
      ├── main/
      │   ├── kotlin/com/rw/aircon/
      │   │   ├── RwAirconApplication.kt
      │   │   ├── config/
      │   │   ├── controller/
      │   │   ├── service/
      │   │   ├── client/
      │   │   ├── model/
      │   │   ├── repository/
      │   │   └── dto/
      │   └── resources/
      │       ├── application.yml
      │       └── schema.sql
      └── test/
  ```
- [x] Create `build.gradle.kts` with Kotlin DSL:
  - Spring Boot 3.x
  - Spring Boot Web
  - Spring Boot Data JPA
  - SQLite JDBC driver (org.xerial:sqlite-jdbc)
  - SQLite Hibernate dialect
  - Jackson JSON
  - Kotlin Coroutines
- [x] Create `settings.gradle.kts` with project name
- [x] Create `application.yml`:
  - `myair.api.base-url: http://192.168.0.10:2025`
  - `myair.api.timeout-ms: 5000`
  - `myair.api.retry-delay-ms: 4000`
  - `spring.datasource.url: jdbc:sqlite:./data/aircon.db`
  - `server.port: 8080`
- [x] Create `RwAirconApplication.kt` with `@SpringBootApplication`
- [x] Add Gradle wrapper files

### 1.2 MyAir API Client
- [x] Create `client/MyAirClient.kt` service class with RestTemplate
- [x] Configure HTTP client in `config/RestTemplateConfig.kt`:
  - Connection timeout (5s)
  - Read timeout (5s)
- [x] Implement `getSystemData(): MyAirResponse` - calls `GET /getSystemData`
- [x] Implement `setAircon(command: Map<String, Any>)` - calls `GET /setAircon?json={...}`
- [x] Handle URL encoding for JSON commands
- [x] Handle empty `{}` response after commands (API returns empty for up to 4s until confirmed)
- [x] Create DTOs in `dto/` matching API response:
  - `MyAirResponse.kt` (root)
  - `AirconsWrapper.kt`, `Aircon.kt`, `AirconInfo.kt`
  - `ZoneInfo.kt`
  - `SystemInfo.kt` (including `suburbTemp`, `isValidSuburbTemp`)
- [x] Error handling:
  - Network timeout -> return cached last-known state with staleness indicator
  - Connection refused -> return cached state
  - Invalid JSON response -> log and return cached state
- [x] Create `service/MyAirCacheService.kt` to cache last successful response

### 1.3 Backend REST API - System Endpoints
- [x] Create `controller/SystemController.kt`
- [x] `GET /api/system/status` - Current system state
  - Response: `{ state, mode, fan, setTemp, myZone, outdoorTemp, isValidOutdoorTemp, filterCleanStatus, airconErrorCode, zones[] }`
- [x] `POST /api/system/power` - Turn system on/off
  - Body: `{ "state": "on" | "off" }`
  - Command: `{"ac1":{"info":{"state":"on"}}}`
- [x] `POST /api/system/mode` - Set AC mode
  - Body: `{ "mode": "cool" | "heat" | "vent" | "dry" }`
  - Command: `{"ac1":{"info":{"mode":"cool"}}}`
- [x] `POST /api/system/fan` - Set fan speed
  - Body: `{ "fan": "low" | "medium" | "high" | "auto" | "autoAA" }`
  - Command: `{"ac1":{"info":{"fan":"high"}}}`
- [x] `POST /api/system/temperature` - Set system target (when myZone=0)
  - Body: `{ "temperature": 16-32 }`
  - Validate range 16-32
  - Command: `{"ac1":{"info":{"setTemp":24}}}`
- [x] `POST /api/system/myzone` - Set controlling zone
  - Body: `{ "zone": 0-3 }` (0=disabled, 1-3=zone number)
  - Command: `{"ac1":{"info":{"myZone":2}}}`
- [x] Create `controller/HealthController.kt`
- [x] `GET /api/health` - Health check
  - Response: `{ status: "ok" | "degraded", myairConnected: boolean, lastSuccessfulPoll: timestamp }`
- [x] Create request/response DTOs in `dto/`

### 1.4 Backend REST API - Zone Endpoints
- [x] Create `controller/ZoneController.kt`
- [x] `GET /api/zones` - List all zones
  - Response: `[{ id, name, myAirZoneId, state, type, value, setTemp, measuredTemp, rssi, error, isMyZone }]`
- [x] `POST /api/zones/{id}/target` - Set zone target temperature
  - Body: `{ "temperature": 16-32 }`
  - Validate range 16-32
  - Map database zone ID to MyAir zone ID (z01, z02, z03)
  - Command: `{"ac1":{"zones":{"z01":{"setTemp":22}}}}`
- [x] `POST /api/zones/{id}/power` - Set zone open/close
  - Body: `{ "state": "open" | "close" }`
  - Validate zone is not current myZone (cannot close myZone)
  - Command: `{"ac1":{"zones":{"z01":{"state":"open"}}}}`
- [x] Create `service/ZoneService.kt` for zone operations

### 1.5 Backend Database & Zone Configuration
- [x] Create `schema.sql`:
  ```sql
  CREATE TABLE IF NOT EXISTS zone (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    my_air_zone_id TEXT NOT NULL UNIQUE
  );
  INSERT OR IGNORE INTO zone (id, name, my_air_zone_id) VALUES (1, 'Living', 'z01');
  INSERT OR IGNORE INTO zone (id, name, my_air_zone_id) VALUES (2, 'Guest', 'z02');
  INSERT OR IGNORE INTO zone (id, name, my_air_zone_id) VALUES (3, 'Upstairs', 'z03');
  ```
- [x] Create `model/Zone.kt` JPA entity
- [x] Create `repository/ZoneRepository.kt` extending JpaRepository
- [x] Configure SQLite dialect in `config/SQLiteDialect.kt` or use community dialect
- [x] Implement zone ID mapping service: database ID <-> MyAir zone ID

### 1.6 Frontend Project Setup
- [ ] Create `frontend/` directory structure:
  ```
  frontend/
  ├── package.json
  ├── tsconfig.json
  ├── vite.config.ts
  ├── tailwind.config.js
  ├── postcss.config.js
  ├── index.html
  └── src/
      ├── main.tsx
      ├── App.tsx
      ├── index.css
      ├── api/
      ├── components/
      │   └── ui/
      ├── pages/
      ├── hooks/
      └── types/
  ```
- [ ] Initialize with `npm create vite@latest frontend -- --template react-ts`
- [ ] Install dependencies:
  - React, React DOM (included with Vite template)
  - Tailwind CSS (`tailwindcss`, `postcss`, `autoprefixer`)
  - ShadCN UI (via `npx shadcn-ui@latest init`)
  - React Query (`@tanstack/react-query`)
  - Axios
- [ ] Configure `vite.config.ts` with API proxy to `http://localhost:8080`
- [ ] Configure Tailwind CSS (`tailwind.config.js`, `postcss.config.js`)
- [ ] Initialize ShadCN and install components: Button, Card, Slider, Switch, Select, Badge, Skeleton, Toast

### 1.7 Frontend Types & API Layer
- [ ] Create `src/types/index.ts`:
  - `SystemStatus` interface matching backend response
  - `Zone` interface with all zone properties
  - `AcMode = "cool" | "heat" | "vent" | "dry"`
  - `FanSpeed = "low" | "medium" | "high" | "auto" | "autoAA"`
  - `ZoneState = "open" | "close"`
  - `PowerState = "on" | "off"`
- [ ] Create `src/api/client.ts` - Axios instance with base configuration
- [ ] Create `src/api/system.ts`:
  - `getSystemStatus(): Promise<SystemStatus>`
  - `setSystemPower(state: PowerState): Promise<void>`
  - `setSystemMode(mode: AcMode): Promise<void>`
  - `setFanSpeed(fan: FanSpeed): Promise<void>`
  - `setSystemTemperature(temp: number): Promise<void>`
  - `setMyZone(zone: number): Promise<void>`
- [ ] Create `src/api/zones.ts`:
  - `getZones(): Promise<Zone[]>`
  - `setZoneTemperature(id: number, temp: number): Promise<void>`
  - `setZonePower(id: number, state: ZoneState): Promise<void>`
- [ ] Add error handling with user-friendly messages

### 1.8 Frontend Dashboard Page
- [ ] Create `src/pages/Dashboard.tsx`
- [ ] System status header section:
  - System power toggle (Switch component)
  - Outdoor temperature display (when `isValidOutdoorTemp` is true)
  - Filter maintenance alert badge (when `filterCleanStatus > 0`)
  - Error indicator badge (when `airconErrorCode` is set)
- [ ] Mode selector (Select or ToggleGroup):
  - Options: Cool, Heat, Vent, Dry
  - Disabled when system is off
- [ ] Fan speed selector:
  - Options: Low, Medium, High, Auto, AutoAA
  - Disabled when system is off
- [ ] System temperature control (visible when myZone=0):
  - Slider or +/- buttons
  - Range: 16-32 with current value display
- [ ] Zone cards layout (responsive CSS Grid: 1 col mobile, 2 col tablet, 3 col desktop)
- [ ] Loading skeleton while fetching
- [ ] Error state with retry button
- [ ] Set up page routing in `App.tsx`

### 1.9 Frontend Zone Cards
- [ ] Create `src/components/ZoneCard.tsx`
- [ ] Display zone name prominently
- [ ] Display current temperature (`measuredTemp`) with unit indicator
- [ ] Target temperature control:
  - Slider or +/- buttons
  - Range: 16-32
  - Shows current `setTemp`
  - Disabled when zone is closed
- [ ] Zone open/close toggle (Switch):
  - Disabled when zone is myZone (cannot close controlling zone)
  - Show tooltip explaining why disabled
- [ ] MyZone indicator badge (highlight controlling zone)
- [ ] Visual styling: muted/dimmed appearance when closed
- [ ] Optional: Signal strength indicator (`rssi`)
- [ ] Optional: Error indicator when `zone.error > 0`

### 1.10 Frontend State Management & Hooks
- [ ] Create `src/hooks/useSystemStatus.ts` - React Query hook for system status
- [ ] Create `src/hooks/useZones.ts` - React Query hook for zones
- [ ] Create `src/hooks/useMutations.ts` - Mutation hooks for all commands
- [ ] Set up React Query provider in `main.tsx`
- [ ] Configure polling:
  - Refetch interval: 10 seconds
  - Stale time: 5 seconds
- [ ] Implement optimistic updates for better UX:
  - Immediately update UI on command
  - Revert on error
- [ ] Toast notifications:
  - Success: "Temperature updated", "Mode changed", etc.
  - Error: "Failed to update. Please try again."
- [ ] Loading states: Disable controls while mutation is pending

### 1.11 Backend Testing
- [ ] Unit tests for `MyAirClient`:
  - Mock HTTP responses
  - Test successful data parsing
  - Test timeout handling
  - Test connection refused handling
  - Test invalid JSON handling
- [ ] Unit tests for services:
  - `ZoneService` zone mapping logic
  - Temperature validation (16-32 range)
- [ ] Integration tests for REST endpoints:
  - Test with mock MyAir responses using `@MockBean`
  - Verify correct command structure sent to API
- [ ] Test with sample response from `docs/myapi-response.json`
- [ ] Add test configuration in `application-test.yml`

### 1.12 Frontend Testing & Polish
- [ ] Configure ESLint with TypeScript rules
- [ ] Enable TypeScript strict mode in `tsconfig.json`
- [ ] Verify responsive design on mobile, tablet, desktop
- [ ] Test production build: `npm run build`
- [ ] Verify API proxy works in development
- [ ] Test all controls with polling updates (no race conditions)
- [ ] Accessibility: keyboard navigation, screen reader labels

---

## Phase 2: Temperature History (Priority: MEDIUM)

### 2.1 Backend Database Schema
- [ ] Create `model/TemperatureLog.kt` entity:
  - `id: Long`, `timestamp: Instant`, `zoneId: Int`
  - `currentTemp: Double`, `targetTemp: Double`, `zoneEnabled: Boolean`
- [ ] Create `model/SystemLog.kt` entity:
  - `id: Long`, `timestamp: Instant`
  - `mode: String`, `outdoorTemp: Double?`, `systemOn: Boolean`
- [ ] Create `repository/TemperatureLogRepository.kt`
- [ ] Create `repository/SystemLogRepository.kt`
- [ ] Update `schema.sql` with new tables

### 2.2 Backend Logging Service
- [ ] Create `service/TemperatureLoggingService.kt`
- [ ] Implement `@Scheduled` task to poll MyAir every 5 minutes
- [ ] Log temperature readings for each zone
- [ ] Log system state (mode, outdoor temp, power state)
- [ ] Add configuration: `logging.interval-minutes: 5`
- [ ] Create `service/DataRetentionService.kt`
- [ ] Implement daily cleanup job for records older than retention period
- [ ] Add configuration: `logging.retention-days: 90`

### 2.3 Backend History API
- [ ] Create `controller/HistoryController.kt`
- [ ] `GET /api/history/zones/{id}?from=&to=`:
  - Query parameters: `from` (ISO timestamp), `to` (ISO timestamp)
  - Response: `[{ timestamp, currentTemp, targetTemp, zoneEnabled }]`
  - Default: last 24 hours if no params
  - Support aggregation for large time ranges (hourly averages for 7d+)
- [ ] `GET /api/history/system?from=&to=`:
  - Response: `[{ timestamp, mode, outdoorTemp, systemOn }]`
- [ ] Create DTOs for history responses

### 2.4 Frontend History View
- [ ] Install charting library: Recharts
- [ ] Create `src/pages/History.tsx` or add section to Dashboard
- [ ] Create `src/components/TemperatureChart.tsx`:
  - Line chart: current temp vs time (solid line)
  - Overlay: target temp line (dashed line, different color)
  - Visual indication of zone on/off periods (background shading)
- [ ] Time range selector: 24h, 7d, 30d, custom date picker
- [ ] Zone selector dropdown to switch between zones
- [ ] Outdoor temperature overlay toggle
- [ ] Add navigation between Dashboard and History

### 2.5 Diagnostics View (Optional Enhancement)
- [ ] Create `src/pages/Diagnostics.tsx` or modal component
- [ ] Display per-zone details: rssi, error status, damper limits
- [ ] Display system info: firmware versions, connectivity status
- [ ] Refresh button

---

## Phase 3: Scheduling (Priority: MEDIUM)

### 3.0 Decision: Native Scenes vs Custom Scheduling
- [ ] Evaluate MyAir native `myScenes` API capabilities:
  - Pro: Works when app is offline (runs on hardware)
  - Pro: Simpler implementation
  - Con: Limited flexibility (no seasons concept in native API)
  - Con: More complex scene structure to manage
- [ ] Decision: Implement custom scheduling for full season support
- [ ] Document rationale in specs or ADR

### 3.1 Backend Database Schema
- [ ] Create `model/Season.kt` entity:
  - `id: Long`, `name: String`
  - `startMonth: Int`, `startDay: Int`, `endMonth: Int`, `endDay: Int`
  - `priority: Int` (higher = wins on overlap), `active: Boolean`
- [ ] Create `model/ScheduleEntry.kt` entity:
  - `id: Long`, `seasonId: Long`, `dayOfWeek: Int` (1-7)
  - `startTime: LocalTime`, `endTime: LocalTime`, `mode: String`
- [ ] Create `model/ZoneSchedule.kt` entity:
  - `id: Long`, `scheduleEntryId: Long`, `zoneId: Int`
  - `targetTemp: Int`, `enabled: Boolean`
- [ ] Create repositories for all entities
- [ ] Update `schema.sql`

### 3.2 Backend Season API
- [ ] Create `controller/SeasonController.kt`
- [ ] `GET /api/seasons` - List all seasons with their schedules
- [ ] `POST /api/seasons` - Create new season
- [ ] `PUT /api/seasons/{id}` - Update season details
- [ ] `DELETE /api/seasons/{id}` - Delete season (cascade to entries)
- [ ] Create season DTOs

### 3.3 Backend Schedule API
- [ ] `GET /api/seasons/{id}/schedule` - Get full schedule for season
- [ ] `PUT /api/seasons/{id}/schedule` - Update entire schedule
  - Validate non-overlapping time periods within same day
  - Validate time ranges (start < end)
- [ ] Create schedule DTOs

### 3.4 Backend Schedule Execution
- [ ] Create `service/ScheduleExecutionService.kt`
- [ ] `determineActiveSeason()`: Find season matching current date by priority
- [ ] `findCurrentPeriod()`: Find time period for current day/time
- [ ] `applyScheduleSettings()`: Send commands to MyAir API
- [ ] Create `@Scheduled` task to run check every minute
- [ ] Log schedule transitions
- [ ] Handle edge cases:
  - No active season -> no action (manual control)
  - No matching period for current time -> maintain last state
  - Overlapping seasons -> use highest priority

### 3.5 Frontend Schedule Management
- [ ] Create `src/pages/Schedules.tsx`
- [ ] Season list view with CRUD operations
- [ ] Create `src/components/SeasonForm.tsx`:
  - Name input
  - Date range pickers (start/end month+day)
  - Priority number input
  - Active toggle
- [ ] Create `src/components/WeeklyScheduleEditor.tsx`:
  - 7-day grid/tabs
  - Time period rows per day
- [ ] Create `src/components/TimePeriodEditor.tsx`:
  - Start/end time pickers
  - Mode selector
  - Per-zone settings: temperature slider, on/off toggle
- [ ] Visual schedule display (timeline or grid view)
- [ ] Validation: prevent overlapping time periods
- [ ] Add navigation to Schedules page

---

## Phase 4: Manual Override (Priority: LOW)

### 4.1 Backend Database Schema
- [ ] Create `model/Override.kt` entity:
  - `id: Long`, `createdAt: Instant`, `expiresAt: Instant`
  - `mode: String?`, `systemTemp: Int?`
  - `zoneOverrides: String` (JSON: `[{zoneId, temp, enabled}]`)
- [ ] Create `repository/OverrideRepository.kt`
- [ ] Update `schema.sql`

### 4.2 Backend Override API
- [ ] Create `controller/OverrideController.kt`
- [ ] `GET /api/override` - Get current active override (if any)
- [ ] `POST /api/override` - Create new override
  - Body: `{ duration: "1h" | "2h" | "4h" | "until_next", mode?, systemTemp?, zoneOverrides? }`
  - Calculate `expiresAt` based on duration
  - "until_next" = calculate from schedule
- [ ] `DELETE /api/override` - Cancel current override
- [ ] Create override DTOs

### 4.3 Backend Override Logic
- [ ] Modify `ScheduleExecutionService` to check for active override first
- [ ] If override exists and not expired -> apply override settings
- [ ] If override expired -> delete and resume schedule
- [ ] Auto-create override when manual change is made while scheduling is active
- [ ] Create `service/OverrideService.kt` for override management

### 4.4 Frontend Override UI
- [ ] Add override indicator banner on Dashboard header
  - Show "Override active - expires in X" when override exists
  - Cancel button to remove override
- [ ] Modify control interactions:
  - When user changes setting with active schedule, prompt for hold duration
- [ ] Create `src/components/OverrideDialog.tsx`:
  - Duration selector: 1h, 2h, 4h, Until next scheduled change
  - Confirm/Cancel buttons
- [ ] Display override status in system header

---

## Development Aids

### Mock MyAir Server (Recommended for Development)
- [ ] Create simple mock server (Node.js or Python) or use existing mock framework
- [ ] Serve `docs/myapi-response.json` on `GET /getSystemData`
- [ ] Handle `GET /setAircon` with state modification (update in-memory state)
- [ ] Return `{}` for 2 seconds after command, then updated state
- [ ] Enables development without hardware
- [ ] Add npm script or shell script to start mock server

---

## Technical Reference

### MyAir API Summary
- Base URL: `http://192.168.0.10:2025`
- `GET /getSystemData` - Full system state JSON
- `GET /setAircon?json={...}` - Send commands (URL-encoded JSON)
- After command: returns `{}` for up to 4 seconds until confirmed
- Zone IDs: z01 (Living), z02 (Guest), z03 (Upstairs)
- All zones use temperature control (type=1)
- myZone: when set (1-10), that zone controls system temp and cannot be closed
- Outdoor temp: `system.suburbTemp` (check `system.isValidSuburbTemp`)

### Temperature Constraints
- Range: 16-32 (integers)
- All temperatures in Celsius
- Backend must validate before sending to API

### Database
- SQLite at `backend/data/aircon.db`
- Schema initialization via `schema.sql` (Spring Boot auto-runs on startup)

### Frontend Patterns
- React Query for server state and mutations
- ShadCN components for UI
- Tailwind CSS for styling
- TypeScript strict mode
- Vite dev server proxy for API calls during development

### Zone ID Mapping
| Database ID | MyAir Zone ID | Name |
|-------------|---------------|------|
| 1 | z01 | Living |
| 2 | z02 | Guest |
| 3 | z03 | Upstairs |

### Future Considerations (Out of Scope)
- **Percentage-based zones**: Spec mentions `type=0` zones use percentage control (5-100%). Current system has all `type=1` zones. UI detection logic is in place but percentage controls are lower priority.
- **Multi-unit support**: MyAir supports up to 4 AC units (`ac1`-`ac4`). Current implementation targets single unit only.
