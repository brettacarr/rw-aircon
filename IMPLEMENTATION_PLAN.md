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

## Project Status: Phase 3 Complete (Scheduling)

**Last Updated:** 2026-01-23
**Status:** Phase 3 complete. Scheduling feature fully implemented (backend and frontend).
**Next Action:** Begin Phase 4 - Manual Override (hold duration, resume schedule)

**Critical Bug Fixed (2026-01-20):** The `findHourlyAveragesByZoneIdAndTimestampBetween` repository method was missing, causing the backend to fail to compile. This has been fixed.

Dashboard MVP is complete with:
- Backend: Kotlin/Spring Boot REST API, MyAir client integration, SQLite database, 32 unit tests
- Frontend: React/TypeScript dashboard, zone cards, state management, ESLint configuration
- All builds and tests passing

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
| 2 | 1.6-1.10 | Frontend setup, Dashboard, Zone cards | Complete |
| 3 | 1.11-1.12 | Testing & polish | Complete |
| 4 | 2.1-2.4 | Temperature history logging & graphs | Complete |
| 5 | 3.1-3.5 | Season-based scheduling system | Complete |
| 6 | 4.1-4.4 | Manual override with hold duration | Not Started |

---

## Phase 1: Dashboard MVP (Priority: HIGH)

### 1.1 Backend Project Setup
- [x] Create `backend/` directory structure
- [x] Create `build.gradle.kts` with Spring Boot, Spring Data JPA, SQLite
- [x] Create `application.yml` with MyAir API and database configuration
- [x] Create `RwAirconApplication.kt` with `@SpringBootApplication`
- [x] Add Gradle wrapper files

### 1.2 MyAir API Client
- [x] Create `client/MyAirClient.kt` service with RestTemplate
- [x] Configure HTTP client with timeouts
- [x] Implement `getSystemData()` and `setAircon()` methods
- [x] Create DTOs matching API response structure
- [x] Error handling with cache fallback
- [x] Create `service/MyAirCacheService.kt`

### 1.3 Backend REST API - System Endpoints
- [x] Create `controller/SystemController.kt`
- [x] Implement endpoints: status, power, mode, fan, temperature, myzone
- [x] Create `controller/HealthController.kt`
- [x] Create request/response DTOs

### 1.4 Backend REST API - Zone Endpoints
- [x] Create `controller/ZoneController.kt`
- [x] Implement endpoints: list zones, set target temperature, set zone power
- [x] Create `service/ZoneService.kt` for zone operations
- [x] Implement zone ID mapping (database ID <-> MyAir zone ID)

### 1.5 Backend Database & Zone Configuration
- [x] Create `schema.sql` with zone table and seed data
- [x] Create `model/Zone.kt` JPA entity
- [x] Create `repository/ZoneRepository.kt`
- [x] Configure SQLite dialect

### 1.6 Frontend Project Setup
- [x] Initialize React + TypeScript project with Vite
- [x] Install and configure Tailwind CSS
- [x] Install and configure ShadCN UI components
- [x] Install React Query and Axios
- [x] Configure Vite API proxy

### 1.7 Frontend Types & API Layer
- [x] Create TypeScript interfaces for all API types
- [x] Create Axios client with base configuration
- [x] Create API functions for system and zone operations
- [x] Add error handling

### 1.8 Frontend Dashboard Page
- [x] Create `src/pages/Dashboard.tsx`
- [x] Implement system status header with controls
- [x] Add mode and fan speed selectors
- [x] Add system temperature control
- [x] Implement responsive zone cards layout
- [x] Add loading and error states

### 1.9 Frontend Zone Cards
- [x] Create `src/components/ZoneCard.tsx`
- [x] Display zone name and temperatures
- [x] Add target temperature control
- [x] Add zone open/close toggle
- [x] Show MyZone indicator
- [x] Add signal strength and error indicators

### 1.10 Frontend State Management & Hooks
- [x] Create React Query hooks for data fetching
- [x] Create mutation hooks for all commands
- [x] Configure polling (10s refetch, 5s stale time)
- [x] Implement optimistic updates
- [x] Add toast notifications

### 1.11 Backend Testing
- [x] Unit tests for MyAirClient
- [x] Unit tests for services
- [x] Controller tests with mocked dependencies
- [x] Add test configuration

### 1.12 Frontend Testing & Polish
- [x] Configure ESLint with TypeScript rules
- [x] Enable TypeScript strict mode
- [x] Verify responsive design
- [x] Test production build
- [x] Verify API proxy
- [x] Test polling updates
- [x] Ensure accessibility features

---

## Phase 2: Temperature History (Priority: MEDIUM)

### 2.1 Backend Database Schema
- [x] Create `model/TemperatureLog.kt` entity:
  - `id: Long`, `timestamp: Instant`, `zoneId: Int`
  - `currentTemp: Double`, `targetTemp: Double`, `zoneEnabled: Boolean`
- [x] Create `model/SystemLog.kt` entity:
  - `id: Long`, `timestamp: Instant`
  - `mode: String`, `outdoorTemp: Double?`, `systemOn: Boolean`
- [x] Create `repository/TemperatureLogRepository.kt`
- [x] Create `repository/SystemLogRepository.kt`
- [x] Update `schema.sql` with new tables

### 2.2 Backend Logging Service
- [x] Create `service/TemperatureLoggingService.kt`
- [x] Implement `@Scheduled` task to poll MyAir every 5 minutes
- [x] Log temperature readings for each zone
- [x] Log system state (mode, outdoor temp, power state)
- [x] Add configuration: `logging.interval-minutes: 5`
- [x] Create `service/DataRetentionService.kt`
- [x] Implement daily cleanup job for records older than retention period
- [x] Add configuration: `logging.retention-days: 90`

### 2.3 Backend History API
- [x] Create `controller/HistoryController.kt`
- [x] `GET /api/history/zones/{id}?from=&to=`:
  - Query parameters: `from` (ISO timestamp), `to` (ISO timestamp)
  - Response: `[{ timestamp, currentTemp, targetTemp, zoneEnabled }]`
  - Default: last 24 hours if no params
  - Support aggregation for large time ranges (hourly averages for 7d+)
- [x] `GET /api/history/system?from=&to=`:
  - Response: `[{ timestamp, mode, outdoorTemp, systemOn }]`
- [x] Create DTOs for history responses

### 2.4 Frontend History View
- [x] Install charting library: Recharts
- [x] Create `src/pages/History.tsx` or add section to Dashboard
- [x] Create `src/components/TemperatureChart.tsx`:
  - Line chart: current temp vs time (solid line)
  - Overlay: target temp line (dashed line, different color)
  - Visual indication of zone on/off periods (background shading)
- [x] Time range selector: 24h, 7d, 30d (custom date picker deferred)
- [x] Zone selector dropdown to switch between zones
- [x] Outdoor temperature overlay toggle
- [x] Add navigation between Dashboard and History

**Note:** Custom date picker deferred to future enhancement. Current implementation supports 24h, 7d, and 30d preset ranges.

### 2.5 Diagnostics View (Optional Enhancement)
- [ ] Create `src/pages/Diagnostics.tsx` or modal component
- [ ] Display per-zone details: rssi, error status, damper limits
- [ ] Display system info: firmware versions, connectivity status
- [ ] Refresh button

---

## Phase 3: Scheduling (Priority: MEDIUM)

### 3.0 Decision: Native Scenes vs Custom Scheduling
- [x] Evaluate MyAir native `myScenes` API capabilities:
  - Pro: Works when app is offline (runs on hardware)
  - Pro: Simpler implementation
  - Con: Limited flexibility (no seasons concept in native API)
  - Con: More complex scene structure to manage
- [x] Decision: Implement custom scheduling for full season support
- [x] Document rationale in specs or ADR

**Architecture Decision (2026-01-23):** Custom scheduling system implemented instead of native MyAir scenes to support full season flexibility with date ranges and priority-based overlap resolution.

### 3.1 Backend Database Schema
- [x] Create `model/Season.kt` entity:
  - `id: Long`, `name: String`
  - `startMonth: Int`, `startDay: Int`, `endMonth: Int`, `endDay: Int`
  - `priority: Int` (higher = wins on overlap), `active: Boolean`
- [x] Create `model/ScheduleEntry.kt` entity:
  - `id: Long`, `seasonId: Long`, `dayOfWeek: Int` (1-7)
  - `startTime: LocalTime`, `endTime: LocalTime`, `mode: String`
- [x] Create `model/ZoneSchedule.kt` entity:
  - `id: Long`, `scheduleEntryId: Long`, `zoneId: Int`
  - `targetTemp: Int`, `enabled: Boolean`
- [x] Create repositories for all entities
- [x] Update `schema.sql`

### 3.2 Backend Season API
- [x] Create `controller/SeasonController.kt`
- [x] `GET /api/seasons` - List all seasons with their schedules
- [x] `POST /api/seasons` - Create new season
- [x] `PUT /api/seasons/{id}` - Update season details
- [x] `DELETE /api/seasons/{id}` - Delete season (cascade to entries)
- [x] Create season DTOs

### 3.3 Backend Schedule API
- [x] `GET /api/seasons/{id}/schedule` - Get full schedule for season
- [x] `PUT /api/seasons/{id}/schedule` - Update entire schedule
  - Validate non-overlapping time periods within same day
  - Validate time ranges (start < end)
- [x] Create schedule DTOs

### 3.4 Backend Schedule Execution
- [x] Create `service/ScheduleExecutionService.kt`
- [x] `determineActiveSeason()`: Find season matching current date by priority
- [x] `findCurrentPeriod()`: Find time period for current day/time
- [x] `applyScheduleSettings()`: Send commands to MyAir API
- [x] Create `@Scheduled` task to run check every minute
- [x] Log schedule transitions
- [x] Handle edge cases:
  - No active season -> no action (manual control)
  - No matching period for current time -> maintain last state
  - Overlapping seasons -> use highest priority

### 3.5 Frontend Schedule Management
- [x] Create `src/pages/Schedules.tsx`
- [x] Season list view with CRUD operations
- [x] Create `src/components/SeasonForm.tsx`:
  - Name input
  - Date range pickers (start/end month+day)
  - Priority number input
  - Active toggle
- [x] Create `src/components/WeeklyScheduleEditor.tsx`:
  - 7-day grid/tabs
  - Time period rows per day
- [x] Create `src/components/TimePeriodEditor.tsx`:
  - Start/end time pickers
  - Mode selector
  - Per-zone settings: temperature slider, on/off toggle
- [x] Visual schedule display (timeline or grid view)
- [x] Validation: prevent overlapping time periods
- [x] Add navigation to Schedules page

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
