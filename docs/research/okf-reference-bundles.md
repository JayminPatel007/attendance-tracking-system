# How OKF's own reference bundles use the spec in practice

Research asset for [Research: how OKF's reference bundles use the spec in practice](https://github.com/JayminPatel007/attendance-tracking-system/issues/171),
a ticket on the map [Wayfinder: transform the LLM wiki into an OKF v0.2 bundle](https://github.com/JayminPatel007/attendance-tracking-system/issues/170).

Read on 2026-08-31, against `GoogleCloudPlatform/knowledge-catalog@main` (`okf/`) and its canonical
successor `GoogleCloudPlatform/open-knowledge-format@main`. Everything below is counted from the
checked-in files, not inferred from the prose.

## Sources read

| Source | What it is |
|---|---|
| [`okf/SPEC.md`](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/SPEC.md) (1,006 lines) | The v0.2 spec, including Appendix A's worked example |
| [`okf/README.md`](https://github.com/GoogleCloudPlatform/knowledge-catalog/blob/main/okf/README.md) (232 lines) | Framing; also the "this copy is frozen" notice |
| `okf/bundles/{acme_retail,crypto_bitcoin,ga4,stackoverflow}/` | **53 concept files, 24 `index.md`, 1 `log.md`, 4 `viz.html`, 1 `.py`** |
| `okf/bundles/acme_retail/attesters/sql_equality.py` (115 lines) | The only attester in the repo |
| `okf/src/reference_agent/` | The **producer**: agent, tools, prompts, index generator |
| `okf/src/reference_agent/viewer/` + each `viz.html` | The only **consumer** in the repo |

**A repository note that matters before anything else.** `okf/README.md` opens with: *"OKF now lives
in its own repository… **Stop using the copy under `okf/`**. It is a frozen snapshot."* I checked the
successor, [`GoogleCloudPlatform/open-knowledge-format`](https://github.com/GoogleCloudPlatform/open-knowledge-format).
Its `SPEC.md` is **byte-identical** (blob sha `c06e3ee` in both), it ships the **same four bundles**,
the same `src/reference_agent`, and adds only `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md` and a
`connectors/gcp-knowledge-catalog.md`. **No validator, no schema, no new bundle.** So every count
below holds for the canonical repo too, and the map is not reading a stale fork.

---

## 0. The headline count

53 concept files. Every top-level frontmatter key, counted:

| Key | Files | Spec status |
|---|---:|---|
| `type` | **53 / 53** | REQUIRED (§4.1) |
| `title` | **53 / 53** | recommended |
| `description` | **53 / 53** | recommended |
| `tags` | **53 / 53** | recommended |
| `generated` | **53 / 53** | §5.2 trust |
| `sources` | **49 / 53** | §5.1 provenance |
| `resource` | **47 / 53** | recommended |
| `status` | **10 / 53** | §5.4 lifecycle |
| `verified` | **8 / 53** | §5.2 trust |
| `stale_after` | **7 / 53** | §5.5 lifecycle |
| `runtime` / `parameters` / `executor` / `attester` | **2 / 53** | §10 attestation |
| `usage_window` | **1 / 53** | §5.1 |
| `not` | **1 / 53** | **not in the spec at all** |
| `okf_version` | **0** | §12 |

**The single most useful cut is per bundle**, because the four bundles are not four samples — they
are one hand-written demo and three machine-generated corpora:

| Key | `acme_retail` (9) | `crypto_bitcoin` (9) | `ga4` (9) | `stackoverflow` (26) |
|---|---:|---:|---:|---:|
| `type` `title` `description` `tags` `generated` | 9 | 9 | 9 | 26 |
| `sources` | 5 | 9 | 9 | 26 |
| `resource` | 3 | 9 | 9 | 26 |
| `status` | **9** | 0 | 0 | **1** |
| `verified` | **8** | **0** | **0** | **0** |
| `stale_after` | **7** | **0** | **0** | **0** |
| attestation family | **2** | 0 | 0 | 0 |
| `usage_window` | **1** | 0 | 0 | 0 |

**`acme_retail` is the spec's Appendix A wearing a bundle costume.** It was created whole in the
single commit [`780fe9d` "migrate format and tooling to OKF v0.2" (#227, 2026-07-24)](https://github.com/GoogleCloudPlatform/knowledge-catalog/commit/780fe9d),
the same commit that introduced v0.2 — it is a fixture written to exercise the new families, not a
corpus that accreted them. Strip it and you get the honest answer for **44 machine-generated pages
across three real bundles**:

> `verified`: **0 / 44**. `stale_after`: **0 / 44**. Attestation: **0 / 44**. `usage_window`:
> **0 / 44**. `status`: **1 / 44**. Credibility signals (`author`, `usage_count`,
> `last_modified`): **0 / 55 source entries**.

The generated bundles carry exactly seven keys and nothing else: `type`, `title`, `description`,
`resource`, `tags`, `generated`, `sources`.

**And the producer explains why, mechanically.** `src/reference_agent/prompts/reference_instruction.md`
is the instruction the authoring agent follows. Its "Frontmatter (YAML)" section documents `type`,
`title`, `description`, `resource`, `tags`, `status`, `generated`, `sources` — and **never mentions
`verified`, `stale_after`, `usage_window`, or any credibility signal.** They are absent from the
bundles because nothing asks for them. `bundle_tools.py`'s `_PREFERRED_KEY_ORDER` *lists* all of
them, so the writer would serialize them in order if they ever appeared; the prompt never produces
one.

This is the map's first hard datum: **§5's trust and lifecycle families have never been exercised by
an agent maintaining a corpus.** They exist in a spec and in one hand-written fixture. Our wiki-sweep
would be the first producer of `verified`/`stale_after` in the wild, which means "the spec says so"
carries roughly zero operating evidence behind it.

---

## 1. Which §5 families are actually written

**`generated` — universal, and it is the *only* trust field that is.** 53/53. Every entry uses the
§7 actor convention, all four models are agent actors (`reference_agent/gemini-2.5-pro`,
`reference_agent/gemini-2.5-flash`, `reference_agent/gemini-3.5-flash`). Crucially it is **written by
the tool, not the agent**: `bundle_tools.py` stamps `generated.by = reference_agent/<model>` and
`generated.at = now()` when the agent leaves it unset. A field the agent cannot forget.

**`verified` — 8/53, all in `acme_retail`, all `human:jsmith@acme` on the same day (2026-07-01).**
One human, one sitting, one fixture. There is no example anywhere in the bundles of the multi-entry
form (`human:` + `process:`) that §5.2 motivates; there is no `process:` actor in any bundle. Both
list form and bare-mapping form appear (`acme_retail` uses the list dash), so the §11 "MUST treat a
bare mapping as one-element list" rule is exercised only by `document.py::normalize_verified`, not by
data.

**`status` — 10/53, two values used.** `stable` × 8 (all `acme_retail`, all redundant — §5.4 says
absent ⇒ `stable`), `deprecated` × 2 (`acme_retail/metrics/gross-margin-legacy.md`, and the one
generated instance, `stackoverflow/tables/stackoverflow_posts.md`). **`draft` is used zero times in
any bundle.** The only load-bearing use of `status` in the entire corpus is `deprecated`.

**`stale_after` — 7/53, all `acme_retail`, and all 7 are the identical instant**
(`2026-12-31T00:00:00Z`), justified in prose as "the cost-allocation standard is reviewed annually".
That is the giveaway: **every real `stale_after` in the corpus is an annual policy-review date**, an
external calendar obligation the doc inherits. Not one is derived from anything about the content.
This is direct evidence for the incumbent's §7 objection (a review-by date "fires on the calendar
rather than on the event that falsifies the note") — OKF's own bundles only use `stale_after` where
an actual calendar obligation existed to copy.

**`not:` — an undocumented extension, and the best idea in the corpus.**
`acme_retail/metrics/gross-margin.md` carries a key that appears nowhere in SPEC.md:

```yaml
not:
  - term: "revenue minus product cost only"
    why: "that is the pre-FY2026 definition (see gross-margin-legacy)…"
    instead: "revenue minus full COGS (product cost + inbound fulfillment + …)"
```

Structured negative knowledge — "the plausible wrong answer, and why". No consumer reads it (§7
below); it was written because §4.1's "producers MAY include any additional keys" invited it. Worth
noting for us not as an OKF feature but as evidence that the spec's extension point gets used for
exactly the thing a closed schema can't anticipate.

---

## 2. What a real `sources[]` entry looks like

**65 source entries across 49 files.** Key frequency:

| `sources[]` key | Entries | Share |
|---|---:|---|
| `resource` | 65 | 100% (REQUIRED) |
| `id` | 65 | **100%** |
| `title` | 63 | 97% |
| `author` | 9 | **14%** — all `acme_retail` |
| `last_modified` | 9 | **14%** — all `acme_retail` |
| `usage_count` | **1** | **1.5%** — one entry, `acme_retail` |

So the credibility signals §5.1 spends its longest passage on — the `usage_count` / `usage_window`
adoption-and-liveness machinery, the coarse-signal caveats, the "credibility is inferred not stored"
argument — are exercised by **one source entry in one hand-written file**. `usage_count` is the most
aspirational field in the spec by a wide margin.

**Resource forms, counted.** 57 / 65 are absolute URLs. **8 are in-bundle paths, all in
`acme_retail`, and all bare-relative-from-bundle-root without a leading `/`**:

```yaml
sources:
  - id: revenue-policy
    resource: policies/revenue-recognition.md      # not /policies/... ; not ../policies/...
    title: Revenue Recognition Policy (FY2026)
    author: human:jsmith@acme
    last_modified: 2026-06-15T00:00:00Z
```

Note the shape when the source is a file in the same bundle: `author` becomes a **person or team**
(`human:jsmith@acme`, `team:data-platform`) and `last_modified` becomes **when that in-repo file last
changed**. `usage_count` is dropped — you can't count query executions of a policy doc. That is the
closest the corpus gets to our situation, and it is 8 entries deep.

**Zero source entries use a scope descriptor** (§5.1's `all queries in BigQuery project X`). It
appears only in SPEC.md.

**Footnote attribution is real, but half of it is broken.** Counting inline `[^id]` citations vs
`[^id]:` definitions:

| Bundle | Files with footnote defs | Files with inline citations | Defs | Inline cites |
|---|---:|---:|---:|---:|
| `acme_retail` | 5 | 5 | 8 | 14 |
| `crypto_bitcoin` | 4 | 4 | 5 | 8 |
| `ga4` | 9 | 9 | 10 | 11 |
| `stackoverflow` | 11 | **2** | 11 | 12 |

**Nine `stackoverflow` files define a footnote that nothing cites, and its label is `1`** — a bare
positional number that joins to no `sources[].id`. Example, `references/joins/posts__votes.md`:

```markdown
[^1]: Verified from [Database Schema Documentation](https://meta.stackexchange.com/…) on Meta Stack Exchange.
```

while its frontmatter declares `id: meta_schema_doc`. The join key the spec is careful to argue for
(§5.1: "labels are keyed rather than positional… a positional index misattributes silently") is
**exactly what the agent regressed to** when left to itself. And in `ga4`, all 7 metric pages place a
bare `[^sample_queries]` on its own line after a code fence — a *document*-level citation shaped like
a per-claim one. `acme_retail` is the only bundle where footnotes genuinely attribute individual
claims, and it is the hand-written one.

Verdict for Q2: `id` + footnote attribution **is** exercised beyond the spec's example, but it
degrades under agent authorship in a specific, predictable way — toward positional labels and
document-level placement. If we adopt it, that failure mode is the thing lint has to catch.

---

## 3. `type` values in the wild

**Seven distinct values across 53 files.** All **Title Case with spaces**; none is a slug, none is
lowercase, none is hyphenated.

| `type` | Files | Bundles |
|---|---:|---|
| `BigQuery Table` | 22 | all four |
| `Reference` | 20 | crypto, ga4, stackoverflow |
| `Metric` | 3 | acme_retail |
| `BigQuery Dataset` | 3 | crypto, ga4, stackoverflow |
| `Attested Computation` | 2 | acme_retail |
| `Policy` | 2 | acme_retail |
| `Skill` | 1 | acme_retail |
| *(`Log`)* | *1* | *`acme_retail/log.md` — see §5* |

**`Reference` is the escape hatch, and it is 38% of the corpus.** In the three generated bundles it
absorbs join paths, enum lookup tables, metric definitions, and license notes — four genuinely
different kinds of page filed under one word because the agent had no better one. This is precisely
the failure the map's settled position predicts: an open `type` lets "should this be a new page?" be
answered by not deciding. OKF's own reference corpus is the exhibit.

Also note `type` tracks the **producer's source system**, not the reader's question: `BigQuery Table`
/ `BigQuery Dataset` name where the metadata was scraped from.

**What keys off `type` mechanically — exactly two things, both in the producer's own tooling:**

1. **`index.md` section grouping.** `bundle/index.py::_build_index_text` groups entries by `type` and
   emits `# {type}` as the section heading verbatim, sections sorted alphabetically. So the `type`
   string *is* the index heading. This is the one place the value has to be human-presentable.
2. **Viewer node colour + filter.** `viewer/generator.py` holds a three-entry `_TYPE_PALETTE`
   (`BigQuery Dataset`, `BigQuery Table`, `Reference`) and falls back to grey; `viz.js` populates a
   type dropdown filter and shows `type` as a chip. `viz.js` also **hardcodes `type === "BigQuery
   Dataset"` as the node to auto-open** when the viewer loads.

Nothing else routes on `type` — including `Attested Computation`, which §10.5 says a consumer
"discovers via `type: Attested Computation`". No code in the repo does that.

---

## 4. `index.md` in practice

**24 `index.md` files. Not one carries frontmatter. `okf_version` appears zero times in the entire
`bundles/` tree** — including all four bundle roots. §12's version declaration is unused by its own
reference bundles.

**They follow §8's section+bullet form exactly**, because they are **machine-generated** —
`regenerate_indexes()` in `bundle/index.py` rewrites every directory's index from the frontmatter of
its children. Only `# ` headings appear (25 of them, zero `##`); bullets are `* [Title](link) - desc`.

**Descriptions are lifted verbatim from frontmatter `description`** — the producer prompt even says
so: *"This is used verbatim in auto-generated `index.md` files, so keep it tight."* The one exception:
a **subdirectory** entry's description is LLM-synthesized from its children by
`synthesizer.py::synthesize_description`, then rolled up (unless the directory has exactly one child,
in which case that child's description is reused).

**The section headings are the `type` values**, plus one synthetic pseudo-type:

```
7 × "# Subdirectories"   6 × "# Reference"      4 × "# BigQuery Table"
3 × "# BigQuery Dataset" 1 × "# Skill"          1 × "# Policy"
1 × "# Metric"           1 × "# Attester"       1 × "# Attested Computation"
```

`# Attester` is a tell: `acme_retail/attesters/index.md` lists `sql_equality.py`, a **`.py` file**.
`index.py` only walks `.md` children, so that entry cannot have been generated — the whole
`acme_retail` index set was hand-written to look like generated output.

Two things this settles for us. First, **§8's "section" has no independent meaning** — a section is a
`type` bucket, so index structure is a projection of the taxonomy, not a second organizing axis. Our
`index.md` is organized by *reader question* (protocol §4), which §8's form can express (the heading
is free text) but which no OKF producer does. Second, **losing `kind: index` / `pages: 33` costs
nothing that anyone in this corpus is buying** — nobody declares `okf_version` either.

---

## 5. `log.md` in practice

**One `log.md` in four bundles**, `acme_retail/log.md`. Git history is decisive:

| Commit | Date | What |
|---|---|---|
| [`780fe9d`](https://github.com/GoogleCloudPlatform/knowledge-catalog/commit/780fe9d) | 2026-07-24 | created, with the rest of the bundle |
| [`62432a0`](https://github.com/GoogleCloudPlatform/knowledge-catalog/commit/62432a0) | 2026-08-21 | mechanical sweep: "make every timestamp an ISO 8601 datetime with an explicit offset" |

**Written once at bundle creation, then touched only by a repo-wide format sweep.** Its four entries
are dated 2026-02-10 → 2026-07-01, i.e. backdated history invented for a fixture that was created in
one commit on 2026-07-24. There is no evidence anywhere in this repo of a `log.md` being maintained.

Two conformance wrinkles worth recording:

- **It carries frontmatter** (`type: Log` / `title:`), which §9 neither shows nor sanctions, and
  which §3.1 arguably forbids by making `log.md` reserved. §11's rule 1 says "every **non-reserved**
  `.md` file contains parseable frontmatter", so this is extra, not required.
- **The viewer ingests it as a concept.** `generator.py::_walk_concepts` skips only `index.md`, never
  `log.md`. So `acme_retail/viz.html` ships **10 nodes for 9 concepts**, the tenth being the log,
  with `type: "Log"` in the type filter. The only consumer in the repo does not honour §3.1's
  reserved-filename rule.

Verdict for Q5, plainly: **the incumbent's reserved-and-unused `log.md` is in exactly the same state
as OKF's.** OKF offers no evidence that giving it a job is a solved problem — it offers evidence that
the format's own flagship bundle wrote one, backdated it, and never came back.

---

## 6. Is there a validator or schema?

**No schema. No JSON Schema, no linter, no `validate` CLI command, no CI conformance job — in either
repo.** The entire enforcement surface is 5 lines in `bundle/document.py`:

```python
# OKF v0.2 §11: `type` is the only always-required frontmatter key.
REQUIRED_FRONTMATTER_KEYS = ("type",)

def validate(self) -> None:
    missing = [k for k in REQUIRED_FRONTMATTER_KEYS if not self.frontmatter.get(k)]
    if missing:
        raise OKFDocumentError(f"Missing required frontmatter keys: {', '.join(missing)}")
```

Called from exactly one place — `bundle_tools.py`'s write path, i.e. it is a **producer guard, not a
bundle checker**. It implements §11's clause 2 only. Clause 1 (parseable frontmatter) is implicit in
`OKFDocument.parse` raising; **clause 3 (`index.md`/`log.md` follow §8/§9) is checked by nothing**,
and as §5 above shows, is violated by the repo's own fixture and consumer.

**But three pieces are worth borrowing verbatim**, and they are the real answer to "borrow rather
than invent". All in `bundle/document.py`:

- `normalize_verified(fm) -> list` — the bare-mapping-is-a-one-element-list rule (§11's one MUST).
- `trust_tier(fm) -> "unverified" | "machine-confirmed" | "human-reviewed"` — 8 lines, keys off the
  `human:` prefix, exactly §5.3.
- `is_stale(fm, now) -> bool` — with a decision we should copy outright: *a date-only `stale_after`
  is **ignored**, not guessed at*, "because `2026-12-31` names a different instant in every
  timezone."

Two more producer-side ideas, both `bundle_tools.py`, both better than anything in SPEC.md:

- **A monotonicity guard on `sources`**: a write is *refused* if the new frontmatter has fewer
  `sources` entries than the file it replaces ("Merge your new source with the existing one"). A
  direct, mechanical defence against an agent silently dropping provenance on recompile. Our sweep
  has the identical exposure.
- **The `generated` stamp is applied by the tool**, never by the agent. If we adopt `generated`, the
  same rule applies: the compiler stamps it, the page author never types it.

Also worth knowing for the `[[slug]]` decision: `_LINK_RE = r"\]\(([^)\s]+\.md)(?:#…)?\)"`. The
consumer's graph is built from **markdown links to `.md` files**. It has no wikilink support at all
— **`[[...]]` appears zero times in all four bundles.** If we keep `[[slug]]`, no OKF consumer
resolves our edges; given the map's "there is no consumer" premise, that is a cost of exactly zero,
but it should be stated rather than discovered.

---

## 7. What `viz.html` reads — the de-facto field list

`viz.js` + `generator.py` are the only consumer. Fields **actually read and rendered**:

| Field | How it is used |
|---|---|
| `type` | node colour, type dropdown filter, detail chip, auto-open heuristic |
| `title` | node label (falls back to concept id) |
| `description` | detail pane |
| `resource` | detail pane, rendered as an outbound `<a>` |
| `tags` | detail chips, **and free-text search matches against them** |
| `status` | badge; `node[status = "deprecated"]` gets its own cytoscape style |
| `verified` | rendered as actor/date list, **and reduced to a `trust_tier` badge** |
| `generated` | rendered as one actor/date line |
| `stale_after` | badge — `stale (since X)` if `is_stale`, else `stale after X` |
| `sources` | list in the detail pane, using **`title` ‖ `resource` ‖ `id`** |
| body | rendered; markdown `.md` links become graph edges |

Fields present in the spec and **read by nothing**: `sources[].author`, `sources[].usage_count`,
`sources[].last_modified`, `usage_window`, `runtime`, `parameters`, `computation`, `executor`,
`attester`, `okf_version`, and `not`. Zero occurrences of any of them in `viz.js`.

That is a sharp answer to "which fields matter": **the consumer reads the seven base keys plus the
three trust/lifecycle keys, and nothing from the credibility or attestation families.** The
credibility signals are stored, shipped in the JSON payload (`sources` is passed through whole), and
never displayed. §10's attestation surface — the spec's longest section — has no consumer in the
repo, and `sql_equality.py` is **referenced by no code and no test**; `grep -rn "attester" src tests`
returns nothing. It is a demonstration artifact.

**One concrete failure the consumer makes visible.** The two `acme_retail/policies/*.md` pages carry
8 outbound links in §6.1's **recommended** bundle-absolute form (`](/metrics/gross-margin.md)`).
The viewer's `_extract_links` explicitly `continue`s on any target starting with `/`. Result: those
two pages contribute **zero edges** to `acme_retail/viz.html` (10 nodes, 6 edges).

This is not an accident, it is a reversal the project made and never propagated into the spec.
Commit [`4c40ef1` (#45)](https://github.com/GoogleCloudPlatform/knowledge-catalog/commit/4c40ef1),
*"Fix OKF cross-links to use file-relative paths so bundles render on GitHub"*:

> Previously the enrichment agent emitted every concept cross-link as a bundle-root-absolute path
> (`/tables/users.md`). That form **breaks when the bundle is browsed on GitHub**, where a leading
> `/` is interpreted as repo-root-relative and every link 404s.

It rewrote 152 links across 40 files, changed the emitter and the prompts, and a follow-up commit
**dropped the viewer's back-compat branch entirely**. Yet SPEC.md §6.1 still calls the absolute form
"**recommended**". Body link forms across the corpus today: **74 dot-relative, 28 bare-relative, 8
absolute** — and all 8 absolute ones are in the fixture added *after* the fix, are broken on GitHub,
and are invisible to the graph.

For a wiki read **in-repo, on GitHub, by an agent doing `cat`**, this is the single most transferable
operational finding in the research: OKF's own producer abandoned `/`-absolute links for exactly our
reading environment, and the spec was never updated to say so.

---

## 8. Where OKF's shape assumes a data catalog

All four bundles are data catalogs. Counted, the assumption shows up in five places.

**1. `resource` is a console/API URL for an external asset — always.** 47/53 concepts carry
`resource`, and **100% of them are absolute URLs into another system**: 24 `bigquery.googleapis.com`,
9 `meta.stackexchange.com`, 7 `support.google.com`, 3 `github.com`, 2 `wiki.acme.internal`, 1
`console.cloud.google.com`, 1 `cloud.google.com`. **Zero point at a file in the same repository.**
The 6 concepts *without* `resource` are precisely the abstract ones — 3 `Metric`, 2
`Attested Computation`, 1 `Skill` — matching §4.1's "absent for concepts that describe abstract
ideas". Every page in `docs/wiki/` is that abstract kind. A `structure` page's nearest "underlying
asset" is a directory of source we already track in `source_paths`, and §6.1's own `/`-absolute form
is the broken one (§7 above). **`resource` has never been exercised as an in-repo pointer.**

**2. `usage_count` is query executions.** §5.1 defines it as "dashboard views, query executions, page
reads", and `acme_retail/log.md` records where the one real number came from: *"a 90-day sample of
`region-us.INFORMATION_SCHEMA.JOBS_BY_PROJECT`"*. There is no analogue for a wiki page — nothing
counts reads of `docs/wiki/backend-sabha.md`. The nearest in-repo signal (commit touch count on
`source_paths`) is a *churn* measure, which is the opposite of the adoption signal `usage_count`
intends. This field is not merely untested for us; it is undefined.

**3. Attestation is SQL equality.** §10's entire machinery — `runtime`, `receipt: [job_id,
executed_sql, result]`, `attester` — is a query-provenance mechanism. The one attester
canonicalizes SQL (`_KEYWORDS` frozenset of `SELECT`, `FROM`, `JOIN`…), and §12's deferred list is
all data-warehouse work ("semantic-layer templates (Looker, dbt)"). The map already rules §10 out;
this is confirmation that there is nothing there to reconsider.

**4. `type` is the source *system's* noun.** `BigQuery Table` / `BigQuery Dataset` are 25 of 53
files. The taxonomy is scraped from a catalog, which is why it needs no admission test — the
producer never has to decide whether a page should exist, only how to describe a table that already
does. **Our situation is the inverse**, which is why the map's closed taxonomy + admission tests are
carrying weight OKF has never had to carry.

**5. Freshness is a policy review cycle.** All 7 `stale_after` values are the identical instant
`2026-12-31T00:00:00Z`, tied to an annual finance standard. There is no example in the corpus of
`stale_after` deriving from anything about the *content*. The incumbent's git-diff staleness has no
counterpart here — not because OKF rejected it, but because a data catalog has no commit history for
the thing it describes. **`stale_after` is not a considered alternative to `source_paths` diffing; it
is what you use when there is nothing to diff.**

**The corollary for a codebase corpus.** OKF's provenance model assumes sources are *external and
immutable-ish* (a docs page, a policy PDF, a table's INFORMATION_SCHEMA). Our sources are *in-repo
and versioned*, which is why the incumbent has `source_paths` globs at all. The map's note that
`source_paths` (invalidation) and `sources[]` (provenance) "are not the same thing wearing two names"
is confirmed by the counts: `sources[]` has no glob anywhere, no `git` reference anywhere, and its
only in-repo instances are 8 hand-written `path/to/file.md` literals with an `author` and a
`last_modified` typed by a human.

---

## What the counts settle

Findings only — decisions belong to the downstream tickets (#173, #174, #175).

- **Adopt-by-default is `type`, `title`, `description`, `tags`, `generated`, `sources`.** That is the
  53/53 set, the set the producer prompt teaches, and (minus `sources[]`'s credibility keys) the set
  the consumer renders. Everything else is a fixture.
- **`generated` is the cheapest real win**, and the reason is mechanical: it is stamped by the tool,
  not typed by the author. Our sweep already knows the model and the time. Note it partly duplicates
  `last_compiled` — `generated.at` is a wall-clock instant where `last_compiled` is a sha, and the
  sha is strictly more useful in a repo.
- **`verified` maps cleanly onto notes and badly onto compiled pages** — the map already suspected
  this; the corpus confirms there is no evidence either way, since 44/44 agent-written pages have no
  `verified` at all. `trust_tier()` is 8 borrowable lines if we adopt it.
- **`status` earns its keep only as `deprecated`.** `stable` is redundant by §5.4 and `draft` is
  unused in the entire corpus. The collision with the incumbent's `disputed` is therefore narrower
  than it looks: OKF's *practised* vocabulary is one value, not three.
- **`stale_after` has no non-calendar precedent.** Every instance in the corpus is an inherited
  policy-review date. That is the strongest available support for the incumbent's §7 rejection.
- **Credibility signals and `usage_window` are aspirational** — 9, 9 and **1** entries respectively,
  all hand-written, all in one file, all unread by the consumer. `usage_count` has no definition for
  a repo corpus at all.
- **`sources[].id` + footnotes work, and degrade predictably.** 65/65 entries have an `id`, but the
  agent-authored bundle regressed to `[^1]` labels that join to nothing and to document-level
  placement. If adopted, lint check: *every `[^label]` in a body resolves to a `sources[].id`, and
  every `sources[].id` is cited at least once.*
- **§11 conformance is cheaper to enforce than to describe**, and no one has done it: five lines
  check clause 2, clause 3 is checked by nobody and violated by the repo's own fixture. Our `lint`
  would be, as far as I can find, the **first §11 checker in existence** — including clause 3, which
  is the one that would have caught `log.md`'s frontmatter and its leak into the graph.
- **Drop `kind: index` / `pages:` with no regret.** 24/24 index files carry zero frontmatter and
  0/4 bundle roots declare `okf_version`. There is nothing to be conformant *with* here beyond
  "no frontmatter".
- **Link form is the finding to act on.** `/`-absolute is spec-recommended, GitHub-broken, and
  abandoned by the producer in #45. Whatever we choose for `[[slug]]`, do not adopt §6.1's
  recommendation on the spec's authority — the spec is stale relative to its own repo.
