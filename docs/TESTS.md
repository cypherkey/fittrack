# FitTrack — Backend tests

Inventory of automated tests under `backend/src/test`. Run from `backend/`:

```bash
# Linux / macOS
./mvnw test
# Windows
.\mvnw.cmd test
```

## Test classes

### `FitTrackApplicationTest`

| Method | Covers |
|--------|--------|
| `contextLoads` | Spring context starts with SQLite + Flyway + security |

### `ExerciseCatalogSeederTest`

| Method | Covers |
|--------|--------|
| `mapsLevelAndMechanic` | Seed enum mapping |
| `instructionsBecomeMarkdownList` | Instructions → markdown |
| `trackedParametersFollowCategoryHeuristicAsFallback` | Category → bitmasks (fallback) |
| `mapsRyotLotToTrackedParameterBits` | Ryot lot → bitmask |

### `GoogleOAuthSuccessHandlerTest`

| Method | Covers |
|--------|--------|
| `redirectsToSpaWithJwtFragment` | OAuth success → SPA redirect `#token=<jwt>` |

### `GoogleOAuthEnabledConditionTest`

| Method | Covers |
|--------|--------|
| `disabledWhenCredentialsMissing` | Google OAuth off without credentials |
| `enabledWhenClientIdAndSecretPresent` | Google OAuth on when id+secret set |

### `ApiIntegrationTest`

| Method | Covers |
|--------|--------|
| `listsSeededCatalogExercisesAndSupportsCustomCrud` | `GET /exercises`, `GET /equipment`, custom exercise POST/PUT/DELETE |
| `templatesAndWorkoutsRoundTripWithClone` | Template create, clone → workout, start/complete, workout GET/PUT |

### `EndpointCoverageTest`

| Method | Covers |
|--------|--------|
| `authMeAndActuatorAndOpenApi` | Login, `GET /me` (incl. `admin`), health, info 401, OpenAPI |
| `adminUserManagement` | Admin users CRUD (`/api/v1/users`); non-admin 403; cannot delete self |
| `lookupsExercisesTemplatesWorkoutsAndReorder` | Equipment, muscles, exercise CRUD, template CRUD + reorder + clone, workout list/create/get/put/reorder/delete |
| `authorizationEdgesForPrivateTemplatesAndWorkouts` | PUBLIC+custom create allowed; private template hidden from other; workout forbidden for non-owner |

## Endpoints vs tests (checklist)

| Endpoint | Exercised by |
|----------|--------------|
| `POST /api/v1/auth/login` | All MockMvc tests (setup) |
| `GET /api/v1/me` | `EndpointCoverageTest.authMeAndActuatorAndOpenApi` |
| `GET /api/v1/users` | `EndpointCoverageTest.adminUserManagement` |
| `POST /api/v1/users` | `EndpointCoverageTest.adminUserManagement` |
| `PUT /api/v1/users/{id}` | `EndpointCoverageTest.adminUserManagement` |
| `DELETE /api/v1/users/{id}` | `EndpointCoverageTest.adminUserManagement` |
| `GET /oauth2/authorization/google` | Manual / enabled only with credentials; condition + success handler unit-tested |
| `GET /api/v1/equipment` | `ApiIntegrationTest`, `EndpointCoverageTest` |
| `GET /api/v1/muscles` | `EndpointCoverageTest` |
| `GET /api/v1/exercise` | `ApiIntegrationTest`, `EndpointCoverageTest` |
| `GET /api/v1/exercise/{id}` | `EndpointCoverageTest` |
| `GET /api/v1/exercise/{id}/history` | `ApiIntegrationTest`, `EndpointCoverageTest` |
| `POST /api/v1/exercise` | `ApiIntegrationTest`, `EndpointCoverageTest` |
| `PUT /api/v1/exercise/{id}` | `ApiIntegrationTest`, `EndpointCoverageTest` |
| `DELETE /api/v1/exercise/{id}` | `ApiIntegrationTest`, `EndpointCoverageTest` |
| `GET /api/v1/templates` | `EndpointCoverageTest` |
| `GET /api/v1/templates/{id}` | `EndpointCoverageTest`, authz edge |
| `POST /api/v1/templates` | `ApiIntegrationTest`, `EndpointCoverageTest` |
| `PUT /api/v1/templates/{id}` | `EndpointCoverageTest` |
| `PATCH /api/v1/templates/{id}/sets/reorder` | `EndpointCoverageTest` |
| `DELETE /api/v1/templates/{id}` | `EndpointCoverageTest` |
| `POST /api/v1/templates/{id}/clone` | `ApiIntegrationTest`, `EndpointCoverageTest` |
| `GET /api/v1/workouts` | `EndpointCoverageTest` |
| `GET /api/v1/workouts/{id}` | `ApiIntegrationTest`, `EndpointCoverageTest` |
| `POST /api/v1/workouts` | `EndpointCoverageTest` |
| `PUT /api/v1/workouts/{id}` | `ApiIntegrationTest`, `EndpointCoverageTest` |
| `PATCH /api/v1/workouts/{id}/sets/reorder` | `EndpointCoverageTest` |
| `POST /api/v1/workouts/{id}/start` | `ApiIntegrationTest`, `EndpointCoverageTest` |
| `POST /api/v1/workouts/{id}/complete` | `ApiIntegrationTest`, `EndpointCoverageTest` |
| `DELETE /api/v1/workouts/{id}` | `EndpointCoverageTest` |
| `GET /actuator/health` | `EndpointCoverageTest` |
| `GET /v3/api-docs` | `EndpointCoverageTest` |

Update this file when adding or renaming tests.
