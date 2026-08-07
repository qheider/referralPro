# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

ReferralPro is a multi-tenant referral marketing platform: a Spring Boot 3 / Java 21 backend API plus an Angular 21 dashboard (`referralPro-dashboard/`). Companies register, create campaigns, generate referral links for referrers, track clicks/conversions, and issue rewards. A newer **ambassador** module layers a dedicated ambassador portal (self-service referral links, analytics, profile) and a company-side admin flow for recruiting and assigning ambassadors to campaigns on top of the original referral flow.

## Commands

### Backend (Spring Boot / Maven, run from repo root)

```powershell
docker-compose up -d                              # start MySQL (required before running backend)
.\mvnw.cmd -DskipTests compile                     # quick compile
.\mvnw.cmd clean install                           # full build
.\mvnw.cmd spring-boot:run                         # run backend on :8080
.\mvnw.cmd test                                    # run all backend tests
.\mvnw.cmd -Dtest=CurrentUserServiceTest test       # run a single test class
.\mvnw.cmd -Dtest=CurrentUserServiceTest#shouldRejectCrossCompanyAccess test  # single test method
```

Known caveat: `src/test/java/ai/actpro/referralPro/ReferralProApplicationTests.java` is packaged under `ai.actpro.referralPro`, but the application class is `com.actpro.referral.ReferralApplication` — this smoke test does not discover a `@SpringBootConfiguration` and will fail/be skipped as a context-load test. The real unit tests live under `src/test/java/com/actpro/referral/...` (e.g. `security/CurrentUserServiceTest`, `ambassador/AmbassadorAdminServiceTest`, `ambassador/CampaignAssignmentServiceTest`, `campaign/CampaignControllerSecurityTest`) and use Mockito rather than a full Spring context; `src/test/resources/application-test.yml` backs any tests that do boot a context (H2 in-memory DB is on the classpath for this).

See `referralPro-dashboard/CLAUDE.md` for frontend commands and structure.

### Docker (full stack)

```powershell
docker-compose up -d --build   # MySQL (3306), backend (8080), frontend (80)
docker-compose logs -f backend
```

## Architecture

### Backend package layout (`src/main/java/com/actpro/referral/`)

Organized by feature, not by layer: `company`, `campaign`, `user`, `referral`, `click`, `conversion`, `reward`, `auth`, `ambassador`, `dashboard`, `security`, `common`, `config`. Controllers stay thin and delegate to services; persistence uses Spring Data repositories plus native SQL for analytics.

Core referral domain flow:
1. Company registers (`POST /api/companies/register`) and receives an API key.
2. Company creates campaigns.
3. A referral link is generated for a referrer (direct customer referral, or via an assigned ambassador).
4. Public `GET /r/{referralCode}` records the click and redirects to the campaign landing page with `?ref=...`.
5. Conversion completion validates campaign state, event name, self-referral, and duplicate conversions.
6. Reward issuance creates rewards for both referrer and referee.

Ambassador flow (layered on top of the above): a company admin creates ambassador profiles and assigns them to campaigns (`ambassador/AmbassadorAdminController` + `AmbassadorAdminService`, `CampaignAssignmentController` + `CampaignAssignmentService`); ambassadors then self-serve through `ambassador/AmbassadorPortalController` + `AmbassadorPortalService` (`/api/ambassador/**`, requires `ROLE_AMBASSADOR`) to view their dashboard, campaigns, referral links, referral history, analytics, and profile.

Database schema is owned by Flyway migrations in `src/main/resources/db/migration` (currently V1–V18; ambassador tables and indexes are V13–V18, including a reshape of `referrals`/`referral_clicks`). JPA entities extend `common/BaseEntity` for `id`/`created_at`/`updated_at`, with auditing enabled in `config/JpaConfig.java`.

### Authentication and tenant isolation (`security/`)

Two authentication mechanisms run as servlet filters ahead of `UsernamePasswordAuthenticationFilter`, registered in `SecurityConfig`:

- **`ApiKeyAuthenticationFilter`** — protects integration endpoints (`/api/referrals/**`, `/api/conversions/**`, `/api/rewards/**`), each guarded by `@PreAuthorize("hasRole('COMPANY')")` on its controller. Reads `Authorization: ApiKey {key}`, resolves a `Company`, and authenticates with authority `ROLE_COMPANY`. `/api/companies/register` is public; every other path under `/api/companies/**` (e.g. `CampaignController`) is a JWT/`COMPANY_ADMIN` dashboard flow via `CurrentUserService`, not part of the API-key surface — an API-key caller reaching those endpoints gets rejected because `CurrentUserService` requires an `AuthenticatedUser` principal that the API-key filter never sets.
- **`JwtAuthenticationFilter`** — protects dashboard/ambassador endpoints (`/api/dashboard/**`, `/api/auth/me`, `/api/ambassador/**`). Reads `Authorization: Bearer {jwt}`, resolves a `DashboardUser`, and authenticates with a `AuthenticatedUser(userId, username, companyId, role)` principal plus authority `ROLE_{role}` (roles: `PLATFORM_ADMIN`, `COMPANY_ADMIN`, `AMBASSADOR`, `CUSTOMER` — see `auth/UserRole`). `@PreAuthorize` on controllers (e.g. `AmbassadorPortalController` requires `hasRole('AMBASSADOR')`) enforces role checks.

Both filters populate `security/CompanyContext` (a `ThreadLocal<Company>`) for the duration of the request and clear it in a `finally` block — but **prefer `security/CurrentUserService` over `CompanyContext`** in new/ambassador-era code: it reads the `AuthenticatedUser` principal off `SecurityContextHolder` and exposes `getCurrentUserId()`, `getCurrentCompanyId()`, `getCurrentUserRole()`, `getCurrentAmbassadorId()` / `getCurrentAmbassadorProfile()` (throws `AccessDeniedException` if the caller isn't an ambassador), and `assertCurrentCompanyAccess(companyId)` for explicit cross-tenant guards. `CompanyContext` predates the role-aware principal and is effectively API-key-flow-only at this point; don't extend it further — extend `CurrentUserService` instead.

## Key conventions

- **Tenant scoping is mandatory.** Use `CurrentUserService` (preferred) or `CompanyContext.getCurrentCompany()` (legacy API-key paths) in service logic; never trust a client-supplied company/ambassador id without checking it against the authenticated principal.
- **Controllers return wrapped responses.** Success responses use `common/ApiResponse<T>`; errors are shaped by `common/GlobalExceptionHandler` into `common/ErrorResponse`, backed by `common/exception/{BadRequestException,NotFoundException,UnauthorizedException}`.
- **Business rules live in services, not controllers** (e.g. `ReferralService`, `ConversionService`, `RewardService`, `CampaignService`, `AmbassadorAdminService`, `AmbassadorPortalService`, `CampaignAssignmentService`).
- **Request/response DTOs are Java records**, under each feature's `dto/` package.
- **Dashboard analytics use native SQL, not derived repository queries** — `dashboard/DashboardService.java` uses `EntityManager` for overview, funnel, leaderboard, time-series, and reward-summary endpoints. Follow this pattern for new cross-entity analytics rather than composing JPA repository methods.
