## Build & Run

### Prerequisites
- Java 17+ (for backend)
- Node.js 18+ (for frontend)

### Backend
```bash
cd backend
./gradlew build        # Build
./gradlew bootRun      # Run dev server (port 8080)
```

### Frontend
```bash
cd frontend
npm install            # Install dependencies
npm run dev            # Dev server (port 5173)
npm run build          # Production build
```

**Note:** If asdf shims are broken, use Homebrew Node directly:
```bash
PATH="/opt/homebrew/Cellar/node/25.3.0/bin:$PATH" npm install
PATH="/opt/homebrew/Cellar/node/25.3.0/bin:$PATH" npm run build
```

## Validation

- Backend tests: `cd backend && ./gradlew test`
- Frontend lint: `cd frontend && npm run lint`
- Frontend typecheck: `cd frontend && npm run build`

## Operational Notes

- Backend proxies to MyAir API at `http://192.168.0.10:2025`
- Frontend dev server proxies `/api` to backend at `http://localhost:8080`
- SQLite database stored at `backend/data/aircon.db`

### Codebase Patterns

- Backend: Spring Boot + Kotlin, REST controllers in `controller/`, MyAir client in `client/`
- Frontend: React + TypeScript + ShadCN, API calls in `src/api/`, components in `src/components/`

### Git Permissions

If you encounter `insufficient permission for adding an object to repository database .git/objects`, fix with:
```bash
sudo chown -R $(whoami) .git/objects
```
