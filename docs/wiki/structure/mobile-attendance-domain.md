---
kind: structure
slug: mobile-attendance-domain
source_paths: [
  apps/mobile/packages/attendance_domain/lib/**,
  apps/mobile/packages/attendance_domain/pubspec.yaml,
  docs/adr/0003-*.md,
  docs/adr/0007-*.md,
  docs/adr/0014-*.md,
  docs/adr/0015-*.md,
  CONTEXT.md
]
decisions: [ADR-0003, ADR-0007, ADR-0014, ADR-0015]
last_compiled: aa7634cf7a76074911b3642c107aabe3062259c7
---

# Mobile Attendance Domain

## Purpose

<!-- [coverage: high -- pubspec.yaml and the package's one library docblock] -->

The mobile mirror of [[backend-attendance]] — intended to hold **Attendance Marking**, the Roster vs
Walk-in `MarkingType`, and the shape of the offline pending-action queue (ADR-0007). Its docblock
predicts that mobile is the primary capture surface (ADR-0003) and that this package would therefore
"evolve richer than the web's counterpart".

**The prediction held; the package did not.** Mobile is indeed the only capture surface and the only
place in the product that works offline — but all of it was written in the app shell, in
`sabha_attendance/lib/sync/` and `lib/roster/`. This package is still its Slice-1 placeholder. See
[[mobile-shell]] and [[attendance-marking]].

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

**Outbound** — one declared `path:` dependency on [[mobile-shared-kernel]], imported by nothing.

**Inbound** — [[mobile-shell]] declares this package and imports nothing from it.

## Data

<!-- [coverage: high -- no persistence dependency, no code] -->

**Owns** — `_none_`. **Reads** — `_none_`.

This is the page a reader would most expect to own the offline queue, and it does not. The three
SQLite tables — `roster_cache`, `pending_markings`, `sync_meta` — are created and written by
`AttendanceStore` in the shell, so they are [[mobile-shell]]'s **Owns** and nothing here.

## Gotchas

<!-- [coverage: medium -- read off the docblock and melos.yaml; not observed running] -->

- Skipped by `melos run test` — no `test/` directory, so `--dir-exists=test` filters it out. The
  offline queue *is* well tested, but those tests live under `sabha_attendance/test/sync/`.

## Covered by

<!-- [coverage: low -- no dossier names this page; the package holds no capability to name] -->

`_none_`. [[attendance-marking]] is the dossier for this capability, and it points at the shell.

## Sources

- [ADR-0003](../../adr/0003-platform-split-by-role.md), [ADR-0007](../../adr/0007-offline-capable-attendance-marking.md), [ADR-0014](../../adr/0014-monorepo-and-framework-scaffolding.md), [ADR-0015](../../adr/0015-bounded-context-seams-as-build-modules.md)
- [CONTEXT.md](../../../CONTEXT.md) — Attendance Marking, Walk-in, Roster
- `apps/mobile/packages/attendance_domain/lib/attendance_domain.dart`
