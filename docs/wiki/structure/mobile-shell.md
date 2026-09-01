---
type: structure
title: Mobile App Shell
description: The Flutter app the Sanchalak carries: roster marking, walk-in capture and the offline queue that syncs them.
resource: apps/mobile/sabha_attendance
aliases: [the Flutter app, the Sanchalak's phone, hajri app, the offline queue]
tags: [offline-sync]
source_paths: [
  apps/mobile/sabha_attendance/lib/**,
  apps/mobile/sabha_attendance/pubspec.yaml,
  apps/mobile/melos.yaml,
  apps/mobile/pubspec.yaml,
  docs/adr/0001-*.md,
  docs/adr/0002-*.md,
  docs/adr/0003-*.md,
  docs/adr/0006-*.md,
  docs/adr/0007-*.md,
  docs/adr/0012-*.md,
  docs/adr/0013-*.md,
  docs/adr/0014-*.md,
  docs/adr/0015-*.md,
  docs/adr/0016-*.md,
  CONTEXT.md
]
issues: [67, 73]
sources:
  - { id: adr-0001, title: "Sabha Occurrence Lifecycle", resource: ../../adr/0001-sabha-occurrence-lifecycle.md }
  - { id: adr-0002, title: "Sanchalak-Initiated Home Sabha Transfer Requires Person Verification", resource: ../../adr/0002-home-sabha-transfer-requires-person-verification.md }
  - { id: adr-0003, title: "Platform Split: Mobile for Sabha-Level Operations, Web for Everything Else", resource: ../../adr/0003-platform-split-by-role.md }
  - { id: adr-0006, title: "BSS Membership Is Additive, Not Replacement", resource: ../../adr/0006-bss-is-additive-not-replacement.md }
  - { id: adr-0007, title: "Mobile App is Offline-Capable for Attendance Marking Only", resource: ../../adr/0007-offline-capable-attendance-marking.md }
  - { id: adr-0012, title: "Sabha Schedule Shapes and Occurrence Materialization", resource: ../../adr/0012-sabha-schedule-shapes-and-occurrence-materialization.md }
  - { id: adr-0013, title: "Directory De-duplication on Person Add", resource: ../../adr/0013-directory-de-duplication-on-person-add.md }
  - { id: adr-0014, title: "Monorepo, Angular Web, Spring Boot Backend Layout, and CI Structure", resource: ../../adr/0014-monorepo-and-framework-scaffolding.md }
  - { id: adr-0015, title: "Bounded-Context Seams Are Build Modules (DDD + Hexagonal + Clean)", resource: ../../adr/0015-bounded-context-seams-as-build-modules.md }
  - { id: adr-0016, title: "OIDC Authentication via Keycloak (Separate Container)", resource: ../../adr/0016-oidc-auth-via-keycloak.md }
  - { id: context, title: "CONTEXT.md — Sanchalak, Sah-Sanchalak, Sabha Occurrence, Roster, Walk-in, Home Sabha", resource: ../../../CONTEXT.md }
last_compiled: aa7634cf7a76074911b3642c107aabe3062259c7
---

# Mobile App Shell

## Purpose

<!-- [coverage: high -- main.dart, pubspec.yaml, the eleven feature docblocks, ADR-0003] -->

The Flutter app for the **Sanchalak** and **Sah-Sanchalak** — the tier *at* the Sabha, per ADR-0003.
It is the product's only attendance-capture surface and the only part of it that works **offline**
(ADR-0007). Everything above the Sabha is [web](web.md)'s.

It is also, in practice, the whole mobile app: the four domain packages the workspace declares are
placeholders, so all mobile logic lives here.

## Layout

<!-- [coverage: high -- directory listing, 35 lib files, plus main.dart read in full] -->

35 files under `lib/`, in **feature directories** — the navigation axis on this unit. Nine of the
eleven follow the same `*_api` / `*_controller` / `*_screen` triad, so the shape is learnable once
and reused.

| Directory | Holds |
|---|---|
| `roster/` | The home screen. Roster load, marking, and the surface every other feature launches from. |
| `sync/` | `AttendanceStore` (SQLite), `PendingMarking`, `SyncEngine`, `SyncStatusScreen`. The offline half. |
| `auth/` | `AuthConfig`, `AuthService` (OIDC via `flutter_appauth`), `Session`, `LoginScreen`. |
| `walk_in/`, `add_person/`, `home_sabha_transfer/`, `selection/` | Directory-facing actions taken during a Sabha. |
| `occurrence_control/`, `monthly_occurrence/` | Cancel / reschedule / venue-override, and materializing a monthly ad-hoc Occurrence (ADR-0012). |
| `password_reset/` | The two logged-out screens: OTP reset, and `who-appointed-me`. |
| `api/` | `api_error.dart` — the shared RFC 9457 ProblemDetail decoder and `apiError` status dispatcher (issue #67). |

No ring table: ADR-0019's module taxonomy governs the JVM contexts only. There is no router package
either — `main.dart` composes every API client by hand and navigates with bare
`Navigator.push(MaterialPageRoute(...))`.

## Exposes

<!-- [coverage: high -- Uri.parse grep across lib/, plus the three generated-client call sites] -->

**Screens**, not routes. `AppShell` switches on `Session.accessToken`: null renders `LoginScreen`,
otherwise `_RosterShell`. Six of the eight actions are gated on a **loaded roster** — Add Person,
Walk-in, transfer and Selection all read `occurrence.sabhaId` from it and refuse with a snackbar
when it is absent. Monthly Occurrence is deliberately exempt, because a monthly ad-hoc Sabha with no
Occurrence yet has no roster to derive an id from (ADR-0012).

**Backend prefixes called.** All `/api/*` — this app never touches `/bff/*`, which is the clean half
of the ADR-0022 split.

| Prefix | Feature |
|---|---|
| `/api/sanchalak/current-roster`, `/current-occurrence`, `/monthly-sabhas` | roster, occurrence_control, monthly_occurrence |
| `/api/occurrences/{id}/markings`, `/api/sync` | roster, sync |
| `/api/occurrences/{id}/{cancel,reschedule,venue-override}`, `/api/sabhas/{id}/occurrences` | occurrence_control, monthly_occurrence |
| `/api/directory/persons`, `/api/directory/walk-in-search` | add_person, home_sabha_transfer, walk_in |
| `/api/occurrences/{id}/walk-ins`, `/api/sanchalak/nominations` | walk_in, selection |
| `/api/home-sabha-transfers`, `/{id}/confirm` | home_sabha_transfer |
| `/api/password-reset/{request,verify,complete}`, `/api/who-appointed-me` | password_reset |

## Talks To

<!-- [coverage: medium -- prefixes grepped here, but attributed to contexts from the backend pages rather than re-derived] -->

**Outbound** — HTTP and OIDC. Nothing else: no socket, no push, no background worker.

| Target | Via |
|---|---|
| [backend-attendance](backend-attendance.md) | `/api/sanchalak/*`, `/api/occurrences/*`, `/api/sync` |
| [backend-identity](backend-identity.md) | `/api/directory/*`, `/api/home-sabha-transfers/*`, `/api/sanchalak/nominations`, `/api/password-reset/*`, `/api/who-appointed-me` |
| Keycloak | Authorization Code + PKCE via `flutter_appauth` (ADR-0016), issuer from `--dart-define` |
| [mobile-sabha-api](mobile-sabha-api.md) | the generated client, imported by `walk_in_api`, `selection_api`, `add_person_api` |

**Inbound** — `_none_`. Nothing calls the app.

## Data

<!-- [coverage: high -- AttendanceStore._onCreate read in full; it is the single DDL site] -->

**Owns** — three SQLite tables, created by `AttendanceStore._onCreate` at schema version 1, in
`sabha_attendance.db` under the platform app-documents directory:

| Table | Holds |
|---|---|
| `roster_cache` | The last-known Roster snapshot plus its `roster_version`. Single-row by `CHECK (id = 1)` — one Sanchalak per device, one open Occurrence at a time. |
| `pending_markings` | The offline queue, keyed `(occurrence_id, person_id)` so a re-mark replaces rather than duplicates. Carries `client_marked_at`, which is what last-write-wins resolves on. |
| `sync_meta` | Key/value; today only `last_synced_at`. |

This is **real local data**, and the only client-side store in the product — [web](web.md) owns nothing,
by a zero-hit grep for every browser persistence API.

**Reads** — `_none_`. Every backend table is reached over HTTP, never directly.

## Gotchas

<!-- [coverage: medium -- each read off the named source file; none exercised at runtime] -->

- **The four domain packages are declared and never imported.** `pubspec.yaml` `path:`-depends on
  [mobile-shared-kernel](mobile-shared-kernel.md), [mobile-identity-domain](mobile-identity-domain.md), [mobile-sabha-domain](mobile-sabha-domain.md) and
  [mobile-attendance-domain](mobile-attendance-domain.md); an import grep across `lib/` and `test/` finds **zero** references to
  any of them. Only [mobile-sabha-api](mobile-sabha-api.md) is really used, in three files. The ADR-0015 seam is
  enforced by a graph nothing crosses because nothing is there to cross it.
- **Platform directories are not committed.** `sabha_attendance/{android,ios}` are gitignored and
  regenerated; a fresh clone needs `flutter create --platforms=android,ios
  --project-name=sabha_attendance_mobile .` inside `sabha_attendance/` before `flutter run`
  (ADR-0014). CI does the same.
- **All configuration is `--dart-define`, resolved at build time**, with defaults pointing at
  `localhost` for `docker-compose`. On an Android emulator every default is wrong: `localhost` must
  become `10.0.2.2`.
- **Recording a Walk-in is online-only**, which is stricter than ADR-0007 — the ADR only rules out
  marking a visitor who is not yet in the Directory. Nothing has amended the ADR to match.
- Dependencies are wired by hand in `main.dart`, and re-created on every `build()` of `AppShell`.
  There is no DI container and no state-management package.

## Covered by

<!-- [coverage: high -- derived: each dossier below names this page and the `lib/` directories it covers] -->

- [attendance-marking](../features/attendance-marking.md) — the Roster, sync and Walk-in paths.
- [person-directory](../features/person-directory.md) — `lib/add_person/`.
- [authentication](../features/authentication.md) — `lib/auth/` and `lib/password_reset/`.
- [home-sabha-transfer](../features/home-sabha-transfer.md) — `lib/home_sabha_transfer/`.

Still uncovered: `lib/selection/`, `lib/occurrence_control/` and `lib/monthly_occurrence/`, all
dossier candidates. `password-reset`, listed here as a candidate before, was **merged** into
[authentication](../features/authentication.md).

## Method

- `lib/main.dart`, `lib/sync/attendance_store.dart` and `apps/mobile/melos.yaml` — the highest-yield sources here; the sync engine is where the offline contract is written down.
- Directory listing over `lib/` for the feature-folder axis: a Flutter app has no `package-info.java` rung to try.
