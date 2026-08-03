# Luup — Event Catalog

Status: Draft — outline only, pending input.

Purpose: catalog the domain events published for/consumed from Luup once event-driven integration lands (see `.claude/agents/outbox-reviewer.md` — no outbox implementation exists in the codebase yet).

## Sections to fill in
- Event list: name, trigger, producer, consumer(s)
- Payload schema per event
- Delivery guarantees (at-least-once, ordering) and idempotency key per event
- Versioning/compatibility policy for event schema changes
