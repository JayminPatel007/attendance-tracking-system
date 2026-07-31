# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

## Before exploring, read these

- **`docs/wiki/index.md`** — the front door: which module owns what, how a capability works end to
  end, and what we learned the hard way. Read it first when you don't yet know the terms. It is
  **derived**, so on conflict the two below win. See `docs/agents/wiki.md`.
- **`CONTEXT.md`** at the repo root — the glossary and domain narrative. Canonical.
- **`docs/adr/`** — read ADRs that touch the area you're about to work in. Canonical.

There is no `CONTEXT-MAP.md`: this repo is **single-context**. Don't go looking for per-app
`CONTEXT.md` files or per-module ADR directories; they don't exist.

If any of these files don't exist, **proceed silently**. Don't flag their absence; don't suggest creating them upfront. The `/domain-modeling` skill (reached via `/grill-with-docs` and `/improve-codebase-architecture`) creates them lazily when terms or decisions actually get resolved.

## File structure

```
/
├── CONTEXT.md                         ← the one glossary, covers all three apps
├── docs/
│   ├── wiki/                          ← the compiled front door; index.md, protocol.md, 4 kind dirs
│   ├── adr/                           ← 0001…0029, system-wide decisions
│   ├── prd/
│   └── dev-setup.md
└── apps/
    ├── backend/
    ├── mobile/
    └── web/
```

The `apps/` split is a **delivery boundary, not a domain boundary** — per ADR-0008 (single bounded
context with internal seams) and ADR-0015 (bounded-context seams as build modules), the backend's
internal module structure carries the seams, and all three apps share one ubiquitous language.
So a domain term means the same thing in `apps/mobile` as it does in `apps/backend`.

## Use the glossary's vocabulary

When your output names a domain concept (in an issue title, a refactor proposal, a hypothesis, a test name), use the term as defined in `CONTEXT.md`. Don't drift to synonyms the glossary explicitly avoids.

If the concept you need isn't in the glossary yet, that's a signal — either you're inventing language the project doesn't use (reconsider) or there's a real gap (note it for `/domain-modeling`).

## Flag ADR conflicts

If your output contradicts an existing ADR, surface it explicitly rather than silently overriding:

> _Contradicts ADR-0008 (single bounded context with internal seams) — but worth reopening because…_

Note that later ADRs supersede earlier ones (e.g. ADR-0016 supersedes ADR-0004's auth shape, and
ADR-0024 moves Zone creation off ADR-0009's original authority). Check for a superseding record
before treating an early ADR as current.
