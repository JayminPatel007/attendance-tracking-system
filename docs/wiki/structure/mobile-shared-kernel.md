---
kind: structure
slug: mobile-shared-kernel
source_paths: [apps/mobile/packages/shared_kernel/**]
decisions: [ADR-0014, ADR-0015]
last_compiled: 09fb2075173eb4fc030ce2c26e85311aa26f064a
---
# Mobile — `shared_kernel`

## Purpose

<!-- [coverage: high] -->

The mobile mirror of [[backend-common-domain]]: cross-context value objects every other mobile
package may depend on, and which may depend on none of them. **Currently a scaffold** — it holds one
placeholder constant and no real types.

## Layout

<!-- [coverage: high] -->

One file, `lib/shared_kernel.dart`, containing a library docstring and
`const String sharedKernelPlaceholder`. Pure Dart with no Flutter import, deliberately, so the
package could be reused outside a Flutter client.

## Exposes

<!-- [coverage: high] -->

_none_

## Talks To

<!-- [coverage: high] -->

**Outbound** — _none_. ADR-0015 forbids it: every other mobile package may depend on
`shared_kernel`, never the reverse.

**Inbound** — declared by [[mobile-identity-domain]], [[mobile-sabha-domain]] and
[[mobile-attendance-domain]] in their pubspecs. All three are themselves scaffolds, so nothing
running imports anything from here. [[mobile-app]] does not depend on it at all.

## Data

<!-- [coverage: high] -->

_none_

## Gotchas

<!-- [coverage: medium -- the empty state is directly observable; whether it is intended to stay empty is a judgement the ADRs do not settle. ] -->

Scaffolded per ADR-0014 ahead of need, and still empty. The mobile app's cross-cutting types came
from the generated [[mobile-sabha-api]] client instead, so the pressure that would have filled this
package never arrived. Treat "put it in `shared_kernel`" as an open decision, not the default.

## Covered by

<!-- [coverage: low -- no feature dossiers exist yet; structurally empty until the first one is admitted. ] -->

_none_

## Sources

- [shared_kernel](../../../apps/mobile/packages/shared_kernel) — `pubspec.yaml` and `lib/shared_kernel.dart`
- [ADR-0015](../../adr/0015-bounded-context-seams-as-build-modules.md)
