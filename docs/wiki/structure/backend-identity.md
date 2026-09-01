---
type: structure
title: Identity Service
description: Owns who a person is, what authority they hold, and how they prove it — persons, role assignments, users, OTP and password reset.
resource: apps/backend/identity-service
aliases: [People, Users, roles, Karyakar, Person, Directory]
tags: [bff]
source_paths: [
  apps/backend/identity-service/*/src/main/**,
  apps/backend/identity-service/*/pom.xml,
  apps/backend/identity-service/pom.xml,
  docs/adr/0015-*.md,
  docs/adr/0017-*.md,
  docs/adr/0018-*.md,
  docs/adr/0019-*.md,
  docs/adr/0029-*.md,
  CONTEXT.md
]
sources:
  - { id: adr-0015, title: "Bounded-Context Seams Are Build Modules (DDD + Hexagonal + Clean)", resource: ../../adr/0015-bounded-context-seams-as-build-modules.md }
  - { id: adr-0017, title: "REST adapters live in `*-application` modules", resource: ../../adr/0017-rest-adapters-live-in-application-modules.md }
  - { id: adr-0018, title: "Application services split: `*-application` vs `*-application-service`", resource: ../../adr/0018-application-service-split.md }
  - { id: adr-0019, title: "Bounded-context module taxonomy: five modules per context, presentation split from application service", resource: ../../adr/0019-bounded-context-module-taxonomy.md }
  - { id: adr-0029, title: "`role_assignments` is identity-owned: read-models may join it, authority checks go through ports", resource: ../../adr/0029-role-assignments-access-rule.md }
  - { id: context, title: "CONTEXT.md — Nirdeshak, Sanchalak, Nirikshak, Kshetra", resource: ../../../CONTEXT.md }
last_compiled: 09fb2075173eb4fc030ce2c26e85311aa26f064a
---

# Identity Service

## Purpose

<!-- [coverage: high -- 5 package-info.java, ADR-0029] -->

Owns **who a person is** (`persons`), **what authority they hold** (`role_assignments`), and
**how they prove it** (`users`, OTP, password reset). It is also where an authority *check*
lives even when the thing being created belongs elsewhere — Sabha definition and Selection are
served from here because the decision "may this caller do it?" is identity's, per ADR-0029.

## Layout

<!-- [coverage: high -- directory listing + 5 package-info.java] -->

The standard five-module ring, identical across all four contexts by ADR-0019.

| Module | Main files | Holds |
|---|---|---|
| `identity-domain/identity-domain-core` | 48 | `User`, `Person`, `OtpChallenge`, `PasswordReset`, `SelectionNomination`, `HomeSabhaTransfer`, plus their domain events and exceptions. Pure — only common-domain allowed. |
| `identity-domain/identity-application-service` | 93 | Nine feature packages in domain vocabulary: `appointment`, `directory`, `selection`, `passwordreset`, `otp`, `transfer`, `sabhadefinition`, `bootstrap`, `session`. Cross-cutting driven ports (`UserRepository`, `IdentityProviderGateway`) stay at the package root. |
| `identity-data-access` | 25 | 21 `Jdbc*` adapters. |
| `identity-application` | 14 | 11 REST controllers (ADR-0017). |
| `identity-messaging` | 7 | `KeycloakAdminRestClient` (ADR-0016), OTP gateway, `HmacOtpHasher`, token generators. |

**Feature packages**: `appointment`, `directory`, `selection`, `passwordreset`, `otp`, `transfer`,
`sabhadefinition`, `bootstrap`, `session` — the useful unit of navigation inside this unit, since
the ring is identical across all four contexts. This is the largest backend unit at ~210 Java
files including tests, roughly the other three contexts combined.

## Exposes

<!-- [coverage: high -- mapping-annotation grep over identity-application] -->

11 controllers. `/api/*` is mobile-facing; `/bff/*` is the web BFF (ADR-0022). Both live side by
side in `identity-application`. Route prefixes only — individual endpoints belong to the
`features/` dossier that owns the capability.

| Prefix | Serves | Controllers |
|---|---|---|
| `/api/directory/*` | mobile | `PersonDirectoryRestController` |
| `/api/password-reset/*` | mobile | `PasswordResetRestController` |
| `/api/home-sabha-transfers/*` | mobile | `HomeSabhaTransferRestController` |
| `/api/sanchalak/nominations` | mobile | `SelectionRestController` |
| `/api/whoami`, `/api/who-appointed-me` | mobile | `IdentityRestController`, `WhoAppointedMeRestController` |
| `/bff/me` | web | `BffSessionController` |
| `/bff/appointments/*` | web | `RoleAppointmentController` |
| `/bff/selection/*` | web | `SelectionBffController` |
| `/bff/directory/*` | web | `DirectoryBffController` |
| `/bff/sabhas`, `/bff/password-reissue` | web | `SabhaDefinitionController`, `PasswordReissueController` |

`/bff/sabhas` is **split across two contexts**: `POST` is identity's (`SabhaDefinitionController`),
while `GET /bff/sabhas/mine` and `DELETE /bff/sabhas/{id}` are [backend-sabha](backend-sabha.md)'s. The prefix alone
does not tell you the owner.

## Talks To

<!-- [coverage: high -- import scan of all four modules] -->

Every cross-context reference goes through `org.sabha.common`. There are **zero** direct imports
of another context's packages from anywhere in this unit.

**Outbound** — ports identity consumes, both implemented in `sabha-data-access`:

| Port | Target | Used by |
|---|---|---|
| `StructuralHierarchyLookup` | sabha | `AppointmentAuthorization`, `SelectionService`, `WebSessionService`, `AddPersonApplicationService` |
| `SabhaProvisioning` | sabha | `SabhaDefinitionService` |

**Inbound** — nine common-domain ports identity *implements* for the other three contexts:
`CallerResolver`, `RoleAssignmentLookup`, `SantLookup`, `MadhyasthaKaryalayaLookup`,
`SanyojakZoneLookup`, `NirdeshakScopeLookup`, `NirikshakAssignmentLookup`,
`RegionalTeamCityLookup`, `UserActivityRecorder`. This is the most-depended-on unit in the
backend — see the port table on [backend-common-domain](backend-common-domain.md) for the whole wiring diagram.

## Data

<!-- [coverage: medium -- writer grep over every Jdbc adapter in all four contexts; ownership is inferred from who writes, since no ownership manifest exists] -->

Migrations are **central**, in `apps/backend/application-container/src/main/resources/db/changelog`,
not per context — so ownership is not derivable from the schema, and that directory is outside this
page's `source_paths`. The rule that reaches it is the table-name grep in `protocol.md` §8.

**Owned** (identity is the only writer): `users`, `persons`, `role_assignments`, `home_sabhas`,
`home_sabha_transfers`, `password_resets`, `selection_nominations`, `user_activity`.

`home_sabhas` is identity's, not sabha's — its only writers are `JdbcPersonDirectory` and
`JdbcSelectionRoster`, both in `identity-data-access`. Sabha owns the *Sabha*; identity owns the
*person-to-Sabha membership*.

**Written by someone else**: `users.default_city_id` is updated by analytics'
`JdbcSantDefaultCity`. `users` is therefore the one identity table with a second writer.

**Read but not owned**: `sabhas`, `sabha_kinds`, `kshetras` (sabha).

`nirikshak_sabha_assignments` has **no production writer** anywhere in the backend — identity reads
it via `JdbcNirikshakAssignmentLookup`, attendance reads it for the proxy check, and the only rows
come from the `slice-14/002-seed.sql` seed. Proxy assignment is not yet a use case.

`users`, `persons` and `role_assignments` are each read directly by other contexts' data-access
modules. That is legal read-model latitude under ADR-0029, not a leak.

## Gotchas

<!-- [coverage: low -- one verified module-local gotcha; no systematic sweep of the other 200 files] -->

- `identity-application/package-info.java` says its ports live in **`identity-infrastructure`**.
  There is no such module — it is `identity-data-access` + `identity-messaging`. Stale comment.

The `@Transactional`-only-in-`*-application-service` rule (ADR-0018, enforced by
`IntraModuleArchitectureRulesTest`) applies to all four contexts, so per the ownership rule it
belongs in `notes/`, not here.

## Covered by

<!-- [coverage: low -- one dossier exists so far; the other eight are candidates only] -->

- [attendance-marking](../features/attendance-marking.md) — for `CallerResolver`, `persons` and the walk-in directory search.

Expected further pages, one per feature package: role-appointment, person-directory, selection,
password-reset, home-sabha-transfer, sabha-definition, mk-bootstrap, web-session.

## Method

- Ring-module class listings, a mapping-annotation grep for `Exposes`, and a writer-SQL grep across every `Jdbc*` adapter for `Data` → Owns.
- `identity-service/**/package-info.java` — five substantive files, the highest-yield source here; nine document a feature package in domain vocabulary.
