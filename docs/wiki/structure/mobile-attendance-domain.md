---
type: structure
title: Mobile Attendance Domain
description: Mobile Attendance Marking, Walk-in and offline-queue types.
resource: apps/mobile/packages/attendance_domain
scaffold: true
aliases: [mobile Attendance Marking, Walk-in, offline queue]
tags: [offline-sync]
source_paths: [
  apps/mobile/packages/attendance_domain/lib/**,
  apps/mobile/packages/attendance_domain/pubspec.yaml,
  docs/adr/0003-*.md,
  docs/adr/0007-*.md,
  docs/adr/0014-*.md,
  docs/adr/0015-*.md,
  CONTEXT.md
]
sources:
  - { id: adr-0003, title: "Platform Split: Mobile for Sabha-Level Operations, Web for Everything Else", resource: ../../adr/0003-platform-split-by-role.md }
  - { id: adr-0007, title: "Mobile App is Offline-Capable for Attendance Marking Only", resource: ../../adr/0007-offline-capable-attendance-marking.md }
  - { id: adr-0014, title: "Monorepo, Angular Web, Spring Boot Backend Layout, and CI Structure", resource: ../../adr/0014-monorepo-and-framework-scaffolding.md }
  - { id: adr-0015, title: "Bounded-Context Seams Are Build Modules (DDD + Hexagonal + Clean)", resource: ../../adr/0015-bounded-context-seams-as-build-modules.md }
  - { id: context, title: "CONTEXT.md — Attendance Marking, Walk-in, Roster", resource: ../../../CONTEXT.md }
last_compiled: aa7634cf7a76074911b3642c107aabe3062259c7
---

# Mobile Attendance Domain

## Purpose

<!-- [coverage: high -- pubspec.yaml and the package's one library docblock] -->

The mobile mirror of [backend-attendance](backend-attendance.md) — intended to hold **Attendance Marking**, the Roster vs
Walk-in `MarkingType`, and the shape of the offline pending-action queue (ADR-0007). Its docblock
predicts that mobile is the primary capture surface (ADR-0003) and that this package would therefore
"evolve richer than the web's counterpart".

**The prediction held; the package did not.** Mobile is indeed the only capture surface and the only
place in the product that works offline — but all of it was written in the app shell, in
`sabha_attendance/lib/sync/` and `lib/roster/`. This package is still its Slice-1 placeholder. See
[mobile-shell](mobile-shell.md) and [attendance-marking](../features/attendance-marking.md).

## Layout

<!-- [coverage: high -- exhaustive; the package is one file] -->

One file, `lib/attendance_domain.dart`. No `test/` directory; no ring table, ADR-0019 being a JVM
decision.

## Exposes

<!-- [coverage: high -- the file is the public surface] -->

`package:attendance_domain/attendance_domain.dart`, exporting `attendanceDomainPlaceholder`. No
routes.

## Talks To

<!-- [coverage: high -- pubspec.yaml dependency block, and an import grep across the workspace] -->

**Outbound** — one declared `path:` dependency on [mobile-shared-kernel](mobile-shared-kernel.md), imported by nothing.

**Inbound** — [mobile-shell](mobile-shell.md) declares this package and imports nothing from it.

## Data

<!-- [coverage: high -- no persistence dependency, no code] -->

**Owns** — `_none_`. **Reads** — `_none_`.

This is the page a reader would most expect to own the offline queue, and it does not. The three
SQLite tables — `roster_cache`, `pending_markings`, `sync_meta` — are created and written by
`AttendanceStore` in the shell, so they are [mobile-shell](mobile-shell.md)'s **Owns** and nothing here.

## Gotchas

<!-- [coverage: medium -- read off the docblock and melos.yaml; not observed running] -->

- Skipped by `melos run test` — no `test/` directory, so `--dir-exists=test` filters it out. The
  offline queue *is* well tested, but those tests live under `sabha_attendance/test/sync/`.

## Covered by

<!-- [coverage: low -- no dossier names this page; the package holds no capability to name] -->

`_none_`. [attendance-marking](../features/attendance-marking.md) is the dossier for this capability, and it points at the shell.

## Method

- `lib/attendance_domain.dart` plus the package manifest. A barrel file exporting nothing, so the method is the manifest rung and the finding is the emptiness.
