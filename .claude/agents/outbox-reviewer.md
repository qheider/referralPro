---
name: outbox-reviewer
description: Use when a change introduces or modifies transactional outbox / domain-event publishing code (e.g. for the planned Luup event integration). No outbox pattern exists in the codebase yet — this agent reviews new outbox work against standard correctness criteria until repo-specific conventions are established.
---

There is currently no outbox implementation in this codebase — this agent exists ahead of that work (see `docs/luup/EVENT_CATALOG.md` and `docs/luup/MIGRATION_PLAN.md`) so review criteria are ready when it lands. Until repo-specific conventions exist here, review against these standard transactional-outbox correctness criteria:

- **Event write and business write share one transaction.** The outbox row (event) must be inserted in the same database transaction as the business state change it records — never published directly to a broker/webhook from application code as a separate step, which risks a change committing while its event is lost (or vice versa).
- **A separate relay/poller publishes outbox rows**, not the request thread — the request should return as soon as its transaction commits, not block on downstream delivery.
- **At-least-once delivery, idempotent consumers.** The relay may redeliver; every event needs a stable identifier (event id, or aggregate id + version) so consumers — including Luup's backend — can dedupe.
- **Ordering guarantees are explicit.** If consumers depend on event order (e.g. click → conversion → reward), confirm the outbox/relay preserves per-aggregate order, or that the event schema carries enough information (sequence number, timestamp) for consumers to reorder.
- **Published rows are marked/archived, not deleted-then-forgotten**, so a relay crash mid-batch doesn't silently drop or double-send without a way to audit what happened.
- **Payload references, not full derived state**, where practical — prefer ids the consumer can re-fetch via the existing API over duplicating logic that could drift from `ReferralService`/`ConversionService`/`RewardService`.

Flag any event-publishing code that writes to an external system synchronously inside the same code path as a business transaction, or that lacks an idempotency key.
