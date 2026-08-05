# FitTrack — Implementation status

Living progress tracker for agents and humans. Read this after [`REQUIREMENTS.md`](REQUIREMENTS.md). Update this file when a phase completes or is deferred.

## Context handoff (~30s)

| | |
|--|--|
| **Stack** | Spring Boot 4.1 + Java 25 + SQLite (WAL) + JWT; Angular **21** SPA under `frontend/` |
| **Done** | Phases 1–9 + deferred **#9** user management (admin API + Users page) |
| **Next** | Deferred **#1** auth hardening; UX polish / E2E |
| **Run** | Backend `backend/`: `.\mvnw.cmd spring-boot:run` -> `:8080`; Frontend `frontend/`: `npm start` -> `:4200` (proxies `/api` to `:8080`) |
| **Key URLs** | Swagger `/swagger-ui.html`, OpenAPI `/v3/api-docs`, health `/actuator/health`, login `POST /api/v1/auth/login`, users (admin) `/api/v1/users` |
| **Docker** | Root `Dockerfile` (SPA+API) via `docker compose up --build` → `:8080` (volume `fittrack-data` → `/data`) |
| **Seed user** | `admin` / `admin` (**ROLE_ADMIN**); env-overridable |
| **Google SSO** | On when `GOOGLE_CLIENT_ID` + `GOOGLE_CLIENT_SECRET` non-empty; handoff `{SPA_CALLBACK}#token=<jwt>` |
| **Frontend prefs** | Angular **21 LTS**, Node **24 LTS**, **NgModules**, **Material**, **npm**; JWT in **localStorage**; home `/`; **dev proxy** `/api`→`:8080` |
| **Deferred** | **#1** auth hardening only |
| **Tests** | [`TESTS.md`](TESTS.md) — `.\mvnw.cmd test` from `backend/`; frontend `ng build` |

## Current focus

**v1 complete** for Phases 1–9. SPA covers auth, exercises, templates, workouts, dashboard, settings, and admin user management. Remaining product debt: **#1** auth hardening.

## Phase checklist

| Phase | Item | Status |
|-------|------|--------|
| 1 | Docs (`REQUIREMENTS.md`, `AGENTS.md`, `README.md`, `STATUS.md`, `TESTS.md`) | **Done** |
| 2 | Backend scaffold (Boot 4.1, Java 25, Maven, SQLite, Flyway, Dockerfile) | **Done** |
| 3 | Domain + migrations | **Done** (V1 schema + V2 `admin` flag) |
| 4 | Security: local login + JWT + default user seed | **Done** |
| 4b | Google OAuth2 SSO + JWT handoff to SPA | **Done** |
| 5 | Exercise seed + read APIs + custom exercise CRUD | **Done** |
| 6 | Template CRUD + clone-to-workout | **Done** |
| 7 | Workout CRUD + set logging + reorder support | **Done** |
| 8 | Angular frontend | **Done** — Material SPA wired to API |
| 9 | Polish (validation, pagination, Docker runbook, sample data) | **Done** |

## Deferred / later

| ID | Item | Notes |
|----|------|--------|
| **#1** | Auth hardening | JWT refresh / shorter access tokens; rate-limit `/auth/login`; fail-fast if `JWT_SECRET` is still the dev default outside local. |
| ~~**#9**~~ | User management | **Done** — `app_user.admin`, `ROLE_ADMIN`, `/api/v1/users` CRUD, admin-only Users page (`/users`). Not public self-register. |

## Implementation notes (agents)

- Package root: `com.fittrack`; template JPA entity is `WorkoutTemplate` -> table `workout_template`
- RPE on **workout** sets is enum `RpeLevel` (`EASY` | `CHALLENGING` | `HARD`), not decimal; distinct from session `WorkoutDifficulty`. Template sets do not have RPE.
- Same exercise may appear on multiple sets; uniqueness is `(template|workout, setNumber)` only
- Set order is client-controlled; `PATCH /api/v1/workouts|templates/{id}/sets/reorder` updates `setNumber`
- Exercise catalog: vendored `data/exercises.json` (UUID ids + `trackedParameters` bitmask from Ryot lots); images as base64 on `image` via `exercise_has_image`; local files under `data/exercise-images/` (gitignored) or GitHub download; seeder skips if exercise table has rows
- Templates: no header timing/`completed` / `totalWeightLifted` (workout stores `startedAt`/`endedAt`/`completed`; session duration derived in UI; per-set `durationSeconds` on sets only)
- User admin: Flyway `V2__user_admin.sql`; seed `admin` is admin; Google JIT users are non-admin
- OpenAPI / Swagger UI: `/v3/api-docs`, `/swagger-ui.html`; JWT bearer for Try it out
- Actuator: only `health` exposed
- SQLite: `foreign_keys=true&journal_mode=WAL`
- Prefer additive Flyway migrations for schema changes (V4: workout timing/`completed`; V5: exercise `video_url`; V6: unique `(user_id, name)` on workout; V7: `app_user.use_metric`; V8: `workout.use_metric`)
- Docker: root `Dockerfile` embeds Angular into Boot `static/`; compose mounts `/data`; SPA deep-link fallthrough via `SpaForwardController`
- Git remote may be `github`; branch `main`
- Frontend: `frontend/` Angular 21 NgModules + Material; see [`FRONTEND.md`](FRONTEND.md)
- Typed SPA clients: **hand-mirrored** from Java DTOs/OpenAPI (not codegen); process in [FRONTEND.md](FRONTEND.md) §8
- Frontend CD: **zoneless** + **signals** (no `zone.js`)
- Test inventory: [`TESTS.md`](TESTS.md)

## How to update this file

When finishing work: mark the phase **Done**, set **Next**, and add one-line notes for anything deferred or non-obvious. Keep this file short; durable product rules belong in `REQUIREMENTS.md`.