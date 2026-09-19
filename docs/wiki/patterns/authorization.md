---
type: pattern
title: Authorization
description: How every authority decision is made — the caller's own roles resolved once at the request edge, each context's policy kept in its own stateless engine.
aliases: [authz, permissions, Authorization Engine, who may, scope, CallerAuthority]
source_paths: [
  apps/backend/**/AuthorizationEngine.java,
  apps/backend/**/AppointmentAuthorization.java,
  apps/backend/**/ReissueAuthorization.java,
  apps/backend/**/DashboardAccess.java,
  apps/backend/**/AuditLogAccess.java,
  apps/backend/**/VisibleSections.java,
  apps/backend/common/common-domain/src/main/java/org/sabha/common/CallerAuthority.java,
  apps/backend/common/common-domain/src/main/java/org/sabha/common/*Lookup.java,
  apps/backend/common/common-domain/src/main/java/org/sabha/common/Role.java,
  apps/backend/common/common-domain/src/main/java/org/sabha/common/OversightRole.java,
  apps/backend/common/common-domain/src/main/java/org/sabha/common/AuthorizedAction.java,
  apps/backend/common/common-domain/src/main/java/org/sabha/common/AuditReadAccess.java,
  apps/backend/common/common-domain/src/main/java/org/sabha/common/AuthorizationDeniedException.java,
  apps/backend/common/common-application/src/main/java/org/sabha/common/web/*.java,
  docs/adr/0001-*.md,
  docs/adr/0009-*.md,
  docs/adr/0011-*.md,
  docs/adr/0023-*.md,
  docs/adr/0025-*.md,
  docs/adr/0027-*.md,
  docs/adr/0029-*.md,
  docs/adr/0030-*.md,
  docs/adr/0032-*.md,
  docs/adr/0033-*.md,
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
  - { id: adr-0030, title: "Caller identity is resolved at the HTTP edge", resource: ../../adr/0030-caller-identity-resolved-at-the-http-edge.md }
  - { id: adr-0032, title: "Caller authority is resolved at the request edge: the facts consolidate, the policies do not", resource: ../../adr/0032-caller-authority-resolved-at-the-request-edge.md }
  - { id: adr-0033, title: "The sabha lookups re-partition by subject", resource: ../../adr/0033-sabha-lookups-re-partition-by-subject.md }
  - { id: context, title: "CONTEXT.md — Roles (each tier has its own role), Geographic hierarchy", resource: ../../../CONTEXT.md }
appears_in: [backend-identity, backend-sabha, backend-attendance, backend-analytics, backend-common-domain, web, attendance-marking]
last_compiled: 03f289295b1ed467219db64fdc0f81ce032f1b10
---

# Authorization

## The pattern

<!-- [coverage: high -- the five engines' class javadocs and constructors, read against ADR-0032's decision table] -->

Authority splits in two (ADR-0032): the **facts** about who the caller is consolidate, the
**policies** about what that permits do not.

The facts are the caller's own rows in `role_assignments`, loaded **once per request, at the edge**,
into `CallerAuthority` — bound by every handler through `@CurrentUser` and passed down. The rows stay
private; the domain sees named questions like `holdsNirdeshakIn` or `runsSabha`. There is exactly one
caller parameter type, which is why a request costs one authority query however many authority
questions it asks, and why no cache exists.

The policies stay apart, one **Authorization Engine** per question — a stateless class in the owning
context's `-application-service` ring. Three properties hold across all of them.

- **It decides; it does not act.** An engine returns a boolean, a sealed scope, or an `Optional`, and
  is never transactional. The calling application service turns a refusal into
  `AuthorizationDeniedException` (`common-domain`), rendered as **403**. Rule R3 enforces the return
  half over a reviewed list of the five engines; the "never throws or mutates" half stays javadoc,
  because a return-type rule cannot see a write.
- **Authority is the caller's *current* scope, never `created_by` or `appointed_by`** (ADR-0025). A
  replacement Nirdeshak may revoke a Sanchalak their predecessor appointed. `ReissueAuthorization` is
  the one licensed exception, under ADR-0004.
- **Each authority set has exactly one definition**, in `common-domain`: `Role.REOPEN_TIERS`,
  `AuthorizedAction.SABHA_SHAPING_ACTIONS`, and `OversightRole` for the two tiers outside the
  operational `Role` enum. Another surface needing the same rule **derives** from the engine rather
  than restating its tier list — the mistake `Deviations` records.

What still arrives through a port is anything keyed by a **target** rather than the caller: a Sabha,
an appointee, a Kshetra. A read-model may `JOIN role_assignments` for a projection; a *decision* may
not, and rule R1 holds that line with a six-file allowlist (ADR-0029).

## Why

<!-- [coverage: high -- ADR-0027's rejection argument and the ADR-0009/0011 → 0025 → 0029 → 0032 trail] -->

Two ladders come first. ADR-0009 puts structural creation at the tier *above* the thing created, and
ADR-0011 puts appointment with the **Nirdeshak of the appointee's Kshetra**, and only from the
Nirdeshak upward with the tier a geographic level above — the eight arms are enumerated in
[role-appointment](../features/role-appointment.md). ADR-0025 then rebinds both to **scope rather
than creator**, lets the Regional Team appoint its own peers behind a last-one-out guard, and strips
the Sah-Nirdeshak of administrative authority. ADR-0029 draws the line the ports depend on:
projections may join, checks must not. ADR-0030 resolves *who is calling* once, at the edge.

ADR-0027 is why there are several engines and not one, and that half stands: a shared **policy**
module was rejected because the questions are structurally different shapes. ADR-0032 found it had
rejected two things in one breath and named only one. A shared **fact** layer failed against
measurement: nine of the questions asked of `role_assignments` were the same statement, differing
only in which scope column they projected. Those nine folded to the edge; no two engines merged.

Its subtractive test sets the engine count — a class is an engine **iff something is left after the
fact layer is removed**. Two had nothing left and were deleted; one private predicate reading two
target-keyed ports was promoted. Six became five by subtraction. What keeps this from becoming the
module ADR-0027 rejected is `CallerAuthority`'s stopping rule — one relation, one key, one query.
ADR-0033 is the worked example on the other side of that line.

## Where it appears

<!-- [coverage: high -- one read of each engine plus an import scan of its constructor ports] -->

| Engine | Page | Question shape |
|---|---|---|
| `AppointmentAuthorization` | [backend-identity](../structure/backend-identity.md) | inverted — is the appointer the Nirdeshak of the appointee's Kshetra, or, from Nirdeshak upward, the tier one geographic level above? The eight arms are enumerated in [role-appointment](../features/role-appointment.md); do not re-derive them from this row |
| `ReissueAuthorization` | [backend-identity](../structure/backend-identity.md) | target-keyed — did this caller appoint the target, or is the target a Sant and the caller MK? The one `appointed_by` check in the backend (ADR-0004) |
| `AuthorizationEngine` | [backend-attendance](../structure/backend-attendance.md) | point, keyed by the target Sabha — shaping, plus the Nirikshak proxy |
| `AuthorizationEngine` (`REOPEN`) | [backend-attendance](../structure/backend-attendance.md) | scope-resolving — Sabha → `(Kshetra, demographic)`, then the Kshetra tiers only (ADR-0001) |
| `AuditLogAccess` | [backend-analytics](../structure/backend-analytics.md) | enumeration — fold the caller's whole geography into a sealed `AuditScope` (ADR-0023). Holds no ports at all and is kept anyway |
| `DashboardAccess` | [backend-analytics](../structure/backend-analytics.md) | non-role policy — Sant universal read plus a persisted default City |
| structural create and delete | [backend-sabha](../structure/backend-sabha.md) | tiered, and **no longer an engine** — the four predicates are read straight off `CallerAuthority`, whose javadoc now carries the create/delete tier table |
| the caller type and the ports | [backend-common-domain](../structure/backend-common-domain.md) | the seam itself: `CallerAuthority`, the surviving lookups, `Role`, `OversightRole`, `AuthorizedAction`, `AuditReadAccess` |
| marking and reopen, end to end | [attendance-marking](../features/attendance-marking.md) | the same rules seen from the capability side |
| section visibility | [web](../structure/web.md) | the client edge — renders what the BFF decided, and decides nothing (see `Deviations`) |

## Deviations

<!-- [coverage: medium -- each case is verified in source; that the list is exhaustive is not] -->

- **Four predicates are read inline, with no engine at all.** Home-Sabha transfer and selection
  nomination both ask `runsSabha`; the selection decision and Sabha definition both ask
  `holdsNirdeshakIn`. Under a caller parameter there is no port to bypass, and two more engine
  classes would only have preserved the byte-for-byte duplication behind two names.
- **`AuditLogAccess` holds zero collaborators and is still an engine.** The deletion rule needs zero
  ports *and* zero policy; its tier fold and `empty ⇒ Denied` are real policy. A reader applying the
  rule mechanically would delete it, so both the ADR and the class say not to.
- **`DashboardAccess` reads no roles.** One persisted-City port and the caller's Sant flag — the
  reason "authorization engine" is not a synonym for "reads `role_assignments`".
- **`VisibleSections` is a nav gate, not an engine.** It sits in identity's `-domain-core`, is a pure
  static function, and answers *what the shell shows* rather than *what the caller may do*. It is
  also where the one-definition rule was broken: it originally mirrored the audit tier set as a
  `Role`-only constant, silently omitting the Regional Team — which is not an operational `Role` —
  and drifted from the BFF. Issue #80 replaced the mirror with the `AuditReadAccess` port over
  `AuditLogAccess`, so the sidebar now admits exactly the set the engine admits.
- **Sabha creation is checked in identity, not sabha.** The Sabha is sabha's aggregate, but the
  decision is *"is this caller the Nirdeshak here?"*, which is identity's fact (ADR-0029). The
  sabha-side delete path asks the **same method** on the same caller, so there is nothing left to
  keep in step — where this list previously recorded a deliberate duplication, the duplication is
  gone rather than defended.
- **The Sah-Nirdeshak is authorized asymmetrically.** They hold the operational half — reopen, and
  acting on the Kshetra's Sabhas — and none of the administrative half (ADR-0025). Read the two
  halves separately or the role looks inconsistent.
- **[web](../structure/web.md) enforces nothing.** It renders the sections the BFF grants; the client-side guard is
  navigation, not a check.

## Method

- ADR-0032 is the source that paid, and it paid more than everything else combined: it is the one place the split between consolidating facts and non-consolidating policies is argued, and it carries the measurements — nine identical statements, six adapters to one, six engines to five — that this page would otherwise have to re-derive from thirty files and would probably get wrong. Its `Where this verdict stops` section is what keeps a reader from applying the edge mechanism to the target-keyed lookups ADR-0033 governs.
- ADR-0027 is still worth reading before this page, not after: its four-row table of question shapes is the comparison ADR-0032 argues *against*, and §3 survives verbatim. A reader who sees only the fold will not understand why the policies stayed apart.
- The engines' own class javadocs carry what no ADR does — the denial path and each engine's rejected alternative. `CallerAuthority` is now the highest-yield single file: the stopping rule, the structural tier table inherited from the deleted `StructuralScopeAuthority`, and the reason the rows are private all exist there and nowhere else.
