# Wiki Protocol

What a wiki page must look like, and what its markers mean. This file is **declarative** — it is
the page contract, readable without reading the compiler.

The **procedure** — how the dirty set is computed, how a sweep runs, what the PR body says — lives
in the `wiki-sweep` skill (`.claude/skills/wiki-sweep/`). The skill is *how*; this file is *what*.
The **reading contract** — what a cold agent does with a page — lives in `docs/agents/wiki.md`.

Everything here is enforced, where enforceable, by `docs/wiki/lint` (§9).

## This wiki is an OKF v0.2 bundle

`docs/wiki/` is an [OKF v0.2](https://github.com/GoogleCloudPlatform/open-knowledge-format/blob/main/SPEC.md)
knowledge bundle, and §9's lint is its §11 conformance checker. The vocabulary below — provenance,
trust, freshness, lifecycle — is OKF's; the field set is not the whole of OKF's, because **there is
no consumer**. This repo does not publish the bundle, does not feed it to any tooling, and does not
consume anyone else's. So conformance buys nothing on its own, and every field here had to justify
itself to this repo's in-repo reader instead. Where a spec field is *declined*, the decline is stated
rather than omitted, so a reader arriving from SPEC.md knows the difference between a gap and a
decision. Those declines are §5.5 `stale_after`, §5.1's credibility signals and footnotes, §5.2/§5.3's
`generated` / `verified` / trust tiers, §5.4's `draft`, §6.3's `references/`, §9's `log.md`, and §10
attestation.

> **One word means two things, and the collision is unavoidable.** OKF's §2 **Concept** is *any
> document in a bundle* — every page here is an OKF Concept. This wiki's own `concept` **type** was
> a narrower thing, a recurring pattern behind a cluster of ADRs, and it has been **renamed
> `pattern`** so the two words never have to share a sentence. If you arrive from the spec: `type:`
> takes this wiki's four values, not OKF's open set, and §2 below says why.

---

## 1. Layout

```
docs/wiki/
├── index.md        ← the bundle root: fixed router + per-type catalog
├── protocol.md     ← this file
├── lint            ← the deterministic linter (executable)
├── log.md          ← reserved by OKF §9; deliberately not used — see below
├── structure/      ← one page per build unit
├── features/       ← one page per durable capability
├── patterns/       ← one page per recurring pattern
└── notes/          ← session learnings; never compiler-written
```

Filenames are `<stem>.md`, lowercase-kebab. **The filename is the page's identity** — it is OKF §2's
Concept ID, it is what links resolve to, and it is what `grep` finds. **Stems are globally unique
across types**, so `patterns/authorization.md` and `features/authorization.md` cannot coexist: that
uniqueness is what makes a bare stem an unambiguous search key, and what keeps a move between type
directories a `sed` plus a red build rather than silent rot.

**`log.md` is declined, not missing.** OKF §9 reserves it for a bundle changelog; this wiki does not
keep one, because `git log --follow docs/wiki/` answers the same question better and cannot fall out
of date. The line stays in the layout above precisely so a reader arriving from §9 does not assume
the file was forgotten. §11 clause 3 binds `log.md` only *when present*, so its absence is conformant.

The compiler **excludes `docs/wiki/**` from its own source scan**, so the wiki never ingests itself.

---

## 2. The four types

| `type` | Answers | Unit | Compiled | `source_paths` are |
|---|---|---|---|---|
| `structure` | "what lives where, and what does it talk to?" | one per build unit | yes | directories |
| `feature` | "how does this capability work, end to end?" | one per durable capability | yes | files across apps + ADRs |
| `pattern` | "what is the pattern behind these scattered ADRs?" | a recurring pattern | yes, second pass | files + ADRs |
| `note` | "what did we learn the hard way?" | a theme | **never** | *what could falsify it* |

**The taxonomy is closed.** Exactly these four values, lowercase, single words. OKF §1 declines to
bless a central taxonomy of concept types, but that is a statement about what the *spec* will not fix
centrally, not a licence for a producer to stay open — and the reference bundles show the cost of
staying open, with `type: Reference` absorbing 38% of the corpus as an escape hatch. The admission
tests below are load-bearing, and an open `type` makes *"should this be a new page?"* answerable by
inventing a type, which is the failure the admission tests exist to prevent.

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
- **pattern** — the pattern recurs in **3+ pages** *and* is cited by **2+ ADRs**. Proposed, never
  created, by the sweep. **Don't force patterns** — the warning matters more under this name than it
  did under `concept`, because `pattern` is the softer word and every codebase thinks it has fifty.
- **note** — see §7.

**No ADR digest and no `CONTEXT.md` mirror page.** Those stay canonical and immutable; a
summarising page of an immutable original is a second copy that can only drift. OKF §6.3's
`references/` convention would be a sanctioned place to mirror them and is **declined for the same
reason** — including for `.claude/skills/wiki-sweep/`, which §6.3's "run instructions" would cover.
A live file mirrored is a second copy; `sources[].resource` (§3) already addresses a bundle-external
artifact with a stable id, which is what `references/` was going to buy.

**Cross-app flows get no type of their own** — a feature dossier *is* the cross-app flow page, and
its `Flow` section is written per app.

### Word budget — prose only, a smell only

The budget counts **prose words**: everything except frontmatter, HTML comments (the coverage tags)
and table rows. Those three are ~45% of a structure page's `wc -w` and none of them is what the
budget is for — coverage tags are metadata *about* the page and invisible to a reader, and a table
is scanned rather than read, so charging a 5-row ring table at the same rate as 150 words of
argument measures the wrong thing. Lint check 8 (§9) computes it, as a **warning**.

| `type` | Budget | Evidence |
|---|---|---|
| `structure` | ~550 | **derived** — n=6, observed 383–527 across four different unit shapes |
| `feature` | ~750 | **provisional** — n=1 (`attendance-marking`, 720) |
| `pattern` | inherits `feature`'s | **unmeasured** — n=0 |

The provisional marks are load-bearing. The first budget (~700/~500) was stated as settled from a
single page's `wc -w`, and the first real sweep appeared to falsify it on all 7 pages — when what had
actually happened was that the units drifted out from under the number. State the sample size, or
the next reader inherits a law where there was an observation.

Both numbers predate `## Method` (§3), which added a section of genuine prose to every compiled page,
so they are due a re-measure rather than a defence the next time a sweep touches enough pages to
resample them.

Size remains a **review smell, never a trigger**: over budget with no admission test met is
*misfiled*, not due for a split.

---

## 3. Frontmatter

### Compiled types (`structure`, `feature`, `pattern`)

```yaml
---
type: structure                 # required; one of structure | feature | pattern | note
title: Identity Service         # required; the page's display label
description: Owns who a person is, what authority they hold, and how they prove it.
resource: apps/backend/identity-service   # `structure` only; the build unit this page describes
aliases: [People, Users, roles, Karyakar]  # optional; open list of search hooks
tags: [bff]                     # optional; closed vocabulary, see below
source_paths: [                 # required; ≥1 glob, what invalidates this page. See below.
  apps/backend/identity-service/*/src/main/**,
  apps/backend/identity-service/*/pom.xml,
  apps/backend/identity-service/pom.xml,
  docs/adr/0015-*.md,
  CONTEXT.md
]
issues: [12, 84, 86]            # optional; feature pages mainly
sources:                        # required, ≥1; what was READ to write this page
  - { id: adr-0015, title: "Bounded-Context Seams Are Build Modules", resource: ../../adr/0015-bounded-context-seams-as-build-modules.md }
  - { id: context, title: "CONTEXT.md — Nirdeshak, Sanchalak", resource: ../../../CONTEXT.md }
appears_in: [backend-identity, backend-sabha]   # `pattern` pages only; bare stems
last_compiled: <full-sha>       # required; the commit this page was compiled against
status: disputed                # optional; see §6. Absent means "current and not deprecated"
disputed_reason: <one line>     # required iff status: disputed
---
```

`title` is **required and is not pinned to the H1.** The H1 is the page's heading; `title` is its
display label. Identical by default, and they may differ only where the H1 reads badly lifted out of
context. This is deliberately *not* an extension of the repo's lint-enforced-duplication precedent
(check 2's catalog fidelity, check 9's sources-are-watched): there, one thing is by construction a
copy of another, so the check polices a duplication the design chose. Here two things are
independently authored, and pinning them would *create* the duplication.

`description` is **required on all four types** — one line, the page's own summary. It is what the
`index.md` catalog prints for every type but `structure`, whose middle column is `resource` instead.

`resource` is **`structure`-only** and is a **bare repo-relative path**, not a URI. It names the build
unit this page describes — a fact that previously lived nowhere machine-readable, only buried inside
`source_paths` globs. It must exist on disk and be a prefix of at least one `source_paths` glob, so a
page cannot describe a unit it does not watch. It is **absent** on `feature`, `pattern` and `note`,
on OKF §4.1's own terms: a dossier describes a capability spread across four build units and has no
single asset to point at, and inventing a URI for it is worse than the field's absence.

**A bare path, not a URI, on evidence.** OKF §6.1 *recommends* a `/`-absolute bundle-relative form,
and its own bundles abandoned it across 152 links because it 404s on GitHub — the reference viewer
dropped support, and the surviving absolute links contribute zero edges to its graph. Same trade
here, same answer, and the same reasoning governs §8's links.

`aliases` is **optional, open, and any type**: a list of search hooks. This domain's vocabulary is
Gujarati, so an agent searching "regional lead" needs a way to reach *Nirdeshak*. It is open —
unlike `tags` — because an alias's whole job is to be the one word some particular reader would have
typed. It lives in frontmatter rather than in the catalog cell it feeds because that is what makes
the catalog a **pure function** of the pages (§4); left in the table it would make every regenerated
row a merge, and merges are where drift lives.

`tags` is **optional, and a closed vocabulary** — currently `offline-sync`, `audit`, `bff`, enforced
by lint check 3. Tags **partition**, which is why they are closed where aliases are open: an open tag
set over 14 pages splits one query into `authz` / `auth` / `authorization` silently. Admission is a
floor **and a ceiling**: a tag must apply to **3+ pages and no more than half of them**. The floor
kills the single-use tag, which is a description of one page and that field now exists. The ceiling
exists on evidence — `ring` would hit 14/14 pages and `port` 13/14, clearing any floor while
partitioning nothing. A tag matching every page is a fact *about* the wiki, not an axis *through* it.

### `sources[]` — what was read, as distinct from what is watched

`sources[]` is the page's **provenance record**; `source_paths` is its **invalidation mechanism**.
They are not one field wearing two names, and the line between them is worth stating in one sentence:

> **`sources[]` names documents to be *read*. `source_paths` names code to be *watched*.**

So **code paths never appear in `sources[]`** — their followable content already lives in
`source_paths` and, on a structure page, in `resource`. And `sources[]` **makes no currency claim of
any kind**: nothing diffs it, nothing reads it for staleness. That restriction is what lets it exist
beside `source_paths` without recreating two mechanisms computing different answers, and lint check 9
is what discharges it, by requiring every in-repo source to be watched by a glob.

Keys are **`id`, `title` and `resource`, and nothing else**, written in flow style, one line per
source. `id` is a lowercase kebab slug — `adr-0015` for an ADR, `context` for the glossary —
conventionally stable across pages so a cross-page grep works, and lint-enforced unique only *within*
a page. Where an `id` names an ADR, lint checks that its `resource` is that ADR: the stable id and
the fragile path sit one line apart, which is the whole point, and a check that they agree costs
nothing.

`sources[]` **replaces the former `decisions:` field and the body `## Sources` citation list.** An
entry's `id: adr-0015` *is* the bare, rename-proof cite `decisions:` was, now one line from the path
it names instead of two hundred. What it buys over the old bullet is addressability: the old ADR
bullet packed four or five links into one comma list, so **no individual ADR was addressable at all**.

Required with ≥1 entry on the compiled types. **Permitted but not required on `note`** — a note is
primary evidence rather than a derivation, the same reason notes are already exempt from checks 4, 7
and 9, and requiring provenance of one would invert what a note is. Absent from `index.md`.

**Credibility signals (§5.1) are all declined**, on four separate grounds. `usage_count` is
*undefined* here rather than untested: it means query executions in a data catalog, and the nearest
in-repo analogue — commit touch count — is a churn measure, the opposite of the adoption signal
intended. `usage_window` exists only to frame it. `author` has zero discriminating power in a
single-maintainer repo. And `last_modified` is the sharpest one: it is git-derivable per source and
therefore the one that *looks* affordable, but it is a stored recency claim standing beside
`last_compiled` + `source_paths`, which already answer "has this source moved?" **by diff rather than
by date** — a second, weaker currency mechanism, which is exactly the review-by date §7 rejects.

### What `source_paths` must contain

`source_paths` is the **single** field that answers "has the world moved?" — the sweep's dirty set
(§2a of the sweep skill) and the reader's staleness check (§5) both read it and nothing else. A
source that shapes a page but isn't listed here is invisible to *both*, in the same direction. Three
rules follow.

**1. Every document in `sources[]` is also a glob here.** ADRs are ~4,500 words per page — the
dominant input, larger than the code read — and until this rule they were watched by nothing at all:
an ADR-only commit dirtied no page, and the reader agreed, because it reads the same field. Write one
`docs/adr/<nnnn>-*.md` glob per cited ADR. The wildcard tail matters: `ADR-0011` is stable,
`0011-role-appointment-authority.md` is one rename from broken, which is the same reason §8 cites
ADRs bare in prose.

The duplication with `sources[]` is deliberate and **lint-enforced** (check 9). A second frontmatter
*axis* was rejected: the reader's check is one `git diff` over one field, so a sweep-only ADR axis
would leave the two mechanisms computing different answers again — exactly the failure this rule
exists to repair. `docs/adr/**` on every page was rejected too: across all 16 real ADR commits the
per-ADR globs fan out to a **median of 1 page** (max 6, and that was the ADR-0015/17/18/19/20
taxonomy commit, which genuinely governs all six backend units), where a blanket glob would dirty
every page every time.

**2. `CONTEXT.md` is on every compiled page.** The glossary is the vocabulary these pages are written
in. Precision was not bought here — unlike ADRs there is no existing field mapping a term to the
pages using it, and inventing one would be new machinery for an event that has occurred **3 times in
199 commits**. Blanket over-firing costs ~2 spurious verdicts a year. Notes are excluded: their
`source_paths` mean *what could falsify this*, and a reworded glossary entry falsifies no trap. This
rule was mandated by the contract and enforced by nothing until check 9 generalised over `sources[]`,
which now closes it for free.

**3. Production source and the manifest — never the test tree.** A structure page describes layout,
routes, ports and tables; a test-only change cannot move a sentence in it. The container's
`apps/backend/application-container/**` fired on 43 of 60 backend commits, **39% of them on
`src/test/**` alone**, because every context's feature PR touches the shared integration suite. So
name `<unit>/*/src/main/**` (or `<unit>/src/main/**` where the unit has no ring submodules) plus the
`pom.xml`s — the manifest is load-bearing, since it is where a dependency edge appears.

> **Write globs that `git` accepts, not that Python does.** Git's pathspec `**` matches **one or
> more** path components; Python's `glob` matches **zero or more**. So
> `identity-service/**/pom.xml` finds 6 files under git and 7 under Python — silently missing the
> aggregator pom — and `common-domain/**/src/main/**` matches 42 files under Python and **nothing**
> under git. Git is the operative engine: both the sweep and the reader run `git diff`, so lint
> resolves globs with `git ls-files` for exactly this reason. Spell the levels out:
> `<unit>/*/src/main/**` and a separate entry for the aggregator.

### `note`

```yaml
---
type: note
title: Persistence Gotchas
description: Traps in the JdbcClient persistence layer that cost a real debugging cycle.
bears_on: [backend-sabha, persistence]          # bare stems
source_paths: [apps/backend/**/dataaccess/**]   # what could FALSIFY this
last_verified: <full-sha>       # HEAD at authoring time
status: disputed                # optional; `deprecated` is FORBIDDEN here — see §6
---
```

`source_paths` carries **different semantics** on a note: for a compiled page it is *what this was
derived from*; for a note it is *what could falsify this*. Same field name deliberately — the
staleness check (§5) and lint checks 3 and 6 then apply uniformly, with no second code path.

`last_verified` is the note's checkpoint, set to `HEAD` at authoring time (the author cannot know
the merge commit). The imprecision is bounded by the PR's lifetime and errs toward reporting dirty.
Despite the name it is **not** OKF §5.2's `verified[]`, which records confirmation events by actors
who did not write the content. This is the note's `last_compiled`: an invalidation checkpoint,
nothing more. Notes converge on OKF's *freshness* model, not its trust model.

`bears_on` and `appears_in` take **bare stems, unlinked.** The argument that wins §8's body links —
GitHub renders them — has no purchase in frontmatter, which is machine-read and never rendered; a
relative path here would be the fragile half with none of the payoff. `sources[].id` is the exact
precedent: a bare join key sitting beside a `resource` that carries the path.

### `index.md`

```yaml
---
okf_version: "0.2"
---
```

**Exactly one key, and only at the bundle root.** OKF §11 clause 3 requires the root to follow §8,
which permits no page-level frontmatter there, so `index.md` carries no `type`, no checkpoint and no
count. `okf_version` is the single exception and is adopted despite appearing **zero** times in any
reference bundle: it survives the test that killed `last_modified` and `generated.by`, because those
made claims git already made better while this makes a claim nothing else makes **at the front door**.
It is also the one field here that cannot drift, since the spec version is fixed by the migration
rather than by the world. `protocol.md` says it too, but `protocol.md` is long and `index.md` is
where a cold agent lands.

`index.md` carries **no global currency claim** — checkpoints are per page (§5). The former `pages:`
count is gone: a number restating the length of a list three lines below it is the purest form of the
second copy this contract keeps deleting.

---

## 4. Skeletons

Sections are **fixed per type, in order**. An **empty section is written `_none_`, never dropped** —
to a cold agent an absent `Talks To` reads identically as "calls nothing" and "nobody looked".

Every prose section on a compiled page carries a coverage tag (§5) as its **first non-empty line**.
`Method` is exempt: it states *how the page was compiled*, which is no more a derivation from
evidence than evidence is.

`docs/wiki/structure/backend-identity.md` is the **reference instance** of the `structure` skeleton —
where a detail below is arbitrary (section order, tag placement), that page is the tiebreak.

### structure

```markdown
# <Unit Name>

## Purpose        <!-- 1–2 lines: what this unit is responsible for -->
## Layout         <!-- ring module -> what lives in it; plus a feature-package line
                       on large units. See [module-ring](../patterns/module-ring.md). -->
## Exposes        <!-- route PREFIXES only; the /api/* mobile vs /bff/* web split.
                       Individual endpoints belong to the feature dossier. -->
## Talks To       <!-- two labelled halves: **Outbound** (target context, port, protocol)
                       and **Inbound** (common-domain ports this unit IMPLEMENTS for others) -->
## Data           <!-- two labelled halves: **Owns** (this unit writes it) and
                       **Reads** (it queries someone else's). See the migration rule in §8. -->
## Gotchas        <!-- module-local only; cross-cutting -> notes/.
                       Compiler-derived note back-links land here (§7). -->
## Covered by     <!-- feature-page backlinks; structurally `_none_` until dossiers exist -->
## Method
```

The two halves of `Talks To` are **bold labels inside the one section**, not `###` subsections —
lint's skeleton check reads `##` headings, so keeping them out of the heading tree means the section
list stays exactly the eight above on every page.

`Talks To` splits Outbound/Inbound because these units are **peers that serve each other**: identity
has 2 outbound edges against 9 common-domain ports it implements. What a context *provides* is
often the load-bearing fact. Inbound duplicates another page's outbound — accepted, because making
a reader reconstruct nine edges by grepping port names defeats the point of the page.

`Data` splits Owns/Reads under the **same bold-labels-inside-one-section** convention as `Talks To`,
for the same reason — the section list stays exactly the eight above on every page. The split is not
cosmetic: it is what §8's migration rule fires on, and it is the fix for this skeleton's known weak
point. `Data` is where **every** confidently-wrong claim so far has landed, because ownership is
inferred from adapter SQL and blurred prose let "this unit touches the table" and "this unit owns
the table" be written as the same sentence. Forcing each table under one label or the other makes
the claim explicit and therefore falsifiable — which is how `home_sabhas` was caught pointing the
wrong way.

A unit that neither owns nor reads a table simply doesn't list it. `backend-container` is the
instructive case: it owns **no table's data** and reads none, so both labels are `_none_` and its
prose carries the schema-custody fact instead — it holds the DDL for all 19 tables, which is a
different axis from data ownership and must not be written as `Owns`, or "who owns `users`?" gets
two answers and the blur is back.

`Layout` keeps the ring table so all pages stay comparable, but the ring is identical everywhere by
ADR-0015/0019 and would be factored into a `module-ring` pattern page — so on a large unit it gains a
**feature-package line**, which is the real navigation axis there. Small units simply have no such
line.

### `## Method` — the section that is not a citation list

The final section on every compiled page is a **method statement**: how this page was compiled, and
where the next compiler should look first. It is not provenance-as-artifact — that moved wholesale
into `sources[]` (§3) — and this is the reason the section survived the move at all.

The three bullets it replaced were an ADR line, a `CONTEXT.md` line and a **code line**, and reading
the code lines together shows what they always were:

> "Class listing + writer/reader SQL grep over `analytics-service/**`"
> "Import scan across all six backend units — the evidence for both halves of Talks To"
> "seven docblocks; the highest-yield source on this page"

That is a method plus a **yield judgement**. OKF has no field for it anywhere, and it is the single
highest-value line on the page for a recompiling agent. So OKF §13.1's supersession cleanly claims
the first two bullets and does not reach the third.

Two consequences. `## Method` **names globs and bare filenames**, which are not followable, so lint
path-checks only genuine markdown links inside it. And the glossary term list that used to trail the
`CONTEXT.md` bullet (`— Nirdeshak, Sanchalak, Nirikshak, Kshetra`) lives on in the `context` entry's
`title`, where it is still greppable.

### feature

```markdown
# <Capability Name>

## What it does      <!-- user-facing, in CONTEXT.md's language -->
## Flow              <!-- per app: mobile/web -> BFF -> context -> data -->
## Rules & authority <!-- who may, what is rejected and with what code -->
## Where the code is <!-- structure-page links; no code detail restated -->
## Amendments        <!-- what changed, when, which issue, why -->
## Method
```

### pattern

```markdown
# <Pattern Name>

## The pattern       <!-- stated once, for the whole repo -->
## Why               <!-- decision trail; ADRs cited by number, never restated -->
## Where it appears  <!-- per-page instances and how each differs -->
## Deviations        <!-- known exceptions -->
## Method
```

### note — never compiled

```markdown
# <Theme>

## <fact title>
**Symptom** · **Cause** · **Fix** · **Discovered** (date + issue/PR)
```

Notes have **no fixed section list** (a theme accretes facts) and **no coverage tags** (§5).

### index.md

Sections are fixed and ordered — `Start here`, `Structure`, `Features`, `Patterns`, `Notes` — and
lint checks that, which is how OKF §11 clause 3's *"the root follows §8"* is asserted here. §8's
normative sentence asks for *sections grouping concepts under a heading*, and this satisfies it; §8's
`* [Title](url) - description` **bullet** block is a fenced example and is **declined** as a producer
extension. The catalog's load-bearing invariant is **every page exactly once — that is what makes
orphan detection possible**, and a table makes that column-checkable where a free-prose tail does not.

The **Start here** router is a **fixed 8-question list**, itself a declared producer extension (§8 has
no view on a router; two of these rows point *out* of the bundle entirely). The compiler fills only
the target cell — it never re-judges the questions. Amend the list here, in `protocol.md`, if it
proves wrong.

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
5, 6 and 7 point at a page; until that page exists the compiler points the row at its type's
catalog anchor instead, so the router never carries a dangling link. That idiom — **a catalog anchor
as the forward reference** — is what makes §8's strictness liveable, and is the sanctioned way to
point at a page that does not exist yet.

Then one catalog table per type, listing **every page exactly once**, and **derived with zero
re-judgement** from the pages' own frontmatter. Every table is three columns:

| Type | Column 1 | Column 2 | Column 3 |
|---|---|---|---|
| `structure` | the page link | `resource` | `aliases`, comma-joined |
| everything else | the page link | `description` | `aliases`, comma-joined |

The middle column is `resource` **or** `description` because `resource` is structure-only (§3) — so
the tables come out the same width for the same reason. §8's *"SHOULD include the description"* is
honoured exactly where the cell holds one, and **declined on the structure table**: a fourth column
would sit beside `dashboards, audit log, re-engagement` as one more second copy.

Because the catalog is a pure function of the pages, **a wrong catalog is a frontmatter bug**, and
lint check 2 asserts cell fidelity rather than merely membership.

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
this, which ADRs govern it, which pages to follow, what the gotchas were — all still usable. But **no
specific claim** (table names, file counts, port lists, route prefixes) may be repeated to a user
without verifying it against source. Stale ⇒ void was rejected: one `.sql` file landing would throw
away thousands of words of still-true ADR synthesis.

**`status: deprecated` is that demotion made permanent and unconditional.** It does not sit beside
stale; it sits one rung below it, and it is the only page state a recompile can never recover. On a
deprecated page the diff above is meaningless — the globs match nothing, so it comes back *clean* —
which is why deprecation must suppress the currency claim itself rather than merely exempt a lint
check. See §6.

Checkpoints are **per page**, not one global SHA, so a budget-limited partial sweep records real
progress and a reader checking one page pays for one page.

**OKF §5.5's `stale_after` is declined.** It is an absolute calendar instant, and every value in the
reference corpus is one annual policy-review date: *it is not an alternative to diffing
`source_paths`, it is what you reach for when there is nothing to diff.* A data catalog has no commit
history for the thing it describes; we do. Checked rather than assumed: all 112 `source_paths` globs
in this wiki are in-repo, `pom.xml` and the Flutter manifests are watched so third-party bumps are
in-repo events, and §7 routes environment facts out of the wiki entirely. Revisit only if a page ever
needs to make a claim about something outside this repo.

### Axis 2 — coverage tags: was this well-sourced when written?

**Coverage tags are a producer extension, and OKF has no opinion on them.** Stating that here is the
point: OKF's trust family is per-*concept*, and this wiki's unit of trust is the **section**, because
that is the unit at which the compiler's sourcing actually varies. A future reader must not
"reconcile" the two by deleting one.

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

### Recording a human verification, in the tag

Today only the *negative* compounds: a reader who checks a claim and finds it wrong has `disputed`,
while a reader who checks it and finds it **right** has nowhere to put that, so the next reader
repeats the check. That asymmetry is a live cost, and it is fixed here rather than by adopting OKF
§5.2's `verified[]` — which is per-*page*, and a page-level "verified" claim on a page whose `Data`
is `low` asserts something nobody checked.

So the positive check goes in the tag's existing free-text reason slot, at the grain the reader
actually works at:

```
<!-- [coverage: high -- verified 2026-08-31 by human:jaymin against SabhaJdbcAdapter; compiled medium] -->
```

**Verification raises the level to `high`.** Levels are defined by reader obligation, so once a human
has checked the claim against source, *"trust it, don't open the source"* is simply the true
obligation; refusing to raise it knowingly overstates the reader's burden. The mixed signal — `high`
now means either "well-sourced compile" or "human-checked" — is what the mandatory reason resolves,
which is why it carries the compiled level (`compiled medium`) alongside.

**On recompile the sweep demotes it, loudly, and only where the prose actually changed.** A sweep
that rewrites the section restores its own honest level and moves the verification into the reason as
history — `[coverage: medium -- shape from adapters; human:jaymin verified at 09fb207, prose has
since changed]` — and reports the lapse in its PR body. A sweep that leaves a section byte-identical
**must leave the verification standing**, or the wipe becomes indiscriminate and every sweep quietly
discards human work.

**Lint asserts nothing about this, on purpose.** A check could assert a verification claim's *shape*
but never that one happened, so it would catch typos while creating a perverse incentive: a reader
whose honest reason doesn't fit the grammar writes a conforming lie, or skips the raise. Same
reasoning that leaves `title` unpinned to the H1. `grep -rn "verified .* by human:" docs/wiki/` finds
every instance.

**OKF §5.2's `generated` is declined** along with `verified[]`. Its value in the reference bundles is
that tooling *stamps* it, and no such mechanism exists here — the sweep is prose an agent follows,
and lint can assert presence but never truth, so an agent hand-editing a page could type
`process:wiki-sweep` and lint would applaud. The decisive reason is the same one that killed
`last_modified`: `git log --format='%an' -- <page>` already answers "sweep or human?" unforgeably, so
a stored `by` is a weaker copy of something git does better. **§5.3's trust tiers** die with both
inputs — and a reworked roll-up over coverage tags was rejected on its own demerits too, since every
page would roll up to its `Data` section and read `low`, telling a reader to distrust six sound
sections.

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
wrong or that the fix is right. So is a demoted verification.

---

## 6. `status`: the two stored states

`status` takes **`disputed` or `deprecated`, and they are mutually exclusive.** One key, not two, and
the exclusivity is the feature — look at what each one *instructs*:

- **`disputed`** — *unconditionally dirty: recompile even though nothing moved.*
- **`deprecated`** — *never recompile: the subject is gone.*

They are directly opposed on the one axis either of them drives, so a page asserting both would be
incoherent and the sweep would have to break the tie arbitrarily. Two keys would buy only the ability
to express a contradiction.

**`disputed` is a deliberate producer extension on a spec key.** OKF §5.4's vocabulary is
`draft | stable | deprecated`; §11 constrains only `type`, and §5.4 is a SHOULD, so this is
conformant. It is stated out loud here for the same reason coverage tags are (§5): so no future
reader reconciles it by deletion. The usual objection — *a consumer would read `disputed` as
`stable`* — is void by this bundle's own premise: there is no consumer.

### `disputed`

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

It stays **page-level** while the positive verification marker (§5) is per-section, and the asymmetry
is deliberate: the two have different consumers. The positive marker is read by humans at read time,
so it lives where reading happens. `disputed` drives the sweep's dirty set, so it must be findable in
frontmatter without parsing bodies — and the reader who sets it is often the one least able to
localise the fault. A reader who *can* localise it should say so in `disputed_reason`, as a courtesy.

### `deprecated`

**The subject this page describes no longer exists.** Adopted from OKF §5.4 because the alternatives
were worse than they look. When a build unit is deleted the incumbent menu was: delete the page, or
edit the globs. Deleting **cascades** — 79 in-wiki links across 14 pages, with `mobile-shell` alone
carrying 13 inbound — so links go red in pages nobody was touching. Editing the globs to dodge that
is a lie no check can catch. `deprecated` is the third move: keep the page, keep inbound links
resolving, mark it not-current.

**It suppresses the currency claim, not just the checks.** §5's diff over globs that match nothing is
always *clean*, so exempting lint alone would promote a dead page to a permanent, silent "current"
verdict. A deprecated page makes no currency claim at all.

- **What it exempts: lint checks 3 and 6, and nothing else** — exactly the checks that assert the
  subject still exists. Coverage tags (4), the checkpoint (5), the skeleton (7) and the word budget
  (8) stay enforced, or the page becomes a rot pocket that drifts out of shape while still resolving
  thirteen inbound links. `last_compiled` stays required: it is free, and it is history.
- **Human-only, in the PR that deletes the subject** — §7's eviction rule verbatim: *same moment,
  same reviewer, no new step.* **The sweep must never author it.** Not merely on marking-versus-
  authoring: the sweep is the one actor that **cannot** distinguish "this unit was deleted" from
  "these globs are wrong". The human deleting the module knows; the compiler is guessing.
- **`deprecated_reason` is required** — one line naming what was deleted and in which PR.
- **Never cleared.** There is no un-deleting, so unlike `disputed` it has no clearing path and the
  sweep must not look for one. The terminal state is a human deleting the page once nothing links to
  it, and there is **no lint check for that**: a check could only nag, and a dead page everyone can
  see is pressure enough.
- **Permitted on the three compiled types; forbidden on `note`.** This is the one field this contract
  *forbids* on notes rather than merely not requiring there, and the prohibition is load-bearing: §7
  already evicts a note by **deleting** it, and the link cascade `deprecated` exists to prevent does
  not exist for notes, since nothing links to one except the back-links the sweep derives into
  `Gotchas` — which the same PR removes. Permitting it would reopen a path §7 spent a paragraph
  closing.
- **`index.md` keeps the page**, with a `(deprecated)` marker in the `Page` cell and the now-dead path
  still in the `Unit` cell. Removal was never available (§4's *every page exactly once*), and the
  catalog already leaks the fact illegibly — the marker turns a puzzle into an explanation. Because
  the catalog is derived (§4), lint check 2 asserts the marker is there.

### Dropped, deliberately

`stale` (computed — §5), `orphaned` (lint check 6's output, recomputed free), `current` (the absence
of the other two), and **`draft`** — which OKF §5.4 offers and which stays dead: it is used **zero
times in the entire reference corpus**, and this wiki's 96 coverage tags already say "provisional" 96
times, at a granularity a reader can act on. A page-level `draft` would be the roll-up §5 already
rejected.

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
So the sweep **derives** a link to the note into a compiled page's `Gotchas` by scanning `notes/*`
frontmatter for `bears_on` entries naming that page. Link, never restate.

An author may hand-add that back-link in the same PR (otherwise the note is invisible from its page
until the next hand-invoked sweep, possibly weeks). Adding one link line to an existing section is
**marking, not authoring**; the next sweep reconciles it.

This makes `bears_on` load-bearing, and therefore a real admission question at write time: *which
page would have to be open for this to save someone?* A note that can't name one probably fails the
trap gate too.

### Eviction — the PR that removes the trap deletes the note

Same moment, same reviewer, no new step. Obsolescence is the *default* fate of a good note: a trap
worth recording is a trap worth fixing, and this repo does fix them. A review-by date was rejected —
it fires on the calendar rather than on the event that falsifies the note, and OKF §5.5's
`stale_after` is declined on the same ground (§5).

Deletion, not deprecation: `status: deprecated` is forbidden on a note (§6).

The write-back trigger, the fix-green precondition and the PR-open timing live in
`docs/agents/wiki.md`.

---

## 8. Links, citations, and the migration rule

**One syntax, two namespaces distinguished by target.**

Every link is an ordinary markdown link, so it renders on GitHub — which is where every human reads
this wiki. The two namespaces the contract used to keep apart *syntactically* are now kept apart at
the **lint** layer, by where they point:

- **Inside the wiki** (lint check 1): the href is a **file-relative path** to another page, and the
  link text is **the target's filename stem**, exactly. `[backend-identity](backend-identity.md)`
  from a sibling page, `[attendance-marking](../features/attendance-marking.md)` across types.
  Inflection stays outside the brackets: `[web](web.md)'s`.
- **Out of the wiki** (lint check 3): relative markdown links, in `## Method` and in the
  machine-readable frontmatter (`source_paths`, `sources[].resource`, `resource`).
- **In prose:** cite **bare** — `per ADR-0011`, and glossary terms in `CONTEXT.md`'s exact wording,
  unlinked. **No anchors into `CONTEXT.md`**: it has no per-term headings, only five coarse `###`
  groups, so an anchor would be both imprecise and one rewording from broken.

**Why link text is pinned to the stem.** Markdown would permit `[the Flutter app](../structure/mobile-shell.md)`,
and that is declined. An agent greps `mobile-shell` and finds every mention in the corpus — a property
of the text being forcibly the stem, and the one capability free prose would *remove*. It also keeps
the whole namespace mechanically checkable.

**Why file-relative, never OKF §6.1's recommended `/`-absolute form.** The spec recommends it; its own
bundles abandoned it across 152 links because it 404s on GitHub, and the reference viewer dropped
support. Adopting it on the spec's authority would reproduce a bug its maintainers already fixed.

**Broken links fail the build, and OKF §6.1 does not object.** Its "consumers MUST tolerate broken
links" binds *consumers*; this repo is the producer, and a stricter producer is conformant. The
spec's substantive point — that a dangling link is a useful forward reference — is answered rather
than waved away, by §4's catalog-anchor idiom: a forward reference that is also a working link. And
the stakes moved in strictness's favour, because a dangling link is now a **404 on GitHub** rather
than inert text.

**OKF §5.1's footnotes are declined.** Keyed to `sources[].id` they would be well-formed, and §8's
rename-proofing argument genuinely does not defend against them. They lose on what they buy. Not
granularity: `per ADR-0011` is already per-claim, 152 times across these pages, against 64 cites in
the bullets they would replace. Not resolvability: `id: adr-0011` sits one line from its `resource`.
Only a rendered marker — and that marker jumps to a definitions block, leaving the reader **two hops**
from the source where an inline link is one and a bare id is zero. The asymmetry with the body links
is deliberate: there the link *is* the navigation, here it decorates a token that already navigates.

### The migration rule

Path intersection is the only invalidation rule for code. Migrations get a second, **content-based**
rule, because the changelog under `apps/backend/application-container/src/main/resources/db/changelog`
is central and partitioned by **slice/issue** — the `features/` axis — so no path glob on a
`structure` page can ever reach it:

> The sweep greps changed `.sql` files for table identifiers and dirties any page listing that table
> under **`Data` → Owns**. A table under **Reads** does not dirty the page.

Putting the changelog glob on all 13 structure pages was rejected: correct by construction, but one
one-table migration would then dirty all 13 pages on every batched sweep.

**The Owns-only restriction is the whole rule.** The original version fired on any table a page's
`Data` mentioned, and replaying it over all 15 real `.sql` commits showed it had never once done its
job: **12 of 12** DDL commits shipped alongside the owning context's own code, so path intersection
had already dirtied the owner and the grep added nothing. Of its 8 hits that path could *not* reach,
**7 were `backend-analytics` merely reading a table somebody else had changed** — noise on the one
context whose job is reading other people's data.

The 8th hit is the shape the rule is now aimed at, and the only shape that needs it: a
**cross-context column addition**. `ALTER TABLE users ADD default_city_id` was analytics' change to
identity's table; no identity code moved, so no glob reached `backend-identity`, and the grep was the
only thing that could. Under the Owns-only rule that replay yields **1 hit, and it is that one** —
identity owns `users`, analytics only reads it.

This is why the rule and the §4 skeleton split are one decision: without the Owns/Reads labels there
is nothing for the grep to discriminate on, and the rule reverts to the noise it was.

**Known gap, stated rather than hidden:** a migration introducing a **brand-new** table cannot dirty
a page by name, because no page lists it yet. Such a migration also dirties via its slice directory →
the owning feature dossier, which is where a new capability's schema belongs anyway. A calendar
backstop would not catch it either, which is part of why §5 declines `stale_after`.

---

## 9. Lint

`docs/wiki/lint` is a **deterministic, checked-in script — no LLM**. It is run by the sweep *and* by
CI, path-filtered on `docs/wiki/**`. CI because `notes/` is human-written, so most wiki PRs contain
no sweep at all, and a sweep-only linter would fire on that breakage weeks later inside an unrelated
PR where it looks like the compiler's fault.

An agent step was rejected: resolving every link, path and glob is a fully specified algorithm,
it is the category of work LLMs are least reliable at, and silently missing one broken link among
sixty is an ordinary agent failure and an impossible one for `test -f`.

The nine checks. Checks 1–7 and 9 are **failures** (exit 1); check 8 is a **warning** — printed in
the same `file:line` format, never affecting the exit code:

| # | Check | Scope |
|---|---|---|
| 1 | every in-wiki link resolves **and** its text is the target's filename stem | all types |
| 2 | filename stems are unique, and `index.md` lists every page exactly once, faithfully | all types + index |
| 3 | frontmatter conformance: OKF §11 clauses 1–2, the required fields, and `sources[]` | all pages |
| 4 | every section carries a coverage tag with a valid level | **compiled types only** |
| 5 | the checkpoint is present and is a real commit; `status`, if set, is `disputed` or `deprecated` with its reason | all pages, not `index.md` |
| 6 | each `source_paths` glob still matches ≥1 file under git — **the orphan check** | all types, incl. notes |
| 7 | skeleton conformance: fixed per-type sections, in order, empties written `_none_` | compiled types + index |
| 8 | prose word count is within the §2 budget — **warning, not failure** | compiled types |
| 9 | every in-repo `sources[].resource` is watched by a `source_paths` glob (§3) | compiled types |

Checks **3 and 6 are skipped entirely on a `status: deprecated` page** (§6). Every assertion those
two make is a claim that the subject still exists, which is exactly what a deprecation retracts.

**Numbers are stable, and that is why §11 has none of its own.** Check 9 was appended rather than
inserted so the existing numbers, cited across this file and the sweep skill, would not move — and
the OKF migration kept that discipline: conformance is asserted by **clauses 1 and 2 inside check 3**
(parseable frontmatter; `type` present, non-empty and one of the four) and **clause 3 across checks 7
and 3** (the index skeleton, and the rule that `index.md` carries nothing but `okf_version`). No
concept here is new enough to spend a number on.

**Check 8 must not be a gate.** §2 fixes size as a smell and never a trigger, so a check that failed
the build would overturn the contract it exists to report on. Notes are exempt with the other
compiled-type checks: they are primary evidence, not derivation, and have no skeleton to be long
relative to. This applies to the conformance assertions too — §11 is a **floor**, and a conformance
check must not quietly become a gate on something this contract never agreed to gate.

**Check 6 is load-bearing and unreachable by any other mechanism here.** A deleted path stops
matching `source_paths`, so `git diff ∩ source_paths` can *never* fire on it — lint is the only way
to catch a page whose subject no longer exists, which would otherwise stay `high`-tagged and
current-looking forever.

**Checks 6 and 9 resolve globs with `git ls-files`, not Python's `glob`.** This is not a style choice.
Git's `**` matches one *or more* path components where Python's matches zero or more, so a glob can
lint clean under Python and match **nothing** under the `git diff` that the sweep and the reader
actually run (§3). Resolving them the same way the consumers do is what keeps the linter honest. When
git is unavailable the script says so and degrades to Python semantics rather than failing.

**Check 9 is what keeps §3's two fields from drifting.** `sources[]` and `source_paths` name
overlapping documents, and hand-maintained duplication decays — silently, and in the direction that
matters, since a page can cite ADR-0011 for years while nothing watches it. It is scoped to compiled
types: a note's `source_paths` mean *what could falsify this*, so an ADR does not belong in them.

Check 5 verifies the SHA is a real commit only when git is available; in a shallow checkout it
degrades to a format check rather than failing.

Lint reads `index.md` plus every page under the four type directories. **`protocol.md` and `log.md`
are excluded**: this file documents the contract, so it quotes illustrative links and skeletons that
are not real.

**OKF §10 Attested Computation is declined**, and the structural parallel is close enough to be worth
naming: `lint` is deterministic, checked-in, no-LLM code that inspects an artifact and returns a
verdict — an **attester** — and the `wiki-sweep` skill is run instructions an agent follows, an
**executor**. §10.3's *"the agent MAY only supply values, it MUST NOT author the computation"* is the
same distrust §6 spells "marking, not authoring". It is declined because the *value* doesn't transfer:
attestation exists so a consumer can refuse to display a number an agent improvised, and this wiki
displays no numbers. Lint already runs unconditionally in CI, which is strictly stronger than an
attester a consumer *may* run.
