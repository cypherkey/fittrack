# FitTrack — Implementation status

Living progress tracker for agents and humans. Read this after [`REQUIREMENTS.md`](REQUIREMENTS.md). Update this file when a phase completes or is deferred.

## Context handoff (≈30s)

| | |
|--|--|
| **Stack** | Spring Boot 4.1 + Java 25 + SQLite (WAL) + JWT; Angular SPA not started |
| **Done** | Phases 1–7 (API, local auth, Google OAuth when credentials set, exercises/templates/workouts, `PATCH …/sets/reorder`) |
| **Next** | **Phase 8 — Angular frontend** under `frontend/` (do not scaffold until starting that phase) |
| **Run** | `backend/`: `.\mvnw.cmd spring-boot:run` → `http://localhost:8080` |
| **Key URLs** | Swagger UI `/swagger-ui.html`, OpenAPI `/v3/api-docs`, health `/actuator/health`, login `POST /api/v1/auth/login` |
| **Docker** | Root `docker-compose.yml` → `docker compose up --build` (volume `fittrack-data` → `/data`) |
| **Seed user** | `admin` / `admin` (env-overridable) |
| **Google SSO** | On when `GOOGLE_CLIENT_ID` + `GOOGLE_CLIENT_SECRET` non-empty; handoff `{SPA_CALLBACK}#token=<jwt>` |
| **Deferred** | **#1** auth hardening; **#9** user-management FE (no public self-register API in v1) |
| **Tests** | [`TESTS.md`](TESTS.md) — `.\mvnw.cmd test` from `backend/` |

## Current focus

**Next:** Phase 8 — Angular frontend.

## Phase checklist

| Phase | Item | Status |
|-------|------|--------|
| 1 | Docs (`REQUIREMENTS.md`, `AGENTS.md`, `README.md`, `STATUS.md`, `TESTS.md`) | **Done** |
| 2 | Backend scaffold (Boot 4.1, Java 25, Maven, SQLite, Flyway, Dockerfile) | **Done** |
| 3 | Domain + migrations | **Done** (see notes) |
| 4 | Security: local login + JWT + default user seed | **Done** |
| 4b | Google OAuth2 SSO + JWT handoff to SPA | **Done** (enabled when `GOOGLE_CLIENT_ID` + `GOOGLE_CLIENT_SECRET` are non-empty) |
| 5 | Exercise seed + read APIs + custom exercise CRUD | **Done** |
| 6 | Template CRUD + clone-to-workout | **Done** |
| 7 | Workout CRUD + set logging + reorder support | **Done** (`PATCH .../sets/reorder`) |
| 8 | Angular frontend | Pending |
| 9 | Polish (validation, pagination, Docker runbook, sample data) | Partial — OpenAPI/Swagger, actuator health-only, docker-compose, WAL |

## Deferred / later

| ID | Item | Notes |
|----|------|--------|
| **#1** | Auth hardening | JWT refresh / shorter access tokens; rate-limit `/auth/login`; fail-fast if `JWT_SECRET` is still the dev default outside local. **Todo later.** |
| **#9** | User management (frontend) | Local user registration API deferred — handled by a **user management** page on the frontend (not a public self-serve register for v1). |

## Implementation notes (agents)

- Package root: `com.fittrack`; template JPA entity is `WorkoutTemplate` → table `workout_template`
- RPE on sets is enum `RpeLevel` (`EASY` \| `CHALLENGING` \| `HARD`), not decimal; distinct from session `WorkoutDifficulty`
- Same exercise may appear on multiple sets; uniqueness is `(template\|workout, setNumber)` only
- Set order is client-controlled; `PATCH /api/v1/workouts\|templates/{id}/sets/reorder` updates `setNumber` without forced 1…N rewrite
- PUBLIC templates may only contain catalog exercises (`isCustom=false`)
- Exercise catalog: vendored `data/exercises.json`; `ExerciseCatalogSeeder` runs **only if `exercise` table has zero rows**
- Google OAuth: enabled when Google client id/secret are both set (`FITTRACK_GOOGLE_OAUTH_ENABLED` alone is not enough); success redirect `{success-redirect}#token=<jwt>`
- OpenAPI / Swagger UI: `/v3/api-docs`, `/swagger-ui.html` (permitAll); JWT bearer scheme configured for Try it out
- Actuator: only `health` exposed publicly; other actuator endpoints not exposed
- SQLite: `foreign_keys=true&journal_mode=WAL` on JDBC URL; backup by copying the DB file (and `-wal`/`-shm` if present) while app is stopped or after checkpoint
- Prefer additive Flyway `V2__*.sql` for future schema changes; delete local DBs only when intentionally resetting
- Docker: root `docker-compose.yml` builds `backend/` and mounts volume `/data`
- Default seed user: `admin` / `admin` (env-overridable)
- Git remote may be `github`; branch `main`
- Test inventory: [`docs/TESTS.md`](TESTS.md)

## How to update this file

When finishing work: mark the phase **Done**, set **Next**, and add one-line notes for anything deferred or non-obvious. Keep this file short; durable product rules belong in `REQUIREMENTS.md`.
