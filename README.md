# FitTrack

Personal fitness tracker: exercises, templates, and workouts.

- **Backend:** Spring Boot 4 + SQLite + local auth / Google OAuth + JWT
- **Frontend:** Angular (planned)
- **Container:** `backend/Dockerfile` + root `docker-compose.yml`
- **Spec:** [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md)
- **Status:** [`docs/STATUS.md`](docs/STATUS.md) (next: Phase 8 Angular)
- **Tests:** [`docs/TESTS.md`](docs/TESTS.md)

Default git branch is `main`. The remote may be named `github` (not always `origin`).

## Layout

```
backend/              Spring Boot API (+ Dockerfile)
frontend/             Angular SPA (Phase 8 — not scaffolded yet)
docs/                 REQUIREMENTS, STATUS, TESTS
docker-compose.yml    API + SQLite volume
```

## Prerequisites

- **JDK 25+** on `PATH` (or set `JAVA_HOME` to a JDK 25+ install). Do not hardcode a machine-specific JDK path in scripts or docs.
- Maven wrapper is included under `backend/` (`mvnw` / `mvnw.cmd` on Windows).

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
| `FITTRACK_CORS_ORIGINS` | Allowed CORS origins | `http://localhost:4200` |
| `FITTRACK_SPA_AUTH_CALLBACK_URL` | SPA OAuth JWT handoff base (hash `#token=<jwt>` appended) | `http://localhost:4200/auth/callback` |

Google SSO enables automatically when both `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` are non-empty (startup stays local-only if they are empty). Start login at `/oauth2/authorization/google`. After Google login, the backend redirects to `{FITTRACK_SPA_AUTH_CALLBACK_URL}#token=<jwt>` (no refresh tokens in v1). Local username/password + JWT always work.

OpenAPI / Swagger UI (no auth required to view):

- UI: http://localhost:8080/swagger-ui.html
- Spec: http://localhost:8080/v3/api-docs

Use **Authorize** with a JWT from `POST /api/v1/auth/login` to try secured endpoints.

Actuator: only `/actuator/health` is exposed.

### Docker Compose

From the repo root (requires Docker):

```bash
docker compose up --build
```

SQLite data is stored in the `fittrack-data` volume at `/data/fittrack.db`. Override secrets via environment or a `.env` file (not committed).

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
