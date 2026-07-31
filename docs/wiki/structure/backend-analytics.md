---
kind: structure
slug: backend-analytics
source_paths: [apps/backend/analytics-service/**]
decisions: [ADR-0008, ADR-0010, ADR-0019, ADR-0023]
last_compiled: 09fb2075173eb4fc030ce2c26e85311aa26f064a
---
# Backend — Analytics Context

## Purpose

<!-- [coverage: high] -->

The read side: roll-up dashboards over the hierarchy, the re-engagement candidate projection
(ADR-0010), the configurable thresholds behind it, and the audit-log viewer (ADR-0023). It writes
almost nothing transactional — its job is to read the other three contexts' tables and answer
scoped questions.

## Layout

<!-- [coverage: high] -->

Five Maven modules per ADR-0019:

| Module | Ring | What lives in it |
|---|---|---|
| `analytics-domain-core` | Entities | `Candidate`, `Thresholds`, `Tier`, `Scope`, `OutcomeKind`, `HomeSabhaHistory`. Small and value-object-shaped — there is no aggregate here. |
| `analytics-application-service` | Use cases | Two authorization engines (`DashboardAccess`, `AuditLogAccess`), `ReEngagementCandidateCalculator` and its `ReEngagementProjectionScanner`, the query ports (`DashboardQueries`, `AuditLogQueries`), and `AuditReadAccessAdapter`. |
| `analytics-data-access` | Interface adapters | Nine `Jdbc*` adapters, mostly read-only projections. |
| `analytics-messaging` | Interface adapters | Empty scaffold. |
| `analytics-application` | Interface adapters | `DashboardBffController`, `AuditLogBffController`. |

## Exposes

<!-- [coverage: high] -->

**`/bff/*` only — no mobile surface.** `/bff/dashboard/overview`, `/bff/dashboard/city`,
`/bff/dashboard/people`, `/bff/dashboard/sabha-tree`, `/bff/dashboard/scope`,
`/bff/dashboard/thresholds`, and `/bff/audit-log`.

## Talks To

<!-- [coverage: medium -- edges derived from `import org.sabha.common.*` and the one `implements` hit; the audit feed's cross-context reads are SQL-level, not port-level, so they do not show up as edges at all. ] -->

**Outbound** — into [[backend-identity]]: `CallerResolver`, `SantLookup`,
`MadhyasthaKaryalayaLookup`, `CallerVisibility`.

**Inbound** — one: `AuditReadAccess`, implemented by `AuditReadAccessAdapter`. Note it lives in
`analytics-application-service`, not in the data-access module where the other contexts put their
common-domain adapters.

The audit log is a **CQRS UNION read-model** with no central table (ADR-0023), so this context reads
directly from the other contexts' tables rather than through ports. Those reads are invisible to a
port-level dependency scan.

## Data

<!-- [coverage: low -- ownership inferred from INSERT/UPDATE targets; this context reads more foreign tables than any other, and the `users` write is a single column. Verify before acting. ] -->

Written here: `reengagement_candidates` (the projection, rebuilt wholesale on a background cadence),
`analytics_thresholds`, and the `default_city_id` column on `users`.

Read-only here: `cities`, `zones`, `kshetras`, `sabhas`, `sabha_kinds`, `home_sabhas`, `persons`,
`role_assignments`, `occurrences`, `occurrence_state_transitions`, `attendance_markings`,
`selection_nominations`.

## Gotchas

<!-- [coverage: medium -- the "never live against transactional tables" rule is stated in `AnalyticsCronJobs`; the 15-minute default was read from the same Javadoc and not from configuration. ] -->

`reengagement_candidates` is a **projection, not a source of truth**. It is rebuilt on a background
schedule (every 15 minutes by default, driven from [[backend-container]]) precisely so dashboards
never run live against the transactional tables. A refresh is idempotent and wholesale, so a missed
tick self-heals — but a dashboard reading it is up to one cadence stale by design.

Every `package-info.java` in this context still reads *"Empty scaffold per ADR-0019"* despite ~48
classes. Same trap as [[backend-sabha]].

## Covered by

<!-- [coverage: low -- no feature dossiers exist yet; structurally empty until the first one is admitted. ] -->

_none_

## Sources

- [analytics-service](../../../apps/backend/analytics-service) — module layout and class inventory
- [ADR-0010](../../adr/0010-re-engagement-candidate-definition.md), [ADR-0023](../../adr/0023-audit-log-read-model-and-viewer-authority.md)
- [CONTEXT.md](../../../CONTEXT.md) — Re-engagement Candidate, Sant, Madhyastha Karyalaya vocabulary
