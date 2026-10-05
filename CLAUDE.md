# attendance-tracking-system

## Agent skills

### Wiki

**Before you `grep` or `find` for where something lives** — which module owns a class, port, table
or route, or how a capability works end to end — open `docs/wiki/index.md` and follow it to the one
`structure/` or `features/` page. Use that page as a **map** to the right files and governing ADRs,
and confirm any specific you act on in the source: the wiki is **derived**, and on conflict
`CONTEXT.md` and the ADRs win.

**At PR-open**, write session learnings back. Reading and write-back rules: `docs/agents/wiki.md`.

### Issue tracker

Issues live as GitHub issues in `JayminPatel007/attendance-tracking-system`, via the `gh` CLI.
External pull requests are **not** a triage surface. See `docs/agents/issue-tracker.md`.

### Triage labels

Canonical vocabulary — `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`,
`wontfix` — used verbatim. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `CONTEXT.md` and one `docs/adr/` at the repo root, shared by all three
apps. See `docs/agents/domain.md`.
