---
kind: structure
slug: mobile-sabha-domain
source_paths: [
  apps/mobile/packages/sabha_domain/lib/**,
  apps/mobile/packages/sabha_domain/pubspec.yaml,
  docs/adr/0003-*.md,
  docs/adr/0014-*.md,
  docs/adr/0015-*.md,
  CONTEXT.md
]
decisions: [ADR-0003, ADR-0014, ADR-0015]
last_compiled: aa7634cf7a76074911b3642c107aabe3062259c7
---

# Mobile Sabha Domain

## Purpose

<!-- [coverage: high -- pubspec.yaml and the package's one library docblock] -->

The mobile mirror of [[backend-sabha]] — intended to hold **Sabha**, **Sabha Occurrence** and
**Roster** as the mobile app sees them. The docblock records a real design constraint worth keeping:
mobile's permission model is narrow, own Sabha(s) only per ADR-0003, so this package would only ever
expose the *read* shapes a Sanchalak or Sah-Sanchalak needs — never the structural-admin surface that
is [[web]]'s.

**It is a scaffold.** The read shapes it describes were written in the app shell instead, as
`OccurrenceView` and `RosterEntry` in `sabha_attendance/lib/roster/roster_api.dart`. See
[[mobile-shell]].

## Layout

<!-- [coverage: high -- exhaustive; the package is one file] -->

One file, `lib/sabha_domain.dart`. No `test/` directory, and no ring table — ADR-0019's module
taxonomy governs the JVM contexts only.

## Exposes

<!-- [coverage: high -- the file is the public surface] -->

`package:sabha_domain/sabha_domain.dart`, exporting `sabhaDomainPlaceholder`. No routes.

## Talks To

<!-- [coverage: high -- pubspec.yaml dependency block, and an import grep across the workspace] -->

**Outbound** — one declared `path:` dependency on [[mobile-shared-kernel]], imported by nothing.

**Inbound** — [[mobile-shell]] declares this package and imports nothing from it.

## Data

<!-- [coverage: high -- no persistence dependency, no code] -->

**Owns** — `_none_`. **Reads** — `_none_`.

The cached Roster is written by the shell's `AttendanceStore`, into its own SQLite file. If these
types are ever filled in, that cache is the thing that would move here — and the ownership question
would move with it.

## Gotchas

<!-- [coverage: medium -- read off the docblock and melos.yaml; not observed running] -->

- Skipped by `melos run test` — no `test/` directory, so `--dir-exists=test` filters it out.

## Covered by

<!-- [coverage: low -- no dossier names this page; the package holds no capability to name] -->

`_none_`. [[attendance-marking]] names `sabha_attendance/lib/roster/` for the Roster shapes, which is
where they actually live.

## Sources

- [ADR-0003](../../adr/0003-platform-split-by-role.md), [ADR-0014](../../adr/0014-monorepo-and-framework-scaffolding.md), [ADR-0015](../../adr/0015-bounded-context-seams-as-build-modules.md)
- [CONTEXT.md](../../../CONTEXT.md) — Sabha, Sabha Occurrence, Roster, Sanchalak, Sah-Sanchalak
- `apps/mobile/packages/sabha_domain/lib/sabha_domain.dart`
