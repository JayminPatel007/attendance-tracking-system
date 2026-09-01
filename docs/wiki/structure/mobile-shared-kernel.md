---
type: structure
title: Mobile Shared Kernel
description: The mobile shared kernel — a declared Melos package that is still a scaffold with no types in it yet.
resource: apps/mobile/packages/shared_kernel
aliases: [the mobile shared kernel]
source_paths: [
  apps/mobile/packages/shared_kernel/lib/**,
  apps/mobile/packages/shared_kernel/pubspec.yaml,
  docs/adr/0014-*.md,
  docs/adr/0015-*.md,
  CONTEXT.md
]
sources:
  - { id: adr-0014, title: "Monorepo, Angular Web, Spring Boot Backend Layout, and CI Structure", resource: ../../adr/0014-monorepo-and-framework-scaffolding.md }
  - { id: adr-0015, title: "Bounded-Context Seams Are Build Modules (DDD + Hexagonal + Clean)", resource: ../../adr/0015-bounded-context-seams-as-build-modules.md }
  - { id: context, title: "CONTEXT.md — Sabha Occurrence", resource: ../../../CONTEXT.md }
last_compiled: aa7634cf7a76074911b3642c107aabe3062259c7
---

# Mobile Shared Kernel

## Purpose

<!-- [coverage: high -- pubspec.yaml and the package's one library docblock] -->

The mobile counterpart of [backend-common-domain](backend-common-domain.md): cross-context value objects every other mobile
package may depend on, and which may depend on nothing. Pure Dart with **no Flutter import**, so the
type layer stays usable outside a Flutter host.

**It is currently a scaffold.** The library declares one placeholder constant and no types. This is
not rot — ADR-0015 budgeted "~4 Dart packages … most of them empty" as accepted one-time
scaffolding, and nothing has yet needed a type here. See [mobile-shell](mobile-shell.md), which holds the code the
package layout anticipated.

## Layout

<!-- [coverage: high -- exhaustive; the package is one file] -->

One file, `lib/shared_kernel.dart`. No `src/` split, no `test/` directory. Too small for a ring
table, and there is no ADR-0019 ring on this platform in any case — the module taxonomy is a JVM
decision.

## Exposes

<!-- [coverage: high -- the file is the public surface] -->

The public library surface is `package:shared_kernel/shared_kernel.dart`, exporting
`sharedKernelPlaceholder` and nothing else. No routes — a Dart library serves no HTTP.

## Talks To

<!-- [coverage: high -- pubspec.yaml dependency block is empty] -->

**Outbound** — `_none_`. The `pubspec.yaml` declares no runtime dependency at all, which is the
enforcement ADR-0015 wanted: the bottom of the dependency order cannot reach upward, because there is
no path entry that would let it compile.

**Inbound** — declared by [mobile-identity-domain](mobile-identity-domain.md), [mobile-sabha-domain](mobile-sabha-domain.md),
[mobile-attendance-domain](mobile-attendance-domain.md) and [mobile-shell](mobile-shell.md), all four via a `path:` dependency. **Declared is
not imported**: no file in any of them imports this package today. The edges are manifest-level only.

## Data

<!-- [coverage: high -- the package has no persistence dependency and no code] -->

**Owns** — `_none_`. **Reads** — `_none_`.

## Gotchas

<!-- [coverage: medium -- read off melos.yaml; not observed running] -->

- `melos run test` filters on `--dir-exists=test`, so this package is skipped entirely. Its absence
  from a green mobile CI run means nothing.

## Covered by

<!-- [coverage: low -- no dossier names this page; the package holds no capability to name] -->

`_none_`.

## Method

- `lib/shared_kernel.dart` and `apps/mobile/melos.yaml`. The package is a single barrel file exporting nothing, so the manifest rung is the whole method — and the honest finding is the emptiness itself.
