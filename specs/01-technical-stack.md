# Technical Stack Specification

## Overview

Web application for controlling a Daikin air conditioning system via the MyAir API. The application displays zone temperatures, allows temperature/mode control, and manages automated schedules by season and day of week.

## Deployment Environment

- **Target Platform:** Raspberry Pi (local server)
- **Network:** Local network only, trusted environment
- **Authentication:** None required

## Backend

- **Language:** Kotlin
- **Framework:** Spring Boot
- **Build System:** Gradle (Kotlin DSL)
- **Database:** SQLite
- **API Style:** REST (JSON)

### Backend Dependencies

- Spring Boot Web (REST controllers)
- Spring Boot Data JPA (database access)
- SQLite JDBC driver
- Jackson (JSON serialization)
- Kotlin Coroutines (for async API calls to MyAir)

## Frontend

- **Framework:** React (TypeScript)
- **UI Components:** ShadCN
- **State Management:** React hooks + React Query (simple approach)
- **Build Tool:** Vite
- **Styling:** Tailwind CSS (required by ShadCN)

## External Integration

- **MyAir API**
  - Protocol: HTTP/JSON
  - Authentication: None
  - Access: Local network only
  - Details: To be provided separately

## Project Structure

```
rw-aircon/
├── backend/
│   ├── build.gradle.kts
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/
│   │   │   └── resources/
│   │   └── test/
│   └── settings.gradle.kts
├── frontend/
│   ├── package.json
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── api/
│   │   └── types/
│   └── vite.config.ts
└── specs/
```

## Development & Build

### Backend
```bash
cd backend
./gradlew build        # Build
./gradlew test         # Run tests
./gradlew bootRun      # Run development server
```

### Frontend
```bash
cd frontend
npm install            # Install dependencies
npm run dev            # Development server
npm run build          # Production build
npm run test           # Run tests
```

## Runtime Configuration

Backend configuration via `application.yml`:
- `myair.api.base-url`: `http://192.168.0.10:2025`
- `spring.datasource.url`: SQLite database path
- `server.port`: Backend API port (default: 8080)

Frontend configuration via environment variables:
- `VITE_API_BASE_URL`: Backend API endpoint
