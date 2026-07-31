# FitTrack

Personal fitness tracker: exercises, templates, and workouts.

- **Backend:** Spring Boot 4 + SQLite + local auth / Google OAuth + JWT
- **Frontend:** Angular 21 (NgModules + Material) under `frontend/`
- **Container:** root `Dockerfile` (API + SPA) + `docker-compose.yml`
- **Spec:** [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md)
- **Status:** [`docs/STATUS.md`](docs/STATUS.md) (Phases 1–9 done) · Frontend: [`docs/FRONTEND.md`](docs/FRONTEND.md)
- **Tests:** [`docs/TESTS.md`](docs/TESTS.md)

Default git branch is `main`. The remote may be named `github` (not always `origin`).

## Layout

```
backend/              Spring Boot API (serves SPA from classpath:/static/ in Docker)
frontend/             Angular 21 SPA
Dockerfile            Multi-stage single image (Node → Maven → JRE)
docker-compose.yml    App + SQLite volume
docs/                 REQUIREMENTS, STATUS, TESTS, FRONTEND
```

## Prerequisites

- **JDK 25+** on `PATH` (or set `JAVA_HOME` to a JDK 25+ install). Do not hardcode a machine-specific JDK path in scripts or docs.
- Maven wrapper is included under `backend/` (`mvnw` / `mvnw.cmd` on Windows).
- **Node.js 24 LTS** + npm for the Angular SPA (`frontend/`).

## Backend runbook

From `backend/`:

```bash
# Linux / macOS
./mvnw test
./mvnw spring-boot:run

# Windows (PowerShell / cmd)
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

- API base: `http://localhost:8080`
- Default seed user: `admin` / `admin` (overridable; see env vars below)
- SQLite DB: `./data/fittrack.db` (created on first run; gitignored). After schema changes, delete local `*.db` files and restart so Flyway can recreate.
- Exercise catalog images: optional local files via `powershell -File scripts/fetch-exercise-images.ps1` into `backend/src/main/resources/data/exercise-images/` (gitignored). On first seed the API also downloads missing images from GitHub raw unless `FITTRACK_SEED_DOWNLOAD_IMAGES=false`. Bytes are stored as base64 on `image.content_base64`.

### Useful environment variables

| Variable | Purpose | Default |
|----------|---------|---------|
| `FITTRACK_DEFAULT_USER` | Seed username | `admin` |
| `FITTRACK_DEFAULT_PASSWORD` | Seed password | `admin` |
| `FITTRACK_DEFAULT_DISPLAY_NAME` | Seed display name | `Admin` |
| `FITTRACK_DEFAULT_EMAIL` | Seed email | `admin@localhost` |
| `FITTRACK_DB_PATH` | SQLite file path | `./data/fittrack.db` |
| `JWT_SECRET` | JWT signing secret (≥256 bits recommended) | dev default in `application.yml` |
| `JWT_EXPIRATION_MINUTES` | Access token lifetime | `720` |
| `FITTRACK_GOOGLE_OAUTH_ENABLED` | Optional flag only (credentials alone enable SSO; this flag alone does not) | `false` |
| `GOOGLE_CLIENT_ID` | Google OAuth client id (required with secret to enable SSO) | _(empty)_ |
| `GOOGLE_CLIENT_SECRET` | Google OAuth client secret | _(empty)_ |
| `FITTRACK_CORS_ORIGINS` | Allowed CORS origins | `http://localhost:4200` (compose defaults to `:8080`) |
| `FITTRACK_SPA_AUTH_CALLBACK_URL` | SPA OAuth JWT handoff base (hash `#token=<jwt>` appended) | `http://localhost:4200/auth/callback` (compose: `:8080`) |
| `FITTRACK_SEED_LOAD_IMAGES` | Load image bytes into DB during catalog seed | `true` |
| `FITTRACK_SEED_DOWNLOAD_IMAGES` | If a classpath image file is missing, download from GitHub raw | `true` |

Google SSO enables automatically when both `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` are non-empty (startup stays local-only if they are empty). Start login at `/oauth2/authorization/google`. After Google login, the backend redirects to `{FITTRACK_SPA_AUTH_CALLBACK_URL}#token=<jwt>` (no refresh tokens in v1). Local username/password + JWT always work.

OpenAPI / Swagger UI (no auth required to view):

- UI: http://localhost:8080/swagger-ui.html
- Spec: http://localhost:8080/v3/api-docs

Use **Authorize** with a JWT from `POST /api/v1/auth/login` to try secured endpoints.

Actuator: only `/actuator/health` is exposed.

## Frontend runbook

From `frontend/` (Node 24 LTS):

```bash
npm install
npm start
```

- SPA: `http://localhost:4200`
- API in dev: same-origin via **`proxy.conf.json`** (`/api`, `/oauth2` → `http://localhost:8080`); `environment.development.ts` sets `apiBaseUrl: ''`
- Backend must still run on `:8080`
- Default login: `admin` / `admin`
- Admin user management: Settings page (seed admin has `admin: true`)
- JWT stored in `localStorage`; Google SSO uses `/auth/callback#token=…` (start URL `/oauth2/authorization/google` is also proxied in dev)

See [`docs/FRONTEND.md`](docs/FRONTEND.md) for SPA requirements.

## Docker (API + SPA)

From the repo root (requires Docker):

```bash
docker compose up --build
# or: docker build -t fittrack . && docker run --rm -p 8080:8080 -v fittrack-data:/data fittrack
```

Opens **http://localhost:8080** (Angular SPA + REST API, same origin). Login `admin` / `admin`.

SQLite data is stored in the `fittrack-data` volume at `/data/fittrack.db`. Override secrets via environment or a `.env` file (not committed). For Google SSO against the container, register redirect `http://localhost:8080/login/oauth2/code/google` and keep compose’s `FITTRACK_SPA_AUTH_CALLBACK_URL` / `FITTRACK_CORS_ORIGINS` on `:8080`.

Day-to-day frontend work can still use `npm start` on `:4200` against a local API on `:8080`.

### SQLite notes

- JDBC URL enables `foreign_keys=true` and `journal_mode=WAL`.
- Prefer **additive** Flyway migrations (`V2__…`) for schema changes. Deleting local `*.db` files resets the database (dev only).
- Backup: stop the app (or checkpoint), then copy `fittrack.db` plus any `-wal` / `-shm` sidecars.

### Login example

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"admin\"}"
```

## Docs for agents and contributors

1. [`docs/STATUS.md`](docs/STATUS.md) — what is done / next / deferred
2. [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md) — product source of truth
3. [`docs/TESTS.md`](docs/TESTS.md) — backend test inventory
4. [`AGENTS.md`](AGENTS.md) — stack constraints and working rules
