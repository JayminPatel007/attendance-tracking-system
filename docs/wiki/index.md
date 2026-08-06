---
kind: index
pages: 14
---
# Attendance Tracking System — Wiki

The front door to this codebase's knowledge. Read `docs/agents/wiki.md` for the reading contract
(what each kind means, how to tell whether a page is still true) and `docs/wiki/protocol.md` for the
page contract.

**The wiki is derived. On conflict, `CONTEXT.md` and `docs/adr/` win.**

All thirteen `structure/` pages are compiled — backend, web and mobile — plus one feature dossier.
`concepts/` and `notes/` are still empty.

## Start here

The eight questions are fixed in `protocol.md`; the compiler fills only the target cell.

| I need to know… | Go to |
|---|---|
| What is this system, in domain terms? | [CONTEXT.md](../../CONTEXT.md) |
| How do I run it locally? | [dev-setup](../dev-setup.md) |
| Which app/module owns X? | [Structure](#structure) |
| How does capability Y work end to end? | [Features](#features) |
| How does authorization work? | [Concepts](#concepts) |
| Why is the backend shaped like this? | [[backend-common-domain]] |
| What must I not break? | [Structure](#structure) |
| What did we learn the hard way? | [Notes](#notes) |

Row 6 points at [[backend-common-domain]] because the shape of the backend *is* its cross-context
port table — the one page that shows all four contexts at once. Rows 5 and 7 still point at their
kind's catalog: no `concepts/` page exists yet, and "what must I not break" is answered today by the
`Gotchas` section of each structure page rather than by a `notes/` page.

## Structure

One page per build unit — 6 backend (4 bounded contexts, `common-domain`, `application-container`),
6 mobile (5 Dart packages plus the app shell), and web as one.

| Page | Unit | Also known as |
|---|---|---|
| [[backend-common-domain]] | `apps/backend/common-domain` | the shared kernel; the ports module |
| [[backend-identity]] | `apps/backend/identity-service` | People, Users, roles; Karyakar, Person, Directory |
| [[backend-sabha]] | `apps/backend/sabha-service` | the structural hierarchy; Kshetra, Zone, City, Sabha Kind |
| [[backend-attendance]] | `apps/backend/attendance-service` | Occurrences and markings; Sabha Occurrence, Roster, Walk-in |
| [[backend-analytics]] | `apps/backend/analytics-service` | dashboards, audit log, re-engagement; Nirdeshak/Sant reporting |
| [[backend-container]] | `apps/backend/application-container` | the Spring Boot app; the composition root, the schema |
| [[web]] | `apps/web` | the Angular admin panel; the web console, the BFF client, the sections |
| [[mobile-shell]] | `apps/mobile/sabha_attendance` | the Flutter app; the Sanchalak's phone, hajri app, the offline queue |
| [[mobile-sabha-api]] | `apps/mobile/packages/sabha_api` | the generated Dart client; the typed API client, the OpenAPI client |
| [[mobile-shared-kernel]] | `apps/mobile/packages/shared_kernel` | the mobile shared kernel — **scaffold, no types yet** |
| [[mobile-identity-domain]] | `apps/mobile/packages/identity_domain` | mobile User/Session types; Karyakar — **scaffold, no types yet** |
| [[mobile-sabha-domain]] | `apps/mobile/packages/sabha_domain` | mobile Sabha/Occurrence/Roster types — **scaffold, no types yet** |
| [[mobile-attendance-domain]] | `apps/mobile/packages/attendance_domain` | mobile Attendance Marking, Walk-in, offline queue — **scaffold, no types yet** |

## Features

One page per durable capability. An issue amends a dossier; it never adds one.

| Page | Capability | Also known as |
|---|---|---|
| [[attendance-marking]] | Recording who attended a Sabha Occurrence, online or offline | hajri; Roster marking, Walk-in, sync |

## Concepts

Recurring patterns that reconcile ADR clusters — the interlink surface over the hand-written docs.

| Page | Pattern | Also known as |
|---|---|---|

_none_

## Notes

Session learnings. Never compiler-written; see `docs/agents/wiki.md` for when one is admitted.

| Page | Theme | Also known as |
|---|---|---|

_none_
