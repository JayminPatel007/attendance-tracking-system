---
kind: structure
slug: backend-identity
source_paths: [apps/backend/identity-service/**]
decisions: [ADR-0015, ADR-0017, ADR-0018, ADR-0019, ADR-0029]
status: draft
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

The standard five-module ring; see [[module-ring]] for what each ring means.

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
`RegionalTeamCityLookup`, `UserActivityRecorder`.

## Data

<!-- [coverage: low -- adapter SQL only; no per-context schema and no ownership manifest exists] -->

Migrations are **central**, in `apps/backend/application-container/src/main/resources/db/changelog`,
not per context — so ownership is not derivable from the schema. That directory is outside this
page's `source_paths`, so this section is knowingly under-invalidated pending issue #147.

Owned (identity is the only writer): `users`, `persons`, `role_assignments`,
`home_sabha_transfers`, `password_resets`, `selection_nominations`, `user_activity`.

Read but not owned: `sabhas`, `home_sabhas` (sabha).

`nirikshak_sabha_assignments` has **no production writer** — identity reads it via
`JdbcNirikshakAssignmentLookup`, attendance reads it, and the only rows come from the
`slice-14/002-seed.sql` seed. Proxy assignment is not yet a use case anywhere.

Note that `users`, `persons` and `role_assignments` are each read directly by three other
contexts' data-access modules. That is legal read-model latitude under ADR-0029, not a leak.

## Gotchas

<!-- [coverage: low -- one verified module-local gotcha; see note below] -->

- `identity-application/package-info.java` says its ports live in **`identity-infrastructure`**.
  There is no such module — it is `identity-data-access` + `identity-messaging`. Stale comment.

The `@Transactional`-only-in-`*-application-service` rule (ADR-0018, enforced by
`IntraModuleArchitectureRulesTest`) applies to all four contexts, so per the ownership rule it
belongs in `notes/`, not here.

## Covered by

<!-- [coverage: low -- no feature pages exist yet] -->

`_none_` — no `features/` pages written. Expected slugs from the nine packages:
role-appointment, person-directory, selection, password-reset, home-sabha-transfer,
sabha-definition, mk-bootstrap, web-session.

## Sources

- [ADR-0015](../../adr/0015-bounded-context-seams-as-build-modules.md), [ADR-0017](../../adr/0017-rest-adapters-live-in-application-modules.md), [ADR-0018](../../adr/0018-application-service-split.md), [ADR-0019](../../adr/0019-bounded-context-module-taxonomy.md), [ADR-0029](../../adr/0029-role-assignments-access-rule.md)
- [CONTEXT.md](../../../CONTEXT.md) — Nirdeshak, Sanchalak, Nirikshak, Kshetra
- `apps/backend/identity-service/**/package-info.java` — five files, the highest-yield source on this page
