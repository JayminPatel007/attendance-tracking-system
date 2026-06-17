# No shared granted-scope module behind the four authorization engines

**Status**: accepted. Records the outcome of the design spike in issue #68; rejects "candidate 5" from the 2026-06-10 architecture review. No code changes.

Four authorization engines each read `role_assignments` to answer "what authority does this caller hold":

- attendance `AuthorizationEngine` — boolean per `AuthorizedAction` (Sabha-shaping vs reopen tiers vs Nirikshak proxy, [ADR-0001](0001-sabha-occurrence-lifecycle.md))
- identity `AppointmentAuthorization` — appointment authority ([ADR-0011](0011-role-appointment-authority.md))
- analytics `DashboardAccess` — sealed `DashboardScope` (Sant universal read, Slice 17)
- analytics `AuditLogAccess` + `JdbcAuditScopeLookup` — pre-resolved geographic id sets ([ADR-0023](0023-audit-log-read-model-and-viewer-authority.md))

The candidate deepening was a common-domain "granted-scope" lookup that resolves the caller's role tiers + geography **once**, leaving each engine only its policy decision. The spike's job was to decide proceed-or-reject before any implementation; the explicit non-goal was a generic `AuthorizationEngine<T>` interface — a hypothetical seam with no behaviour behind it.

**Decision: reject. We do not introduce a shared granted-scope module.** The reasons below are load-bearing; this ADR exists so future architecture reviews do not re-suggest it.

## Why reject

### 1. The four engines ask structurally different questions; no single "granted scope" shape serves them

| Engine | Question shape | How it reads `role_assignments` |
| --- | --- | --- |
| attendance `AuthorizationEngine` | **point predicate keyed by the target** — "does this caller hold a granting tier at *this Sabha's* scope?" (plus a Nirikshak proxy assignment on a *specific* Sabha) | pushes the target scope in, gets a boolean, via `RoleAssignmentLookup` |
| identity `AppointmentAuthorization` | **relative / inverted** — "does the appointer hold the tier *one rung above* at the *parent* scope of the role being filled?" | a rank-and-containment check, via `AppointerAuthorityLookup` |
| analytics `AuditLogAccess` | **geographic enumeration** — collect *all* the caller's Kshetra/Zone/City ids into sets | enumerates the caller's whole geography, via `AuditScopeLookup` |
| analytics `DashboardAccess` | **non-role policy** — Sant universal read + persisted default City | does not read `role_assignments` at all |

Two engines push a target scope *in* and want a boolean; one wants the caller's whole geography enumerated *out*; one never touches roles. A single lookup cannot serve both the point-predicate and the enumeration shapes without either a fat interface or forcing the point-predicate callers to over-fetch the caller's entire assignment set and re-filter it in memory. Either way the per-engine policy — which is the actual complexity — moves back into each engine. That **relocates** complexity rather than removing it, and so fails the deep-module test (a small interface is necessary but not sufficient; it must also reduce total complexity behind that interface).

### 2. The cross-context seam ADR-0019 cares about is already solved

[ADR-0019](0019-bounded-context-module-taxonomy.md) requires cross-context dependencies to go through common-domain ports. The `role_assignments` table is identity-owned, and that seam is **already** factored, correctly and minimally, for each engine's shape:

- `RoleAssignmentLookup` (common-domain) — the point lookups attendance needs.
- `AppointerAuthorityLookup` (identity-local, *not* common) — appointment's check is identity-on-identity, so it stays inside identity and never crosses a seam.
- `AuditScopeLookup` (analytics-local) — analytics' geographic enumeration, implemented in `analytics-data-access`.
- `MadhyasthaKaryalayaLookup`, `NirikshakAssignmentLookup` (common-domain) — the membership/proxy facts.

A shared granted-scope port would not improve seam compliance; it would add a *fifth*, awkwardly-general port that every engine has to bend to. The current ports are smaller and each pulls its weight.

### 3. The policies differ for real domain reasons — and #66 already proved the "same fact" is not actually shared

The first spike question was whether the shared fact is genuinely common given the policies differ for domain reasons (Nirikshak proxy, reopen tiers, Sant universal read, MK override). They are not common, and the canonical evidence is in the read-model refactor that lands *next to* this: [#66](0019-bounded-context-module-taxonomy.md) (PR #94, `CallerVisibility` / `VisibilityTier`) had to **split the Nirikshak in two** — `NIRIKSHAK` (a Kshetra-tier reopen authority resolved through `role_assignments`) versus `NIRIKSHAK_PROXY` (an explicit, mutable Sabha set resolved through `nirikshak_sabha_assignments`) — precisely because the same role means two different things to two different consumers. A shared granted-scope layer would have to re-expose that split, and others like it (reopen tiers vs shaping authority; Sant universal read; MK override; appointment's "one tier up"), to every engine. The split *is* the policy; there is no policy-free "fact" underneath it worth naming once.

### 4. What #66 already did is the right amount of sharing — and it was on the read side, not here

#66 consolidated the one place duplication was genuinely byte-for-byte and self-evidently coupled: two CQRS read adapters whose javadoc literally warned each other to "keep them in step", plus a third Java parse of the same encoding. That duplication was real and enumeration-shaped, so it collapsed cleanly into `CallerVisibility` + `VisibilityTier` + `SabhaKind`. After #66, the duplication remaining across the four *write-side* engines is not byte-duplicated logic — it is four different policy questions that happen to read the same table. That is not the same kind of duplication and does not warrant the same remedy.

### 5. The remaining duplication is per-port test scaffolding; consolidating it would couple four contexts' tests

The visible repetition is that each engine's unit test rebuilds fakes of *its* ports (`RoleAssignmentLookup`, `StructuralHierarchyLookup`, `AppointerAuthorityLookup`, `AuditScopeLookup`, …). Those fakes differ because the ports differ; there is no single fixture that answers all four engines' questions. A shared test fixture would couple four bounded contexts' test suites to one scaffold — directly against the module-isolation ADR-0019 and ADR-0008 buy us. Each engine's stateless, DB-free testability (`DashboardAccess`, `AuditLogAccess`, and the appointment/attendance engines all run without a database) is already the win; it does not need a shared scaffold.

### 6. It is the hypothetical seam the spike warned against

A lookup general enough to span a point predicate, an inverted rank check, a geographic enumeration, and a non-role policy is, in practice, the generic `AuthorizationEngine<T>` the spike explicitly ruled out — an interface with no shared behaviour behind it, only four callers each unpacking it differently.

## Consequences

- The four engines keep their current ports and their current per-engine policy. No new common-domain type is introduced.
- `CallerVisibility` / `VisibilityTier` / `SabhaKind` (#66) remain the **read-side** consolidation only; this ADR deliberately does not extend them to the write-side engines.
- A future engine that is itself a *geographic enumeration* (like audit) may reasonably reuse `AuditScopeLookup`'s shape or the `VisibilityTier` vocabulary; that is a local, shape-matched reuse, not the cross-cutting granted-scope module rejected here.
- If a fifth and sixth engine ever appear with the *same* question shape and genuinely identical policy, revisit this decision then — with concrete duplication in hand, not anticipated.
- The companion access rule for the identity-owned `role_assignments` table — read-models may join it, authority/membership checks go through identity-owned common-domain ports — is recorded in [ADR-0029](0029-role-assignments-access-rule.md) (issue #79). It moves the clear Sant-lookup offenders this spike left in place; the two ADRs are consistent in preferring small, shape-matched ports over one general lookup.
