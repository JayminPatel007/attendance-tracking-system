---
kind: structure
slug: mobile-sabha-domain
source_paths: [apps/mobile/packages/sabha_domain/**]
decisions: [ADR-0014, ADR-0015]
last_compiled: 09fb2075173eb4fc030ce2c26e85311aa26f064a
---
# Mobile — `sabha_domain`

## Purpose

<!-- [coverage: high] -->

Intended as the mobile mirror of [[backend-sabha]] — the Sabha, Sabha Kind and structural-hierarchy
types. **Currently a scaffold**: one placeholder constant and a docstring.

## Layout

<!-- [coverage: high] -->

One file, `lib/sabha_domain.dart`: a library docstring and a placeholder constant. Pure Dart, no
Flutter import.

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

## Gotchas

<!-- [coverage: medium -- the empty state is directly observable; the explanation for it is inferred from the app shell's dependency list. ] -->

Empty for the same reason as [[mobile-identity-domain]]: the app takes its Sabha types from the
generated [[mobile-sabha-api]] models. Note that mobile's role split (ADR-0003) means the Sanchalak
never creates structure, so most of what [[backend-sabha]] owns has no mobile counterpart to mirror
in the first place.

## Covered by

<!-- [coverage: low -- no feature dossiers exist yet; structurally empty until the first one is admitted. ] -->

_none_

## Sources

- [sabha_domain](../../../apps/mobile/packages/sabha_domain) — `pubspec.yaml` and `lib/sabha_domain.dart`
- [ADR-0015](../../adr/0015-bounded-context-seams-as-build-modules.md)
