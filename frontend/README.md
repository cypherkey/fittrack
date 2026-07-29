# FitTrack frontend

Angular 21 SPA (NgModules + Angular Material). See [`../docs/FRONTEND.md`](../docs/FRONTEND.md).

```bash
npm install
npm start
```

Dev server: http://localhost:4200

`npm start` uses `proxy.conf.json` so `/api` and `/oauth2` are forwarded to `http://localhost:8080` (run the backend separately). Development `apiBaseUrl` is empty (same-origin).
