# Copilot Instructions for `referralPro`

## Build, run, and test commands

### Backend (Spring Boot / Maven)

- Start MySQL for local development: `docker-compose up -d`
- Compile backend: `.\mvnw.cmd -DskipTests compile`
- Full backend build: `.\mvnw.cmd clean install`
- Run backend locally: `.\mvnw.cmd spring-boot:run`
- Run all backend tests: `.\mvnw.cmd test`
- Run a single backend test class: `.\mvnw.cmd -Dtest=ReferralProApplicationTests test`

Current backend test caveat: `src\test\java\ai\actpro\referralPro\ReferralProApplicationTests.java` is packaged under `ai.actpro.referralPro`, while the application class is `com.actpro.referral.ReferralApplication`, so the existing smoke test does not currently discover a `@SpringBootConfiguration`.

### Frontend (`referralPro-dashboard`, Angular 21)

- Install dependencies: `cd referralPro-dashboard && npm install`
- Start dashboard dev server: `cd referralPro-dashboard && npm start`
- Build dashboard: `cd referralPro-dashboard && npm run build`
- Run dashboard tests once: `cd referralPro-dashboard && npm test -- --watch=false`
- Run a single dashboard spec file: `cd referralPro-dashboard && npm test -- --watch=false --include src/app/app.spec.ts`

Current frontend test caveat: the only spec file is the Angular scaffold test in `src/app/app.spec.ts`, and it currently fails because it still expects the default generated heading.

### Linting

- No dedicated lint command is currently configured in the root Maven project or in `referralPro-dashboard/package.json`.

## High-level architecture

This repository has two application surfaces:

1. **Spring Boot backend** in `src/main/java/com/actpro/referral`
2. **Angular dashboard** in `referralPro-dashboard`

The backend is organized by feature package (`company`, `campaign`, `user`, `referral`, `click`, `conversion`, `reward`, `auth`, `dashboard`, `security`, `common`) rather than by technical layer. Controllers stay thin and delegate business rules to services; persistence is handled with Spring Data repositories plus a few native SQL analytics queries in `dashboard/DashboardService.java`.

The domain flow is:

1. Company registers and receives an API key.
2. Company creates campaigns.
3. A referrer generates a referral link.
4. Public `GET /r/{referralCode}` records the click and redirects to the campaign landing page with `?ref=...`.
5. Conversion completion validates campaign state, event name, self-referral, and duplicate conversions.
6. Reward issuance creates rewards for both referrer and referee.

Database schema is owned by Flyway migrations in `src/main/resources/db/migration`. JPA entities inherit `common/BaseEntity` for `id`, `created_at`, and `updated_at`, with auditing enabled in `config/JpaConfig.java`.

Authentication is split by client type:

- **API key auth** protects the integration APIs (`/api/referrals/**`, `/api/conversions/**`, `/api/rewards/**`, company management endpoints).
- **JWT auth** protects dashboard endpoints (`/api/dashboard/**`, `/api/auth/me`).

Both auth paths populate `security/CompanyContext.java`, a `ThreadLocal<Company>` used throughout the service layer for tenant isolation.

The Angular app is a standalone-component app with route-level lazy loading:

- `app.routes.ts` keeps `/login` public and lazy-loads `/dashboard`.
- `core/guards/auth.guard.ts` blocks protected routes.
- `core/interceptors/auth.interceptor.ts` injects `Authorization: Bearer <token>`.
- `core/services/auth.service.ts` stores the JWT and current user in `localStorage`.
- `src/environments/environment.ts` points the dashboard at `http://localhost:8080/api`.

## Key conventions

- **Tenant scoping is mandatory.** Backend service logic should use `CompanyContext.getCurrentCompany()` or repository methods scoped by `companyId`; cross-tenant access is not the default.
- **Controllers return wrapped responses.** Successful JSON responses use `common/ApiResponse<T>`, while errors are shaped by `common/GlobalExceptionHandler` into `ErrorResponse`.
- **Write business rules in services, not controllers.** Examples include `ReferralService`, `ConversionService`, `RewardService`, and `CampaignService`.
- **Use Java records for request DTOs where the codebase already does.** The request DTOs in `company/dto`, `campaign/dto`, `referral/dto`, and `conversion/dto` follow that pattern.
- **Dashboard analytics live in native SQL, not repository-derived queries.** `dashboard/DashboardService.java` uses `EntityManager` and native SQL for overview, funnel, leaderboard, time-series, and reward summary endpoints.
- **Frontend code is organized by `core` / `features` / `shared`.** Reusable auth plumbing belongs in `core`; routed UI belongs in `features`; shared types live in `src/app/shared/models`.
- **The dashboard uses standalone Angular APIs.** Follow the existing pattern of standalone components, `provideRouter`, `provideHttpClient`, and functional guards/interceptors instead of introducing NgModules.
