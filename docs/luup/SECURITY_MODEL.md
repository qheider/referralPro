# Luup — Security Model

Status: Draft — outline only, pending input.

Purpose: define how the deeper Luup integration authenticates, authorizes, and stays tenant-isolated, building on ReferralPro's existing `ApiKeyAuthenticationFilter` / `JwtAuthenticationFilter` and `CurrentUserService` conventions (see `.claude/rules/security.md`).

## Sections to fill in
- Auth mechanism for new/changed endpoints (API key, JWT, or a new mechanism)
- Tenant-isolation guarantees specific to this integration
- Secrets/credential handling on the Luup side
- Threat model notes specific to the new surface area (e.g. event delivery, webhooks)
