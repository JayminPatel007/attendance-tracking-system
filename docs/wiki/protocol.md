# Wiki Protocol

What a wiki page must look like, and what its markers mean. This file is **declarative** — it is
the page contract, readable without reading the compiler.

The **procedure** — how the dirty set is computed, how a sweep runs, what the PR body says — lives
in the `wiki-sweep` skill (`.claude/skills/wiki-sweep/`). The skill is *how*; this file is *what*.
The **reading contract** — what a cold agent does with a page — lives in `docs/agents/wiki.md`.

Everything here is enforced, where enforceable, by `docs/wiki/lint` (§9).

---

## 1. Layout

```
docs/wiki/
├── index.md        ← front door: fixed router + per-kind catalog
├── protocol.md     ← this file
├── lint            ← the deterministic linter (executable)
├── log.md          ← reserved; unused
├── structure/      ← one page per build unit
├── features/       ← one page per durable capability
├── concepts/       ← one page per recurring pattern
└── notes/          ← session learnings; never compiler-written
```

Filenames are `<slug>.md`, lowercase-kebab. **Slugs are globally unique across kinds** — `[[slug]]`
is location-independent, so `concepts/authorization.md` and `features/authorization.md` cannot
coexist.

The compiler **excludes `docs/wiki/**` from its own source scan**, so the wiki never ingests itself.

---

## 2. The four kinds

| Kind | Answers | Unit | Compiled | `source_paths` are |
|---|---|---|---|---|
| `structure` | "what lives where, and what does it talk to?" | one per build unit | yes | directories |
| `feature` | "how does this capability work, end to end?" | one per durable capability | yes | files across apps + ADRs |
| `concept` | "what is the pattern behind these scattered ADRs?" | a recurring pattern | yes, second pass | files + ADRs |
| `note` | "what did we learn the hard way?" | a theme | **never** | *what could falsify it* |

**Ownership rule.** Structure and feature pages cross-link and never restate each other:

> Would a pure file move change this sentence? → **structure**.
> Would a product decision change it? → **feature**.

**Admission — when a new page is created rather than a section appended.** Size is a review smell,
never a trigger: a page over its cap with no admission test met is *misfiled*, not due for a split.

- **structure** — fixed by the build. A new Maven module, Dart package or app, and nothing else.
  Manifest-declared, so the sweep **creates** these without judgement — with one mechanical
  carve-out: **a unit with no source files beneath it gets no page** (see the sweep skill, §2d).
- **feature** — a new durable capability a user could name. **An issue or PR amends a dossier; it
  never adds one.** The sweep only *proposes* candidates in the PR body.
- **concept** — the pattern recurs in **3+ pages** *and* is cited by **2+ ADRs**. Proposed, never
  created, by the sweep. Don't force concepts.
- **note** — see §7.

**No ADR digest and no `CONTEXT.md` mirror page.** Those stay canonical and immutable; a
summarising page of an immutable original is a second copy that can only drift.

**Cross-app flows get no kind of their own** — a feature dossier *is* the cross-app flow page, and
its `Flow` section is written per app.

### Word budget — prose only, a smell only

The budget counts **prose words**: everything except frontmatter, HTML comments (the coverage tags)
and table rows. Those three are ~45% of a structure page's `wc -w` and none of them is what the
budget is for — coverage tags are metadata *about* the page and invisible to a reader, and a table
is scanned rather than read, so charging a 5-row ring table at the same rate as 150 words of
argument measures the wrong thing. Lint check 8 (§9) computes it, as a **warning**.

| Kind | Budget | Evidence |
|---|---|---|
| `structure` | ~550 | **derived** — n=6, observed 383–527 across four different unit shapes |
| `feature` | ~750 | **provisional** — n=1 (`attendance-marking`, 720) |
| `concept` | inherits `feature`'s | **unmeasured** — n=0 |

The provisional marks are load-bearing. The first budget (~700/~500) was stated as settled from a
single page's `wc -w`, and the first real sweep appeared to falsify it on all 7 pages — when what had
actually happened was that the units drifted out from under the number. State the sample size, or
the next reader inherits a law where there was an observation.

Size remains a **review smell, never a trigger**: over budget with no admission test met is
*misfiled*, not due for a split.

---

## 3. Frontmatter

### Compiled kinds (`structure`, `feature`, `concept`)

```yaml
---
kind: structure                 # required; one of structure | feature | concept
slug: backend-sabha             # required; globally unique, matches the filename
source_paths: [apps/backend/sabha-service/**]   # required; ≥1 glob, what this was derived from
decisions: [ADR-0015, ADR-0019] # optional; bare ADR ids
issues: [12, 84, 86]            # optional; feature pages mainly
appears_in: [[backend-identity]], [[backend-sabha]]   # concept pages only
last_compiled: <full-sha>       # required; the commit this page was compiled against
status: disputed                # optional; see §6. Absent means "not disputed"
disputed_reason: <one line>     # required iff status: disputed
---
```

There is **no** `draft`, `current`, `stale` or `orphaned` status. See §6.

### `note`

```yaml
---
kind: note
slug: persistence-gotchas
compiled: false
bears_on: [[backend-sabha]], [[persistence]]
source_paths: [apps/backend/**/dataaccess/**]   # what could FALSIFY this
last_verified: <full-sha>       # HEAD at authoring time
status: disputed                # optional, as above
---
```

`source_paths` carries **different semantics** on a note: for a compiled page it is *what this was
derived from*; for a note it is *what could falsify this*. Same field name deliberately — the
staleness check (§5) and lint checks 3 and 6 then apply uniformly, with no second code path.

`last_verified` is the note's checkpoint, set to `HEAD` at authoring time (the author cannot know
the merge commit). The imprecision is bounded by the PR's lifetime and errs toward reporting dirty.

### `index.md`

```yaml
---
kind: index
pages: 33
---
```

`index.md` carries **no global currency claim** — checkpoints are per page (§5). At most it may
carry a derived oldest-page line.

---

## 4. Skeletons

Sections are **fixed per kind, in order**. An **empty section is written `_none_`, never dropped** —
to a cold agent an absent `Talks To` reads identically as "calls nothing" and "nobody looked".

Every prose section on a compiled page carries a coverage tag (§5) as its **first non-empty line**.
`Sources` is exempt: it *is* the evidence, not a derivation from it.

`docs/wiki/structure/backend-identity.md` is the **reference instance** of the `structure` skeleton —
where a detail below is arbitrary (section order, tag placement), that page is the tiebreak.

### structure

```markdown
# <Unit Name>

## Purpose        <!-- 1–2 lines: what this unit is responsible for -->
## Layout         <!-- ring module -> what lives in it; plus a feature-package line
                       on large units. See [[module-ring]]. -->
## Exposes        <!-- route PREFIXES only; the /api/* mobile vs /bff/* web split.
                       Individual endpoints belong to the feature dossier. -->
## Talks To       <!-- two labelled halves: **Outbound** (target context, port, protocol)
                       and **Inbound** (common-domain ports this unit IMPLEMENTS for others) -->
## Data           <!-- tables owned; see the migration rule in §8 -->
## Gotchas        <!-- module-local only; cross-cutting -> notes/.
                       Compiler-derived [[note-slug]] back-links land here (§7). -->
## Covered by     <!-- [[feature]] backlinks; structurally `_none_` until dossiers exist -->
## Sources
```

The two halves of `Talks To` are **bold labels inside the one section**, not `###` subsections —
lint's skeleton check reads `##` headings, so keeping them out of the heading tree means the section
list stays exactly the eight above on every page.

`Talks To` splits Outbound/Inbound because these units are **peers that serve each other**: identity
has 2 outbound edges against 9 common-domain ports it implements. What a context *provides* is
often the load-bearing fact. Inbound duplicates another page's outbound — accepted, because making
a reader reconstruct nine edges by grepping port names defeats the point of the page.

`Layout` keeps the ring table so all pages stay comparable, but the ring is identical everywhere by
ADR-0015/0019 and already factored into `[[module-ring]]` — so on a large unit it gains a
**feature-package line**, which is the real navigation axis there. Small units simply have no such
line.

### feature

```markdown
# <Capability Name>

## What it does      <!-- user-facing, in CONTEXT.md's language -->
## Flow              <!-- per app: mobile/web -> BFF -> context -> data -->
## Rules & authority <!-- who may, what is rejected and with what code -->
## Where the code is <!-- [[structure]] links; no code detail restated -->
## Amendments        <!-- what changed, when, which issue, why -->
## Sources
```

### concept

```markdown
# <Pattern Name>

## The pattern       <!-- stated once, for the whole repo -->
## Why               <!-- decision trail; ADRs cited by number, never restated -->
## Where it appears  <!-- per-page instances and how each differs -->
## Deviations        <!-- known exceptions -->
## Sources
```

### note — never compiled

```markdown
# <Theme>

## <fact title>
**Symptom** · **Cause** · **Fix** · **Discovered** (date + issue/PR)
```

Notes have **no fixed section list** (a theme accretes facts) and **no coverage tags** (§5).

### index.md

The **Start here** router is a **fixed 8-question list**. The compiler fills only the target cell —
it never re-judges the questions. Amend the list here, in `protocol.md`, if it proves wrong.

| # | I need to know… |
|---|---|
| 1 | What is this system, in domain terms? |
| 2 | How do I run it locally? |
| 3 | Which app/module owns X? |
| 4 | How does capability Y work end to end? |
| 5 | How does authorization work? |
| 6 | Why is the backend shaped like this? |
| 7 | What must I not break? |
| 8 | What did we learn the hard way? |

Rows 1 and 2 point **out** of the wiki, to `CONTEXT.md` and `docs/dev-setup.md`, which already
answer them better than a wiki page would. Rows 3, 4 and 8 point at same-file catalog anchors. Rows
5, 6 and 7 point at a `[[slug]]`; until that page exists the compiler points the row at its kind's
catalog anchor instead, so the router never carries a dangling link.

Then one catalog table per kind, listing **every page exactly once** — that is what makes orphan
detection possible — with an **Also known as** column. This domain's vocabulary is Gujarati; an
agent searching "regional lead" needs a hook to reach *Nirdeshak*.

---

## 5. The two currency axes

They are **independent, and neither substitutes for the other**. A page can be perfectly current and
still confidently wrong.

### Axis 1 — `last_compiled`: has the world moved?

**Staleness is computed by the reader, not stored.** Before trusting a page:

```sh
git diff --quiet <last_compiled>..HEAD -- <source_paths>
```

Non-zero exit → stale. Nothing is written down, so no marker can lie. A stored `stale` flag would
survive the sweep that fixed it; that is exactly the "checkpoint that lies" failure a per-page
checkpoint exists to avoid.

**A stale page demotes from fact to map.** Navigation survives; specifics don't. Which unit owns
this, which ADRs govern it, which slugs to follow, what the gotchas were — all still usable. But **no
specific claim** (table names, file counts, port lists, route prefixes) may be repeated to a user
without verifying it against source. Stale ⇒ void was rejected: one `.sql` file landing would throw
away thousands of words of still-true ADR synthesis.

Checkpoints are **per page**, not one global SHA, so a budget-limited partial sweep records real
progress and a reader checking one page pays for one page.

### Axis 2 — coverage tags: was this well-sourced when written?

One tag per prose section, as the section's **first non-empty line**, written as an HTML comment so
it stays exact for an agent reading the raw markdown without cluttering the rendered page:

```
<!-- [coverage: high] -->
<!-- [coverage: medium -- shape from package-info.java; specifics not cross-checked] -->
<!-- [coverage: low -- adapter SQL only; no per-context schema and no ownership manifest exists] -->
```

Levels are defined by **reader obligation**, never by source count:

| Level | The reader's obligation |
|---|---|
| `high` | Trust it. Don't open the source. |
| `medium` | Trust the shape. Verify any specific you'll act on. |
| `low` | This is a lead. Read the source. |

A reason (` -- …`) is **required** on `low` and `medium` and optional on `high`.

**Counting sources is rejected as the rule.** Twenty-one JDBC adapters are not twenty-one witnesses
to "who owns this table" — they are twenty-one places you must *infer* it from, which is why that
section was weak. A count rule would have stamped the seed page's `Data` section `high`, and `Data`
is exactly where the confidently-wrong claim lived. Count measures how much the compiler read;
confidence measures whether what it read answers the question the section asks.

Judgement is less reproducible than counting — accepted, and mitigated by phrasing the levels as the
reader's obligation, because *"would I want a reader acting on this without opening the source?"* has
a defensible answer where *"how confident am I?"* does not.

**`notes/` are exempt from coverage tags.** A note is *primary evidence* — an eyewitness account of a
trap actually hit and actually fixed — not a derivation from sources. `high` would be meaningless
and `low` false. A note's credibility marker is its `Discovered` line.

### The review trigger

**Changed prose ∩ a `low`/`medium` tag, plus any tag that got worse.** Not "every `low` section":
two of the seed page's three `low` sections are *structurally permanent* (`Covered by` is empty
until dossiers exist; `Data` is inference-only), so "read every `low`" means re-reading ~26
byte-identical thin sections every sweep — which is how targeted scrutiny degrades into reading the
whole diff, and then sweeps stop happening.

The dangerous combination is thin sourcing **plus new claims**, where the compiler is inventing with
the least to check itself against. A thin section that hasn't changed is honest and already warns
the reader at read time — the tag doing its job.

Clearing a `disputed` flag is a review trigger too: the sweep is asserting either that the reader was
wrong or that the fix is right.

---

## 6. `status: disputed` — the only stored status

Any reader, agent or human, may set `status: disputed` plus a one-line `disputed_reason`. The line is
**marking versus authoring**: one flag and one line of reason, never page prose. On compiled pages it
is cleared only by the compiler; on `notes/`, which are human-authored by construction, a human may
also clear or delete.

This exists because verification would otherwise never compound: a reader who checks a claim, finds
the page wrong, and moves on discards the finding, and the next reader rediscovers the same error.

`disputed` is the only state **not derivable by any other mechanism**. No `git diff` finds it — these
errors are compile-time, so the sources never moved. No lint check finds it — links resolve and the
skeleton conforms. No coverage tag reliably warns, because a `high` section can be confidently wrong
too; the tag records how well-sourced the compiler was, not whether it was right.

**A disputed page is unconditionally dirty** on the next sweep, regardless of `last_compiled`. It is
the one signal meaning "recompile even though nothing moved."

Dropped, deliberately: `stale` (computed — §5), `orphaned` (lint check 6's output, recomputed free),
`draft` (neither computable nor actionable; the coverage tags say it better, per section), `current`
(the absence of `disputed`).

---

## 7. `notes/`: admission, retrieval, eviction

### Admission — routing first, `notes/` as residue

A learning is **routed, not collected**. Checked **in order**; `notes/` is what's left when every
canonical surface has declined it:

1. environment / tooling → `docs/dev-setup.md`
2. a decision → an ADR
3. vocabulary → `CONTEXT.md`
4. behaviour of one build unit → that `structure/` page's `Gotchas`
5. cross-cutting, no owning surface → **`notes/`**

Then two further gates, both of which must hold:

- **Trap, not trivia.** Its absence must make an agent write *confidently wrong* code or burn a real
  debugging cycle — not merely be something it didn't know.
- **Repo-true, not machine-true.** Anything true of one developer's machine is disqualified outright.

The most common outcome of "I learned something" is **not** a notes page.

### Retrieval — the reverse edge is derived

`bears_on` runs note → page, but the agent is reading the *page*, which doesn't know the note exists.
So the sweep **derives** `[[note-slug]]` back-links into a compiled page's `Gotchas` by scanning
`notes/*` frontmatter for `bears_on` entries naming that page. Link, never restate.

An author may hand-add that back-link in the same PR (otherwise the note is invisible from its page
until the next hand-invoked sweep, possibly weeks). Adding one `[[slug]]` line to an existing section
is **marking, not authoring**; the next sweep reconciles it.

This makes `bears_on` load-bearing, and therefore a real admission question at write time: *which
page would have to be open for this to save someone?* A note that can't name one probably fails the
trap gate too.

### Eviction — the PR that removes the trap deletes the note

Same moment, same reviewer, no new step. Obsolescence is the *default* fate of a good note: a trap
worth recording is a trap worth fixing, and this repo does fix them. A review-by date was rejected —
it fires on the calendar rather than on the event that falsifies the note.

The write-back trigger, the fix-green precondition and the PR-open timing live in
`docs/agents/wiki.md`.

---

## 8. Links, citations, and the migration rule

**Two namespaces, kept strictly apart.**

- **Inside the wiki:** `[[slug]]`, exclusively. Never a relative path to another wiki page.
- **Out of the wiki:** relative markdown links, **only** in the `Sources` section and in the
  machine-readable frontmatter (`source_paths`, `decisions`).
- **In prose:** cite **bare** — `per ADR-0011`, and glossary terms in `CONTEXT.md`'s exact wording,
  unlinked. **No anchors into `CONTEXT.md`**: it has no per-term headings, only five coarse `###`
  groups, so an anchor would be both imprecise and one rewording from broken.

Concentrating out-links gives one bounded place to lint, keeps prose surviving renames (`ADR-0011` is
stable; `0011-role-appointment-authority.md` is a filename), and matches the repo's existing idiom.
Relative depth is two-valued: `../../adr/…` and `../../../CONTEXT.md` from a kind directory,
`../adr/…` and `../../CONTEXT.md` from `index.md`.

### The migration rule

Path intersection is the only invalidation rule for code. Migrations get a second, **content-based**
rule, because the changelog under `apps/backend/application-container/src/main/resources/db/changelog`
is central and partitioned by **slice/issue** — the `features/` axis — so no path glob on a
`structure` page can ever reach it:

> The sweep greps changed `.sql` files for table identifiers and dirties any page whose `Data`
> section already lists that table.

Putting the changelog glob on all 13 structure pages was rejected: correct by construction, but one
one-table migration would then dirty all 13 pages on every batched sweep.

**Known gap, stated rather than hidden:** a migration introducing a **brand-new** table cannot dirty
a page by name, because no page lists it yet. Such a migration also dirties via its slice directory →
the owning feature dossier, which is where a new capability's schema belongs anyway.

---

## 9. Lint

`docs/wiki/lint` is a **deterministic, checked-in script — no LLM**. It is run by the sweep *and* by
CI, path-filtered on `docs/wiki/**`. CI because `notes/` is human-written, so most wiki PRs contain
no sweep at all, and a sweep-only linter would fire on that breakage weeks later inside an unrelated
PR where it looks like the compiler's fault.

An agent step was rejected: resolving every wikilink, path and glob is a fully specified algorithm,
it is the category of work LLMs are least reliable at, and silently missing one broken link among
sixty is an ordinary agent failure and an impossible one for `test -f`.

The eight checks. Checks 1–7 are **failures** (exit 1); check 8 is a **warning** — printed in the
same `file:line` format, never affecting the exit code:

| # | Check | Scope |
|---|---|---|
| 1 | `[[slug]]` resolves to an existing page | all kinds |
| 2 | slugs are globally unique across kinds | all kinds |
| 3 | relative links in `Sources` and paths in frontmatter resolve on disk | all kinds, incl. note `source_paths` |
| 4 | every section carries a coverage tag with a valid level | **compiled kinds only** |
| 5 | the checkpoint (`last_compiled` / `last_verified`) is present and is a real commit; and `status`, if set, is `disputed` with a reason | all pages, not `index.md` |
| 6 | each `source_paths` glob still matches ≥1 file — **the orphan check** | all kinds, incl. notes |
| 7 | skeleton conformance: fixed per-kind sections, in order, empties written `_none_` | compiled kinds |
| 8 | prose word count is within the §2 budget — **warning, not failure** | compiled kinds |

**Check 8 must not be a gate.** §2 fixes size as a smell and never a trigger, so a check that failed
the build would overturn the contract it exists to report on — and would have failed the seed set's
own dossier, which is the right size. Notes are exempt with the other compiled-kind checks: they are
primary evidence, not derivation, and have no skeleton to be long relative to.

**Check 6 is load-bearing and unreachable by any other mechanism here.** A deleted path stops
matching `source_paths`, so `git diff ∩ source_paths` can *never* fire on it — lint is the only way
to catch a page whose subject no longer exists, which would otherwise stay `high`-tagged and
current-looking forever.

Check 5 verifies the SHA is a real commit only when git is available; in a shallow checkout it
degrades to a format check rather than failing. It also rejects any `status` other than `disputed` —
the stored-status rule of §6 would otherwise have nothing enforcing it.

Lint reads `index.md` plus every page under the four kind directories. **`protocol.md` and `log.md`
are excluded**: this file documents the contract, so it quotes illustrative slugs and skeletons that
are not real links.
