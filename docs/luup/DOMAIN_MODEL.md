# Luup — Domain Model

Status: Draft — outline only, pending input.

Purpose: define the domain entities and relationships specific to the Luup integration, and how they map onto ReferralPro's existing model (`Company`, `Campaign`, referral/click/conversion/reward entities — see repo-root `CLAUDE.md` for the current schema and `src/main/resources/db/migration` for the source of truth).

## Sections to fill in
- New or extended entities required for this integration
- Relationship to existing `Company`/`Campaign`/referral/conversion/reward entities
- Identity mapping (Luup's `externalUserId` vs. ReferralPro's internal ids)
- Invariants and lifecycle rules
