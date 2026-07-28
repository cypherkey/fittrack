# Agent instructions — FitTrack

This repository is a fitness tracking monorepo (Spring Boot 4 backend + Angular frontend).

## Read first

Before implementing or changing behavior, read:

- [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md) — product requirements, domain model, API sketch, and phased build plan

Treat that document as the source of truth unless the user explicitly overrides it in chat.

## Stack constraints

- Backend: **Spring Boot 4.1.x** (latest 4.x), **Java** latest supported by that Boot release (prefer Java 25+)
- Database: **SQLite**
- Auth: **local username/password** and **Google OAuth2**; API auth via **JWT**. Seed default local user (`admin`/`admin`, overridable) on first start
- Container: provide **`backend/Dockerfile`** (multi-stage; SQLite via volume)
- Frontend: **Angular** under `frontend/` — scaffold only after backend API/auth are usable
- Monorepo: keep `backend/` and `frontend/` in this same git repository

## Working rules

1. Follow the implementation phases in `docs/REQUIREMENTS.md` §13 unless asked to skip ahead.
2. Prefer small, vertical slices (entity → migration → service → API → tests).
3. Do not commit secrets (Google client id/secret, JWT secrets, local DB files). Default seed password is for local/dev only.
4. When requirements are ambiguous, prefer the defaults in §14 of the requirements doc, then ask.
5. Update `docs/REQUIREMENTS.md` when durable product/design decisions change.
