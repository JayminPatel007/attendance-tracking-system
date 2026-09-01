---
okf_version: "0.2"
---
# Attendance Tracking System — Wiki

The front door to this codebase's knowledge. Read `docs/agents/wiki.md` for the reading contract
(what each type means, how to tell whether a page is still true) and `docs/wiki/protocol.md` for the
page contract.

**The wiki is derived. On conflict, `CONTEXT.md` and `docs/adr/` win.**

## Start here

The eight questions are fixed in `protocol.md`; the compiler fills only the target cell.

| I need to know… | Go to |
|---|---|
| What is this system, in domain terms? | [CONTEXT.md](../../CONTEXT.md) |
| How do I run it locally? | [dev-setup](../dev-setup.md) |
| Which app/module owns X? | [Structure](#structure) |
| How does capability Y work end to end? | [Features](#features) |
| How does authorization work? | [authorization](patterns/authorization.md) |
| Why is the backend shaped like this? | [backend-common-domain](structure/backend-common-domain.md) |
| What must I not break? | [Structure](#structure) |
| What did we learn the hard way? | [Notes](#notes) |

Row 6 points at [backend-common-domain](structure/backend-common-domain.md) because the shape of the
backend *is* its cross-context port table — the one page that shows all four contexts at once.

## Structure

One page per build unit — 6 backend (4 bounded contexts, `common-domain`, `application-container`),
6 mobile (5 Dart packages plus the app shell), and web as one.

| Page | Unit | Also known as |
|---|---|---|
| [backend-analytics](structure/backend-analytics.md) | `apps/backend/analytics-service` | dashboards, audit log, re-engagement, Nirdeshak/Sant reporting |
| [backend-attendance](structure/backend-attendance.md) | `apps/backend/attendance-service` | Occurrences and markings, Sabha Occurrence, Roster, Walk-in |
| [backend-common-domain](structure/backend-common-domain.md) | `apps/backend/common-domain` | the shared kernel, the ports module |
| [backend-container](structure/backend-container.md) | `apps/backend/application-container` | the Spring Boot app, the composition root, the schema |
| [backend-identity](structure/backend-identity.md) | `apps/backend/identity-service` | People, Users, roles, Karyakar, Person, Directory |
| [backend-sabha](structure/backend-sabha.md) | `apps/backend/sabha-service` | the structural hierarchy, Kshetra, Zone, City, Sabha Kind |
| [mobile-attendance-domain](structure/mobile-attendance-domain.md) (scaffold) | `apps/mobile/packages/attendance_domain` | mobile Attendance Marking, Walk-in, offline queue |
| [mobile-identity-domain](structure/mobile-identity-domain.md) (scaffold) | `apps/mobile/packages/identity_domain` | mobile User/Session types, Karyakar |
| [mobile-sabha-api](structure/mobile-sabha-api.md) | `apps/mobile/packages/sabha_api` | the generated Dart client, the typed API client, the OpenAPI client |
| [mobile-sabha-domain](structure/mobile-sabha-domain.md) (scaffold) | `apps/mobile/packages/sabha_domain` | mobile Sabha/Occurrence/Roster types |
| [mobile-shared-kernel](structure/mobile-shared-kernel.md) (scaffold) | `apps/mobile/packages/shared_kernel` | the mobile shared kernel |
| [mobile-shell](structure/mobile-shell.md) | `apps/mobile/sabha_attendance` | the Flutter app, the Sanchalak's phone, hajri app, the offline queue |
| [web](structure/web.md) | `apps/web` | the Angular admin panel, the web console, the BFF client, the sections |

## Features

One page per durable capability. An issue amends a dossier; it never adds one.

| Page | Description | Also known as |
|---|---|---|
| [attendance-marking](features/attendance-marking.md) | Recording who attended a Sabha Occurrence, online or offline. | hajri, Roster marking, Walk-in, sync |
| [authentication](features/authentication.md) | Signing in to the mobile and web apps, and resetting a forgotten password. | login, sign-in, OIDC, Keycloak, session, password reset, forgot password, OTP, who appointed me, reissue |
| [home-sabha-transfer](features/home-sabha-transfer.md) | Moving a Person's Home Sabha into another Sabha, confirmed by an OTP to the Person themselves. | Verified Home Sabha Transfer, HSAT, transfer, Roster move, Sanchalak pull, OTP consent |
| [occurrence-lifecycle](features/occurrence-lifecycle.md) | How a Sabha Occurrence comes into being, opens, finalizes, and is shaped or reopened along the way. | Sabha Occurrence, occurrence states, auto-open, auto-finalize, materialization, cancel, revert, reschedule, venue override, reopen, Effective Slot, monthly ad-hoc |
| [person-directory](features/person-directory.md) | Finding a Person in the central Directory, and adding one under the two-signal de-duplication rule. | Directory, add person, de-duplication, dedup, mobile lookup, person picker, Karyakar lookup |
| [role-appointment](features/role-appointment.md) | Appointing a Karyakar into a role at a scope, and revoking that role again. | appointment, Karyakar appointment, Nirdeshak, Sanchalak, Sanyojak, Regional Team, revocation, my authority, who may appoint |
| [sabha-definition](features/sabha-definition.md) | Defining a Sabha and its Sanchalak in one act, and registering or retiring the Sabha Kind it is an instance of. | define a Sabha, Sabha Kind, Sabha Type, schedule shape, weekly recurring, monthly ad-hoc, soft-retire, retired kind, standing venue |
| [sanchalak-proxy](features/sanchalak-proxy.md) | A Nirikshak exercising the Sanchalak's toolkit on an assigned Sabha when the Sanchalak is unavailable. | proxy mode, acting as Sanchalak, Nirikshak, on behalf of, last seen, assigned Sabhas, stand-in |
| [structural-hierarchy](features/structural-hierarchy.md) | Creating and deleting the City, Zone and Kshetra chain the whole organisation hangs off. | structural admin, geography, City, Zone, Kshetra, Sanyojak, Regional Team, Madhyastha Karyalaya, block-if-non-empty, tier above |

## Patterns

Recurring patterns that reconcile ADR clusters — the interlink surface over the hand-written docs.

| Page | Description | Also known as |
|---|---|---|
| [authorization](patterns/authorization.md) | How every authority decision is made — one stateless engine per context, resolving current scope through identity-owned ports. | authz, permissions, Authorization Engine, who may, scope, RoleAssignmentLookup |
| [module-ring](patterns/module-ring.md) | The five-module Clean-Architecture ring every backend bounded context is built from, and what the Maven graph enforces. | ring, the hexagon, five modules, Clean Architecture, domain-core, application-service, data-access |

## Notes

Session learnings. Never compiler-written; see `docs/agents/wiki.md` for when one is admitted.

| Page | Description | Also known as |
|---|---|---|

_none_
