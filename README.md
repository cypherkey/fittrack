# FitTrack

Personal fitness tracker: exercises, templates, and workouts.

- **Backend:** Spring Boot 4 + SQLite + local auth / Google OAuth + JWT
- **Frontend:** Angular (planned)
- **Container:** `backend/Dockerfile`
- **Spec:** [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md)

## Layout

```
backend/     Spring Boot API (+ Dockerfile)
frontend/    Angular SPA
docs/        Requirements and design
```

## Backend

Requires JDK 25+. From `backend/`:

```bash
./mvnw test
./mvnw spring-boot:run
```

Default local user: `admin` / `admin`. API base: `http://localhost:8080`.

Example:

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"admin\"}"
```