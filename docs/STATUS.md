# FitTrack - Implementation status

Living progress tracker for agents and humans. Read this after [`REQUIREMENTS.md`](REQUIREMENTS.md). Update this file when a phase completes or is deferred.

## Current focus

**Next:** Phase 5 - exercise catalog seed (free-exercise-db) + exercise read APIs + custom exercise CRUD.

## Phase checklist

| Phase | Item | Status |
|-------|------|--------|
| 1 | Docs (`REQUIREMENTS.md`, `AGENTS.md`, `README.md`, `STATUS.md`) | **Done** |
| 2 | Backend scaffold (Boot 4.1, Java 25, Maven, SQLite, Flyway, Dockerfile) | **Done** |
| 3 | Domain + migrations | **Done** (see notes) |
| 4 | Security: local login + JWT + default user seed | **Done** |
| 4b | Google OAuth2 SSO + JWT handoff to SPA | **Deferred** until Google client credentials are configured (`fittrack.oauth2.google.enabled`) |
| 5 | Exercise seed + read APIs + custom exercise CRUD | **Next** |
| 6 | Template CRUD + clone-to-workout | Pending |
| 7 | Workout CRUD + set logging + reorder support | Pending |
| 8 | Angular frontend | Pending |
| 9 | Polish (validation, pagination, Docker runbook, sample data) | Pending |

## Implementation notes (agents)

- Package root: `com.fittrack`; template JPA entity is `WorkoutTemplate` -> table `workout_template`
- RPE on sets is enum `RpeLevel` (`EASY` | `CHALLENGING` | `HARD`), not decimal; distinct from session `WorkoutDifficulty`
- Same exercise may appear on multiple sets; uniqueness is `(template|workout, setNumber)` only
- Set order is client-controlled; server does not auto-renumber to 1..N
- PUBLIC templates may only contain catalog exercises (`isCustom=false`)
- Google OAuth client autoconfig is **excluded** until enabled; local JWT auth works without Google secrets
- After Google callback (when enabled): redirect SPA to `http://localhost:4200/auth/callback#token=<jwt>`; no refresh tokens in v1
- Default seed user: `admin` / `admin` (env-overridable)
- SQLite file: `./data/fittrack.db` (gitignored). After schema changes, delete local DB files and restart so Flyway can recreate
- Git remote name in this repo may be `github` (not `origin`); default branch `main`

## How to update this file

When finishing work: mark the phase **Done**, set **Next**, and add one-line notes for anything deferred or non-obvious. Keep this file short; durable product rules belong in `REQUIREMENTS.md`.
