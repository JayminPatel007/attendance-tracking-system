# Audit log is a read-model over existing tables, viewable by Nirdeshak and above within scope

**Status**: accepted (Slice 19, #20).

The system has been accumulating audit data since Slice 2, but there is **no central audit-log table**. Every slice records its own provenance inline on the row it writes:

| Source | What it audits | Actor column | Timestamp | Proxy column |
|---|---|---|---|---|
| `occurrence_state_transitions` | Occurrence lifecycle (cancel/reschedule/open/finalize/reopen) | `actor_user_id` | `at_timestamp` | `on_behalf_of_user_id` |
| `role_assignments` | Role appointment (ADR-0011) | `appointed_by` | `appointed_at` | — |
| `sabhas` | Sabha definition (Slice 12) | `created_by` | `created_at` | — |
| `cities` / `zones` / `kshetras` / `sabha_kinds` | Structural creation (ADR-0009) | `created_by` | `created_at` | — |
| `selection_nominations` | BSS/YSS nominate + decide (Slice 16) | `nominated_by` / `decided_by` | `nominated_at` / `decided_at` | — |

Slice 19 exposes this to humans. The question is *how* to surface a single chronological feed over five heterogeneous tables, and *who* may read it.

## Decision: a UNION read-model, not a new write-path table

The audit feed is a **CQRS read-model** (ADR-0008 single bounded context, shared DB): a JDBC adapter `JdbcAuditFeed` UNIONs the source tables into one projected shape behind the `AuditLogQueries` port. Nothing on any write path changes — no central table to populate, no backfill, no dual-write to keep consistent. The issue framed this slice as "audit data is captured anyway; this is just the viewer," and a read-model is the literal expression of that framing.

The projected shape is:

```
(id, at, actor_user_id, on_behalf_of_user_id, target_type, target_id, action, detail,
 kshetra_id, zone_id, city_id)
```

Each UNION branch **resolves the entry's geography** (`kshetra_id`, `zone_id`, `city_id`) by joining out to the structural tables *inside the branch*. This is the load-bearing move: geography resolution is the one thing that differs per target type (an Occurrence reaches geography via its Sabha; a role appointment via whichever of `sabha_id`/`kshetra_id`/`zone_id`/`city_id` it carries; a structural creation *is* its own geography), so each branch pays that cost once and the rest of the pipeline — the scope filter, the actor-name join, the ordering — is uniform over the projected columns. The complex part is sealed inside one adapter; `AuditLogQueries` exposes `find(scope, filter)` and nothing else.

`AuditTargetType` is an enum (`OCCURRENCE`, `SABHA`, `ROLE_ASSIGNMENT`, `STRUCTURAL`, `PERSON`) rather than a free-text token: it is a closed set the viewer filters on, unlike the extensible `sabha_kind`.

### Attendance markings are excluded

`attendance_markings` carries `marked_by_user_id` / `marked_at` and could in principle be a sixth branch, but it is the system's highest-frequency operational data — including it would drown the governance-and-lifecycle signal the viewer exists to show, and attendance is already surfaced through the dashboards and rosters. The issue's target-type list (Occurrence / Person / RoleAssignment / Sabha) does not name it. Excluded for v1; revisit if an "attendance provenance" need is articulated.

## Decision: viewable by Nirdeshak and above, geographically scoped

Authority to see the audit surface is **Nirdeshak-and-above**, and what each caller sees is **filtered to their geographic scope** — the same scoping spine the dashboard uses (ADR-0010, Slice 17), applied to the resolved geography of each entry rather than to a candidate's home Sabha:

- **Nirdeshak / Sah-Nirdeshak** → the Kshetra(s) they hold (`kshetra_id IN …`).
- **Sanyojak** → the Zone(s) they coordinate (`zone_id IN …`).
- **Regional Team** → the City(ies) they oversee (`city_id IN …`).
- **Sant** → universal read across the State (their read access is universal per CONTEXT.md; the audit surface honours it).
- **Madhyastha Karyalaya** → state-wide.
- **Sanchalak / Sah-Sanchalak / Nirikshak** → **no access** (403). Their work flows through their own dashboards and mobile screens (ADR-0003); they do not get an oversight surface in v1.

State-level entries with no resolvable geography (a `sabha_kind` creation, an MK-scoped role appointment — all-`NULL` geography) are visible **only to state-wide callers** (Sant, MK). A scoped caller's predicate is `kshetra_id IN (…) OR zone_id IN (…) OR city_id IN (…)`, and `NULL` matches none of them, so state-level rows naturally fall outside a Nirdeshak's or Sanyojak's view without a special case.

The decision is encoded as `AuditLogAccess` (the Authorization Engine), which maps a caller to a sealed `AuditScope` (`Unrestricted` | `Scoped(kshetras, zones, cities)` | `Denied`) using the existing `MadhyasthaKaryalayaLookup` and `SantDirectory` ports plus a new `AuditScopeLookup` that reads the geographic role rows. A caller who resolves to no scope at all is `Denied` — the same outcome as a sub-Nirdeshak role, reached without enumerating the forbidden roles. The engine is stateless apart from its ports, so the rule is exercised without a database, mirroring `DashboardAccess`.

The web shell gains an `AUDIT_LOG` `Section`; `VisibleSections` unlocks it for the same authority set, so the sidebar nav and the BFF agree on who sees it from one source of truth (Slice 9 pattern).

## Consequences

- The proxy-action filter (`on_behalf_of_user_id IS NOT NULL`, Slice 14) only ever matches `OCCURRENCE` entries, because that is the only source table with the column. The toggle is still global — it simply yields only Occurrence rows when on. This is correct, not a bug: Nirikshak-as-Sanchalak proxy is the only proxied authority in the system.
- Per-entity history (an Occurrence detail linking to "its" audit trail) is just `find` with `target_type` + `target_id` pinned — no separate query path.
- A single source row can emit two entries (a `selection_nominations` row emits both a NOMINATED and a decided entry), so `id` is not unique across the feed; consumers key on `(id, action)`.
- Because geography is resolved in SQL, adding a future audited table means adding one UNION branch with its own geography join — the scope filter and authority engine are untouched.
