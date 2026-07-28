# FitTrack — Frontend requirements (Angular)

Agent-readable SPA requirements. Product API/domain: [`REQUIREMENTS.md`](REQUIREMENTS.md). Progress: [`STATUS.md`](STATUS.md).

**Open questions accepted — ready to scaffold Phase 8.**

**Status:** Phase 8 scaffold started — Angular 21 NgModule app under `frontend/` (auth + shell + stubs). Feature CRUD next.

**UX reference (inspiration only):** [Ryot demo](https://demo.ryot.io/_s/acl_vUMPnPirkHlT) — collapsible side nav, Fitness area (Workouts / Templates / Exercises), list + search, theme toggle. FitTrack is **workout-focused** (no Ryot media/collections); do not copy Ryot branding or non-fitness domains.

---

## 1. Goals (v1 SPA)

- Authenticate (local username/password + optional Google SSO via backend handoff)
- Browse catalog exercises; create/edit/delete **own** custom exercises
- Manage workout **templates** (private/public) and **clone** to a workout
- Log and edit **workouts** (sets with client-controlled `setNumber` / reorder)
- Call FitTrack REST API with `Authorization: Bearer <jwt>`

### Non-goals (v1 SPA)

- Public self-registration (user management page is deferred **#9**)
- Offline-first / PWA sync
- Serving exercise images from free-exercise-db (paths may display later)
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
| Components | **NgModules — not standalone** | Preference locked: `standalone: false` on components/directives/pipes; declare in feature 
gModule`s |
| Routing | `AppRoutingModule` + feature routing modules | Lazy-load feature modules where sensible |
| HTTP | `HttpClientModule` (or `provideHttpClient` only if forced by CLI — prefer module style) | Interceptor attaches JWT |
| State | Services + RxJS (BehaviorSubject / signals optional later) | No NgRx required for v1 |
| Forms | Reactive forms | Prefer `ReactiveFormsModule` |
| Dates | Angular `DatePipe` / native `Date` / backend ISO-8601 instants | Align with `performedAt` Instant |

### CLI / project conventions

When scaffolding (
g new` / generate):

```text
--standalone=false
```

Set defaults in `angular.json` schematics:

```json
"@schematics/angular:component": { "standalone": false, "style": "scss" },
"@schematics/angular:directive": { "standalone": false },
"@schematics/angular:pipe": { "standalone": false }
```

Bootstrap via `AppModule` + `platformBrowserDynamic().bootstrapModule(AppModule)` (not standalone `bootstrapApplication` unless CLI forces a thin wrapper — convert to NgModule app).

Install Material: 
g add @angular/material` (pick a theme; support light/dark if inexpensive).

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
| Settings (minimal) | `/settings` | Display me + API base info; user mgmt later (#9) |

Unauthenticated: `/login`, `/auth/callback` (Google JWT hash handoff).

Out of scope vs Ryot: Media, Measurements, Collections, Discord links, Ryot Analytics as product.

---

## 4. Auth UX

1. **Local login** — form → `POST /api/v1/auth/login` → store JWT in **`localStorage` (locked)** → navigate home
2. **Google** — link/button to `{apiBase}/oauth2/authorization/google` (full page redirect). Callback route `/auth/callback` reads `#token=…`, stores JWT in **`localStorage`**, clears hash, navigates home
3. **HTTP interceptor** — `Authorization: Bearer <token>` on API calls; on 401 clear token and redirect to login
4. **Auth guard** — protect app routes; login page redirects away if already authenticated
5. **CORS** — SPA origin `http://localhost:4200` (already default on backend)

Environment (locked):

```ts
apiBaseUrl: 'http://localhost:8080'  // environment.ts — absolute URL; CORS already allows :4200
```

---

## 5. Feature screens (v1)

### Login
- Username / password, submit, error display
- Optional "Continue with Google" when SSO is configured (can always show; backend fails if unset)

### Exercises
- Paginated/filterable list (q, muscle, equipment, category) — Material table or list + filters
- Detail view (instructions markdown render, sanitized)
- Custom exercise create/edit/delete (owner only); catalog read-only in UI

### Templates
- List own templates; browse `visibility=PUBLIC`
- Create/edit with sets editor (exercise picker, setNumber, reps/weight/duration/distance/RPE)
- Enforce UI rule: PUBLIC templates cannot add custom exercises
- Clone → dialog for `performedAt` + name → create workout → navigate to workout

### Workouts
- List with date range filter (Material datepicker)
- Create/edit workout header (`performedAt`, name, difficulty, notes, duration)
- Sets table: add/remove/reorder (CDK drag-drop when practical; else up/down calling `PATCH .../sets/reorder` or full PUT — prefer reorder endpoint when only order changes)
- Show computed `totalWeightLifted` from API

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

## 8. API alignment checklist

SPA must cover (see backend OpenAPI `/swagger-ui.html`):

- Auth: login, me; Google callback
- Lookups: equipment, muscles
- Exercises CRUD + list filters
- Templates CRUD, clone, sets reorder
- Workouts CRUD, list range, sets reorder

Types: mirror backend enums (`WorkoutDifficulty`, `RpeLevel`, `TemplateVisibility`, `ExerciseLevel`, …).

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
| J | API base URL | **`http://localhost:8080`** in `environment.ts` | Locked |

---

## 11. References

- [Angular releases / LTS](https://angular.dev/reference/releases)
- [Node.js releases](https://nodejs.org/en/about/previous-releases)
- [Angular Material](https://material.angular.dev/)
- Ryot demo (UX inspiration): https://demo.ryot.io/_s/acl_vUMPnPirkHlT

