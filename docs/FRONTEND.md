# FitTrack — Frontend requirements (Angular)

Agent-readable SPA requirements. Product API/domain: [`REQUIREMENTS.md`](REQUIREMENTS.md). Progress: [`STATUS.md`](STATUS.md).

**Phase 8 complete.**

**Status:** Phase 8 complete — feature CRUD wired to API (exercises, templates, workouts, settings/user mgmt).

**UX reference (inspiration only):** [Ryot demo](https://demo.ryot.io/_s/acl_vUMPnPirkHlT) — collapsible side nav, Fitness area (Workouts / Templates / Exercises), list + search, theme toggle. FitTrack is **workout-focused** (no Ryot media/collections); do not copy Ryot branding or non-fitness domains.

---

## 1. Goals (v1 SPA)

- Authenticate (local username/password + optional Google SSO via backend handoff)
- Browse catalog exercises; create/edit/delete **own** custom exercises
- Manage workout **templates** (private/public) and **clone** to a workout
- Log and edit **workouts** (sets with client-controlled `setNumber` / reorder)
- Call FitTrack REST API with `Authorization: Bearer <jwt>`

### Non-goals (v1 SPA)

- Public self-registration (admins manage users in Settings; no public register)
- Offline-first / PWA sync
- User-uploaded exercise media (catalog images come from seed/API as base64)
- Native mobile shells
- Matching Ryot's media/analytics product surface

---

## 2. Stack (locked preferences)

| Layer | Choice | Notes |
|-------|--------|--------|
| Framework | **Angular 21.x (latest LTS)** | As of mid-2026: v22 is *Active*; v21 is the newest **LTS**. Prefer LTS for the SPA. |
| Runtime | **Node.js 24.x (latest LTS)** | "Krypton" Active/Maintenance LTS; also satisfies Angular 21 engines (`^20.19 \|\| ^22.12 \|\| ^24`) |
| Package manager | **npm** (locked) | Default Angular CLI package manager |
| UI | **Angular Material** | + Angular CDK; use theme from `ng add @angular/material` + dark toggle |
| Components | **NgModules — not standalone** | Preference locked: `standalone: false` on components/directives/pipes; declare in feature `NgModule`s |
| Routing | `AppRoutingModule` + feature routing modules | Lazy-load feature modules where sensible |
| HTTP | `HttpClientModule` (or `provideHttpClient` only if forced by CLI — prefer module style) | Interceptor attaches JWT |
| State | **Signals** for UI state + RxJS `HttpClient` Observables for API | **Zoneless** (Angular 21 default). No `zone.js` / no `provideZoneChangeDetection`. No NgRx. |
| Forms | Reactive forms | Prefer `ReactiveFormsModule` |
| Dates | Angular `DatePipe` / native `Date` / backend ISO-8601 instants | Align with `performedAt` Instant |

### CLI / project conventions

When scaffolding (`ng new` / generate):

```text
--standalone=false
```

Set defaults in `angular.json` schematics:

```json
"@schematics/angular:component": { "standalone": false, "style": "scss" },
"@schematics/angular:directive": { "standalone": false },
"@schematics/angular:pipe": { "standalone": false }
```

Bootstrap via `AppModule` + `platformBrowser().bootstrapModule(AppModule)` (zoneless; no Zone provider).

### Change detection (zoneless)

Angular 21 apps are **zoneless by default**. FitTrack:

- Does **not** load `zone.js` or call `provideZoneChangeDetection()`
- Uses **`signal()`** for component/service UI state updated after HTTP (`loading`, lists, `AuthService.user`, `ThemeService.mode`, …)
- Keeps API services as `Observable` + `.subscribe()` that call `.set()` / `.update()` on signals
- Template event handlers (`(click)`, etc.) still schedule change detection


Install Material: `ng add @angular/material` (pick a theme; support light/dark if inexpensive).

---

## 3. App shell & IA (inspired by Ryot Fitness)

### Layout

- **Collapsible left sidenav** (Material `mat-sidenav`)
- Top bar: app name **FitTrack**, theme toggle (v1 dark mode), current user display name, logout
- Main content: routed pages with page title + primary actions

### Nav (authenticated)

| Item | Route | Backend |
|------|-------|---------|
| Dashboard / Home | **`/`** (alias `/dashboard` optional) | Summary of recent workouts (list API) |
| Workouts | `/workouts` | `GET/POST/PUT/DELETE /api/v1/workouts`, reorder |
| Templates | `/templates` | Templates CRUD + clone |
| Exercises | `/exercises` | Catalog + custom CRUD |
| Settings | `/settings` | Display me + API base; **admin user management** (#9) |

Unauthenticated: `/login`, `/auth/callback` (Google JWT hash handoff).

Out of scope vs Ryot: Media, Measurements, Collections, Discord links, Ryot Analytics as product.

---

## 4. Auth UX

1. **Local login** — form → `POST /api/v1/auth/login` → store JWT in **`localStorage` (locked)** → navigate home
2. **Google** — link/button to `{apiBase}/oauth2/authorization/google` (full page redirect). Callback route `/auth/callback` reads `#token=…`, stores JWT in **`localStorage`**, clears hash, navigates home
3. **HTTP interceptor** — `Authorization: Bearer <token>` on API calls; on 401 clear token and redirect to login
4. **Auth guard** — protect app routes; login page redirects away if already authenticated
5. **CORS** — production / absolute API URLs need backend CORS (`FITTRACK_CORS_ORIGINS`, default `http://localhost:4200`). **Local `ng serve` uses a DEV proxy** (same-origin `/api`, `/oauth2`) so browser CORS preflight is avoided.

Environment:

```ts
// environment.development.ts — relative; ng serve proxies to :8080 (proxy.conf.json)
apiBaseUrl: ''

// environment.ts (production builds) — absolute backend origin
apiBaseUrl: 'http://localhost:8080'
```

---

## 5. Feature screens (v1)

### Login
- Username / password, submit, error display
- Optional "Continue with Google" when SSO is configured (can always show; backend fails if unset)

### Exercises
- Paginated/filterable list (q, muscle, equipment, category) — Material table or list + filters; show catalog image thumbnail from API `contentBase64`
- Detail view (instructions markdown render, sanitized; gallery of seeded images as data URLs)
- Custom exercise create/edit/delete (owner only); catalog read-only in UI

### Templates
- List own templates; browse `visibility=PUBLIC` (no template-level duration or total weight)
- Create/edit with sets editor (exercise picker, setNumber, reps/weight/duration/distance/RPE)
- Enforce UI rule: PUBLIC templates cannot add custom exercises
- Clone → dialog for `performedAt` + name → create workout → navigate to workout

### Workouts
- List with date range filter (Material datepicker)
- Create/edit workout header (`performedAt`, name, difficulty, notes, duration)
- Sets table: add/remove/reorder (CDK drag-drop when practical; else up/down calling `PATCH .../sets/reorder` or full PUT — prefer reorder endpoint when only order changes)
- Show computed `totalWeightLifted` from API (workout header; not on templates)

### Dashboard (light)
- Recent workouts + shortcuts to log workout / open templates (avoid Ryot-style media widgets)

---

## 6. Angular module map (proposed)

```
frontend/src/app/
  app.module.ts
  app-routing.module.ts
  core/           # AuthService, TokenStorage, interceptors, guards (CoreModule, forRoot)
  shared/         # SharedModule (Material modules, pipes e.g. markdown)
  layout/         # ShellComponent, sidenav
  features/
    auth/
    dashboard/
    exercises/
    templates/
    workouts/
    settings/
```

Each feature: `XxxModule` + `XxxRoutingModule` + components with `standalone: false`.

---

## 7. Material usage guidelines

- Prefer Material components for forms, tables, dialogs, snackbars, sidenav, toolbar, buttons, icons
- Density: comfortable default; mobile-friendly sidenav overlay
- Theme: one primary Material theme; **dark mode toggle in v1** (locked)
- Avoid inventing a parallel design system

---

## 8. Typed API clients (frontend ↔ backend)

FitTrack does **not** auto-generate TypeScript from OpenAPI today. Typed clients are **hand-maintained** mirrors of the Spring Boot JSON API. Backend OpenAPI is the discoverability / contract check; TypeScript models and `HttpClient` services are the SPA contract.

### Sources of truth (backend)

| Source | Location / URL | Use for |
|--------|----------------|---------|
| Java DTOs (records) | `backend/src/main/java/com/fittrack/web/dto/` | Field names, nullability, request vs response shapes |
| Domain enums | `backend/.../domain/*` (e.g. `RpeLevel`, `WorkoutDifficulty`) | Exact JSON string values (`EASY`, `HARD`, …) |
| Controllers | `backend/.../web/*Controller.java` | Paths, HTTP methods, query params, status codes |
| OpenAPI / Swagger | `http://localhost:8080/v3/api-docs` · UI `/swagger-ui.html` | Browse & verify after backend changes; Try it out with JWT |

Jackson serializes Java records as **camelCase** JSON matching the record component names (`displayName`, `totalWeightLifted`, `contentBase64`, `custom`, …). Enums serialize as their **name** strings. Exercise images include `contentType` + `contentBase64` for data-URL rendering.

### Frontend layout (typed clients)

```
frontend/src/app/core/
  models/          # TypeScript interfaces / string-union enums (DTO mirrors)
    enums.ts
    user.ts
    exercise.ts
    template.ts
    workout.ts
    lookup.ts
    page-response.ts
  api/             # Thin HttpClient services (one per resource)
    exercise-api.service.ts
    template-api.service.ts
    workout-api.service.ts
    lookup-api.service.ts
    user-api.service.ts
```

Auth login / `me` types live in `models/user.ts` and are used by `AuthService` (not a separate `auth-api` service).

Services are `@Injectable({ providedIn: 'root' })`, call `${environment.apiBaseUrl}/api/v1/...`, and return `Observable<T>` with generics matching the models. The JWT interceptor attaches `Authorization: Bearer …`; feature components should not re-implement HTTP.

### How to derive / update a typed client

When the backend API changes (or when adding a new endpoint):

1. **Change backend first** — DTO + controller (+ Flyway if schema); run tests; confirm Swagger shows the new shape.
2. **Open the Java DTO / enum** (and/or Swagger schema) and map to TypeScript:
   - `record FooResponse(...)` → `export interface Foo { ... }` (drop the `Response` suffix for entity-shaped types when clearer, e.g. `ExerciseResponse` → `Exercise`)
   - `record FooRequest(...)` → `export interface FooRequest { ... }`
   - Java `boolean` / `Boolean` → `boolean` (optional / nullable API fields as `field?: boolean | null`)
   - Java `Instant` → `string` (ISO-8601)
   - Java enum → string union or `enums.ts` const/type (must match names exactly)
3. **Update or add** `core/models/*.ts`.
4. **Update or add** `core/api/*-api.service.ts` methods (`get`/`post`/`put`/`patch`/`delete` + `HttpParams` for query strings).
5. **Wire UI** to the service; keep forms aligned with `*Request` types.
6. **Smoke-check** against a running API (`ng serve` + backend); prefer comparing a real JSON response to the interface.

### Naming & mapping conventions

| Backend | Frontend |
|---------|----------|
| `ExerciseResponse` | `Exercise` (or `ExerciseResponse` if you prefer 1:1 names) |
| `ExerciseRequest` | `ExerciseRequest` |
| `PageResponse<T>` | `PageResponse<T>` (`content`, `page`, `size`, `totalElements`, `totalPages`) |
| `RpeLevel.CHALLENGING` | `'CHALLENGING'` |
| Query params (`q`, `from`, `to`, `visibility`) | Same names on `HttpParams` / `*ListParams` |

Do **not** invent parallel field names (e.g. do not rename `custom` to `isCustom` in TS unless the JSON changes).

### What is intentionally not generated

- No `openapi-generator` / `ng-openapi-gen` / Orval pipeline in v1
- No committed `openapi.json` snapshot in the repo (fetch live from `/v3/api-docs` when needed)
- Regenerating clients is a **possible later improvement**; until then, treat hand mirrors + Swagger as the process

### API surface checklist (SPA must cover)

See also Swagger UI:

- Auth: login, me; Google callback
- Lookups: equipment, muscles
- Exercises CRUD + list filters
- Templates CRUD, clone, sets reorder
- Workouts CRUD, list range, sets reorder
- Users (admin): list/create/update/delete

---

## 9. Scaffold plan (when Phase 8 starts)

1. Install **Node 24 LTS**
2. `npx @angular/cli@21` new project in `frontend/` with routing, SCSS, **`--standalone=false`**
3. Configure schematics for non-standalone generation
4. `ng add @angular/material`
5. Core auth + shell + login
6. Feature modules wired to API
7. Update `STATUS.md` and root README frontend runbook (`ng serve` → `:4200`)

---

## 10. Locked decisions

Open questions resolved — all defaults accepted. Ready to scaffold Phase 8.

| # | Topic | Decision | Status |
|---|--------|----------|--------|
| A | Angular version | **Angular 21.x LTS** | Locked |
| B | JWT storage | **`localStorage`** | Locked |
| C | Dark mode in v1? | **Yes**, simple Material theme toggle | Locked |
| D | Markdown for exercise instructions? | **Yes**, sanitized markdown pipe/library | Locked |
| E | Set reorder UX | **CDK drag-drop** when practical; else up/down buttons | Locked |
| F | Measurements (Ryot)? | **No** (not in backend) | Locked |
| G | Package manager | **npm** | Locked |
| H | Material base theme | Whatever **`ng add @angular/material`** selects; + dark toggle | Locked |
| I | Home route | **`/`** (optional `/dashboard` redirect) | Locked |
| J | API base URL | Dev: **`''`** via `proxy.conf.json` → `:8080`; prod `environment.ts`: absolute backend URL | Locked |
| K | Change detection | **Zoneless** + **signals** for UI state (no `zone.js`) | Locked |

---

## 11. References

- [Angular releases / LTS](https://angular.dev/reference/releases)
- [Node.js releases](https://nodejs.org/en/about/previous-releases)
- [Angular Material](https://material.angular.dev/)
- Ryot demo (UX inspiration): https://demo.ryot.io/_s/acl_vUMPnPirkHlT

