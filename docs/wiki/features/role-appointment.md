---
type: feature
title: Role Appointment
description: Appointing a Karyakar into a role at a scope, and revoking that role again.
aliases: [appointment, Karyakar appointment, Nirdeshak, Sanchalak, Sanyojak, Regional Team, revocation, my authority, who may appoint]
tags: [bff]
source_paths: [
  apps/backend/identity-service/*/src/main/**,
  apps/web/src/app/sections/role-appointment/**,
  apps/web/src/app/sections/my-authority/**,
  apps/web/projects/identity-domain/src/**,
  apps/backend/application-container/src/main/resources/db/changelog/slice-2/**,
  apps/backend/application-container/src/main/resources/db/changelog/slice-10/**,
  apps/backend/application-container/src/main/resources/db/changelog/slice-11/**,
  apps/backend/application-container/src/main/resources/db/changelog/issue-89/**,
  docs/adr/0002-*.md,
  docs/adr/0009-*.md,
  docs/adr/0011-*.md,
  docs/adr/0013-*.md,
  docs/adr/0025-*.md,
  docs/adr/0026-*.md,
  docs/adr/0029-*.md,
  CONTEXT.md
]
issues: [12, 86, 89, 90]
sources:
  - { id: adr-0002, title: "Sanchalak-Initiated Home Sabha Transfer Requires Person Verification", resource: ../../adr/0002-home-sabha-transfer-requires-person-verification.md }
  - { id: adr-0009, title: "Structural Creation Authority Lives at the Tier Above", resource: ../../adr/0009-structural-creation-authority.md }
  - { id: adr-0011, title: "Role Appointment Authority", resource: ../../adr/0011-role-appointment-authority.md }
  - { id: adr-0013, title: "Directory De-duplication on Person Add", resource: ../../adr/0013-directory-de-duplication-on-person-add.md }
  - { id: adr-0025, title: "Appointment is scope-based; the Regional Team is self-replicating; Sah-Nirdeshak holds no appointment authority", resource: ../../adr/0025-scope-based-appointment-rt-self-replication-sah-nirdeshak.md }
  - { id: adr-0026, title: "Deletion Model", resource: ../../adr/0026-deletion-model.md }
  - { id: adr-0029, title: "`role_assignments` is identity-owned: read-models may join it, authority checks go through ports", resource: ../../adr/0029-role-assignments-access-rule.md }
  - { id: context, title: "CONTEXT.md — Roles (each tier has its own role), Geographic hierarchy, Karyakar", resource: ../../../CONTEXT.md }
last_compiled: 6e43fd984ca097e05d67237d341afc11c0bf41ea
---

# Role Appointment

## What it does

<!-- [coverage: high -- ADR-0011 and ADR-0025 read against AppointableRole, AppointmentScope and RoleAppointmentService] -->

A **Karyakar** appoints another into a role at a **scope**: Sanchalak / Sah-Sanchalak onto a Sabha,
Nirikshak / Sah-Nirdeshak / Nirdeshak onto a (Kshetra, demographic), Sanyojak onto a Zone, Regional
Team member or Sant onto a City. One act does three things in one transaction — resolve or create the
appointee **Person**, mint their login if they have none, and record the **RoleAssignment**.

**Revocation** is the same act in reverse, and is a *state change*, not a delete (ADR-0026): the row
survives with `revoked_by` / `revoked_at`, and the holder's appointees and created structures stay
attached to the scope rather than cascading.

## Flow

<!-- [coverage: medium -- backend read end to end; the web section read from its component and scope table, not its template] -->

**Web** — the only appointment surface. `role-appointment` picks a role, renders the one scope input
that role needs (`ROLE_SCOPE`, the form's own policy), then finds the appointee through the shared
person picker or creates one inline. `POST /bff/appointments` returns `201` appointed or `200` name
soft-warn. A Sah-Nirdeshak appointment first reads `GET /bff/appointments/sah-nirdeshak-cap` for the
X/2 chip. `my-authority` answers *"what may I create or appoint here?"* from a static client-side
mirror of the engines, with no endpoint of its own.

**Backend** — `RoleAppointmentService` in a fixed order: authorize, retired-kind guard, cap guard,
resolve-or-create the Person via [person-directory](person-directory.md)'s add service, create the
`User` only when the Person holds no login, save the `role_assignments` row. A name soft-warn
short-circuits the whole act — nothing is created, the candidates bubble up.
`RoleRevocationService` mirrors it: load the active assignment, authorize by **its** scope, apply the
last-one-out guard, mark revoked, and disable the login if that was the User's last active role.

**Mobile** — `_none_` for appointing. It reads the lineage instead: `GET /api/who-appointed-me`
serves the reissue path described in [authentication](authentication.md).

## Rules & authority

<!-- [coverage: high -- AppointmentAuthorization and RoleRevocationService read line by line against ADR-0011 and ADR-0025] -->

- **The ladder.** `AppointmentAuthorization` resolves the target's geographic containment upward
  (Sabha → Kshetra → Zone → City) and asks whether the appointer holds the tier **one rung above at
  the parent scope**. It is the inverted-predicate engine of
  [authorization](../patterns/authorization.md), and revocation reuses it unchanged — which is what
  makes authority a function of **current scope**, never of who appointed (ADR-0025).
- **Two exceptions to the ladder.** The Regional Team is **self-replicating**: a peer at the same
  (City, demographic) may appoint another, alongside the Madhyastha Karyalaya's bootstrap path. A
  Sant is recorded by the MK, not appointed by a tier.
- **The Sah-Nirdeshak holds no appointment authority at all** (ADR-0025) and is capped at **two** per
  (Kshetra, demographic). `SahNirdeshakCap` owns both the threshold and the *reached* rule so the
  write path and the chip cannot disagree; breaching it is `SahNirdeshakCapReachedException` → **409**.
- **Rejections.** Denial → **403**. Username collision → **409** `USERNAME_TAKEN`. Revoking the last
  Regional Team member of a (City, demographic) → **409**. An unknown or already-revoked assignment
  → **404**. A retired Sabha Kind blocks new Sabha-scoped roles → **409**.
- **Initial Home Sabha needs no OTP.** Creating the Person inline is the single documented exception
  to ADR-0002's verification requirement — the appointer is creating them right now.
- **The `role_assignments` rule.** A read-model may join the table; an authority *check* goes through
  a port (ADR-0029).

**Divergence worth knowing:** the ladder above grants a Nirdeshak authority to appoint a Sanchalak,
but `VisibleSections` grants the `ROLE_APPOINTMENT` web section to the **Madhyastha Karyalaya only**.
Nothing else exposes the endpoint, so a Nirdeshak's appointment authority currently has no surface to
be exercised from.

## Where the code is

<!-- [coverage: high -- direct paths, all verified present] -->

- [backend-identity](../structure/backend-identity.md) — the `appointment` feature package, `RoleAppointmentController`, and the
  `JdbcAppointerAuthorityLookup` / `JdbcRevokableRoleAssignments` / `JdbcSahNirdeshakCountLookup` adapters.
- [backend-common-domain](../structure/backend-common-domain.md) — `StructuralHierarchyLookup`, `MadhyasthaKaryalayaLookup`, `AuthorizedAction`,
  `AuthorizationDeniedException`, `ConflictException`.
- [backend-sabha](../structure/backend-sabha.md) — implements the hierarchy lookup the containment walk reads.
- [backend-container](../structure/backend-container.md) — `slice-2` (`role_assignments`), `slice-10` (`zone_id`), `slice-11` (the
  appointment audit columns, `demographic`, `city_id`), `issue-89` (`revoked_at` / `revoked_by`).
- [web](../structure/web.md) — the `role-appointment` and `my-authority` sections, and the shared picker in `identity-domain`.

## Amendments

<!-- [coverage: medium -- reconstructed from changelog headers, class javadocs and ADR consequences; issue-to-change mapping is inferred] -->

- **Slice 11** (issue #12) — the capability: one transaction over Person, User and RoleAssignment,
  with `appointed_by` / `appointed_at`, `demographic` and `city_id`.
- **ADR-0025** — rebound authority from creator to **current scope**, made the Regional Team
  self-replicating, and stripped the Sah-Nirdeshak of administrative authority.
- **Issue #86** — the cap of two, enforced at write time and surfaced proactively as a chip.
- **Issue #89** — revocation: `revoked_at` / `revoked_by`, the last-one-out guard, and login
  withdrawal on the last active role. There is still **no revoke control in the web app** — the
  endpoint ships without a caller.
- **Issue #90** — the authority matrix, deliberately static data rather than a fetch.

## Method

- `AppointmentAuthorization.canAppoint` is a single switch over the eight appointable roles and is
  the source that paid: it is the whole ladder, the two exceptions and the Sant carve-out in twenty
  lines, where ADR-0011 and ADR-0025 take three thousand words and disagree on the Regional Team
  until you read which supersedes which.
- The divergence in `Rules & authority` came from reading `VisibleSections.forMember` beside that
  switch. Neither file is suspicious alone; only the pair shows the authority with no surface, so
  re-check them together on recompile.
