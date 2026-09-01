# Wiki

How to **read** the in-repo wiki, and how to **write back** what a session learned the hard way.

Enter at **`docs/wiki/index.md`**. Its `Start here` router is eight fixed questions over a catalog
of every page; three of them point straight back out to `CONTEXT.md` and `docs/dev-setup.md`.

The wiki is an **OKF v0.2 knowledge bundle** (`okf_version` is declared at the front door). You do
not need to know that to read it — everything below is the whole reading contract — but it is why
the frontmatter looks the way it does.

If `docs/wiki/` doesn't exist, **proceed silently** — same rule as the rest of `docs/agents/`.

## The four types

| `type` | Answers | Written by |
|---|---|---|
| `structure/` | what lives where, and what does it talk to | the compiler |
| `features/` | how a capability works end to end | the compiler |
| `patterns/` | the pattern behind a cluster of ADRs | the compiler |
| `notes/` | what we learned the hard way | **you**, in a feature PR |

**The wiki is derived. On conflict, `CONTEXT.md` and `docs/adr/` win** — they are canonical and
immutable, and the wiki cites them rather than restating them. Glossary terms and ADR numbers appear
**bare** in wiki prose; the resolvable links live in each page's `sources[]` frontmatter, one entry
per document, keyed `adr-0011` / `context`.

A page's `## Method` section is **not** a citation list. It names *the source that paid and why* —
read it if you are about to verify or recompile something, and skip it otherwise.

`index.md` marks two facts inline, after the page link: **`(deprecated)`** — the unit this page
describes is gone, so the page makes no currency claim — and **`(scaffold)`** — the unit is declared
and buildable but holds no types yet, so the page describes an empty package rather than a thin one.
Both are derived from frontmatter and lint-checked, so neither can lie to you.

The full page contract is `docs/wiki/protocol.md`. You only need it if you are compiling.

## Before trusting a page

```sh
git diff --quiet <last_compiled>..HEAD -- <the page's source_paths>
```

Non-zero exit → the page is **stale**. Staleness is computed, never stored, so no marker can lie to
you. `notes/` use `last_verified` for the same check.

**Unless the page says `status: deprecated`** — then the subject it describes has been deleted, and
the check above is meaningless (the globs match nothing, so the diff comes back clean forever). A
deprecated page makes **no currency claim at all**. Its `deprecated_reason` says what went and when.
Links into it still resolve, which is the whole reason the page is still here; treat everything in it
as history.

**A stale page demotes from fact to map.** Still usable: which unit owns this, which ADRs govern it,
which pages to follow, what the gotchas were. **No longer repeatable to a user without checking the
source:** table names, file counts, port lists, route prefixes — any specific claim.

Also read the per-section `[coverage: …]` tag, which answers a *different* question — how
well-sourced the page was when written. A page can be perfectly current and still confidently wrong.

- `high` — trust it, don't open the source.
- `medium` — trust the shape, verify any specific you'll act on.
- `low` — this is a lead, read the source.

## You may mark, but never author

Found a page wrong? Set `status: disputed` and a one-line `disputed_reason` in its frontmatter.
One flag, one line — **never page prose**. Without this, every reader rediscovers the same error and
throws it away; verification has to compound somewhere.

Compiled pages are cleared only by the compiler. On `notes/` only, you may also clear or delete.

**Found a page right? Say so, in the tag you just acted on.** This is the other half, and it is
cheap on purpose. You opened the source *because* a section read `medium` — if the claim held up,
raise that section's tag and record the check in the reason slot:

```
<!-- [coverage: high -- verified 2026-08-31 by human:jaymin against SabhaJdbcAdapter; compiled medium] -->
```

Verification raises the level because levels are defined by **reader obligation**: once you have
checked it against source, *"trust it, don't open the source"* is simply true. One line, in a file
you already have open, at the same PR-open moment as everything else below.

This asks nothing speculative. Do **not** go verifying sections you had no reason to open — that is
a sweep's job, and an invitation demanding unprompted work is how a mechanism dies unused. Only the
check you already performed.

`status: deprecated` is **not** yours to set here: it is human-only, in the PR that deletes the
subject it describes.

You may also hand-add a link to a note into a page's `Gotchas` — also marking, not authoring. Use a
file-relative markdown link whose text is the note's filename stem, like every other in-wiki link.
The next sweep reconciles it.

Recompiling the wiki is a `/wiki-sweep`, hand-invoked, in its own docs-only PR. Never fold one into
a feature PR.

## Write-back: turning a session learning into a note

**Trigger: PR-open, after the fix is green.** Deliberately late — a mid-session "gotcha" is very
often the agent's own confusion, and a green fix is a free filter. The note ships **in the feature
PR**, because the person reviewing the PR that contained the bug is the only reader with the context
to judge whether the gotcha is real. A docs-only PR two weeks later gets a rubber stamp, which is how
a junk drawer fills.

### Route it first — most learnings are not notes

In order. `notes/` is the **residue**, reached only when every surface above has declined the fact:

1. environment / tooling → `docs/dev-setup.md`
2. a decision → an ADR
3. vocabulary → `CONTEXT.md`
4. behaviour of one build unit → that `structure/` page's `Gotchas`
5. cross-cutting, no owning surface → **`notes/`**

Then two gates, **both** of which must hold:

- **Trap, not trivia.** Its absence must make an agent write *confidently wrong* code or burn a real
  debugging cycle. Not merely something you didn't know.
- **Repo-true, not machine-true.** True of one developer's machine → disqualified outright.

And one admission question, because it decides whether the note is ever *found*: **which page would
have to be open for this to save someone?** That page goes in `bears_on`. A note that can't name one
probably fails the trap gate too.

### The note

```markdown
---
type: note
title: Persistence Gotchas
description: Traps in the JdbcClient persistence layer that cost a real debugging cycle.
bears_on: [backend-sabha, persistence]          # bare filename stems, not links
source_paths: [apps/backend/**/dataaccess/**]   # what could FALSIFY this
last_verified: <HEAD sha at authoring time>
---
# Persistence Gotchas

## <fact title>
**Symptom** · **Cause** · **Fix** · **Discovered** (date + issue/PR)
```

Notes carry **no coverage tags** — a note is primary evidence, not a derivation. The `Discovered`
line is its credibility marker.

`source_paths` on a note means *what could falsify this*, not *what this came from*. That is what
makes the staleness check above fire on the commit that fixes the trap.

Despite the name, `last_verified` is **not** a record that someone confirmed the note — it is the
note's checkpoint, `HEAD` at the moment you wrote it, so the staleness check above needs no second
code path.

### Deleting is part of the job

**The PR that removes a trap deletes the note.** Same moment, same reviewer, no new step.
Obsolescence is the default fate of a good note — a trap worth recording is a trap worth fixing, and
this repo does fix them.

Deleting, not deprecating. `status: deprecated` exists for a *compiled* page whose build unit was
removed, where deleting the page would break links across the wiki. Nothing links to a note except
the back-link the sweep derives into a `Gotchas` section, and the same PR removes that.
