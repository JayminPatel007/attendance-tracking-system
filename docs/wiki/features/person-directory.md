---
type: feature
title: Person Directory
description: Finding a Person in the central Directory, and adding one under the two-signal de-duplication rule.
aliases: [Directory, add person, de-duplication, dedup, mobile lookup, person picker, Karyakar lookup]
tags: [bff]
source_paths: [
  apps/backend/identity-service/*/src/main/**,
  apps/mobile/sabha_attendance/lib/add_person/**,
  apps/web/projects/identity-domain/src/**,
  apps/web/src/app/sections/role-appointment/**,
  apps/backend/application-container/src/main/resources/db/changelog/slice-2/**,
  apps/backend/application-container/src/main/resources/db/changelog/slice-6/**,
  docs/adr/0005-*.md,
  docs/adr/0007-*.md,
  docs/adr/0013-*.md,
  CONTEXT.md
]
issues: [7, 81, 104]
sources:
  - { id: adr-0005, title: "Single Organization, Not Multi-Tenant", resource: ../../adr/0005-single-organization-not-multi-tenant.md }
  - { id: adr-0007, title: "Mobile App is Offline-Capable for Attendance Marking Only", resource: ../../adr/0007-offline-capable-attendance-marking.md }
  - { id: adr-0013, title: "Directory De-duplication on Person Add", resource: ../../adr/0013-directory-de-duplication-on-person-add.md }
  - { id: context, title: "CONTEXT.md — Person, Directory, Home Sabha, Sabha Kind", resource: ../../../CONTEXT.md }
last_compiled: 6e43fd984ca097e05d67237d341afc11c0bf41ea
---

# Person Directory

## What it does

<!-- [coverage: high -- ADR-0013 read against Person.java, AddPersonApplicationService and the slice-6 migration] -->

The **Directory** is the one list of every **Person** the organisation knows. Every other capability
starts by finding someone in it, so the Directory's job is that a Person exists **once**.

Identity is **mobile-keyed** (ADR-0013): a Person carries either their own mobile, unique
system-wide across every State the single deployment covers (ADR-0005), or a `guardianPersonId`
link to the parent whose phone they share. Exactly one of the two, enforced in `Person.create`.

Adding a Person therefore runs a two-signal de-duplication check — a **hard block** on an exact
mobile match, and a **soft warning** on a close name — and registers the new Person's first **Home
Sabha** in the same transaction.

## Flow

<!-- [coverage: high -- both ends read end to end: AddPersonApplicationService and JdbcPersonDirectory, then add_person_api.dart and person-picker.component.ts] -->

**Mobile** — the Sanchalak's add-person screen, three steps in `AddPersonController`:

1. `GET /api/directory/persons?mobile=…`. A hit routes to the existing Person's profile — the
   forced redirect; a `404` is an *outcome*, not an error, and means the number is new.
2. Details entry, then `POST /api/directory/persons`. `201` created, `200` name soft-warn,
   `409` mobile hard block (the block can also race the submit), `422` a domain-rule rejection.
3. Re-submitting with `overrideDuplicateWarning` carries the adder's "none of these" past the warn.

**Web** — no add-person screen of its own. `PersonPickerComponent` in `identity-domain` is the
shared directory-first search every appointing screen composes: search by mobile or by name within a
Kshetra, pick, and credentials are auto-suggested. Inline Person *creation* stays feature-private to
[role-appointment](role-appointment.md).

**Backend** — `PersonDirectoryRestController` (`/api/*`) and `DirectoryBffController` (`/bff/*`) are
mobile-bearer and cookie-session twins over the one `SearchDirectoryUseCase`. Both split the exact
lookup and the fuzzy lookup across **two paths**, because a single path answering with either a
Person or a candidate list can only be typed `Object`, which the generated clients render untyped.
`AddPersonApplicationService` orders the checks — mobile block, Home Sabha resolution, retired-kind
guard, name candidates — then `JdbcPersonDirectory` writes `persons` and `home_sabhas` together.

## Rules & authority

<!-- [coverage: high -- ADR-0013 cross-checked against the service, the adapter SQL and slice-6's constraints] -->

- **Who.** Any authenticated Karyakar. There is **no Authorization Engine here**: adding is not a
  scoped authority, so the only check is `CallerResolver.requireUserId`, whose failure is a 403
  through [authorization](../patterns/authorization.md)'s denial path.
- **Mobile hard block.** An exact match throws `MobileAlreadyRegisteredException` → **409** with
  `code: MOBILE_ALREADY_REGISTERED` and an `existingPersonId` extension the client redirects on.
  Backed by a real `persons_mobile_unique` constraint, so the block cannot be raced away.
- **Name soft warn.** Up to **five** candidates, matched by `dmetaphone` equality **or** Levenshtein
  distance ≤ 2, and **nothing is created**. Overriding past a non-empty list publishes
  `PersonAddedOverDuplicateWarning` for audit.
- **Mobile XOR guardian.** Neither is `GuardianOrMobileRequiredException` → **422**, backed by a
  `CHECK`. Guardian-linked children hold `NULL` mobiles, distinct in Postgres, so siblings never collide.
- **Retired Sabha Kind.** A Home Sabha whose kind is soft-retired rejects the add (**409**), checked
  through sabha's `StructuralHierarchyLookup`.
- **Always online.** ADR-0007 permits offline for marking only; the de-dup check must hit the live
  Directory, so nothing here is ever queued.

## Where the code is

<!-- [coverage: high -- direct paths, all verified present] -->

- [backend-identity](../structure/backend-identity.md) — the `directory` feature package, both controllers, and
  `JdbcPersonDirectory`, which is also the transfer and contact adapter.
- [backend-common-domain](../structure/backend-common-domain.md) — `CallerResolver`, `StructuralHierarchyLookup`, `SabhaKindRetiredException`.
- [backend-sabha](../structure/backend-sabha.md) — implements the hierarchy lookup the Kshetra scope and the retired-kind guard read.
- [backend-container](../structure/backend-container.md) — `slice-2` (the `persons` / `home_sabhas` core), `slice-6` (guardian link,
  unique mobile, the fuzzy extensions and indexes), and the ProblemDetail mapping.
- [mobile-shell](../structure/mobile-shell.md) — `lib/add_person/`.
- [mobile-sabha-api](../structure/mobile-sabha-api.md) — `PersonDirectoryRestControllerApi`, which the mobile lookup calls whole.
- [web](../structure/web.md) — `PersonPickerComponent` and the credential suggestions in `identity-domain`.

## Amendments

<!-- [coverage: medium -- slice numbering and issue attribution reconstructed from changelog headers and class javadocs, not from the PRs] -->

- **Slice 2** — `persons` and `home_sabhas` as part of the core schema.
- **Slice 6** (issue #7) — de-duplication: the guardian link, the system-wide mobile constraint,
  `fuzzystrmatch` + `pg_trgm`, and the phonetic and trigram indexes.
- **Slice 7** — `findByMobileForWalkIn`, the Walk-in-shaped twin of the mobile lookup.
- **Issue #81** — the picker and the Directory service moved to the `identity-domain` web library so
  every appointing screen composes one search.
- **Issue #104** — the contract split: fuzzy name search moved to its own path, and the add
  response's discriminator and candidate list were declared always-present.

**Divergence from ADR-0013, worth knowing:** the ADR scopes the name search to the **City**. The
implementation scopes it to the **Kshetra** of the Home Sabha, and `slice-6`'s header says so
explicitly — *"true City scope arrives with Slice 10"*. Slice 10 shipped the City tier; this search
was never widened, and the ADR has not been amended to match.

## Method

- The whole page turns on one file pair read together: `AddPersonApplicationService` for the order of
  the checks, and `slice-6/001-person-directory.sql` for the constraints and indexes underneath them.
  The migration is the source that paid — it is the only place the *matcher* is written down
  (`dmetaphone`, trigram, the Kshetra scope), and its header comment is where the standing divergence
  from ADR-0013's City scope is admitted. No Java file states it.
- The two controllers' javadocs carry the issue-#104 two-path argument, which is recoverable nowhere
  else and explains a route layout that otherwise looks redundant.
