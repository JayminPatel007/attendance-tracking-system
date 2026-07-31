---
kind: index
pages: 0
---
# Attendance Tracking System — Wiki

The front door to this codebase's knowledge. Read `docs/agents/wiki.md` for the reading contract
(what each kind means, how to tell whether a page is still true) and `docs/wiki/protocol.md` for the
page contract.

**The wiki is derived. On conflict, `CONTEXT.md` and `docs/adr/` win.**

No pages have been compiled yet — the catalogs below are empty and rows 3–8 of the router therefore
point at empty sections. Run the `wiki-sweep` skill to compile the first pages.

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

_none_

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
