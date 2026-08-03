# CLAUDE.md (referralPro-dashboard)

Guidance specific to the Angular dashboard. See the repo-root `CLAUDE.md` for backend and cross-cutting conventions.

## Commands

```powershell
cd referralPro-dashboard
npm install
npm start                                          # dev server on :4200, proxies API from environment.ts
npm run build                                      # production build -> dist/referral-pro-dashboard/
npm test -- --watch=false                          # run all specs once
npm test -- --watch=false --include src/app/app.spec.ts   # run a single spec
```

No lint command is configured in either the Maven project or `package.json`.

## Frontend structure (`src/app/`)

Standalone-component Angular app, no NgModules:

- `core/` — `guards/auth.guard.ts` (route protection), `interceptors/auth.interceptor.ts` (injects `Authorization: Bearer <token>`), `services/` (auth + dashboard API services). JWT and current user are stored in `localStorage`.
- `features/` — routed UI, lazy-loaded per area: `auth/` (login), `dashboard/` (company admin analytics, `dashboard.routes.ts`), `ambassador/` (ambassador self-service portal — dashboard, campaigns, campaign detail, referrals, analytics, profile, `ambassador.routes.ts`, `ambassador-layout.component.ts` as the shell).
- `shared/` — `models/` (TypeScript interfaces mirroring backend DTOs), `components/`, `utils/`.
- `app.routes.ts` keeps `/login` public and lazy-loads the `dashboard` and `ambassador` feature areas.
- `src/environments/environment.ts` / `environment.prod.ts` set `apiUrl` (defaults to `http://localhost:8080/api` in dev).

## Key conventions

- **`core` for reusable auth plumbing, `features` for routed UI, `shared` for cross-feature types/components** — follow existing standalone-component, `provideRouter`/`provideHttpClient`, functional-guard/interceptor patterns; don't introduce NgModules.
