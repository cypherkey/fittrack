# FitTrack - Implementation status

Living progress tracker for agents and humans. Read this after [`REQUIREMENTS.md`](REQUIREMENTS.md). Update this file when a phase completes or is deferred.

## Current focus

**Next:** Phase 8 - Angular frontend (or Phase 9 polish if API tweaks are needed first).

## Phase checklist

| Phase | Item | Status |
|-------|------|--------|
| 1 | Docs (`REQUIREMENTS.md`, `AGENTS.md`, `README.md`, `STATUS.md`) | **Done** |
| 2 | Backend scaffold (Boot 4.1, Java 25, Maven, SQLite, Flyway, Dockerfile) | **Done** |
| 3 | Domain + migrations | **Done** (see notes) |
| 4 | Security: local login + JWT + default user seed | **Done** |
| 4b | Google OAuth2 SSO + JWT handoff to SPA | **Done** (enabled when `GOOGLE_CLIENT_ID` + `GOOGLE_CLIENT_SECRET` are non-empty; local-only startup without secrets) |
| 5 | Exercise seed + read APIs + custom exercise CRUD | **Done** |
| 6 | Template CRUD + clone-to-workout | **Done** |
| 7 | Workout CRUD + set logging + reorder support | **Done** |
| 8 | Angular frontend | Pending |
| 9 | Polish (validation, pagination, Docker runbook, sample data) | Pending (basic validation/pagination already present) |

## Implementation notes (agents)

- Package root: `com.fittrack`; template JPA entity is `WorkoutTemplate` -> table `workout_template`
- RPE on sets is enum `RpeLevel` (`EASY` | `CHALLENGING` | `HARD`), not decimal; distinct from session `WorkoutDifficulty`
- Same exercise may appear on multiple sets; uniqueness is `(template|workout, setNumber)` only
- Set order is client-controlled; server does not auto-renumber to 1..N
- PUBLIC templates may only contain catalog exercises (`isCustom=false`)
- Exercise catalog: vendored `backend/src/main/resources/data/exercises.json` (free-exercise-db); `ExerciseCatalogSeeder` upserts on startup. Tests use a small fixture under `src/test/resources/data/`
- Google OAuth: `OAuth2ClientAutoConfiguration` stays excluded from the main app; `GoogleOAuthConfig` imports it only when Google client id/secret are both set. Success redirect: `{fittrack.oauth2.success-redirect}#token=<jwt>` (env `FITTRACK_SPA_AUTH_CALLBACK_URL`)
- Default seed user: `admin` / `admin` (env-overridable)
- SQLite file: `./data/fittrack.db` (gitignored). After schema changes, delete local DB files and restart so Flyway can recreate
- Git remote name in this repo may be `github` (not `origin`); default branch `main`

## How to update this file

When finishing work: mark the phase **Done**, set **Next**, and add one-line notes for anything deferred or non-obvious. Keep this file short; durable product rules belong in `REQUIREMENTS.md`.
