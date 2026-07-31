---
kind: structure
slug: mobile-identity-domain
source_paths: [apps/mobile/packages/identity_domain/**]
decisions: [ADR-0014, ADR-0015]
last_compiled: 09fb2075173eb4fc030ce2c26e85311aa26f064a
---
# Mobile — `identity_domain`

## Purpose

<!-- [coverage: high] -->

Intended as the mobile mirror of [[backend-identity]] — User, Session and credential types for the
login flow. **Currently a scaffold**: one placeholder constant and a docstring.

## Layout

<!-- [coverage: high] -->

One file, `lib/identity_domain.dart`: a library docstring and
`const String identityDomainPlaceholder`. Pure Dart. Its docstring says the HTTP adapter would live
in "a future `identity_data` package" — that package does not exist.

## Exposes

<!-- [coverage: high] -->

_none_

## Talks To

<!-- [coverage: high] -->

**Outbound** — `shared_kernel` ([[mobile-shared-kernel]]), declared in `pubspec.yaml`. Nothing is
actually imported from it.

**Inbound** — _none_. [[mobile-app]] does not depend on this package.

## Data

<!-- [coverage: high] -->

_none_

## Gotchas

<!-- [coverage: medium -- the mismatch between the docstring's plan and the shipped app is directly observable; calling the package obsolete rather than pending is a judgement. ] -->

The docstring says these types "land in Slice 2." Slice 2 shipped; they did not. The mobile login
flow lives in `sabha_attendance/lib/auth/`, and its model types come from the generated
[[mobile-sabha-api]] client. This package is the plan that the OpenAPI decision (issue #73)
overtook. Compare [[web]]'s `analytics-domain`, where the same thing happened and was written down.

## Covered by

<!-- [coverage: low -- no feature dossiers exist yet; structurally empty until the first one is admitted. ] -->

_none_

## Sources

- [identity_domain](../../../apps/mobile/packages/identity_domain) — `pubspec.yaml` and `lib/identity_domain.dart`
- [ADR-0014](../../adr/0014-monorepo-and-framework-scaffolding.md), [ADR-0015](../../adr/0015-bounded-context-seams-as-build-modules.md)
