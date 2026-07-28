# FitTrack — Requirements & Design

Agent-readable product and technical specification for the FitTrack fitness tracking application.

Inspired by concepts in [Ryot](https://github.com/ignisda/ryot), scoped to workouts, exercises, and templates. Exercise catalog seeded from [free-exercise-db](https://github.com/yuhonas/free-exercise-db).

---

## 1. Goals

Build a personal fitness tracker where users can:

- Sign in with **local username/password** or **Google SSO** (OAuth2 / OIDC)
- Browse a seeded exercise library with typed tracking parameters
- Create reusable workout templates (private or public)
- Log workouts at a specific datetime (multiple per day allowed), optionally cloned from a template
- Persist everything in SQLite via a Spring Boot API, with an Angular SPA frontend
- Run the backend in Docker via a provided Dockerfile

### Non-goals (v1)

- Social feeds, comments, or follows beyond public templates
- User-uploaded media (catalog images are seeded/referenced via the `Image` model)
- Nutrition, sleep, or media tracking (Ryot-style multi-domain tracking)
- Native mobile apps
- Multi-tenant orgs / teams
- Offline-first sync

---

## 2. Repository layout (monorepo)

```
fittrack/
  AGENTS.md                 # Pointer for coding agents
  docs/
    REQUIREMENTS.md         # This document (source of truth)
  backend/                  # Spring Boot 4 API
    Dockerfile              # Container image for the API
  frontend/                 # Angular SPA (after API stabilizes)
  README.md
```

One git repository holds frontend and backend.

---

## 3. Tech stack

| Layer | Choice | Notes |
|-------|--------|--------|
| Backend | **Spring Boot 4.1.x** (latest 4.x) | Spring Framework 7, Jakarta EE 11 |
| Language | **Java 25** (preferred) or latest LTS available | Boot 4.1 supports Java 17–26; prefer newest stable JDK that Boot supports |
| Build | Maven | Gradle acceptable if preferred later |
| DB | SQLite | File-backed; suitable for single-node / personal deploy |
| ORM | Spring Data JPA + Hibernate | Dialect for SQLite (e.g. community SQLite dialect) |
| Migrations | Flyway | Versioned SQL under `backend/src/main/resources/db/migration` |
| Auth | Spring Security: local form/password + Google OAuth2 + JWT | Dual login; JWT for API — see §6 |
| API style | REST + JSON | Versioned under `/api/v1` |
| Frontend | Angular (latest stable when scaffolded) | Consumes REST API; local login + OAuth redirect |
| Container | Docker (`backend/Dockerfile`) | Multi-stage build; SQLite data via volume |

---

## 4. Domain model

### 4.1 User

| Field | Type | Notes |
|-------|------|--------|
| id | UUID | Primary key |
| email | string? | Unique when present; from Google or optional for local |
| username | string? | Unique when present; required for local password accounts |
| passwordHash | string? | BCrypt (or Spring default); null for SSO-only users |
| displayName | string | From Google `name` or local profile |
| googleSubject | string? | Unique OIDC `sub` when linked to Google; null for local-only |
| avatarUrl | string? | Optional picture URL |
| createdAt / updatedAt | instant | Audit |

**Account types:**

- **Local:** `username` + `passwordHash` set; `googleSubject` null (unless later linked)
- **Google SSO:** provisioned on first successful Google login (`googleSubject` + profile fields); `passwordHash` null unless also given a local password later

A user may eventually have both local credentials and a Google link; v1 may keep them separate rows or allow link — default: separate is fine; JIT Google upsert by `googleSubject`.

**Default local user (first-time DB):** on first application start / empty user table, seed one local admin/demo account (see §8.1). Changeable via env vars; never commit production passwords.

### 4.2 Exercise (relational catalog + custom)

Exercises may be **catalog** (seeded from free-exercise-db, global) or **custom** (created by a user). Prefer normalized lookup/join tables over JSON columns.

#### Exercise

| Field | Type | Notes |
|-------|------|--------|
| id | string | Catalog: free-exercise-db `id` (e.g. `Ab_Roller`). Custom: UUID string |
| name | string | Display name |
| force | string? | e.g. pull, push (nullable in source data) |
| level | enum | `BEGINNER` \| `INTERMEDIATE` \| `EXPERT` |
| mechanic | enum? | `COMPOUND` \| `ISOLATION` (nullable when source is null) |
| equipmentId | FK → Equipment | Required when known; resolve/create from seed string |
| instructions | text | Markdown (seed: join instruction steps into markdown, e.g. numbered list) |
| category | string? | strength, stretching, etc. |
| trackedParameters | set/enum flags | Which metrics apply when logging this exercise |
| isCustom | boolean | `false` for seeded catalog; `true` for user-created |
| addedBy | FK → User? | Required when `isCustom`; null for catalog exercises |

Do **not** store `primaryMuscles`, `secondaryMuscles`, or `images` on `Exercise`.

**Custom exercises:** authenticated users may create exercises with `isCustom=true` and `addedBy` = current user. Only `addedBy` may update/delete their custom exercises. Catalog exercises (`isCustom=false`) are read-only via API (managed by seed). List/browse APIs return catalog exercises for everyone, plus the current user's custom exercises.
#### Equipment

Lookup table populated from distinct equipment values in the seed (and any future additions).

| Field | Type | Notes |
|-------|------|--------|
| id | UUID or long | Primary key |
| name | string | Unique, e.g. `body only`, `machine`, `barbell` |

Relationship: `Exercise` *──1 `Equipment` (many exercises share one equipment).

#### Muscle

Lookup table of anatomical targets; populate from seed (union of primary + secondary muscle names) plus any curated list.

| Field | Type | Notes |
|-------|------|--------|
| id | UUID or long | Primary key |
| name | string | Unique, e.g. `abdominals`, `hamstrings` |

#### exerciseHasMuscle (M:N)

| Field | Type | Notes |
|-------|------|--------|
| exerciseId | FK → Exercise | Part of composite PK |
| muscleId | FK → Muscle | Part of composite PK |
| isPrimary | boolean | `true` = primary muscle from seed; `false` = secondary |

Unique constraint on `(exerciseId, muscleId)` (one row per pair).

#### Image

| Field | Type | Notes |
|-------|------|--------|
| id | UUID or long | Primary key |
| path | string | Unique storage key / relative path from free-exercise-db (e.g. `Ab_Roller/0.jpg`) |
| altText | string? | Optional |

#### exerciseHasImage (M:N)

| Field | Type | Notes |
|-------|------|--------|
| exerciseId | FK → Exercise | Part of composite PK |
| imageId | FK → Image | Part of composite PK |
| sortOrder | int | Display order |

#### Enums (Exercise)

```
ExerciseLevel:     BEGINNER | INTERMEDIATE | EXPERT
ExerciseMechanic:  COMPOUND | ISOLATION
```

#### Tracked parameters

An exercise may enable one or more of:

| Parameter | Meaning | Typical use |
|-----------|---------|-------------|
| `REPS` | Repetition count | Strength sets |
| `WEIGHT` | Load (store unit: kg; UI may convert) | Strength |
| `DURATION` | Time (seconds) | Cardio, holds, stretches |
| `DISTANCE` | Distance (meters) | Run, row, cycle |

Mapping for seed import (v1 heuristic):

- Category `cardio` → `DURATION`, `DISTANCE` (weight/reps off by default)
- Category `stretching` / `plyometrics` → `DURATION` (and `REPS` if commonly rep-based; default `DURATION` + `REPS`)
- Default / `strength` / `powerlifting` / `olympic weightlifting` / `strongman` → `REPS`, `WEIGHT`
- Allow admin/user override later; v1 can store defaults only on `Exercise`

### 4.3 Template

Same shape as a workout: header metadata plus a flat list of sets. Owned by a user; can be cloned into a workout. Differs from workout mainly by **visibility** (no `performedAt` / `sourceTemplateId`).

| Field | Type | Notes |
|-------|------|--------|
| id | UUID | |
| userId | FK → User | Owner (same role as `Workout.userId`) |
| name | string? | Optional title |
| durationSeconds | int? | Planned session duration |
| totalWeightLifted | decimal? | Optional planned/estimated total (kg); may be computed from sets |
| difficulty | enum? | `EASY` \| `MEDIUM` \| `HARD` (same enum as workout) |
| notes | string? | |
| visibility | enum | `PRIVATE` \| `PUBLIC` — template-only |
| createdAt / updatedAt | instant | |

#### TemplateSet

Same structure as `WorkoutSet`: one row per planned set; each set points at an exercise directly.

| Field | Type | Notes |
|-------|------|--------|
| id | UUID | |
| templateId | FK → Template | |
| exerciseId | FK → Exercise | Which exercise this set is for |
| setNumber | int | Order within the template |
| reps | int? | Planned |
| weightKg | decimal? | Planned |
| durationSeconds | int? | Planned |
| distanceMeters | decimal? | Planned |
| rpe | decimal? | Optional planned RPE |
| notes | string? | Optional per-set notes |

**Unique constraint:** `(templateId, setNumber)` — set numbers are unique within a template (e.g. 1…N).

No `completed` flag on template sets (that is workout/logging-only).

**Clone template → workout:** create a new `Workout` for the current user with chosen `performedAt`; copy template header fields that apply (`name`, `durationSeconds`, `difficulty`, `notes`, and optionally recompute `totalWeightLifted`); for each `TemplateSet`, create a matching `WorkoutSet` (same `exerciseId`, `setNumber`, metrics, notes; `completed` default true); set `sourceTemplateId`; leave template unchanged.

Public templates: any authenticated user may **read** and **clone**; only owner may **edit/delete**. No template search in v1 (list own templates and/or browse public list is enough).

### 4.4 Workout

Tied to a specific user and a **datetime** (not date-only), so multiple workouts on the same calendar day are allowed. Structurally mirrors `Template` + `TemplateSet`.

| Field | Type | Notes |
|-------|------|--------|
| id | UUID | |
| userId | FK → User | |
| performedAt | datetime (instant) | When the workout occurred (UTC recommended; UI may display local) |
| name | string? | Optional title |
| durationSeconds | int? | Total session duration |
| totalWeightLifted | decimal? | Computed and/or stored metadata (kg) |
| difficulty | enum? | `EASY` \| `MEDIUM` \| `HARD` |
| notes | string? | |
| sourceTemplateId | FK? | Optional provenance if cloned |
| createdAt / updatedAt | instant | |

#### Difficulty enum

Shared by `Template` and `Workout`:

```
WorkoutDifficulty: EASY | MEDIUM | HARD
```

#### WorkoutSet

One row per set in a workout. Flat: each set points at an exercise directly. Mirrors `TemplateSet`, plus logging fields.

| Field | Type | Notes |
|-------|------|--------|
| id | UUID | |
| workoutId | FK → Workout | |
| exerciseId | FK → Exercise | Which exercise this set is for |
| setNumber | int | Order within the workout |
| reps | int? | |
| weightKg | decimal? | |
| durationSeconds | int? | |
| distanceMeters | decimal? | |
| completed | boolean | default true; workout-only |
| rpe | decimal? | optional |
| notes | string? | Optional per-set notes |

**Unique constraint:** `(workoutId, setNumber)` — set numbers are unique within a workout (e.g. 1…N across the session).

Only parameters enabled on the exercise should be required/validated; others may be null.

**Computed metadata:** on save, optionally recompute `totalWeightLifted` = Σ (reps × weightKg) for sets with both values; store on workout for fast list views. Same idea may apply to templates when sets change.

---

## 5. ER relationships (logical)

```
User 1──* Template
User 1──* Workout
User 1──* Exercise (custom only, via addedBy)
Equipment 1──* Exercise
Exercise *──* Muscle          via exerciseHasMuscle (isPrimary boolean)
Exercise *──* Image           via exerciseHasImage (sortOrder)
Template 1──* TemplateSet *──1 Exercise
Workout 1──* WorkoutSet *──1 Exercise
Template (optional) ──<cloned into>── Workout
```

---

## 6. Authentication & authorization

Dual authentication; both issue the same **JWT** for `/api/v1`.

### Local username/password

- Spring Security form or JSON login endpoint (e.g. `POST /api/v1/auth/login` with `{ "username", "password" }`) → returns JWT
- Passwords stored as hashes only (`passwordHash`)
- Optional later: `POST /api/v1/auth/register` — not required for v1 if only the seeded default user exists initially

### Google SSO

- Google OAuth2 / OpenID Connect via Spring Security OAuth2 Client
- JIT upsert `User` by `googleSubject` on login
- After successful OAuth, issue the same JWT (redirect handoff to SPA)

### Common

- **API protection:** All `/api/v1/**` authenticated except health/actuator and auth login endpoints; clients send `Authorization: Bearer <jwt>`
- **Frontend:** local login form and/or Google redirect; store JWT for API calls; CORS for Angular origin
- **Token strategy:** JWT after either local or Google success (`sub` = user id, expiry); signing secret via env (never committed)
- **Authorization rules:**
  - Users read/write only their own workouts and private templates
  - Public templates: readable/cloneable by any authenticated user
  - Exercise catalog (`isCustom=false`): read-only for all authenticated users (seed managed by app)
  - Custom exercises (`isCustom=true`): owner (`addedBy`) may create/update/delete; listed to owner alongside catalog

Config via env / `application.yml`: Google client id/secret (optional if only local login in a given env), JWT signing key, default seed user credentials. Secrets never committed.

---

## 7. REST API sketch (`/api/v1`)

### Auth / me

- `POST /api/v1/auth/login` — local `{ "username", "password" }` → `{ "token", "user" }`
- Google OAuth via Spring Security endpoints (`/oauth2/authorization/google`, callback) → JWT handoff
- `GET /api/v1/me` — current user profile

### Exercises

- `GET /api/v1/exercises` — list/search catalog + current user's custom (query: `q`, `muscle`, `equipment`, `category`, `customOnly`, page)
- `GET /api/v1/exercises/{id}` — detail (catalog or own custom)
- `POST /api/v1/exercises` — create custom exercise (`isCustom=true`, `addedBy` = current user)
- `PUT /api/v1/exercises/{id}` — update own custom exercise only
- `DELETE /api/v1/exercises/{id}` — delete own custom exercise only

### Templates

- `GET /api/v1/templates` — own templates; optional `visibility=PUBLIC` for browse
- `GET /api/v1/templates/{id}` — get with sets (if owner or public)
- `POST /api/v1/templates` — create (with optional sets)
- `PUT /api/v1/templates/{id}` — update metadata / replace sets (owner)
- `DELETE /api/v1/templates/{id}` — delete (owner)
- `POST /api/v1/templates/{id}/clone` — body: `{ "performedAt": "ISO-8601", "name": "..." }` → creates Workout

### Workouts

- `GET /api/v1/workouts` — list for current user (filter by `performedAt` range)
- `GET /api/v1/workouts/{id}` — detail with sets (each set includes `exerciseId`)
- `POST /api/v1/workouts` — create (empty or with sets); include `performedAt`
- `PUT /api/v1/workouts/{id}` — update metadata / replace structure
- `DELETE /api/v1/workouts/{id}` — delete

Errors: problem+json or simple `{ "message", "code" }` with consistent HTTP status codes.

---

## 8. Seed data (first-time / empty DB)

### 8.1 Default local user

On first start when no users exist (or via Flyway seed migration / ApplicationRunner idempotent insert):

| Field | Default | Override |
|-------|---------|----------|
| username | `admin` | env `FITTRACK_DEFAULT_USER` |
| password | `admin` | env `FITTRACK_DEFAULT_PASSWORD` (hash at seed time) |
| displayName | `Admin` | optional |
| email | `admin@localhost` | optional |

- Insert only if that username does not already exist (idempotent)
- Document clearly in README that defaults are for **local/dev** and must be changed for any shared deploy
- Do not log the password at info level

### 8.2 Exercise seed import

Source: [yuhonas/free-exercise-db](https://github.com/yuhonas/free-exercise-db) — `dist/exercises.json`.

Import strategy (backend startup or one-shot command):

1. Ship a copy under `backend/src/main/resources/data/exercises.json` **or** download at build time (prefer vendored file for reproducibility)
2. Upsert lookup rows first: distinct `equipment` → `Equipment`; union of `primaryMuscles` + `secondaryMuscles` → `Muscle`; distinct image paths → `Image`
3. Upsert `Exercise` by `id`; map `level` / `mechanic` to enums; set `equipmentId`; convert `instructions[]` to a single markdown `instructions` text field; set `trackedParameters` via category heuristic (§4.2); set `isCustom=false`, `addedBy=null`
4. Replace join rows: `exerciseHasMuscle` with `isPrimary` from the two source lists (`primaryMuscles` → true, `secondaryMuscles` → false); `exerciseHasImage` with `sortOrder` from array index
5. Serving image bytes is optional post-v1; store paths on `Image` regardless

Do not mutate upstream exercise ids.

---

## 9. Backend package structure (suggested)

```
com.fittrack
  FitTrackApplication
  config/          # Security, CORS, SQLite, Jackson, JWT
  domain/          # entities
  repository/
  service/
  web/             # controllers + DTOs
  security/        # local UserDetails + OAuth user service + JWT
  seed/            # default user + exercise importer
```

---

## 10. Frontend (later phase)

- Angular SPA under `frontend/`
- Feature areas: local login, Google login, exercise browser, templates CRUD, workout logger/calendar
- Auth: local login and/or Google against backend; call APIs with Bearer JWT
- Do not scaffold until core API endpoints and auth work end-to-end

---

## 11. Configuration, local run & Docker

- SQLite file path: e.g. `./data/fittrack.db` (gitignore DB files); in Docker mount a volume at that path
- Google OAuth redirect URI registered for local backend (e.g. `http://localhost:8080/login/oauth2/code/google`) when SSO is enabled
- Profiles: `local` (default), optional `test` with in-memory or temp SQLite
- Env: `FITTRACK_DEFAULT_USER`, `FITTRACK_DEFAULT_PASSWORD`, `JWT_SECRET`, Google client credentials

### Dockerfile (`backend/Dockerfile`)

- Multi-stage: build with JDK + Maven, run with JRE/JDK slim image matching supported Java
- Expose API port (8080)
- Persist SQLite via volume (e.g. `/data`)
- Pass secrets/config via env or env-file, not baked into the image
- Document `docker build` / `docker run` in root or backend README

---

## 12. Testing expectations

- Unit tests for services (clone template, weight totals, auth upsert)
- `@SpringBootTest` / MockMvc for API happy paths
- Seed importer test with a small fixture JSON subset

---

## 13. Implementation phases (ordered)

Agents and humans should follow this order unless told otherwise:

1. **Docs** — this file + `AGENTS.md` + root `README.md` (current)
2. **Backend scaffold** — Spring Boot 4.1, Java 25 (or latest supported), Maven, SQLite, Flyway, **`backend/Dockerfile`**
3. **Domain + migrations** — User (local + SSO fields), Equipment, Muscle, Image, Exercise + join tables, Template + `TemplateSet`, Workout + `WorkoutSet`
4. **Security** — local login + Google OAuth2, JWT, `/api/v1/me`, **seed default local user** on first start
5. **Exercise seed + read APIs + custom exercise CRUD**
6. **Template CRUD + clone-to-workout**
7. **Workout CRUD + set logging + metadata** (`performedAt` datetime)
8. **Angular frontend** scaffold and wire to API
9. **Polish** — validation, pagination, README runbook, Docker usage, sample data

---

## 14. Open decisions (defaults applied)

| Topic | Default for v1 | Change if needed |
|-------|----------------|------------------|
| ID type | **UUID** for app entities; catalog `Exercise.id` = free-exercise-db string; custom `Exercise.id` = UUID string | Locked |
| Weight unit | **Store kg** (`weightKg`, `totalWeightLifted`); UI may convert for display | Locked |
| Auth | **Local username/password + Google SSO**; both issue **JWT** Bearer tokens | Locked |
| Default user | Seed on first start: username/password `admin`/`admin` (overridable via env) | Locked |
| Docker | **`backend/Dockerfile`** multi-stage; SQLite on a volume | Locked |
| Public templates | Listed to all logged-in users; **no search** in v1 | Locked |
| Difficulty | Enum `EASY` \| `MEDIUM` \| `HARD` (nullable) | Locked |
| Muscle on join | `exerciseHasMuscle.isPrimary` boolean (not an enum) | Locked |
| Workout time | **`performedAt` datetime** (not date-only); multiple workouts per day allowed | Locked |
| Custom exercises | Allowed: `isCustom` + optional `addedBy` (FK → User; required when custom) | Locked |
| Template vs workout | Same shape: header + flat sets (`TemplateSet` ↔ `WorkoutSet`); template adds `visibility`, workout adds `performedAt` / `completed` / `sourceTemplateId` | Locked |

---

## 15. References

- [Spring Boot 4.1 system requirements](https://docs.spring.io/spring-boot/system-requirements.html) — Java 17+, compatible through Java 26
- [free-exercise-db](https://github.com/yuhonas/free-exercise-db)
- [Ryot](https://github.com/ignisda/ryot) — inspirational product only; stack and scope differ
