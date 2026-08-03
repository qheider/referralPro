---
paths:
  - "src/main/resources/db/migration/**"
  - "src/main/java/**/*Entity.java"
  - "src/main/java/**/entity/**"
---

# Database / Flyway conventions

- Schema is owned by Flyway migrations in `src/main/resources/db/migration` (currently V1–V18; ambassador tables and indexes are V13–V18, including a reshape of `referrals`/`referral_clicks`).
- Never edit a migration that has already shipped — Flyway checksums applied migrations. A schema change is always a new `V{n+1}__description.sql`.
- New tenant-scoped tables should carry the standard audit columns (`id`, `created_at`, `updated_at`) matching `common/BaseEntity`, and an index on their company/tenant foreign key — tenant filtering happens in application code, not row-level security, so that index is what keeps it fast.
- Destructive changes (dropped columns/tables, narrowed types, `NOT NULL` without a backfill) need an explicit data-preserving path — this is a live multi-tenant schema.
