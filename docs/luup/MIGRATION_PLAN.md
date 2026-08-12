# Luup — Migration Plan

Status: Draft — outline only, pending input.

Purpose: define how existing Luup integration data/traffic (currently the direct API-key flow in `docs/LUUP_REFERRAL_RUNDOWN.md`) transitions to whatever this initiative introduces, without breaking Luup's live integration.

## Sections to fill in
- Current state vs. target state
- Backward-compatibility window / dual-write or dual-read strategy if needed
- Data backfill requirements (see `.claude/rules/database.md` for migration conventions)
- Rollback plan
