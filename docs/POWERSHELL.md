# FitTrack — PowerShell API client

Hand-maintained PowerShell module for scripting against `/api/v1` with a JWT Bearer token. Same “keep in sync with the API” discipline as the SPA clients in [`FRONTEND.md`](FRONTEND.md) §8.

## Location

| Path | Role |
|------|------|
| [`scripts/FitTrack/FitTrack.psd1`](../scripts/FitTrack/FitTrack.psd1) | Module manifest |
| [`scripts/FitTrack/FitTrack.psm1`](../scripts/FitTrack/FitTrack.psm1) | Cmdlets + HTTP helpers |
| [`scripts/Import-FitTrackWorkouts.ps1`](../scripts/Import-FitTrackWorkouts.ps1) | Import workouts/sets from `import.json` |
| [`scripts/import.config.example.json`](../scripts/import.config.example.json) | Example JWT + exercise map (copy → `import.config.json`) |

**Import (from repo root):**

```powershell
Import-Module .\scripts\FitTrack\FitTrack.psd1 -Force
Connect-FitTrack -Token '<jwt>' -BaseUrl 'http://localhost:8080'
```

Auth is session-scoped in the module (`Connect-FitTrack` / `Disconnect-FitTrack` / `Get-FitTrackSession`). There is no password login helper yet—SSO/script users supply a JWT.

## Legacy workout import

[`Import-FitTrackWorkouts.ps1`](../scripts/Import-FitTrackWorkouts.ps1) loads `scripts/import.json` (gitignored) and creates/updates FitTrack workouts for **one exercise at a time**.

1. Copy [`import.config.example.json`](../scripts/import.config.example.json) → `scripts/import.config.json` (gitignored). Set `token` (JWT) and `exerciseMap` (import name → FitTrack name or UUID).
2. **Check only** (no writes): verify the mapped exercise exists and that each matching import set has exactly the fields required by that exercise’s `trackedParameters` (reps / weight / duration / distance). Missing required or extra untracked fields fail the check:

```powershell
.\scripts\Import-FitTrackWorkouts.ps1 -Exercise 'Dumbbell Hammer Curl' -CheckOnly
```

3. **Import** sets for that exercise (lbs → kg). By default, if a FitTrack workout with the same `name` already exists, the script resolves its id and **appends** the new sets (keeping other exercises). If that workout already has sets for this exercise, it **warns and skips** unless you pass `-Force` (replace this exercise’s sets only):

```powershell
.\scripts\Import-FitTrackWorkouts.ps1 -Exercise 'Dumbbell Hammer Curl'
.\scripts\Import-FitTrackWorkouts.ps1 -Exercise 'Dumbbell Hammer Curl' -Force
```

A `409 Conflict` on create is treated the same way: look up the existing workout by name and append.

Optional: `-Map @{ 'Cable Face Pull' = 'Face Pull' }` merges over config `exerciseMap`; `-ConfigPath` / `-ImportPath` override paths; `-WhatIf` / `-Confirm` via `SupportsShouldProcess`.

Do **not** commit `import.config.json` or live JWTs.

## Covered API surface (must stay aligned)

When backend REST for these areas changes, update the module in the **same change** (or immediately after) as the SPA clients.

| Area | HTTP | Module cmdlets |
|------|------|----------------|
| Exercises | `GET /api/v1/exercise`, `GET /api/v1/exercise/{id}` | `Get-FitTrackExercise` |
| Exercises | `PUT/DELETE /api/v1/exercise/{id}/favorite` | `Set-FitTrackExerciseFavorite` |
| Templates | `GET /api/v1/templates`, `GET /api/v1/templates/{id}` | `Get-FitTrackTemplate` |
| Workouts | `GET /api/v1/workouts`, `GET /api/v1/workouts/team`, `GET /api/v1/workouts/{id}` | `Get-FitTrackWorkout` (`-Team`, `-ExerciseId`) |
| Workouts | `POST /api/v1/workouts`, `PUT /api/v1/workouts/{id}` | `Set-FitTrackWorkout` |
| Workout sets | `PATCH /api/v1/workouts/{id}/sets/{setId}` | `Set-FitTrackWorkoutSet` |

**Not covered yet** (do not invent unofficial endpoints here; add a cmdlet when product needs them): exercise create/update/delete/history/notes; template create/update/delete/clone/reorder; workout delete/start/complete/reorder; users; lookups; `me`.

Request/response field names must match Java DTOs / OpenAPI JSON exactly (e.g. `weightKg`, `useMetric`, `trackedParameters`, `rpe` enum strings `EASY` \| `CHALLENGING` \| `HARD`).

## How to update when the API changes

Mirror the SPA process ([FRONTEND.md](FRONTEND.md) §8):

1. **Change backend first** — DTO + controller (+ Flyway if schema); tests + Swagger confirm the shape.
2. **Update SPA** hand-mirrored models/services if the change affects UI clients.
3. **Update this module** in `scripts/FitTrack/FitTrack.psm1` (+ export list in `FitTrack.psd1` if adding/removing cmdlets):
   - New query params → optional parameters on the matching `Get-*` cmdlet and `Invoke-FitTrackApi -Query`.
   - New/changed request body fields → document in `Set-*` comment-based help and accept via `-Body` hashtables or explicit parameters (prefer explicit params for set patches; `-Body` for full workout replace).
   - Renamed/removed fields → update examples and any default payload shaping; do not keep stale aliases.
4. **Bump `ModuleVersion`** in `FitTrack.psd1` when the public cmdlet surface or required payload shape changes in a breaking way (minor bump for additive params).
5. **Smoke-check** against a running API:

```powershell
Import-Module .\scripts\FitTrack\FitTrack.psd1 -Force
Connect-FitTrack -Token $token -BaseUrl 'http://localhost:8080'
Get-FitTrackExercise -Size 5
Get-FitTrackTemplate
Get-FitTrackWorkout
```

## Agent / contributor checklist

When finishing a PR that changes REST DTOs or exercise/template/workout endpoints:

- [ ] SPA types/services updated per FRONTEND.md §8
- [ ] `scripts/FitTrack` cmdlets / help / exports updated for any **covered** surface above
- [ ] This doc’s “Covered API surface” table updated if a cmdlet was added or scope changed
- [ ] `ModuleVersion` bumped if the module public API changed

Do **not** commit JWTs, passwords, or live tokens in examples or scripts.

## Design notes

- PowerShell 5.1+ compatible (`ConvertTo-Json -Depth 20` for nested sets).
- `Invoke-FitTrackApi` is private to the module; callers use exported `Get-*` / `Set-*` / session cmdlets.
- List endpoints that return a JSON array emit **one pipeline object per element** (so `@()` / `foreach` work). Do not treat a bare `Object[]` as a single list item.
- Weight remains **kg** in API payloads (same as SPA/backend); no unit conversion in this module.