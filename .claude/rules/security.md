---
paths:
  - "src/main/java/**/*.java"
---

# Tenant isolation and auth conventions

- Tenant scoping is mandatory: never trust a client-supplied company/ambassador id without checking it against the authenticated principal.
- Prefer `security/CurrentUserService` over `security/CompanyContext` in new/ambassador-era code. `CompanyContext` (a `ThreadLocal<Company>`) is legacy and effectively API-key-flow-only — don't extend it further.
- `CurrentUserService` exposes `getCurrentUserId()`, `getCurrentCompanyId()`, `getCurrentUserRole()`, `getCurrentAmbassadorId()` / `getCurrentAmbassadorProfile()` (throws `AccessDeniedException` if the caller isn't an ambassador), and `assertCurrentCompanyAccess(companyId)` for explicit cross-tenant guards — use the guard at any boundary where a company id crosses from request into a cross-tenant-capable query.
- Two auth mechanisms run as servlet filters ahead of `UsernamePasswordAuthenticationFilter`: `ApiKeyAuthenticationFilter` (integration endpoints, `Authorization: ApiKey {key}`, grants `ROLE_COMPANY`) and `JwtAuthenticationFilter` (dashboard/ambassador endpoints, `Authorization: Bearer {jwt}`, grants `ROLE_{role}` for `PLATFORM_ADMIN` / `COMPANY_ADMIN` / `AMBASSADOR` / `CUSTOMER`). Match `@PreAuthorize` role checks to the endpoint's actual intended audience.
