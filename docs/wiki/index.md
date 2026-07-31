---
kind: index
pages: 13
---
# Attendance Tracking System — Wiki

The front door to this codebase's knowledge. Read `docs/agents/wiki.md` for the reading contract
(what each kind means, how to tell whether a page is still true) and `docs/wiki/protocol.md` for the
page contract.

**The wiki is derived. On conflict, `CONTEXT.md` and `docs/adr/` win.**

The first sweep compiled all 13 `structure` pages. No `feature`, `concept` or `note` pages exist
yet, so router rows 5–8 still point at their (empty) kind catalogs.

## Start here

The eight questions are fixed in `protocol.md`; the compiler fills only the target cell.

| I need to know… | Go to |
|---|---|
| What is this system, in domain terms? | [CONTEXT.md](../../CONTEXT.md) |
| How do I run it locally? | [dev-setup](../dev-setup.md) |
| Which app/module owns X? | [Structure](#structure) |
| How does capability Y work end to end? | [Features](#features) |
| How does authorization work? | [Concepts](#concepts) |
| Why is the backend shaped like this? | [Concepts](#concepts) |
| What must I not break? | [Notes](#notes) |
| What did we learn the hard way? | [Notes](#notes) |

## Structure

One page per build unit — 6 backend (4 bounded contexts, `common-domain`, `application-container`),
6 mobile (5 Dart packages plus the app shell), and web as one.

| Page | Unit | Also known as |
|---|---|---|
| [[backend-common-domain]] | `apps/backend/common-domain` | shared kernel, cross-context ports |
| [[backend-identity]] | `apps/backend/identity-service` | users, Karyakar, Person Directory, role appointment |
| [[backend-sabha]] | `apps/backend/sabha-service` | structure, City/Zone/Kshetra, Sabha Kind |
| [[backend-attendance]] | `apps/backend/attendance-service` | Occurrence, marking, roster, offline sync |
| [[backend-analytics]] | `apps/backend/analytics-service` | dashboards, re-engagement, audit log |
| [[backend-container]] | `apps/backend/application-container` | Spring Boot app, migrations, cron, security |
| [[mobile-app]] | `apps/mobile/sabha_attendance` | the Flutter app, Sanchalak app |
| [[mobile-sabha-api]] | `apps/mobile/packages/sabha_api` | generated Dart client |
| [[mobile-shared-kernel]] | `apps/mobile/packages/shared_kernel` | mobile shared kernel (scaffold) |
| [[mobile-identity-domain]] | `apps/mobile/packages/identity_domain` | mobile identity mirror (scaffold) |
| [[mobile-sabha-domain]] | `apps/mobile/packages/sabha_domain` | mobile sabha mirror (scaffold) |
| [[mobile-attendance-domain]] | `apps/mobile/packages/attendance_domain` | mobile attendance mirror (scaffold) |
| [[web]] | `apps/web` | the Angular app, oversight UI, BFF client |

## Features

One page per durable capability. An issue amends a dossier; it never adds one.

| Page | Capability | Also known as |
|---|---|---|

_none_

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
