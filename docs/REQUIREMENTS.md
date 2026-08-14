# FitTrack — Requirements & Design

Agent-readable product and technical specification for the FitTrack fitness tracking application.

Inspired by concepts in [Ryot](https://github.com/ignisda/ryot), scoped to workouts, exercises, and templates. Exercise catalog seeded from [free-exercise-db](https://github.com/yuhonas/free-exercise-db).

---

## 1. Goals

Build a personal fitness tracker where users can:

- Sign in with **local username/password** or **Google SSO** (OAuth2 / OIDC)
- Browse a seeded exercise library with typed tracking parameters
- Create reusable workout templates (private or public)
- Log workouts with optional `startedAt` / `endedAt` (multiple per day allowed), optionally cloned from a template
- Persist everything in SQLite via a Spring Boot API, with an Angular SPA frontend
- Run the full app (API + SPA) in Docker via a root Dockerfile

### Non-goals (v1)

- Social feeds, comments, or follows beyond public templates
- User-uploaded media (catalog images are seeded into `Image.contentBase64` and returned by the API)
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
    REQUIREMENTS.md         # Product/design source of truth
    STATUS.md               # Implementation progress (done / next / deferred)
    TESTS.md                # Backend test inventory
  backend/                  # Spring Boot 4 API (also serves SPA from classpath:/static/)
  frontend/                 # Angular SPA
  Dockerfile                # Multi-stage: Angular + Spring Boot single image
  docker-compose.yml        # App + SQLite volume
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
| ORM | Spring Data JPA + Hibernate | Dialect for SQLite (community SQLite dialect). See §3.1 |
| Migrations | Flyway | Versioned SQL under `backend/src/main/resources/db/migration` |
| Auth | Spring Security: local form/password + Google OAuth2 + JWT | Dual login; JWT for API — see §6 |
| API style | REST + JSON | Versioned under `/api/v1` |
| Frontend | Angular (latest stable when scaffolded) | Consumes REST API; local login + OAuth redirect |
| Container | Docker (root `Dockerfile`) | Multi-stage: Node builds SPA → Maven embeds into Boot static → JRE; SQLite via volume |

### 3.1 SQLite / JPA mapping conventions

| Java type | SQLite storage | Notes |
|-----------|----------------|--------|
| `Instant` | TEXT (ISO-8601) | Via converter |
| `boolean` | INTEGER 0/1 | Via converter |
| Weight / distance (`Double`) | REAL | `weightKg`, `distanceMeters`, `totalWeightLifted` |
| Enums | TEXT | Stored as enum name |
| UUID ids | TEXT | String UUID |

`trackedParameters` on `Exercise` is an **INTEGER bitmask**: `REPS=1`, `WEIGHT=2`, `DURATION=4`, `DISTANCE=8` (see `TrackedParameters`).

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
| admin | boolean | `ROLE_ADMIN` when true |
| useMetric | boolean | Unit preference; `true` = metric, `false` = imperial; default `true` |
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
| id | UUID string | Catalog and custom: UUID. Catalog ids are stable UUIDs derived from the upstream free-exercise-db slug at seed-prep time |
| name | string | Display name |
| force | string? | `push` \| `pull` \| `static` (nullable in source data) |
| level | enum | `BEGINNER` \| `INTERMEDIATE` \| `EXPERT` |
| mechanic | enum? | `COMPOUND` \| `ISOLATION` (nullable when source is null) |
| equipmentId | FK → Equipment | Required when known; resolve/create from seed string |
| instructions | text | Markdown (seed: join instruction steps into markdown, e.g. numbered list) |
| videoUrl | string? | Optional URL to a demo/instruction video |
| category | string? | `strength` \| `stretching` \| `plyometrics` \| `strongman` \| `powerlifting` \| `cardio` \| `olympic weightlifting` |
| trackedParameters | int bitmask | `REPS=1`, `WEIGHT=2`, `DURATION=4`, `DISTANCE=8` |
| isCustom | boolean | `false` for seeded catalog; `true` for user-created |
| addedBy | FK → User? | Required when `isCustom`; null for catalog exercises |

Do **not** store `primaryMuscles`, `secondaryMuscles`, or `images` on `Exercise`.

**Custom exercises:** authenticated users may create exercises with `isCustom=true` and `addedBy` = current user. Only `addedBy` may update/delete their custom exercises. Catalog exercises (`isCustom=false`) are read-only via API (managed by seed). List/browse APIs return catalog exercises for everyone, plus the current user's custom exercises. Any existing exercise (catalog or custom) may be referenced on templates and workouts.
#### Equipment

Lookup table populated from distinct equipment values in the seed (and any future additions).

| Field | Type | Notes |
|-------|------|--------|
| id | UUID | Primary key (string UUID) |
| name | string | Unique, e.g. `body only`, `machine`, `barbell` |

Relationship: `Exercise` *──1 `Equipment` (many exercises share one equipment).

#### Muscle

Lookup table of anatomical targets; populate from seed (union of primary + secondary muscle names) plus any curated list.

| Field | Type | Notes |
|-------|------|--------|
| id | UUID | Primary key (string UUID) |
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
| id | UUID | Primary key (string UUID) |
| path | string | Unique storage key / relative path from free-exercise-db (e.g. `Ab_Roller/0.jpg`) — seed lookup key |
| contentBase64 | string? | Base64-encoded image bytes loaded at seed time |
| contentType | string? | e.g. `image/jpeg` |
| altText | string? | Optional (defaults to exercise name on seed) |

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
ExerciseForce:     push | pull | static
ExerciseCategory:  strength | stretching | plyometrics | strongman | powerlifting | cardio | olympic weightlifting
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

### 4.3 WorkoutTemplate (table `workout_template`)

Same shape as a workout: header metadata plus a flat list of sets. Owned by a user; can be cloned into a workout. Differs from workout mainly by **visibility** (no `startedAt` / `endedAt` / `completed` / `sourceTemplateId`).

JPA entity name: `WorkoutTemplate`. Database table: **`workout_template`**.

| Field | Type | Notes |
|-------|------|--------|
| id | UUID | |
| userId | FK → User | Owner (same role as `Workout.userId`) |
| name | string? | Optional title |
| difficulty | enum? | `EASY` \| `MEDIUM` \| `HARD` (same enum as workout) |
| notes | string? | |
| visibility | enum | `PRIVATE` \| `PUBLIC` — template-only |
| createdAt / updatedAt | instant | |

Templates do **not** store session timing/`completed` or `totalWeightLifted` — those belong on **workouts** / **workout_set** when logging. Session length is derived in the UI from `endedAt − startedAt`.

#### TemplateSet (table `template_set`)

Same structure as `WorkoutSet`: one row per planned set; each set points at an exercise directly. The same exercise may appear on multiple sets (no unique on exerciseId).

| Field | Type | Notes |
|-------|------|--------|
| id | UUID | |
| workoutTemplateId | FK → WorkoutTemplate | |
| exerciseId | FK → Exercise | Which exercise this set is for |
| setNumber | int | Client-controlled order within the template (frontend may reorder) |
| reps | int? | Planned |
| weightKg | decimal? | Planned |
| durationSeconds | int? | Planned |
| distanceMeters | decimal? | Planned |
| notes | string? | Optional per-set notes |

**Unique constraint:** `(workoutTemplateId, setNumber)` — set numbers must be unique within a template. The API must support reordering: clients send explicit `setNumber` values; the server does **not** auto-rewrite to contiguous 1…N. When replacing/reordering sets, the client is responsible for sending a conflict-free set of numbers (unique within the template).

No `completed` flag on template sets (that is workout/logging-only).

**Clone template → workout:** create a new `Workout` for the current user with `startedAt`/`endedAt` unset and `completed=false`; default workout `name` to `Workout YYYY-MM-DD` (current local date) unless the clone request supplies a non-blank name; set `useMetric` from the current user's preference; copy template `difficulty` and `notes`; recompute workout `totalWeightLifted` from cloned sets; for each `TemplateSet`, create a matching `WorkoutSet` (same `exerciseId`, `setNumber`, metrics including per-set `durationSeconds`/`weightKg`; `rpe` starts unset; set `completed=false`; do **not** copy template-set notes onto the workout set — personal notes live in `user_exercise_notes`); set `sourceTemplateId`; leave template unchanged.

**Public templates:** any authenticated user may **read** and **clone**; only owner may **edit/delete**. No template search in v1. Both **PRIVATE** and **PUBLIC** templates may include any exercise the owner can use (catalog + their own custom exercises). Cloning a public template copies its exercise references as-is (including the owner's custom exercises) onto the new workout.

### 4.4 Workout

Tied to a specific user and a **datetime** (not date-only), so multiple workouts on the same calendar day are allowed. Structurally mirrors `WorkoutTemplate` + `TemplateSet`.

| Field | Type | Notes |
|-------|------|--------|
| id | UUID | |
| userId | FK → User | |
| startedAt | datetime (instant)? | When the workout started (UTC recommended; UI may display local); nullable until logged |
| endedAt | datetime (instant)? | When the workout ended; nullable; session duration = `endedAt − startedAt` in the UI |
| name | string? | Optional title; **unique per user** when set (trimmed); blank stored as null; different users may reuse the same name |
| completed | boolean | Whether the workout session is finished; default `false` |
| useMetric | boolean | Unit system for this workout; default = owner's `User.useMetric` at create/clone; editable on update |
| totalWeightLifted | decimal? | Computed and/or stored metadata (kg) |
| difficulty | enum? | `EASY` \| `MEDIUM` \| `HARD` |
| notes | string? | |
| sourceTemplateId | FK → WorkoutTemplate? | Optional provenance if cloned |
| createdAt / updatedAt | instant | |

#### Difficulty enum

Shared by `WorkoutTemplate` and `Workout` (session-level):

```
WorkoutDifficulty: EASY | MEDIUM | HARD
```

#### RPE enum (per set)

Perceived effort on a set (template planned or workout logged). More values may be added later:

```
RpeLevel: EASY | CHALLENGING | HARD
```

#### WorkoutSet

One row per set in a workout. Flat: each set points at an exercise directly. Mirrors `TemplateSet`, plus logging fields. Same exercise may appear on multiple sets.

| Field | Type | Notes |
|-------|------|--------|
| id | UUID | |
| workoutId | FK → Workout | |
| exerciseId | FK → Exercise | Which exercise this set is for |
| setNumber | int | Client-controlled order; frontend may reorder sets |
| reps | int? | |
| weightKg | decimal? | |
| durationSeconds | int? | |
| distanceMeters | decimal? | |
| completed | boolean | default false; workout-only |
| rpe | enum? | `EASY` \| `CHALLENGING` \| `HARD` |

**Unique constraint:** `(workoutId, setNumber)`. Same reorder rules as templates: client owns `setNumber`; server enforces uniqueness and does not auto-renumber.

Only parameters enabled on the exercise should be required/validated; others may be null.

**User exercise notes:** personal notes for an exercise are stored in `user_exercise_notes` (not on `workout_set`). One row per `(userId, exerciseId)`; shared across all workouts. Workout set API responses include `exerciseNotes` from that table when sets are loaded.

#### UserExerciseNote (table `user_exercise_notes`)

| Field | Type | Notes |
|-------|------|--------|
| id | UUID | |
| userId | FK → User | |
| exerciseId | FK → Exercise | |
| notes | string? | TEXT; blank clears / deletes the row |

**Unique constraint:** `(userId, exerciseId)`.

**Computed metadata:** on workout save, recompute `totalWeightLifted` = Σ (reps × weightKg) for sets with both values; store on the workout header for fast list views. API responses for workouts and templates also include **`setCount`** (number of sets) so list views do not need the nested `sets` array.

---

## 5. ER relationships (logical)

```
User 1──* WorkoutTemplate
User 1──* Workout
User 1──* Exercise (custom only, via addedBy)
Equipment 1──* Exercise
Exercise *──* Muscle          via exerciseHasMuscle (isPrimary boolean)
Exercise *──* Image           via exerciseHasImage (sortOrder)
User 1──* UserExerciseNote *──1 Exercise
WorkoutTemplate 1──* TemplateSet *──1 Exercise
Workout 1──* WorkoutSet *──1 Exercise
WorkoutTemplate (optional) ──<cloned into>── Workout
```

---

## 6. Authentication & authorization

Dual authentication; both issue the same **JWT** for `/api/v1`.

### Local username/password

- Spring Security form or JSON login endpoint (e.g. `POST /api/v1/auth/login` with `{ "username", "password" }`) → returns JWT
- Passwords stored as hashes only (`passwordHash`)
- Local user creation via **admin user management** (`/api/v1/users`, Users page for `ROLE_ADMIN`). Not a public self-serve `POST /auth/register`.

### Google SSO

- Google OAuth2 / OpenID Connect via Spring Security OAuth2 Client — **enabled when both** `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` are non-empty (credentials alone are enough; `GOOGLE_OAUTH_ENABLED` alone is not)
- JIT upsert `User` by `googleSubject` on login
- **JWT handoff to SPA:** after successful OAuth callback, backend redirects to the Angular app, e.g. `http://localhost:4200/auth/callback#token=<jwt>` (hash preferred so the token is less likely to hit server logs; SPA base via `FRONTEND_URL` (callback = `{FRONTEND_URL}/auth/callback`)). SPA stores the JWT and clears it from the URL. No refresh tokens in v1; access token lifetime from `fittrack.jwt.expiration-minutes` (default ~12h); re-login when expired.

### Common

- **API protection:** All `/api/v1/**` authenticated except health/actuator and auth login endpoints; clients send `Authorization: Bearer <jwt>`
- **Frontend:** local login form and/or Google redirect; store JWT for API calls; CORS for Angular origin
- **Token strategy:** JWT after either local or Google success (`sub` = user id, expiry); signing secret via env (never committed)
- **Authorization rules:**
  - Users read/write only their own workouts and private templates
  - Public templates: readable/cloneable by any authenticated user; may include catalog or the owner's custom exercises
  - Exercise catalog (`isCustom=false`): read-only for all authenticated users (seed managed by app)
  - Custom exercises (`isCustom=true`): owner (`addedBy`) may create/update/delete; listed to owner alongside catalog; any existing exercise may be referenced on templates/workouts

Config via env / `application.yml`: Google client id/secret (optional if only local login in a given env), JWT signing key, default seed user credentials. Secrets never committed.

---

## 7. REST API sketch (`/api/v1`)

### Auth / me

- `POST /api/v1/auth/login` — local `{ "username", "password" }` → `{ "token", "user" }`
- Google OAuth via Spring Security endpoints (`/oauth2/authorization/google`, callback) → redirect SPA with JWT in URL hash (§6)
- `GET /api/v1/me` — current user profile
- `PATCH /api/v1/me` — update current user preferences (`{ "useMetric": true|false }`)

### Exercises

- `GET /api/v1/exercise` — list/search catalog + current user's custom (query: `q`, `muscle`, `equipment`, `category`, `customOnly`, page)
- `GET /api/v1/exercise/{id}` — detail for any existing exercise (catalog or custom)
- `GET /api/v1/exercise/{id}/history` — current user's set history for the exercise from **completed** workouts only (ordered by workout `startedAt` desc, then `setNumber` asc); each row: `startedAt`, `setNumber`, `reps`, `weightKg`
- `GET /api/v1/exercise/{id}/notes` — current user's personal notes for the exercise (`{ exerciseId, notes }`)
- `PUT /api/v1/exercise/{id}/notes` — upsert/clear personal notes (`{ "notes": "..." | null }`; blank/null deletes the row)
- `POST /api/v1/exercise` — create custom exercise (`isCustom=true`, `addedBy` = current user)
- `PUT /api/v1/exercise/{id}` — update own custom exercise only
- `DELETE /api/v1/exercise/{id}` — delete own custom exercise only

### Templates

- `GET /api/v1/templates` — own templates; optional `visibility=PUBLIC` for browse
- `GET /api/v1/templates/{id}` — get with sets (if owner or public)
- `POST /api/v1/templates` — create (with optional sets); sets may reference catalog or the owner's custom exercises
- `PUT /api/v1/templates/{id}` - update metadata / replace sets (owner); client supplies `setNumber` for order/reorder (no server auto-renumber to 1…N)
- `PATCH /api/v1/templates/{id}/sets/reorder` - body: `{ "items": [ { "setId", "setNumber" } ] }` — updates `setNumber` only; uniqueness rules apply, no forced contiguous rewrite
- `DELETE /api/v1/templates/{id}` - delete (owner)
- `POST /api/v1/templates/{id}/clone` — body: `{ "name": "..." }` (optional; default `Workout YYYY-MM-DD`) → creates Workout with unset `startedAt`/`endedAt` and `completed=false`

### Workouts

- `GET /api/v1/workouts` — list for current user (filter by `startedAt` range)
- `GET /api/v1/workouts/{id}` — detail with sets (each set includes `exerciseId`)
- `POST /api/v1/workouts` — create (empty or with sets); optional `startedAt` / `endedAt` / `completed` / `useMetric` (defaults to the user's preference when omitted)
- `PUT /api/v1/workouts/{id}` - update metadata / replace structure; client supplies `setNumber` to support frontend reorder (no server auto-renumber to 1…N)
- `PATCH /api/v1/workouts/{id}/sets/reorder` - body: `{ "items": [ { "setId", "setNumber" } ] }` — same client-owned `setNumber` rules as templates
- `PATCH /api/v1/workouts/{id}/sets/{setId}` — partial update of one set (owner): any of `completed`, `reps`, `weightKg`, `durationSeconds`, `distanceMeters`, `rpe` (explicit `null` clears nullable fields); recomputes workout `totalWeightLifted` when reps/weight change. Personal exercise notes use `PUT /api/v1/exercise/{id}/notes` (not this endpoint).
- `POST /api/v1/workouts/{id}/start` — set `startedAt` to now if unset (idempotent if already started)
- `POST /api/v1/workouts/{id}/complete` — set `endedAt` to now, `completed=true`, recompute `totalWeightLifted` from sets (Σ reps × weightKg); session duration is `endedAt − startedAt` (not stored). If `startedAt` was unset, set it to now as well
- `DELETE /api/v1/workouts/{id}` - delete

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

Source: [yuhonas/free-exercise-db](https://github.com/yuhonas/free-exercise-db) — `dist/exercises.json` (+ exercise image files).

Vendored catalog: `backend/src/main/resources/data/exercises.json` with **UUID** `id` values (stable hashes of the upstream slug). Image relative paths in the JSON still match free-exercise-db folders (e.g. `Ab_Roller/0.jpg`).

Optional local image files: `backend/src/main/resources/data/exercise-images/**` (gitignored; fetch with `scripts/fetch-exercise-images.ps1`). If a file is missing, the seeder may download from GitHub raw (`fittrack.seed.download-images`, default true).

Import strategy (backend startup `ApplicationRunner`):

1. **Skip entirely if the `exercise` table already has any rows** (idempotent; does not re-upsert on later startups)
2. Otherwise upsert lookup rows: distinct `equipment` → `Equipment`; union of muscles → `Muscle`
3. For each distinct image path → `Image`: set `path`, load bytes → `contentBase64` + `contentType`, optional `altText`
4. Insert `Exercise` by UUID `id`; map enums; set `equipmentId`; markdown `instructions`; `trackedParameters` from seed (Ryot `lot` → bitmask; category heuristic only as fallback); `isCustom=false`, `addedBy=null`
5. Join rows: `exercise_has_muscle`, `exercise_has_image` (`sortOrder` = array index)
6. API returns image metadata **and** `contentBase64` / `contentType` on exercise responses for the SPA to render as data URLs

Toggle image loading with `fittrack.seed.load-images` / `FITTRACK_SEED_LOAD_IMAGES` (tests typically disable this).

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

## 10. Frontend (Phase 8)

Full SPA requirements: **[`FRONTEND.md`](FRONTEND.md)** (source of truth for Angular).

### Locked stack preferences

| Item | Choice |
|------|--------|
| Angular | **21.x LTS** (newest LTS; v22 is Active — use only if explicitly chosen) |
| Node | **24.x LTS** |
| UI | **Angular Material** (+ CDK) |
| Components | **NgModules** — `standalone: false` (not standalone-by-default) |
| UX inspiration | [Ryot demo Fitness IA](https://demo.ryot.io/_s/acl_vUMPnPirkHlT) (nav + lists; not media features) |

### Feature areas

- Local login, Google callback (`#token=<jwt>`), JWT interceptor + guards
- Exercises, templates (incl. clone), workouts (incl. set reorder)
- Light dashboard; settings + **admin user management** (`#9` done)

`FRONTEND.md` locked. Phase 8 SPA implemented under `frontend/`.

---

## 11. Configuration, local run & Docker

- SQLite file path: e.g. `./data/fittrack.db` (gitignore DB files); JDBC URL enables `foreign_keys=true` and `journal_mode=WAL`
- **Local day-to-day:** API on `:8080`, SPA via `ng serve` on `:4200` with proxy
- **Docker:** root **`docker-compose.yml`** builds the root **`Dockerfile`** (SPA + API in one image) and mounts volume `fittrack-data` at `/data`
- Single image serves the Angular SPA from Spring Boot `classpath:/static/` (same origin); production `apiBaseUrl` is `''`
- OpenAPI / Swagger UI (permitAll): `/swagger-ui.html`, `/v3/api-docs` (JWT bearer scheme for Try it out)
- Actuator: only `/actuator/health` exposed
- Google OAuth redirect URI: `{FRONTEND_URL}/login/oauth2/code/google` (compose default `FRONTEND_URL=http://localhost:8080`; use `https://…` when the app is served over TLS). JWT handoff uses `{FRONTEND_URL}/auth/callback`. `server.forward-headers-strategy=framework` for reverse proxies.
- Profiles: `local` (default), optional `test` with in-memory or temp SQLite
- Env: `FITTRACK_DEFAULT_USER`, `FITTRACK_DEFAULT_PASSWORD`, `JWT_SECRET`, `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET`, `FRONTEND_URL`, `DB_PATH`, `LOG_LEVEL` (see root README)

### Dockerfile (repo root)

- Multi-stage: **Node 24** builds Angular → **JDK 25** Maven package with SPA copied into `src/main/resources/static/` → **JRE 25** runtime
- Expose port **8080** (API + SPA)
- Persist SQLite via volume (e.g. `/data`)
- Pass secrets/config via env or env-file, not baked into the image
- Document `docker compose up --build` / `docker build -t fittrack .` in root README

---

## 12. Testing expectations

- Unit tests for services (clone template, weight totals, auth upsert)
- `@SpringBootTest` / MockMvc for API happy paths and endpoint coverage
- Seed importer test with a small fixture JSON subset
- Living inventory: [`TESTS.md`](TESTS.md)

---

## 13. Implementation phases (ordered)

Track live progress in [`STATUS.md`](STATUS.md). Ordered phases:

1. **Docs** — this file + `AGENTS.md` + `STATUS.md` + `TESTS.md` + root `README.md`
2. **Backend scaffold** — Spring Boot 4.1, Java 25 (or latest supported), Maven, SQLite, Flyway, root **`Dockerfile`** (API + SPA)
3. **Domain + migrations** — User (local + SSO fields), Equipment, Muscle, Image, Exercise + join tables, `WorkoutTemplate` + `TemplateSet`, Workout + `WorkoutSet`
4. **Security** — local login + JWT + `/api/v1/me` + seed default local user; Google OAuth when credentials enabled
5. **Exercise seed + read APIs + custom exercise CRUD**
6. **Template CRUD + clone-to-workout**
7. **Workout CRUD + set logging + client-driven set reorder** (`startedAt`/`endedAt` datetimes + `completed`; `PATCH …/sets/reorder`)
8. **Angular frontend** scaffold and wire to API
9. **Polish** — validation, pagination, README runbook, Docker Compose usage, OpenAPI, sample data

Deferred (tracked in STATUS): **#1** auth hardening. **#9** user management is done (admin API + Users page; no public self-register).

---

## 14. Open decisions (defaults applied)

| Topic | Default for v1 | Change if needed |
|-------|----------------|------------------|
| ID type | **UUID** for all entities including catalog `Exercise.id` (stable UUID derived from upstream slug at seed prep); lookups `Equipment`/`Muscle`/`Image` also UUID | Locked |
| Weight unit | **Store kg** as SQLite REAL / Java `Double` (`weightKg`, `totalWeightLifted`); UI may convert kg↔lb for display/input when imperial; **do not** convert distance meters | Locked |
| Auth | **Local username/password + Google SSO**; both issue **JWT** Bearer tokens | Locked |
| Google JWT handoff | Redirect to SPA `#token=<jwt>`; no refresh token in v1 | Locked |
| Default user | Seed on first start: username/password `admin`/`admin` (overridable via env) | Locked |
| Docker | Root **`Dockerfile`** multi-stage (Angular + Boot single image); SQLite on a volume | Locked |
| Public templates | Listed to all logged-in users; **no search**; may include catalog or owner's custom exercises | Locked |
| Difficulty | Enum `EASY` \| `MEDIUM` \| `HARD` (nullable, session-level) | Locked |
| RPE (per set) | Enum `EASY` \| `CHALLENGING` \| `HARD` on **workout sets only** (nullable; more values later). Not on template sets. | Locked |
| Muscle on join | `exerciseHasMuscle.isPrimary` boolean (not an enum) | Locked |
| Workout time | **`startedAt` / `endedAt` datetimes** (not date-only; both nullable); session duration derived in UI; **`completed`** boolean on workout; multiple workouts per day allowed | Locked |
| Custom exercises | Allowed: `isCustom` + `addedBy`; create/update/delete owner-only; referenceable on any template/workout by id | Locked |
| Template vs workout | Same shape: header + flat sets; table `workout_template` / entity `WorkoutTemplate` | Locked |
| trackedParameters | INTEGER bitmask on Exercise | Locked |
| Set order | Client-controlled `setNumber`; API supports reorder; no server auto-renumber | Locked |
| Progress tracking | Living file [`docs/STATUS.md`](STATUS.md) | Locked |

---

## 15. References

- [Spring Boot 4.1 system requirements](https://docs.spring.io/spring-boot/system-requirements.html) — Java 17+, compatible through Java 26
- [free-exercise-db](https://github.com/yuhonas/free-exercise-db)
- [Ryot](https://github.com/ignisda/ryot) — inspirational product only; stack and scope differ
