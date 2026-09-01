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
- **Never author or clear `status: deprecated`.** It is human-only, set in the PR that deletes the
  subject. You are the one actor that **cannot** tell "this unit was deleted" from "these globs are
  wrong" — the human deleting the module knows; you are guessing. See `protocol.md` §6.
- **Never drop a `sources[]` entry on recompile.** Adding one is ordinary; removing one sheds
  provenance quietly, which is exactly the drift `sources[]` exists to stop. If a source genuinely no
  longer informs the page, say so in the PR body rather than deleting it silently.
- **Never rewrite `CONTEXT.md` or `docs/adr/`.** They are canonical and immutable. Cite them.
- **Sweeps are separate and batched** — never folded into a feature PR. The per-page cost is
  dominated by *input* (~4,500 words of ADRs for one page), so batching amortises the re-reads.

## Budget

**`deep_scan: false` for `structure/` pages.** Seven pages have now been compiled with **no source
file body read at all** — the finding that has held up twice, and the expensive habit this skill
exists to prevent. Dossiers need more.

Cost is **~10–20 tool calls per structure page**. The driver is whether the unit's `Data` can be
read off a manifest or must be **inferred from adapters** — inference is the structural weak point
where every wrong claim so far has landed, and it is what makes a page expensive *and* low-coverage
at the same time.

### The source ladder

Try in order. The top rung is worth more than the rest combined **when it is real**, and one call
tells you whether it is here:

```sh
grep -rL "Empty scaffold" --include=package-info.java <unit>/
```

0. **The page's own `## Method` section**, if it has one. It is a method statement plus a yield
   judgement written by the last compiler — *"seven docblocks; the highest-yield source on this
   page"* — and it is the cheapest rung on this ladder. Read it before choosing the others, and
   **rewrite it** when you are done, so the next sweep inherits what you learned rather than what
   the sweep before you did.
1. **`package-info.java` — but only the files that grep lists.** Substantive in `identity`
   (14 files; 9 are feature-package docs naming the feature in domain vocabulary), `attendance`
   (5, multi-paragraph) and `common-domain` (1, enumerates the library). In `sabha` and `analytics`
   all 5 are ADR-0019 ring scaffolds reading `"Empty scaffold per ADR-0019"` — identical everywhere,
   therefore zero information. One grep is cheaper than the five reads this rung used to prescribe.
2. **Module manifests and directory listings** — always available, and the only rung that exists at
   all outside the JVM (a Dart package or the Angular app has no `package-info.java`).
3. **A class listing** per ring module — the `Holds` column comes from here.
4. **A mapping-annotation grep** (`@GetMapping`/`@PostMapping`/…) — this is what `Exposes` is built
   from.
5. **A writer-SQL grep** (`INSERT INTO`/`UPDATE`/`DELETE FROM` across the unit's adapters) — this is
   what `Data` → **Owns** is inferred from, and the reason `Data` earns a `low`/`medium` tag more
   often than any other section. A table the unit only ever `SELECT`s belongs under **Reads**, and
   that distinction is now load-bearing: §2b fires on Owns alone. When the grep leaves you unsure
   which side a table falls on, that uncertainty *is* the section's coverage tag — say so rather
   than picking a label to look decisive.

Rungs 3–5 always work; rung 1 sometimes doesn't. **State the ladder as an order to try, not a law** —
the previous version of this section named rung 1 as *the* high-yield source on a sample of one
context, and the first real sweep found it worthless in two of five units.

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

`source_paths` now carries a page's **ADR globs and `CONTEXT.md`** alongside its code globs, so this
one diff covers the hand-written docs that are the dominant input — they used to be watched by
nothing. It also carries **production source and manifests only, never the test tree**. Both rules,
and the git-versus-Python `**` trap that decides how the globs must be spelled, are `protocol.md` §3;
when you create or edit a page's `source_paths`, follow it there rather than re-deriving. Lint check
9 fails the build if a cited ADR goes unwatched.

**b. The migration rule** — path intersection cannot reach migrations, because the changelog under
`apps/backend/application-container/src/main/resources/db/changelog` is central and partitioned by
slice/issue (the `features/` axis), not by context. So:

```sh
git diff --name-only <sha>..HEAD -- '*.sql'
```

Grep the changed `.sql` for table identifiers (`CREATE TABLE`, `ALTER TABLE`, `DROP TABLE`) and dirty
any page listing that table under **`Data` → Owns**. A table under **Reads** does **not** dirty the
page.

The Owns-only restriction is the rule, not a refinement of it. Replayed over all 15 real `.sql`
commits, the unrestricted version never once added an owner — 12 of 12 DDL commits shipped with the
owning context's code, which path intersection had already caught — and 7 of its 8 otherwise-
unreachable hits were `backend-analytics` merely *reading* someone else's table. The one hit worth
having was a **cross-context column addition** (analytics adding a column to identity's `users`),
where no identity code moved and this grep was the only mechanism that could reach the page. Aimed
at Owns, that replay yields exactly that hit and nothing else. See `protocol.md` §8.

**A page compiled before the Owns/Reads split has no Owns list, so this rule cannot fire on it.**
Migrate the section while you are recompiling the page anyway — the ownership judgement comes from
rung 5's writer-SQL grep, which you are running for `Data` regardless. Say in the PR body which
pages still lack the labels.

*Known gap, state it in the PR body when it applies:* a **brand-new** table cannot dirty a page by
name, because no page lists it yet. Route it via the slice directory to the owning feature dossier —
where a new capability's schema belongs anyway — or to the candidates list (step 7).

**c. `status: disputed`** — unconditionally dirty, regardless of `last_compiled`. This is the one
signal meaning "recompile even though nothing moved."

**A `status: deprecated` page is never dirty, by any rule above.** Skip it entirely: do not diff it,
do not recompile it, do not clear the marker. Its subject is gone, so §2a's diff comes back clean
anyway (the globs match nothing) — which is precisely why deprecation is a suppression of the
currency claim and not merely a lint exemption. This is the one sweep obligation that is purely *not
touching* something.

**d. Missing `structure/` pages** — build units are manifest-declared, so this needs no judgement:

```sh
git diff --name-only <sha>..HEAD -- 'apps/backend/pom.xml' 'apps/mobile/**/pubspec.yaml'
```

A new `<module>` entry or `pubspec.yaml` means a `structure/` page is **missing**; create it. Without
this the wiki silently never grows.

**Carve-out: a unit with no source files beneath it gets no page.** Check before creating:

```sh
find <unit> -path '*/src/*' -name '*.java' -o -path '*/lib/*' -name '*.dart' | head -1
```

Empty output → skip it, and say so in the PR body. `apps/backend/pom.xml` declares **7** modules
against the wiki's 6 pages: `coverage-aggregate` is a JaCoCo report aggregator with no source, and
has nothing to say under any skeleton section.

**Do not test `packaging`.** Four of the five context aggregators are `<packaging>pom</packaging>`
and rightly have pages — `identity-service` alone holds 210 files. Source presence is the
discriminator; packaging is not. The test stays mechanical, so §2d keeps the property worth having:
the sweep creates structure pages **without judgement**. A legitimately new-but-empty module simply
gets its page from the next sweep after code lands, which is the right default — an empty module has
nothing to document.

**`apps/web/angular.json` is deliberately absent from that glob list, and this is not an oversight.**
The rule creates *one page per declared unit*, which is only sound where the taxonomy is one page per
declared unit — true of Maven modules and Dart packages, false of web. Under #144 web is **a single
fixed page** no matter how many Angular projects `angular.json` declares, so watching it here would
make the sweep create a page per new library — the opposite of the taxonomy. The gap it appears to
leave is closed one section up: `angular.json` is in the `web` page's own `source_paths`, so a new project
dirties the web page by **§2a path intersection** and gets folded into its `Layout` table, which is
where a new library belongs. §2d creates pages; §2a maintains them, and web needs only the second.

The count is not close, so this is unlikely to flip: `angular.json` declares 8 projects, and **four
of the seven libraries are lone-`public-api.ts` scaffolds** with nothing to say under any skeleton
section — the same emptiness the carve-out above already refuses to give a page to.

Report the dirty set before compiling. If it is empty, say so and stop — an empty sweep is a
successful sweep.

## 3. Compile each dirty page

Per `protocol.md`: fixed sections in order, empties written `_none_`, a coverage tag as the first
non-empty line of every prose section except `Method`.

**Links are the one obligation you can get mechanically wrong.** Every in-wiki link is an ordinary
markdown link whose href is a **file-relative path** and whose text is the **target's filename stem**
— so you must now *compute a path* where you previously emitted a bare slug, and a page you **move**
between type directories rewrites its **inbound** links rather than none of them. Lint check 1
catches both, which is the point; but write them right the first time rather than treating the linter
as the author.

**Write `## Method` deliberately, and narrowly.** It is not a citation list — the citations live in
`sources[]` — it is *how you compiled this page and where the next compiler should look first*. What
it owes is **the yield judgement**: name the source that paid and say why it paid, in a sentence.
That is the half a cold recompiling agent measured as changing its plan (`protocol.md` §3). Do **not**
restate which greps fed which section: that is this ladder, in this order, and the next agent is
reading it too. Extra bullets are permitted where the derivation genuinely took several distinct
techniques; they are not owed, and "nice context" is not a reason to write one.

**Frontmatter you now author**: `title`, `description`, `aliases` (search hooks, open list — this is
where they live now, not in the catalog cell), `tags` (the closed vocabulary in `protocol.md` §3 —
do not invent one), `resource` on structure pages, `scaffold: true` on a structure page whose unit
declares no types yet (**delete the key** in the sweep that first finds one — unlike `deprecated`,
this one is yours to author, because you can see it by reading the package), and `sources[]` with a
stable `id` per document. Keep the scaffold fact **out of `description`**: it has a field now, and
the catalog derives its marker from the field.

`docs/wiki/structure/backend-identity.md` is the **reference instance** of the `structure` skeleton.
Read it before compiling your first structure page — it is cheaper than re-deriving the shape from
the contract, and it is the tiebreak wherever the contract leaves a detail arbitrary.

**Respect a human verification in a coverage tag.** A tag reading
`[coverage: high -- verified 2026-08-31 by human:jaymin against SabhaJdbcAdapter; compiled medium]`
records a check a person actually performed. If your recompile leaves that section **byte-identical**,
**leave it standing**. If you rewrite the prose, **demote it** to your own honest level and move the
verification into the reason as history — `[coverage: medium -- shape from adapters; human:jaymin
verified at 09fb207, prose has since changed]` — and report the lapse in the PR body (step 9). Wiping
it silently reintroduces the non-compounding defect through the back door; preserving it across a
rewrite claims a person read prose they never saw.

Tag honestly. Coverage is **reader obligation**, not source count: `high` means *don't open the
source*, so it is a promise, not a compliment. The count rule is explicitly rejected — twenty-one
adapters you must *infer* ownership from is a `low` section, not a `high` one. Both
confidently-wrong claims the prototype produced landed in sections it had already tagged `low`/
`medium`; the tag is the mechanism working, so don't inflate it.

## 4. Derive the note back-links

For each compiled `structure`/`pattern` page, scan `notes/*` frontmatter for `bears_on` entries
naming that page and emit a markdown link to the note into its `Gotchas` section — file-relative
href, text equal to the note's filename stem, like every other in-wiki link. Link, never restate.

`bears_on` itself carries **bare stems**, not links: frontmatter is machine-read and never rendered.

Authors may have hand-added a back-link in a feature PR (this is *marking*, not authoring, and is
allowed) — **reconcile** it rather than duplicating or deleting it.

You are writing the *compiled* page here, not the note. Note bodies stay untouched.

## 5. Clear resolved disputes

If a page carried `status: disputed` and the recompile settles it, drop `status` and
`disputed_reason`. Every cleared dispute goes in the PR body as a review trigger — you are asserting
either that the reader was wrong or that the fix is right, and that deserves eyes.

Only compiled pages. A disputed **note** is cleared by a human. **`deprecated` is never cleared by
anyone** — there is no un-deleting, so do not look for a clearing path.

## 6. Advance the checkpoints

Set `last_compiled: <HEAD sha>` **only on pages you actually finished.** A page you skipped, ran out
of budget on, or left half-verified keeps its old SHA. This is the whole point of per-page
checkpoints: a partial sweep records real progress without any checkpoint lying.

Never touch `last_verified` on a note.

## 7. Regenerate `index.md`, and propose — don't create

**The catalog is a pure function of the pages — regenerate it, don't re-judge it.** Every row is
derived from frontmatter: the link from the filename, the middle cell from `resource` (structure) or
`description` (everything else), the third cell from `aliases` comma-joined, and a `(deprecated)`
marker from `status` or a `(scaffold)` marker from `scaffold: true`. There is no hand-written cell
left, which is the point: a wrong catalog is now a **frontmatter bug**, and lint check 2 asserts cell fidelity as well as *every page exactly once*.

There is **no `pages:` count to refresh** — it is gone with `index.md`'s frontmatter, which now
carries `okf_version` and nothing else.

The one judgement left is the router: fill rows 5–7 with a link once the target page exists,
otherwise leave the row pointing at its type's catalog anchor. The eight router questions are fixed
in `protocol.md` — never re-judge them.

`features/` and `patterns/` pages are **proposed in the PR body and not created**. Their admission
tests are judgement calls that stay with a human: a dossier is a durable capability a user could
name (an issue *amends* one, never adds one), and a pattern must recur in 3+ pages *and* be cited by
2+ ADRs. Don't force patterns.

## 8. Lint

```sh
docs/wiki/lint
```

Deterministic, nine checks, no LLM — and it is this bundle's OKF §11 conformance checker as well as
its producer linter. Fix every failure and re-run until clean. Do not open the PR
with a failing lint — CI runs the same script on `docs/wiki/**` and will catch it anyway.

**Check 8 is a warning, not a failure** — a page over its prose budget is a smell (`protocol.md` §2).
Don't split a page to silence it; carry the warning into the PR body and let the reviewer judge.

## 9. Open the docs-only PR

Docs-only: the diff touches `docs/wiki/**` and nothing else.

The body has **two sections with different jobs**, and the first one is **derived, never
free-written** — if a free-text guess list and the page tags ever disagree, one of them is lying to
somebody.

```markdown
## Pages rewritten
- <stem> — <one line: what moved, why it was dirty (path / table / disputed / new unit)>

## Review these  <!-- DERIVED: changed prose ∩ a low|medium tag, tags that got worse, cleared disputes, demoted verifications -->
- <stem> · `<section>` · `[coverage: low -- …]` — <the claim you are least sure of>
- <stem> · `<section>` — **verification demoted**: <who verified it, and what prose changed under it>

## Findings  <!-- free text, NOT a review trigger -->
<what you learned about the wiki's own design — a skeleton section that produced filler, a
mechanism that could not be satisfied. Feeds protocol.md amendments or new issues.>

## Candidates  <!-- proposed only; not created -->
- feature: `<stem>` — <the capability, and what suggested it>
- pattern: `<stem>` — <the pattern, which pages, which ADRs>
```

**Review these** is the reviewer's whole job. It is deliberately *not* "every `low` section": the
prototype page is 4 `high` / 3 `low`, and two of those `low` sections are structurally permanent, so
"read every `low`" means re-reading byte-identical thin sections every sweep until targeted scrutiny
degrades into reading the whole diff — and then sweeps stop happening. The dangerous combination is
**thin sourcing plus new claims**, where the compiler is inventing with the least to check itself
against.

Never merge it yourself.
