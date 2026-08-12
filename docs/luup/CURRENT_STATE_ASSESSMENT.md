# Current-State Assessment

Status: Complete — Phase 1 of the Luup ambassador-capability migration (see `docs/luup/IMPLEMENTATION_PLAN.md` once drafted, and the phase plan tracked outside this repo).

Read-only investigation performed 2026-08-02. No production code was modified to produce this document. All file:line citations were verified directly against the source tree on branch `Luupchamp-integration-claude`.

## 1. Architecture and package map

Package root: `com.actpro.referral`, organized by feature under `src/main/java/com/actpro/referral/`:

`company`, `campaign`, `user`, `referral`, `click`, `conversion`, `reward`, `auth`, `ambassador`, `dashboard`, `security`, `common`, `config`.

Controllers are thin and delegate to services; persistence uses Spring Data repositories plus native SQL (`dashboard/DashboardService.java`) for analytics. Two parallel referral mechanisms currently coexist in the `referral` and `click` packages (see §5.1) — this is the most consequential architectural fact for planning subsequent phases.

Frontend: `referralPro-dashboard/` (Angular 21), out of scope for this assessment; see `referralPro-dashboard/CLAUDE.md`.

## 2. Database tables and Flyway inventory

Latest applied migration is **V18**; the next migration must be **V19**.

| Version | File | Purpose |
|---|---|---|
| V1 | `V1__create_companies.sql` | `companies` table |
| V2 | `V2__create_campaigns.sql` | `campaigns` table |
| V3 | `V3__create_platform_users.sql` | `platform_users` (referrer/customer identities) |
| V4 | `V4__create_referrals.sql` | `referrals` table |
| V5 | `V5__create_referral_clicks.sql` | `referral_clicks` table |
| V6 | `V6__create_conversions.sql` | `conversions` table |
| V7 | `V7__create_rewards.sql` | `rewards` table |
| V8 | `V8__create_dashboard_users.sql` | `dashboard_users` (company admin logins) |
| V9 | `V9__add_missing_updated_at_columns.sql` | Audit-column backfill |
| V10 | `V10__insert_test_dashboard_users.sql` | Seed data |
| V11 | `V11__add_company_registration_fields.sql` | Company profile fields |
| V12 | `V12__add_dashboard_user_profile_fields.sql` | Dashboard user profile fields |
| V13 | `V13__create_ambassador_profiles.sql` | `ambassador_profiles` (ambassador module start) |
| V14 | `V14__create_campaign_ambassador_assignments.sql` | `campaign_ambassador_assignments` |
| V15 | `V15__create_referral_links.sql` | `referral_links` — introduces the `ReferralLink`/`publicToken` model, distinct from `referrals.referral_code` |
| V16 | `V16__reshape_referral_clicks.sql` | Reshaped `referral_clicks` to add `referral_link_id`, `company_id`, `campaign_id`, `ambassador_user_id`, `session_id`, `ip_hash`, `referrer_url` columns |
| V17 | `V17__reshape_referrals.sql` | Reshaped `referrals` (added `referral_link_id` FK to `ReferralLink`, ambassador linkage) |
| V18 | `V18__add_ambassador_indexes.sql` | Indexes for ambassador queries |

V16/V17 added the columns needed for full click attribution and for linking a `Referral` to a `ReferralLink`, but — per §5.1/§5.2 — the application code that would populate and use those columns was never finished.

## 3. Authentication and authorization paths

Two servlet filters run ahead of `UsernamePasswordAuthenticationFilter` (`security/`):

- **`ApiKeyAuthenticationFilter`** (`security/ApiKeyAuthenticationFilter.java`) — protects integration endpoints. Reads `Authorization: ApiKey {key}` (line 65-72), looks up `companyRepository.findByApiKey(apiKey)` (line 76), authenticates with a `PreAuthenticatedAuthenticationToken` carrying `ROLE_COMPANY` (line 90-97). Explicitly skips public endpoints including `/r/**` (line 113) and JWT-protected endpoints (`/api/dashboard/**`, `/api/auth/me`, line 120-121).
- **`JwtAuthenticationFilter`** (`security/JwtAuthenticationFilter.java`) — protects dashboard/ambassador endpoints. Reads `Authorization: Bearer {jwt}` (line 80-86), validates via `JwtTokenProvider`, re-verifies the resolved `DashboardUser` is `ACTIVE` and matches company/username/role from the token (line 45-50), authenticates with principal `AuthenticatedUser(userId, username, companyId, role)` and authority `ROLE_{role}` (line 55-64).

Both filters populate `CompanyContext` and clear it in a `finally` block that also calls `SecurityContextHolder.clearContext()` (`ApiKeyAuthenticationFilter.java:100-106`, `JwtAuthenticationFilter.java:72-77`).

### `CompanyContext` vs `CurrentUserService`

- `CompanyContext` (`security/CompanyContext.java`) is a bare `ThreadLocal<Company>` with `setCurrentCompany`/`getCurrentCompany`/`clear` — no user identity, no role, no ambassador linkage. Both filters populate it, but it's effectively only meaningful on the API-key path today (the JWT path also sets it, but nothing in ambassador-era code reads it).
- `CurrentUserService` (`security/CurrentUserService.java`) is the modern, principal-aware alternative: `getCurrentUserId()`, `getCurrentCompanyId()`, `getCurrentUserRole()` (lines 30-40) read off the `AuthenticatedUser` principal; `getCurrentAmbassadorId()`/`getCurrentAmbassadorProfile()` (lines 42-62) throw `AccessDeniedException` if the caller isn't `UserRole.AMBASSADOR`; `assertCurrentCompanyAccess(companyId)` (line 64-68) is the explicit cross-tenant guard. All ambassador-package services (`AmbassadorAdminService`, `AmbassadorPortalService`, `CampaignAssignmentService`) already use this exclusively — no ambassador-era code depends on `CompanyContext`.

This matches `CLAUDE.md`'s documented convention; no drift found.

## 4. Confirmed gaps (this phase's primary finding)

### 4.1 Ambassador referral links are non-functional for click tracking (higher severity than the source document assumed)

Two distinct, unconnected link mechanisms exist side by side:

- **Legacy/direct API-key flow** (`docs/LUUP_REFERRAL_RUNDOWN.md`, the flow Luup currently integrates against): `POST /api/referrals/generate` creates a `Referral` with a `referralCode` (`Referral.java:47-48`, unique) and a plain string `referralLink` column (`Referral.java:50-51` — a stored URL, not a relation). `GET /r/{referralCode}` (`ReferralRedirectController.java:26-33`) looks the referral up by `referralRepository.findByReferralCodeWithCampaign(referralCode)` and 404s via `NotFoundException("Referral code not found")` if not found. This path works today and is what the existing `docs/LUUP_REFERRAL_RUNDOWN.md` documents.
- **Ambassador flow** (added V15, `ReferralLink.java`): `ReferralLink` has its own `publicToken` (line 44-45, unique) plus `company`, `campaign`, `ambassadorUser`, `assignment`, `destinationUrl`, `status`, `clickCount`, `expiresAt`. `AmbassadorPortalService.toReferralLinkSummary` (`AmbassadorPortalService.java:487-497`) builds the URL an ambassador is actually shown as `baseUrl + "/r/" + referralLink.getPublicToken()` (line 491).

**The gap:** `ReferralRedirectController` only has one route, `/r/{referralCode}`, and it only queries the `Referral` repository (§ above) — it never queries `ReferralLinkRepository` by `publicToken`. Because `ReferralLink.publicToken` values are generated independently of `Referral.referralCode` values, an ambassador's shared link (`/r/{publicToken}`) will almost always 404 today. This is a functional break in the ambassador self-serve flow, not a cosmetic one — it's more severe than the source document's framing ("resolved through the wrong field"), because there's no fallback path at all, not just a wrong-field lookup.

### 4.2 Click recording requires a pre-existing `Referral` and leaves most attribution columns unset

`ReferralClickService.recordClick(Referral referral, String ipAddress, String userAgent)` (`click/ReferralClickService.java:17-26`) takes an already-resolved `Referral` as a required parameter and only sets `referral`, `ipAddress`, `userAgent` (lines 21-23) before saving.

`ReferralClick` (`click/ReferralClick.java`) declares, but the service never populates: `referralLink` (line 31-32), `company` (line 35-36), `campaign` (line 38-40), `ambassadorUser` (line 42-44), `sessionId` (line 46-47), `ipHash` (line 52-53), `referrerUrl` (line 58-59 — the controller never reads the `Referer` header at all, `ReferralRedirectController.java:27-37`). `clickedAt` (line 61-62) does get a value, but only via the field initializer default (`LocalDateTime.now()` at class-load/instantiation time), not an explicit service-set timestamp.

Because `recordClick` requires a `Referral`, and §4.1 means ambassador links can't resolve to anything, a click on an ambassador link cannot currently be recorded at all — it 404s before `ReferralClickService` is ever invoked.

### 4.3 API keys are stored and compared as plaintext

`Company.apiKey` (`company/Company.java:26-27`) is a single `@Column(name = "api_key", nullable = false, unique = true) String apiKey` on the `Company` entity — there is no separate key-id/secret-hash model, no scopes, no expiry, no rotation, no revocation. `ApiKeyAuthenticationFilter.doFilterInternal` (`ApiKeyAuthenticationFilter.java:72-76`) extracts the raw header value and does a direct `companyRepository.findByApiKey(apiKey)` lookup — a plaintext equality comparison at the database level. The key is generated once at company registration and never rotates; there is no revocation path other than manual DB edits.

### 4.4 Ambassador account creation has no delivery mechanism for the generated credential

`AmbassadorAdminService.createAmbassador` (`ambassador/AmbassadorAdminService.java:52-85`) calls `generateTemporaryPassword()` (line 332-334 — a random 24-character string from `PASSWORD_ALPHABET`), encodes it with `passwordEncoder.encode(...)` (line 66), and sets `DashboardUser.status = PENDING` / `AmbassadorProfile.status = INVITED` (lines 70, 81). **The generated plaintext password is never returned, logged, emailed, or stored anywhere accessible** — it exists only in the local variable passed to `passwordEncoder.encode(...)`. A repo-wide search for email/mail-sending code and for "invit"/"token" usage outside this service, `AmbassadorProfile`, and the `AmbassadorStatus.INVITED` enum constant found nothing — there is no invitation-token entity, no email queue, no notification of any kind. As written, a newly created ambassador has no way to learn their password and log in; this flow is currently a dead end.

### 4.5 No transactional outbox, no scheduled workers

A repository-wide, case-insensitive search for "outbox" under `src/main/java` returned zero matches. A search for `@Scheduled` also returned zero matches. Nothing in the codebase today performs deferred/asynchronous side-effect processing, retry, or event publication of any kind. Every phase from "Ambassador Applications" onward in the roadmap (see below) that emits an event assumes this infrastructure exists — it must be built (Phase 6) before those phases can be implemented as specified.

## 5. Existing ambassador/campaign/referral/conversion/reward behavior (baseline, working today)

Per `CLAUDE.md` and confirmed by `docs/LUUP_REFERRAL_RUNDOWN.md`:

1. Company registers (`POST /api/companies/register`), receives a plaintext API key (§4.3).
2. Company creates campaigns (`POST /api/companies/{companyId}/campaigns`) with `rewardType`, `referrerRewardValue`, `refereeRewardValue`, `conversionEventName`, active date range.
3. A referral link is generated for a referrer — either directly (`POST /api/referrals/generate`, external/API-key flow) or, in the newer ambassador flow, an ambassador is assigned to a campaign (`ambassador/CampaignAssignmentService`) and gets a `ReferralLink` (currently broken per §4.1).
4. Public `GET /r/{code}` records a click and redirects to the campaign landing page with `?ref=...` — functional for the direct/API-key flow, non-functional for ambassador links.
5. `POST /api/conversions` validates campaign active-date range, exact `eventName` match, blocks self-referral and duplicate conversions.
6. Reward issuance auto-generates coupon codes for both referrer and referee, retrievable via `GET /api/rewards/users/{externalUserId}`.

Ambassador-side additions on top of this (`ambassador/AmbassadorAdminController`+`AmbassadorAdminService`, `CampaignAssignmentController`+`CampaignAssignmentService`, `AmbassadorPortalController`+`AmbassadorPortalService`, `ROLE_AMBASSADOR`-gated `/api/ambassador/**`): ambassador CRUD, campaign assignment, self-serve dashboard/campaigns/referral-links/history/analytics/profile — all correctly tenant-scoped via `CurrentUserService`, but built on top of the broken link-resolution path (§4.1).

## 6. Existing test coverage

Real unit tests live under `src/test/java/com/actpro/referral/`:
- `security/CurrentUserServiceTest.java`
- `campaign/CampaignControllerSecurityTest.java`
- `ambassador/AmbassadorAdminServiceTest.java`
- `ambassador/CampaignAssignmentServiceTest.java`

These are Mockito-based (no full Spring context). Separately, `src/test/java/ai/actpro/referralPro/ReferralProApplicationTests.java` is packaged under `ai.actpro.referralPro` while the application class is `com.actpro.referral.ReferralApplication` — this smoke test does not discover a `@SpringBootConfiguration` and will fail/be skipped as a context-load test (documented, known issue — not something this assessment is flagging as new).

No tests currently exercise `ReferralRedirectController`, `ReferralClickService`, `ApiKeyAuthenticationFilter`, or the ambassador-link resolution path — consistent with those paths being broken/incomplete rather than regressed.

## 7. Dependency map — which later phases are blocked by which confirmed gaps

| Gap | Blocks |
|---|---|
| §4.1/§4.2 Referral-link resolution & click attribution | Phase 2 (direct fix); indirectly blocks Phase 8 (Public Referred-Customer Journey) and Phase 9 (Referral Workflow), both of which assume working, fully-attributed clicks |
| §4.3 Plaintext API keys | Phase 3 (direct fix); no other phase strictly depends on this, but it's a standing credential-exposure risk independent of the Luup work |
| §4.4 No ambassador invitation delivery | Phase 4 (direct fix); blocks Phase 7 (Ambassador Applications), which explicitly wires approval → invitation in the same transaction |
| §4.5 No outbox / no scheduled workers | Phase 6 (direct build-out); blocks every phase from 7 onward that emits or consumes an event (7, 8, 9, 10, 11, 12, 13, 16), and blocks any worker-based reconciliation in 11/12/16 |

No gap found in this assessment blocks Phase 5 (Tenant Authorization Foundation) — `CurrentUserService` is already sound and consistently used in ambassador-era code; Phase 5 is closure/hardening, not a bug fix.

## Summary

The codebase's existing direct/API-key referral flow (the one `docs/LUUP_REFERRAL_RUNDOWN.md` documents and Luup integrates against today) works as designed. The newer ambassador layer is architecturally sound where it depends on `CurrentUserService` and tenant scoping, but its self-serve referral link is currently **non-functional end-to-end** (§4.1/§4.2), its account-creation flow is a **dead end** with no way for a new ambassador to receive credentials (§4.4), and the platform has no credential-security hardening (§4.3) or event/async infrastructure (§4.5) yet. These four gaps are the correct Phase 2–6 scope, in the order already sequenced.
