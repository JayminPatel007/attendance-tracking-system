---
kind: structure
slug: mobile-identity-domain
source_paths: [
  apps/mobile/packages/identity_domain/lib/**,
  apps/mobile/packages/identity_domain/pubspec.yaml,
  docs/adr/0014-*.md,
  docs/adr/0015-*.md,
  docs/adr/0016-*.md,
  CONTEXT.md
]
decisions: [ADR-0014, ADR-0015, ADR-0016]
last_compiled: aa7634cf7a76074911b3642c107aabe3062259c7
---

# Mobile Identity Domain

## Purpose

<!-- [coverage: high -- pubspec.yaml and the package's one library docblock] -->

The mobile mirror of [[backend-identity]] — intended to hold `User`, `Session` and the
credential-related types the login flow works in. Pure Dart; the docblock reserves the HTTP adapter
for a future `identity_data` package that does not exist.

**It is a scaffold, and the code it anticipated went elsewhere.** The mobile session and OIDC flow
were built in the app shell instead — `sabha_attendance/lib/auth/` (ADR-0016) — so this package has
stayed at its Slice-1 placeholder while the capability shipped. Read [[mobile-shell]] for the real
thing.

## Layout

<!-- [coverage: high -- exhaustive; the package is one file] -->

One file, `lib/identity_domain.dart`. No `test/` directory. No ring table: the ADR-0019 taxonomy is a
JVM decision and has no mobile counterpart.

## Exposes

<!-- [coverage: high -- the file is the public surface] -->

`package:identity_domain/identity_domain.dart`, exporting `identityDomainPlaceholder`. No routes.

## Talks To

<!-- [coverage: high -- pubspec.yaml dependency block, and an import grep across the workspace] -->

**Outbound** — one declared `path:` dependency on [[mobile-shared-kernel]], which no file imports.

**Inbound** — [[mobile-shell]] declares a `path:` dependency on this package and imports nothing from
it. That is the shape to expect across all four mobile domain packages: the dependency graph
ADR-0015 drew is real in the manifests and empty in the source.

## Data

<!-- [coverage: high -- no persistence dependency, no code] -->

**Owns** — `_none_`. **Reads** — `_none_`.

The access token is held in memory by the shell's `Session`, not here.

## Gotchas

<!-- [coverage: medium -- read off the docblock and melos.yaml; not observed running] -->

- The docblock says these types "land in Slice 2". Slice 2 shipped; the types did not. Treat the
  docblock as intent, not as a description of the package.
- Skipped by `melos run test` — no `test/` directory, so `--dir-exists=test` filters it out.

## Covered by

<!-- [coverage: low -- no dossier names this page; the package holds no capability to name] -->

`_none_`.

## Sources

- [ADR-0014](../../adr/0014-monorepo-and-framework-scaffolding.md), [ADR-0015](../../adr/0015-bounded-context-seams-as-build-modules.md), [ADR-0016](../../adr/0016-oidc-auth-via-keycloak.md)
- [CONTEXT.md](../../../CONTEXT.md) — Karyakar, Sanchalak
- `apps/mobile/packages/identity_domain/lib/identity_domain.dart`
