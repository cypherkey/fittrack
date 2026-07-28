# FitTrack

Personal fitness tracker: exercises, templates, and workouts.

- **Backend:** Spring Boot 4 + SQLite + local auth / Google OAuth + JWT
- **Frontend:** Angular (planned)
- **Container:** `backend/Dockerfile`
- **Spec:** [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md)
- **Status:** [`docs/STATUS.md`](docs/STATUS.md)

Default git branch is `main`. The remote may be named `github` (not always `origin`).

## Layout

```
backend/     Spring Boot API (+ Dockerfile)
frontend/    Angular SPA
docs/        Requirements, status, and design
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
| `FITTRACK_GOOGLE_OAUTH_ENABLED` | Enable Google OAuth2 autoconfig | `false` |
| `GOOGLE_CLIENT_ID` | Google OAuth client id | _(empty)_ |
| `GOOGLE_CLIENT_SECRET` | Google OAuth client secret | _(empty)_ |
| `FITTRACK_CORS_ORIGINS` | Allowed CORS origins | `http://localhost:4200` |
| `FITTRACK_SPA_AUTH_CALLBACK_URL` | SPA OAuth JWT handoff base (hash `#token=<jwt>` appended) | `http://localhost:4200/auth/callback` |

Google SSO stays deferred until credentials are set and `FITTRACK_GOOGLE_OAUTH_ENABLED=true`. Local username/password + JWT work without Google secrets. After Google login, the backend will redirect to `{FITTRACK_SPA_AUTH_CALLBACK_URL}#token=<jwt>` (no refresh tokens in v1).

### Login example

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"admin\"}"
```

## Docs for agents and contributors

1. [`docs/STATUS.md`](docs/STATUS.md) — what is done / next / deferred
2. [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md) — product source of truth
3. [`AGENTS.md`](AGENTS.md) — stack constraints and working rules
