# Agent instructions — FitTrack

This repository is a fitness tracking monorepo (Spring Boot 4 backend + Angular frontend).

## Read first

Before implementing or changing behavior, read (in order):

1. [`docs/STATUS.md`](docs/STATUS.md) — **what is done / next / deferred** (session continuity)
2. [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md) — product requirements, domain model, API sketch, locked decisions
3. [`docs/FRONTEND.md`](docs/FRONTEND.md) — Angular SPA requirements (Phase 8)
4. [`docs/TESTS.md`](docs/TESTS.md) — backend test inventory

Treat `REQUIREMENTS.md` / `FRONTEND.md` as product sources of truth unless the user explicitly overrides them in chat. Treat `STATUS.md` as the progress source of truth; update it when phases complete.

## Stack constraints

- Backend: **Spring Boot 4.1.x** (latest 4.x), **Java** latest supported by that Boot release (prefer Java 25+)
- Database: **SQLite**
- Auth: **local username/password** and **Google OAuth2**; API auth via **JWT**. Seed default local user (`admin`/`admin`, overridable) on first start. Google SSO is implemented and enables when `GOOGLE_CLIENT_ID` + `GOOGLE_CLIENT_SECRET` are set (see STATUS)
- Container: provide **`backend/Dockerfile`** (multi-stage; SQLite via volume)
- Frontend: **Angular 21 LTS** under `frontend/` — **NgModules** (`standalone: false`), **Angular Material**, Node **24 LTS**. See [`docs/FRONTEND.md`](docs/FRONTEND.md). Scaffold only after frontend open questions are accepted.
- Monorepo: keep `backend/` and `frontend/` in this same git repository

## Working rules

1. Follow the implementation phases in `docs/REQUIREMENTS.md` §13 / `docs/STATUS.md` unless asked to skip ahead.
2. Prefer small, vertical slices (entity → migration → service → API → tests).
3. Do not commit secrets (Google client id/secret, JWT secrets, local DB files). Default seed password is for local/dev only.
4. When requirements are ambiguous, prefer the defaults in §14 of the requirements doc, then ask.
5. Update `docs/REQUIREMENTS.md` when durable product/design decisions change.
6. Update `docs/STATUS.md` when implementation progress or deferrals change.
