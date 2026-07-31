# attendance-tracking-system

## Agent skills

### Wiki

The front door to this codebase's knowledge is `docs/wiki/index.md` — an agent-compiled wiki over
`CONTEXT.md` and the ADRs. It is **derived**: on conflict, those win. Check a page's currency before
trusting its specifics, and write session learnings back at PR-open. See `docs/agents/wiki.md`.

### Issue tracker

Issues live as GitHub issues in `JayminPatel007/attendance-tracking-system`, via the `gh` CLI.
External pull requests are **not** a triage surface. See `docs/agents/issue-tracker.md`.

### Triage labels

Canonical vocabulary — `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`,
`wontfix` — used verbatim. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `CONTEXT.md` and one `docs/adr/` at the repo root, shared by all three
apps. See `docs/agents/domain.md`.
