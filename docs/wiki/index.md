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
| How does authorization work? | [Patterns](#patterns) |
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
| [mobile-attendance-domain](structure/mobile-attendance-domain.md) | `apps/mobile/packages/attendance_domain` | mobile Attendance Marking, Walk-in, offline queue |
| [mobile-identity-domain](structure/mobile-identity-domain.md) | `apps/mobile/packages/identity_domain` | mobile User/Session types, Karyakar |
| [mobile-sabha-api](structure/mobile-sabha-api.md) | `apps/mobile/packages/sabha_api` | the generated Dart client, the typed API client, the OpenAPI client |
| [mobile-sabha-domain](structure/mobile-sabha-domain.md) | `apps/mobile/packages/sabha_domain` | mobile Sabha/Occurrence/Roster types |
| [mobile-shared-kernel](structure/mobile-shared-kernel.md) | `apps/mobile/packages/shared_kernel` | the mobile shared kernel |
| [mobile-shell](structure/mobile-shell.md) | `apps/mobile/sabha_attendance` | the Flutter app, the Sanchalak's phone, hajri app, the offline queue |
| [web](structure/web.md) | `apps/web` | the Angular admin panel, the web console, the BFF client, the sections |

## Features

One page per durable capability. An issue amends a dossier; it never adds one.

| Page | Description | Also known as |
|---|---|---|
| [attendance-marking](features/attendance-marking.md) | Recording who attended a Sabha Occurrence, online or offline. | hajri, Roster marking, Walk-in, sync |

## Patterns

Recurring patterns that reconcile ADR clusters — the interlink surface over the hand-written docs.

| Page | Description | Also known as |
|---|---|---|

_none_

## Notes

Session learnings. Never compiler-written; see `docs/agents/wiki.md` for when one is admitted.

| Page | Description | Also known as |
|---|---|---|

_none_
