# Wiki

How to **read** the in-repo wiki, and how to **write back** what a session learned the hard way.

Enter at **`docs/wiki/index.md`**. Its `Start here` router is eight fixed questions over a catalog
of every page; three of them point straight back out to `CONTEXT.md` and `docs/dev-setup.md`.

If `docs/wiki/` doesn't exist, **proceed silently** — same rule as the rest of `docs/agents/`.

## The kinds

| Kind | Answers | Written by |
|---|---|---|
| `structure/` | what lives where, and what does it talk to | the compiler |
| `features/` | how a capability works end to end | the compiler |
| `concepts/` | the pattern behind a cluster of ADRs | the compiler |
| `notes/` | what we learned the hard way | **you**, in a feature PR |

**The wiki is derived. On conflict, `CONTEXT.md` and `docs/adr/` win** — they are canonical and
immutable, and the wiki cites them rather than restating them. Glossary terms and ADR numbers appear
**bare** in wiki prose; the links live in each page's `Sources` section.

The full page contract is `docs/wiki/protocol.md`. You only need it if you are compiling.

## Before trusting a page

```sh
git diff --quiet <last_compiled>..HEAD -- <the page's source_paths>
```

Non-zero exit → the page is **stale**. Staleness is computed, never stored, so no marker can lie to
you. `notes/` use `last_verified` for the same check.

**A stale page demotes from fact to map.** Still usable: which unit owns this, which ADRs govern it,
which slugs to follow, what the gotchas were. **No longer repeatable to a user without checking the
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

You may also hand-add a `[[note-slug]]` back-link into a page's `Gotchas` — also marking, not
authoring. The next sweep reconciles it.

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
kind: note
slug: persistence-gotchas
compiled: false
bears_on: [[backend-sabha]], [[persistence]]
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

### Deleting is part of the job

**The PR that removes a trap deletes the note.** Same moment, same reviewer, no new step.
Obsolescence is the default fate of a good note — a trap worth recording is a trap worth fixing, and
this repo does fix them.
