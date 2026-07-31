---
kind: structure
slug: mobile-attendance-domain
source_paths: [apps/mobile/packages/attendance_domain/**]
decisions: [ADR-0014, ADR-0015]
last_compiled: 09fb2075173eb4fc030ce2c26e85311aa26f064a
---
# Mobile — `attendance_domain`

## Purpose

<!-- [coverage: high] -->

Intended as the mobile mirror of [[backend-attendance]] — Occurrence, marking and lifecycle types.
**Currently a scaffold**: one placeholder constant and a docstring.

## Layout

<!-- [coverage: high] -->

One file, `lib/attendance_domain.dart`: a library docstring and a placeholder constant. Pure Dart,
no Flutter import.

## Exposes

<!-- [coverage: high] -->

_none_

## Talks To

<!-- [coverage: high] -->

**Outbound** — `shared_kernel` ([[mobile-shared-kernel]]), declared in `pubspec.yaml` and unused.

**Inbound** — _none_. [[mobile-app]] does not depend on this package.

## Data

<!-- [coverage: high] -->

_none_

The offline SQLite store people expect to find here lives in `sabha_attendance/lib/sync/` — see
[[mobile-app]].

## Gotchas

<!-- [coverage: medium -- the empty state is directly observable; the claim that the offline model is the natural resident of this package is a judgement. ] -->

This is the emptiest package with the strongest case for existing: mobile's offline queue
(`pending_marking`, `attendance_store`) is a genuine local attendance model, not a wire shape, so it
is exactly the kind of thing the generated [[mobile-sabha-api]] client cannot supply. It currently
lives in the app shell instead. If any mirror package earns filling, it is this one.

## Covered by

<!-- [coverage: low -- no feature dossiers exist yet; structurally empty until the first one is admitted. ] -->

_none_

## Sources

- [attendance_domain](../../../apps/mobile/packages/attendance_domain) — `pubspec.yaml` and `lib/attendance_domain.dart`
- [ADR-0007](../../adr/0007-offline-capable-attendance-marking.md), [ADR-0015](../../adr/0015-bounded-context-seams-as-build-modules.md)
