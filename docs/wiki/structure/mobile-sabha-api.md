---
type: structure
title: Mobile Sabha API Client
description: The Dart client generated from the backend's OpenAPI spec; the mobile app's only transport to the API.
resource: apps/mobile/packages/sabha_api
aliases: [the generated Dart client, the typed API client, the OpenAPI client]
source_paths: [
  apps/mobile/packages/sabha_api/lib/**,
  apps/mobile/packages/sabha_api/pubspec.yaml,
  apps/mobile/packages/sabha_api/.openapi-generator-ignore,
  apps/mobile/melos.yaml,
  docs/adr/0014-*.md,
  docs/adr/0015-*.md,
  CONTEXT.md
]
issues: [73, 75]
sources:
  - { id: adr-0014, title: "Monorepo, Angular Web, Spring Boot Backend Layout, and CI Structure", resource: ../../adr/0014-monorepo-and-framework-scaffolding.md }
  - { id: adr-0015, title: "Bounded-Context Seams Are Build Modules (DDD + Hexagonal + Clean)", resource: ../../adr/0015-bounded-context-seams-as-build-modules.md }
  - { id: context, title: "CONTEXT.md — Walk-in, Roster, Sabha Occurrence", resource: ../../../CONTEXT.md }
last_compiled: aa7634cf7a76074911b3642c107aabe3062259c7
---

# Mobile Sabha API Client

## Purpose

<!-- [coverage: high -- pubspec.yaml, .openapi-generator-ignore, melos.yaml generate:api script] -->

The typed Dart client, **entirely generated** from `apps/backend/openapi.json` (issue #73). It is
the mobile twin of [web](web.md)'s `shared-data-access` library and exists for the same reason: a
hand-rolled parser drifts from the backend silently (issue #75), a generated one cannot.

It is the **fifth** Dart package, and the only one added after the Slice-1 scaffold — ADR-0014 and
ADR-0015 both describe a four-package mobile workspace. It is also the only mobile package other
than [mobile-shell](mobile-shell.md) with real code in it.

## Layout

<!-- [coverage: high -- directory listing and file counts] -->

103 Dart files under `lib/`, flat by generator convention rather than by any ring:

| Directory | Files | Holds |
|---|---|---|
| `lib/api/` | 20 | One service per backend controller, named after it — `AttendanceRestControllerApi`, `SelectionRestControllerApi`, `PersonDirectoryRestControllerApi`, and 17 more. |
| `lib/model/` | 74 | Request/response models — `WalkInRequest`, `WalkInCandidate`, `NominateRequest`, `NominateResponse`, … |
| `lib/auth/` | 5 | `HttpBearerAuth`, `ApiKeyAuth`, `OAuth`, `HttpBasicAuth` and the `Authentication` base. Mobile uses the bearer one. |
| `lib/` root | 4 | `api.dart` (the barrel), `api_client.dart`, `api_exception.dart`, `api_helper.dart`. |

Regenerate with `melos run generate:api`, which runs `@openapitools/openapi-generator-cli` against
`../backend/openapi.json` at a version pinned in `openapitools.json`. **Do not hand-edit** anything
here — the next regeneration overwrites it.

## Exposes

<!-- [coverage: high -- the barrel file and the api/ listing] -->

`package:sabha_api/api.dart` re-exports the whole surface; consumers import that one barrel. No
routes of its own — it *calls* routes. The 20 services cover **every** backend controller, `/bff/*`
included, so the mobile binary compiles in the web BFF's client surface and never calls it. That
mirrors [web](web.md) exactly, in the opposite direction.

## Talks To

<!-- [coverage: high -- pubspec.yaml, plus an import grep for consumers] -->

**Outbound** — the backend over HTTP, through the `http` package. `ApiClient` takes a `basePath` and
an `Authentication`; it holds no URL of its own, so the target is whatever the caller passes.

**Inbound** — [mobile-shell](mobile-shell.md) imports it in three files: `walk_in_api.dart`, `selection_api.dart`
and `add_person_api.dart`. It is the only mobile package the shell actually imports.

## Data

<!-- [coverage: high -- no persistence dependency; models are transport DTOs] -->

**Owns** — `_none_`. **Reads** — `_none_`.

The 74 models are wire shapes, not stored rows. The generated client has no persistence dependency;
nothing here reaches SQLite.

## Gotchas

<!-- [coverage: medium -- read off .openapi-generator-ignore and melos.yaml; the drift gate itself is a backend test not read here] -->

- **`pubspec.yaml` and `test/**` are hand-stabilized**, listed in `.openapi-generator-ignore` so
  regeneration does not clobber the workspace SDK and `http` constraints or ship generator skeleton
  tests into the melos workspace. Everything else in the package is disposable.
- **Nothing here is tested.** No `test/` directory, so `melos run test` skips it. The guarantee comes
  from the backend's generate-and-diff gate (issue #73), not from mobile CI.
- The generated `ApiException` is a different type from the shell's error contract. Every feature
  client that uses this package bridges one to the other by hand — see `selection_api.dart`, which
  reconstructs an `http.Response` from an `ApiException` so the shared `apiError` seam still applies.

## Covered by

<!-- [coverage: high -- derived: both dossiers below link this page; the two that don't are named too] -->

- [attendance-marking](../features/attendance-marking.md) — `AttendanceRestControllerApi` on the Walk-in path.
- [person-directory](../features/person-directory.md) — `PersonDirectoryRestControllerApi`, which the mobile lookup calls whole.

[authentication](../features/authentication.md) and [home-sabha-transfer](../features/home-sabha-transfer.md) route around this package on purpose: both
mobile clients are hand-rolled `http`.

## Method

- `.openapi-generator-ignore` and `apps/mobile/melos.yaml` — the manifest rung, and the highest-yield source on this page: everything under `lib/` is generated, so the generator configuration says more than the output does.
- The generated sources were listed, not read: reading generated Dart would describe the spec at one remove.
