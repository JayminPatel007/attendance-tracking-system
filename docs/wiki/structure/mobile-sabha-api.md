---
kind: structure
slug: mobile-sabha-api
source_paths: [apps/mobile/packages/sabha_api/**]
decisions: [ADR-0014, ADR-0015]
issues: [73]
last_compiled: 09fb2075173eb4fc030ce2c26e85311aa26f064a
---
# Mobile — `sabha_api`

## Purpose

<!-- [coverage: high] -->

The typed Dart client, **generated** from the backend's committed OpenAPI spec (issue #73). It is
the mobile app's entire model layer in practice: the request/response types the app uses come from
here, not from the hand-written `*_domain` mirror packages.

## Layout

<!-- [coverage: high] -->

103 Dart files under `lib/`, in three generated directories:

| Directory | Contents |
|---|---|
| `api/` | One class per tagged API group. |
| `model/` | The generated DTOs. |
| `auth/` | Generated auth helpers (OAuth/bearer plumbing). |

No ring split — the openapi-generator's layout, not this repo's. Dependencies are `http`,
`collection`, `intl`, `meta`; no Flutter import, so it is pure Dart.

## Exposes

<!-- [coverage: high] -->

_none_

A client library, not a server. It calls `/api/*`; it serves nothing.

## Talks To

<!-- [coverage: medium -- the generation edge is exact (melos script + openapitools.json); which backend contexts appear in the generated surface follows from the spec covering all `/api/*` routes, which was not enumerated file by file. ] -->

**Outbound** — HTTP against the backend's `/api/*` surface, so effectively every context that
exposes one: [[backend-identity]] and [[backend-attendance]].

**Inbound** — [[mobile-app]] is its only consumer.

Its true upstream is not a package but a **file**: `apps/backend/openapi.json`. The generator is
pinned in `apps/mobile/openapitools.json` and invoked by `melos run generate:api`.

## Data

<!-- [coverage: high] -->

_none_

Stateless client code. The offline store belongs to [[mobile-app]].

## Gotchas

<!-- [coverage: medium -- the regeneration workflow is stated in melos.yaml; the consequence of hand-editing is the standard generated-code inference, not something this repo documents. ] -->

**Generated — do not hand-edit.** Regenerate with `melos run generate:api`, then `melos bootstrap`.
`packages/sabha_api/.openapi-generator-ignore` protects the pubspec and the skeleton tests from
being overwritten; anything else you change is lost on the next run.

Because the source of truth is the *committed* `openapi.json`, this package can be current with the
spec and stale against the backend at the same time. The backend's drift gate is what keeps the spec
honest — see [[backend-container]].

## Covered by

<!-- [coverage: low -- no feature dossiers exist yet; structurally empty until the first one is admitted. ] -->

_none_

## Sources

- [sabha_api](../../../apps/mobile/packages/sabha_api) — `pubspec.yaml` and generated `lib/` layout
- [melos.yaml](../../../apps/mobile/melos.yaml) — the `generate:api` script
- [openapitools.json](../../../apps/mobile/openapitools.json) — pinned generator version
