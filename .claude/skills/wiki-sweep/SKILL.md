---
name: wiki-sweep
description: Compile and refresh docs/wiki — detect which pages the code has moved out from under, recompile them, and open a docs-only PR. Invoke by hand when the wiki needs catching up; sweeps are batched, never run inside a feature PR.
disable-model-invocation: true
---

# Wiki sweep

You are the wiki compiler. One session does **both** halves — detection and compilation — and lands a
**docs-only PR**. Nothing self-merges.

This file is the **procedure**. The page contract — skeletons, frontmatter, coverage vocabulary,
link rules — is `docs/wiki/protocol.md`. Read it before compiling anything; don't re-derive it here.

## Standing rules

- **`HEAD` only.** Uncommitted changes are ignored, so the wiki never describes a state that never
  existed. If the working tree is dirty, say so and compile against `HEAD` regardless.
- **Refuse rather than guess.** If git is unavailable, **abort**. No mtime fallback — a wrong
  checkpoint is worse than no ingest.
- **Exclude `docs/wiki/**`** from every diff and every source scan, or the wiki ingests itself.
- **Recompiling a page re-reads *all* its sources**, not only the changed ones.
- **Never write `notes/` bodies.** Notes are human-authored (`docs/agents/wiki.md`); you read them as
  input only.
- **Never rewrite `CONTEXT.md` or `docs/adr/`.** They are canonical and immutable. Cite them.
- **Sweeps are separate and batched** — never folded into a feature PR. The per-page cost is
  dominated by *input* (~4,500 words of ADRs for one page), so batching amortises the re-reads.

## Budget

Measured on the hardest real unit (`backend-identity`: 210 Java files, 9 feature packages):
**11 tool calls, no source file body read at all** — directory listings, an import scan, a table-name
grep, five `package-info.java`, two ADRs. Output was 681 words.

So: **`deep_scan: false` for `structure/` pages.** `package-info.java`, module manifests and
directory listings are the high-yield sources; method bodies are not. Dossiers will need more.
If the budget runs out mid-sweep, **stop cleanly** — per-page checkpoints exist precisely so partial
progress is recordable (step 6).

---

## 1. Preconditions

```sh
git rev-parse --git-dir          # abort if this fails
git rev-parse HEAD               # the sweep's target sha
git status --porcelain           # note a dirty tree; do not act on it
```

Read `docs/wiki/protocol.md`, then every page's frontmatter (not yet its body).

## 2. Compute the dirty set

A page is dirty if **any** of these fire.

**a. Path intersection** — per page, using *that page's* checkpoint:

```sh
git diff --name-only <page.last_compiled>..HEAD -- <each source_paths glob>
```

Per-page SHAs mean N diffs instead of one; that is the price of never having a checkpoint that lies.

**b. The migration rule** — path intersection cannot reach migrations, because the changelog under
`apps/backend/application-container/src/main/resources/db/changelog` is central and partitioned by
slice/issue (the `features/` axis), not by context. So:

```sh
git diff --name-only <sha>..HEAD -- '*.sql'
```

Grep the changed `.sql` for table identifiers (`CREATE TABLE`, `ALTER TABLE`, `DROP TABLE`) and dirty
any page whose `Data` section already lists that table.

*Known gap, state it in the PR body when it applies:* a **brand-new** table cannot dirty a page by
name, because no page lists it yet. Route it via the slice directory to the owning feature dossier —
where a new capability's schema belongs anyway — or to the candidates list (step 7).

**c. `status: disputed`** — unconditionally dirty, regardless of `last_compiled`. This is the one
signal meaning "recompile even though nothing moved."

**d. Missing `structure/` pages** — build units are manifest-declared, so this needs no judgement:

```sh
git diff --name-only <sha>..HEAD -- 'apps/backend/pom.xml' 'apps/mobile/**/pubspec.yaml'
```

A new `<module>` entry or `pubspec.yaml` means a `structure/` page is **missing**; create it. Without
this the wiki silently never grows.

Report the dirty set before compiling. If it is empty, say so and stop — an empty sweep is a
successful sweep.

## 3. Compile each dirty page

Per `protocol.md`: fixed sections in order, empties written `_none_`, a coverage tag as the first
non-empty line of every prose section except `Sources`, `[[slug]]` inside the wiki and relative links
only in `Sources` and frontmatter.

`docs/wiki/structure/backend-identity.md` is the **reference instance** of the `structure` skeleton.
Read it before compiling your first structure page — it is cheaper than re-deriving the shape from
the contract, and it is the tiebreak wherever the contract leaves a detail arbitrary.

Tag honestly. Coverage is **reader obligation**, not source count: `high` means *don't open the
source*, so it is a promise, not a compliment. The count rule is explicitly rejected — twenty-one
adapters you must *infer* ownership from is a `low` section, not a `high` one. Both
confidently-wrong claims the prototype produced landed in sections it had already tagged `low`/
`medium`; the tag is the mechanism working, so don't inflate it.

## 4. Derive the note back-links

For each compiled `structure`/`concept` page, scan `notes/*` frontmatter for `bears_on` entries
naming that page and emit `[[note-slug]]` links into its `Gotchas` section. Link, never restate.

Authors may have hand-added a back-link in a feature PR (this is *marking*, not authoring, and is
allowed) — **reconcile** it rather than duplicating or deleting it.

You are writing the *compiled* page here, not the note. Note bodies stay untouched.

## 5. Clear resolved disputes

If a page carried `status: disputed` and the recompile settles it, drop `status` and
`disputed_reason`. Every cleared dispute goes in the PR body as a review trigger — you are asserting
either that the reader was wrong or that the fix is right, and that deserves eyes.

Only compiled pages. A disputed **note** is cleared by a human.

## 6. Advance the checkpoints

Set `last_compiled: <HEAD sha>` **only on pages you actually finished.** A page you skipped, ran out
of budget on, or left half-verified keeps its old SHA. This is the whole point of per-page
checkpoints: a partial sweep records real progress without any checkpoint lying.

Never touch `last_verified` on a note.

## 7. Update `index.md`, and propose — don't create

Refresh the catalogs so **every page is listed exactly once** (that is what makes lint's orphan
detection possible), refresh `pages:`, and fill router rows 5–7 with a `[[slug]]` once the target
page exists. The eight router questions are fixed in `protocol.md` — never re-judge them.

`features/` and `concepts/` pages are **proposed in the PR body and not created**. Their admission
tests are judgement calls that stay with a human: a dossier is a durable capability a user could
name (an issue *amends* one, never adds one), and a concept must recur in 3+ pages *and* be cited by
2+ ADRs. Don't force concepts.

## 8. Lint

```sh
docs/wiki/lint
```

Deterministic, seven checks, no LLM. Fix every failure and re-run until clean. Do not open the PR
with a failing lint — CI runs the same script on `docs/wiki/**` and will catch it anyway.

## 9. Open the docs-only PR

Docs-only: the diff touches `docs/wiki/**` and nothing else.

The body has **two sections with different jobs**, and the first one is **derived, never
free-written** — if a free-text guess list and the page tags ever disagree, one of them is lying to
somebody.

```markdown
## Pages rewritten
- [[slug]] — <one line: what moved, why it was dirty (path / table / disputed / new unit)>

## Review these  <!-- DERIVED: changed prose ∩ a low|medium tag, tags that got worse, cleared disputes -->
- [[slug]] · `<section>` · `[coverage: low -- …]` — <the claim you are least sure of>

## Findings  <!-- free text, NOT a review trigger -->
<what you learned about the wiki's own design — a skeleton section that produced filler, a
mechanism that could not be satisfied. Feeds protocol.md amendments or new issues.>

## Candidates  <!-- proposed only; not created -->
- feature: `<slug>` — <the capability, and what suggested it>
- concept: `<slug>` — <the pattern, which pages, which ADRs>
```

**Review these** is the reviewer's whole job. It is deliberately *not* "every `low` section": the
prototype page is 4 `high` / 3 `low`, and two of those `low` sections are structurally permanent, so
"read every `low`" means re-reading byte-identical thin sections every sweep until targeted scrutiny
degrades into reading the whole diff — and then sweeps stop happening. The dangerous combination is
**thin sourcing plus new claims**, where the compiler is inventing with the least to check itself
against.

Never merge it yourself.
