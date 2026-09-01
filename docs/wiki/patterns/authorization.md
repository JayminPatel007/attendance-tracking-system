---
type: pattern
title: Authorization
description: How every authority decision is made — one stateless engine per context, resolving current scope through identity-owned ports.
aliases: [authz, permissions, Authorization Engine, who may, scope, RoleAssignmentLookup]
source_paths: [
  apps/backend/**/AuthorizationEngine.java,
  apps/backend/**/AppointmentAuthorization.java,
  apps/backend/**/SabhaDefinitionAuthorization.java,
  apps/backend/**/StructuralScopeAuthority.java,
  apps/backend/**/DashboardAccess.java,
  apps/backend/**/AuditLogAccess.java,
  apps/backend/**/VisibleSections.java,
  apps/backend/common-domain/src/main/java/org/sabha/common/Role.java,
  apps/backend/common-domain/src/main/java/org/sabha/common/OversightRole.java,
  apps/backend/common-domain/src/main/java/org/sabha/common/AuthorizedAction.java,
  apps/backend/common-domain/src/main/java/org/sabha/common/AuditReadAccess.java,
  apps/backend/common-domain/src/main/java/org/sabha/common/AuthorizationDeniedException.java,
  docs/adr/0001-*.md,
  docs/adr/0009-*.md,
  docs/adr/0011-*.md,
  docs/adr/0023-*.md,
  docs/adr/0025-*.md,
  docs/adr/0027-*.md,
  docs/adr/0029-*.md,
  CONTEXT.md
]
sources:
  - { id: adr-0001, title: "Sabha Occurrence Lifecycle", resource: ../../adr/0001-sabha-occurrence-lifecycle.md }
  - { id: adr-0009, title: "Structural Creation Authority Lives at the Tier Above", resource: ../../adr/0009-structural-creation-authority.md }
  - { id: adr-0011, title: "Role Appointment Authority", resource: ../../adr/0011-role-appointment-authority.md }
  - { id: adr-0023, title: "Audit log is a read-model over existing tables, viewable by Nirdeshak and above within scope", resource: ../../adr/0023-audit-log-read-model-and-viewer-authority.md }
  - { id: adr-0025, title: "Appointment is scope-based; the Regional Team is self-replicating; Sah-Nirdeshak holds no appointment authority", resource: ../../adr/0025-scope-based-appointment-rt-self-replication-sah-nirdeshak.md }
  - { id: adr-0027, title: "No shared granted-scope module behind the four authorization engines", resource: ../../adr/0027-no-shared-granted-scope-module-behind-the-authorization-engines.md }
  - { id: adr-0029, title: "`role_assignments` is identity-owned: read-models may join it, authority checks go through ports", resource: ../../adr/0029-role-assignments-access-rule.md }
  - { id: context, title: "CONTEXT.md — Roles (each tier has its own role), Geographic hierarchy", resource: ../../../CONTEXT.md }
appears_in: [backend-identity, backend-sabha, backend-attendance, backend-analytics, backend-common-domain, web, attendance-marking]
last_compiled: 18c0993c1c22d3217d62a879beed639914f74aee
---

# Authorization

## The pattern

<!-- [coverage: high -- the five engines' class javadocs and constructors, read against ADR-0025 and ADR-0027] -->

Every *"may this caller do it?"* is answered by an **Authorization Engine**: a stateless class in the
owning context's `-application-service` ring that takes the caller and the target, and returns a
decision. There is one per authority question, deliberately not one for the system. Four properties
hold across all of them.

- **It decides; it does not act.** An engine returns a boolean or a sealed scope, and never throws
  or mutates. The calling application service turns a refusal into `AuthorizationDeniedException`
  (`common-domain`), which the container's handler renders as **403**. Having no side effects is
  what lets every engine be exercised end to end without a database.
- **Authority is the caller's *current* scope, never `created_by` or `appointed_by`** (ADR-0025). A
  replacement Nirdeshak may revoke a Sanchalak their predecessor appointed; nothing strands when a
  role-holder leaves.
- **The scope facts arrive through identity-owned `common-domain` ports**, one shaped per question —
  `RoleAssignmentLookup`, `StructuralHierarchyLookup`, `MadhyasthaKaryalayaLookup`, `SantLookup`,
  `SanyojakZoneLookup`, `RegionalTeamCityLookup`, `NirikshakAssignmentLookup`. A read-model may
  `JOIN role_assignments` for a projection; a *decision* may not (ADR-0029).
- **Each authority set has exactly one definition**, in `common-domain`: `Role.REOPEN_TIERS`,
  `AuthorizedAction.SABHA_SHAPING_ACTIONS`, and `OversightRole` for the two tiers that sit outside
  the operational `Role` enum. Another surface needing the same rule **derives** from the engine
  rather than restating its tier list — the mistake `Deviations` records.

## Why

<!-- [coverage: high -- ADR-0027's rejection argument and the ADR-0009/0011 → 0025 → 0029 trail] -->

Two ladders come first. ADR-0009 puts structural creation at the tier *above* the thing created, and
ADR-0011 puts appointment one rung *above* the role being filled, at its parent scope. ADR-0025 then
rebinds both to **scope rather than creator**, lets the Regional Team appoint its own peers behind a
last-one-out guard, and strips the Sah-Nirdeshak of administrative authority. ADR-0029 draws the line
the ports depend on: projections may join, checks must not.

ADR-0027 is why there are several engines and not one. A shared granted-scope module was designed
and **rejected**: the engines' questions are structurally different shapes — a point predicate keyed
by target, an inverted rank-and-containment check, a geographic enumeration, and a policy that
reads no roles at all. One lookup serving all four would either grow fat or force the point-predicate
callers to over-fetch and re-filter, moving the per-engine policy back into each engine. That
relocates complexity instead of removing it, so the plurality below is the decision, not drift.

## Where it appears

<!-- [coverage: high -- one read of each engine plus an import scan of its constructor ports] -->

| Engine | Page | Question shape |
|---|---|---|
| `AppointmentAuthorization` | [backend-identity](../structure/backend-identity.md) | inverted — does the appointer hold the tier one rung above, at the parent scope? |
| `SabhaDefinitionAuthorization` | [backend-identity](../structure/backend-identity.md) | point — is the caller the Nirdeshak over this `(Kshetra, demographic)`? |
| `StructuralScopeAuthority` | [backend-sabha](../structure/backend-sabha.md) | tiered — does the caller hold the scope one tier above? Create and delete share it |
| `AuthorizationEngine` | [backend-attendance](../structure/backend-attendance.md) | point, keyed by the target Sabha — shaping, plus the Nirikshak proxy |
| `AuthorizationEngine` (`REOPEN`) | [backend-attendance](../structure/backend-attendance.md) | scope-resolving — Sabha → `(Kshetra, demographic)`, then the Kshetra tiers only |
| `AuditLogAccess` | [backend-analytics](../structure/backend-analytics.md) | enumeration — fold the caller's whole geography into a sealed `AuditScope` |
| `DashboardAccess` | [backend-analytics](../structure/backend-analytics.md) | non-role policy — Sant universal read plus a persisted default City |
| the ports and the vocabulary | [backend-common-domain](../structure/backend-common-domain.md) | the seam itself: the lookups, `Role`, `OversightRole`, `AuthorizedAction`, `AuditReadAccess` |
| marking and reopen, end to end | [attendance-marking](../features/attendance-marking.md) | the same rules seen from the capability side |
| section visibility | [web](../structure/web.md) | what the client renders once the BFF has decided |

## Deviations

<!-- [coverage: medium -- the four cases are each verified in source; that the list is exhaustive is not] -->

- **`DashboardAccess` reads no roles.** It consults `SantLookup` and a persisted City and nothing
  else, so it is an engine by shape and contract but not by input — the fourth row of ADR-0027's
  table, and the reason "authorization engine" is not a synonym for "reads `role_assignments`".
- **`VisibleSections` is a nav gate, not an engine.** It sits in identity's `-domain-core`, is a pure
  static function, and answers *what the shell shows* rather than *what the caller may do*. It is
  also where the one-definition rule was broken: it originally mirrored the audit tier set as a
  `Role`-only constant, silently omitting the Regional Team — which is not an operational `Role` —
  and drifted from the BFF. Issue #80 replaced the mirror with the `AuditReadAccess` port over
  `AuditLogAccess`, so the sidebar now admits exactly the set the engine admits.
- **Sabha creation is checked in identity, not sabha.** The Sabha is sabha's aggregate, but the
  decision is *"is this caller the Nirdeshak here?"*, which is identity's fact (ADR-0029). The same
  predicate is carried a second time by `StructuralScopeAuthority` for the sabha-side delete path —
  a deliberate duplication, so a tier's authority cannot drift between create and delete.
- **The Sah-Nirdeshak is authorized asymmetrically.** They hold the operational half — reopen, and
  acting on the Kshetra's Sabhas — and none of the administrative half (ADR-0025). Read the two
  halves separately or the role looks inconsistent.
- **[web](../structure/web.md) enforces nothing.** It renders the sections the BFF grants; the client-side guard is
  navigation, not a check.

## Method

- ADR-0027's four-row table of question shapes is the source that paid, and it paid more than everything else combined: it is the one place the cluster is already reconciled, naming each engine, its question shape and its port. Written without it, this page would have re-derived that comparison from five class files and probably landed on "they all read `role_assignments`", which is false of `DashboardAccess`.
- The engines' own class javadocs carry what no ADR does — the denial path, the pure-decision contract, and each engine's rejected alternative. `VisibleSections` is the highest-yield single file: the nav-gate half and the issue-#80 mirror-versus-derive history exist nowhere else.
