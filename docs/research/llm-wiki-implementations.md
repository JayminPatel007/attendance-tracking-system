# How existing LLM Wiki implementations compile a codebase wiki

Research asset for [Research: how existing LLM Wiki implementations compile a codebase wiki](https://github.com/JayminPatel007/attendance-tracking-system/issues/143),
a ticket on the map [Wayfinder: In-repo LLM Wiki as the codebase front door](https://github.com/JayminPatel007/attendance-tracking-system/issues/142).

Read on 2026-07-30. The goal is **not** to adopt any of these — the map rules out third-party
implementations — but to extract what has already been proven, so our own decisions start from
evidence rather than from scratch.

## Sources read

| Source | What it is | Codebase-specific? |
|---|---|---|
| [karpathy/llm-wiki gist](https://gist.github.com/karpathy/442a6bf555914893e9891c11519de94f) (75 lines, Apr 2026) | The origin. An **idea file**, deliberately abstract, meant to be pasted into an agent so the agent instantiates a version with you. | No — general knowledge bases |
| [yysun `git-wiki` skill](https://github.com/yysun/awesome-agent-world/blob/HEAD/skills/git-wiki/SKILL.md) (v1.6.0, 308 lines) | An Agent Skill that maintains a `.wiki/` for a **code repo**, driven by git. The write-up is [Bringing the LLM Wiki Idea to a Codebase](https://dev.to/yysun/bringing-the-llm-wiki-idea-to-a-codebase-22go). | **Yes — the closest analogue to what we want** |
| [ussumant/llm-wiki-compiler](https://github.com/ussumant/llm-wiki-compiler) (305★) | Claude Code plugin. Has a v2 "codebase mode". Its [`COMPILE_PROTOCOL.md`](https://github.com/ussumant/llm-wiki-compiler/blob/HEAD/COMPILE_PROTOCOL.md) is a self-contained, plugin-free protocol file. | Partly |
| [nashsu/llm_wiki](https://github.com/nashsu/llm_wiki) (15.6k★) | Tauri desktop app + MCP server over a document collection. | No — documents, not code |

A note on relevance: **nashsu is the most popular and the least applicable.** It is a document
knowledge base with a GUI, a graph engine, and PDF parsing; its sources are papers and articles, not
a repo. Its value here is as evidence of which parts of Karpathy's pattern survived contact with a
large user base (see [What everyone kept](#what-everyone-kept)). The two that actually compile from
code are yysun's `git-wiki` and llm-wiki-compiler's codebase mode.

---

## 1. Page kinds — what they settle on

Karpathy names no taxonomy at all. He lists "summaries, entity pages, concept pages, comparisons, an
overview, a synthesis" as examples and then explicitly says the document "describes the idea, not a
specific implementation" — page formats "depend on your domain". **Every taxonomy below is a
downstream invention, not scripture.**

### yysun `git-wiki` — one directory per page kind

Nine optional directories under a `.wiki/` root, created as needed:

| Directory | Holds |
|---|---|
| `overview/` | Orientation: what the project is, startup path, storage, reading path |
| `features/` | One page per significant product capability or implementation area |
| `flows/` | Sequences, pipelines, request lifecycles |
| `concepts/` | Cross-cutting ideas and patterns |
| `entities/` | Data models, schemas, types |
| `risks/` | Invariants, compatibility contracts, fragile behaviour, "things not to break" |
| `reference/` | Code-shaped pages when a source path needs focused explanation |
| `bug-fixes/` | One page per notable fix or patch |
| `notes/` | Ad-hoc or open questions |

Only `index.md` is **required**. The rest are created on demand — the taxonomy is a menu, not a
mandate.

The sharpest idea in this source is that **the taxonomy is not the navigation.** The skill insists
the front door is organised around the reader's questions, in this fixed order:

1. What is this? 2. Get started 3. Why does it exist? 4. What happens when I run it?
5. Where is data saved? 6. What are the important moving parts? 7. What should I avoid breaking?
8. Where do I look first?

`features/`, `entities/`, `flows/`, `reference/` are explicitly demoted to "supporting evidence, not
the first mental model". Its stated quality bar: `index.md` orients a new reader in **under 2
minutes**.

Two more conventions worth stealing:

- **Page naming is reader-first**: `login-check.md` over `auth-middleware.md`, `saved-data-shape.md`
  over `user-schema.md`. Source-path-derived names (`reference/src-api-routes.md`) are reserved for
  reference pages, where no clearer human name exists.
- **`risks/` has no equivalent in any other source.** "What should I avoid breaking" is a page kind
  the code cannot tell you and the ADRs only partly do.

### llm-wiki-compiler — two page kinds, not nine

A much flatter model, and the flatness is the point:

- **`topics/{slug}.md`** — the unit of compilation. In codebase mode a topic is a *module or
  service*, discovered by looking for directories with their own manifest file (`package.json`,
  `go.mod`). Cross-cutting topics (`infrastructure`, `testing`, `deployment`) are auto-created when
  the matching files exist.
- **`concepts/{slug}.md`** — cross-cutting patterns appearing in **3+ topics**, compiled in a second
  pass *after* all topic pages are written, by reading the topic pages back. Explicitly
  "interpretive, not just factual", with a required "What This Means" section. Guarded: only create
  one if it connects 3+ topics with a **non-obvious** insight — "don't force concepts."

Its codebase topic page has a **fixed section template**, which is the most directly reusable
artifact I found:

```
Purpose · Architecture · Talks To · API Surface · Data · Key Decisions · Gotchas · Sources
```

`Talks To` is the standout — it forces each module page to name its outbound edges with protocol and
endpoint (`user-service (REST: /api/users/:id) — subscription status lookup`), which is how a reader
reconstructs a cross-module flow without a flow page existing.

### The disagreement worth noticing

These two taxonomies **conflict**, and it is a real fork, not a detail:

- yysun says: **flows and entities get their own pages**, named for human ideas.
- llm-wiki-compiler says: **the module is the page**; flows emerge from each module's `Talks To`
  section; concepts are derived, not authored, and only when they span 3+ modules.

Module-per-page scales mechanically (discovery is automatic — find the manifests) but has no natural
home for a flow that crosses five modules. Question-per-page reads better cold but requires human
judgement about what the questions are, every time.

## 2. How pages interlink

Near-unanimous, and the one place all four sources converge:

- **`[[wikilink]]` by slug** — target matches the destination filename without `.md`. Used by
  Karpathy, yysun, nashsu. llm-wiki-compiler makes it configurable (`link_style`:
  `"markdown"` → `[label](relative/path.md)` vs `"obsidian"` → `[[relative/path]]`). yysun's stated
  reason for `[[slug]]` is decisive: it "lets lint resolve links deterministically" — links are
  checkable precisely because they are location-independent slugs.
- **`index.md` is the entry point, and it is not optional.** Karpathy: content-oriented catalog, one
  line per page, updated on every ingest; the LLM reads the index *first*, then drills in. He notes
  this works to ~100 sources / hundreds of pages and "avoids the need for embedding-based RAG
  infrastructure". llm-wiki-compiler regenerates `INDEX.md` every run unconditionally ("cheap, even
  if nothing changed") and adds an **Also Known As / aliases** column — alternate names someone might
  search for. That's a cheap, high-value trick for a domain with heavy non-English vocabulary.
- **`log.md`** — chronological, append-only, records ingests/queries/lint runs. Karpathy's tip:
  a consistent line prefix (`## [2026-04-02] ingest | Title`) makes it greppable
  (`grep "^## \[" log.md | tail -5`).
- **YAML frontmatter on every page.** yysun's is the code-aware one:
  `title, type, status(draft|active|stale), source_paths[], updated_at`. **`source_paths[]` is the
  load-bearing field** — it is what makes a git diff resolvable to a set of pages (§3), and what
  makes deletion cascade correctly. nashsu independently arrived at the same field (`sources[]`) and
  uses it for three separate mechanisms: relevance scoring (source overlap is weighted ×4.0, higher
  than a direct link at ×3.0), cascade cleanup on source deletion, and traceability.
- **Backlinks are not authored.** No source maintains them by hand; they're either derived by a tool
  (Obsidian, nashsu's graph) or simply not present. Nobody hand-maintains a backlinks section.

## 3. What triggers a recompile, and how a diff maps to pages

This is where the two codebase implementations are genuinely instructive, and where Karpathy is
silent (his trigger is manual: you drop a source in and say "ingest").

### yysun — git SHA checkpoint, stored in the wiki

The mechanism, in full:

1. `index.md` frontmatter carries `last_commit: <full-sha>` — the checkpoint.
2. On ingest: `git rev-parse HEAD`, then `git diff --name-status -M <last_commit> HEAD`, excluding
   the wiki root itself.
3. `-M` means **renames are detected**, and the status codes (A/M/D/R) drive different handling:
   deleted source → mark the page `status: stale` with a note; renamed → update the page.
4. Pages whose **`source_paths[]` intersects the changed-file set** are updated or marked stale.
   That's the invalidation rule: it's a set intersection, made possible by frontmatter.
5. `last_commit` advances to HEAD **only after** the complete changed-file set is processed. A
   partial or scoped ingest deliberately leaves the checkpoint unmoved, and notes that coverage is
   partial.

Three hard rules it commits to, each of which is a lesson:

- **`HEAD` only.** Staged and uncommitted changes are ignored, for ingest *and* for query
  verification. The wiki describes committed reality, so it never describes a state that never
  existed.
- **No filesystem fallback.** "If git metadata is unavailable, do not fall back to filesystem
  scanning. Ingest and lint are unavailable until git access works again." A wrong checkpoint is
  worse than no ingest.
- **Ingest is automatic — "Do not ask the user before ingesting."** Friction is the thing that kills
  wikis.

### llm-wiki-compiler — file-list state, four invocation modes

`{output}/.compile-state.json` holds the previous file list, topic slugs, and `last_compiled`; each
source is diffed to new/changed/unchanged. Four modes: *(default)* incremental → recompile only
topics whose sources changed; `--full`; `--topic {slug}`; `--dry-run` (report, write nothing).

One rule here is easy to get wrong and worth copying: when recompiling a topic, **read all of that
topic's sources, not just the changed ones** — otherwise the rewrite loses the context the unchanged
files supplied. Also: always exclude the output directory from the scan, so the wiki never ingests
itself.

Its explicit contrast with **Google Code Wiki / DeepWiki** is the most useful framing in any of these
sources:

| | Code Wiki / DeepWiki | LLM Wiki Compiler |
|---|---|---|
| Input | Source code (AST parsing) | Knowledge files + optional code |
| Output | API docs + architecture diagrams | Synthesised articles with coverage indicators |
| Infrastructure | Hosted platform + embeddings | Zero infra |
| Updates | Full regen every commit | Incremental, changed topics only |
| Consumer | Developers reading docs | The AI agent (and you) |

Its one-line thesis: those tools answer *"what does this code do?"*; this one answers **"what does
this project know?"** For us, that distinction *is* the case for building it — the code already
answers the first question, and a cold agent still fails at the second.

Note the tension between the two: **llm-wiki-compiler's default `deep_scan: false` means it doesn't
read source code at all** — it synthesises READMEs, ADRs, API contracts, Dockerfiles and CHANGELOGs,
using code only for structure signals. yysun reads the code. Cheapness vs. fidelity, and it is a real
choice.

## 4. Stopping pages going stale or confidently wrong

Five distinct mechanisms across the sources. They're complementary, not alternatives.

**a. Mark, never delete.** Both codebase implementations state this as a hard rule. yysun: "Do not
delete stale pages automatically — mark them and note the drift so the user can decide."
llm-wiki-compiler: "Stale content is flagged / re-ordered / annotated, never removed. The wiki is a
time-series artifact." Backed by a safety rule that sources are read-only and nothing outside the
output directory is ever written.

**b. A `status` field with a `stale` value** (`draft|active|stale`), set when the source changed
significantly or the thing described no longer exists. Cheap, and it makes staleness a queryable
property rather than a vibe.

**c. Per-section coverage tags** — llm-wiki-compiler's best idea. Every section heading carries
`[coverage: high|medium|low -- N sources]`, computed **per section, not per article**:

- `high` (5+ sources) — trust it, don't read the raw files.
- `medium` (2–4) — check raw sources for granular detail.
- `low` (0–1) — read the raw files.

This is an explicit **confidence signal aimed at the agent reading the page**, telling it when the
wiki is enough and when to go to source. It directly attacks "confidently wrong": a low-coverage
section is still written, but it announces its own thinness. Paired with a no-placeholders rule —
every section must contain specific factual content or be marked `[coverage: low]` and say so.

**d. Time decay and conflict resolution.** Topics are classified time-sensitive (default) or stable;
claims age at 6mo/18mo or 24mo/48mo. Stale bullets get a literal `⚠️ [YYYY-MM, may be stale]` prefix
so a reader can skim past them; timelines run newest-first. On conflict, prefer the materially newer
source and **record the shift explicitly**: `"YYYY-MM: {old framing} → {new framing}"`. Its stated
reasoning for erring toward time-sensitive is the sentence to remember: *"the cost of annotating a
date is low; the cost of confident-sounding stale claims is high."*

**e. Lint as a first-class operation.** Karpathy names it as one of the three core operations
alongside Ingest and Query. The union of checks across the sources:

- Broken `[[wikilinks]]` — target page doesn't exist
- Orphan pages — no inbound links; or all listed source files are gone
- Contradictions between pages (conflicting dates, metrics, decisions)
- Stale pages — sources changed since `last_commit`
- Missing coverage — files changed since `last_commit` with no page
- Missing cross-references — two pages sharing 3+ sources that don't link to each other
- Concepts mentioned across pages but lacking their own page
- Low-coverage sections, as improvement candidates
- Schema drift — pages not in the schema, schema entries with no page

Two rules on lint: output is grouped Errors / Warnings / Suggestions / **Next Questions** (the last
is generative — what to investigate next), and **it does not auto-fix unless asked.**

**f. Verify-on-doubt at query time.** yysun's query flow: read `index.md`, read relevant pages, but
*"if the page is missing or marked stale, verify directly against git-tracked source before
answering"*, and always verify against source when the answer involves specific values (counts,
names, configs). The wiki is a starting point with a fallback path, never the last word — which is
the honest way to make a synthesised layer safe.

Supporting writing conventions that reduce wrongness at the source: separate facts (read from source)
from inferences (your interpretation); use concrete file paths as anchors (`src/api/routes.ts:42`);
summarise rather than paste code, since pasted code goes stale silently.

## 5. What they explicitly did *not* do

- **Karpathy did not specify an implementation.** Deliberately: "This document is intentionally
  abstract… Everything mentioned above is optional and modular — pick what's useful, ignore what
  isn't." The pattern is meant to be instantiated per-domain in collaboration with the agent. Taking
  any of the taxonomies below as canon would be misreading the source.
- **No embeddings, no vector store, no RAG infrastructure.** Karpathy is explicit that an index file
  works "surprisingly well at moderate scale (~100 sources, ~hundreds of pages)" and "avoids the need
  for embedding-based RAG infrastructure". Search tooling (e.g. `qmd`) is listed as *optional*, for
  later, if the wiki outgrows the index. Every implementation followed this.
- **No AST parsing / API-doc generation.** The deliberate anti-goal versus Code Wiki/DeepWiki. Nobody
  in this lineage parses code to generate reference docs; the value claimed is synthesis of knowledge
  that *isn't* in the code.
- **The human does not write the wiki.** Karpathy: "You never (or rarely) write the wiki yourself."
  The human curates sources, asks questions, and reviews. Inverting this reintroduces exactly the
  maintenance burden the pattern exists to remove — his diagnosis is that "humans abandon wikis
  because the maintenance burden grows faster than the value."
- **No exhaustive coverage.** yysun: "Prefer useful coverage over exhaustive coverage — a good wiki
  is navigable, not encyclopedic." For 100+ file repos, "focus on depth over breadth: deeply document
  the 10 most important modules rather than shallowly touching 50." Target for a medium project:
  **10–20 pages**. Pages capped at **~500 words** — split rather than grow.
- **No automatic deletion, ever** (§4a), and no removal of a topic from the schema without human
  approval — flag as a candidate instead.
- **No uncommitted state** (yysun, §3).
- **No auto-fix on lint.**
- **llm-wiki-compiler doesn't read code by default** (`deep_scan: false`).
- **No page-kind hierarchy in the dev.to write-up.** It names the page kinds and the git mechanism
  but never specifies which pages a given file change invalidates — the invalidation rule only
  appears in the skill itself, as the `source_paths[]` intersection. Worth flagging: the write-up is
  a sketch; the skill is the artifact.

## What everyone kept

The intersection across all four — including the desktop app that changed everything else — is the
strongest signal in this research, because these are the parts that survived four independent
reimplementations:

1. Three operations: **Ingest, Query, Lint**
2. **`index.md`** as catalog and navigation entry point
3. **`log.md`**, append-only, parseable prefix
4. **`[[wikilink]]`** cross-references
5. **YAML frontmatter on every page**, including source traceability (`source_paths[]` / `sources[]`)
6. Plain markdown in a git repo — "you get version history, branching, and collaboration for free"

nashsu's README has a literal "What We Kept from the Original" section listing exactly items 1–5,
after replacing the CLI with a desktop app, adding a graph engine, a relevance model, community
detection, and a Rust backend. That it kept precisely these is the best available evidence of which
parts are load-bearing.

## Implications for this map

Findings only — the decisions belong to the downstream tickets.

- **Page taxonomy** ([Decide the wiki's page taxonomy](https://github.com/JayminPatel007/attendance-tracking-system/issues/144))
  inherits a live fork: yysun's nine reader-question directories vs llm-wiki-compiler's
  module-page-plus-derived-concept. Our monorepo has a third axis neither handles — three apps
  (backend/web/mobile) with cross-app flows. Note that llm-wiki-compiler's monorepo discovery
  (one topic per directory-with-a-manifest) would map our backend's bounded-context modules to pages
  almost for free, but has no home for a vertical slice that crosses all three apps. yysun's `risks/`
  ("what not to break") and llm-wiki-compiler's `Talks To` and `Gotchas` sections are the page
  elements with no existing home in our `CONTEXT.md` / ADR layer.
- **Entry point** ([Decide where the wiki lives and how a cold agent enters it](https://github.com/JayminPatel007/attendance-tracking-system/issues/145)):
  `index.md` is unanimous and non-optional; the aliases column and the 2-minute orientation bar are
  cheap adoptions. Open for us: whether a dot-directory (`.wiki/`, untracked in yysun's default) or a
  tracked `docs/wiki/` — yysun explicitly supports an untracked wiki root while still reading its
  `last_commit`, which we probably don't want since we'd lose review of generated pages.
- **Compile mechanism** ([Decide the compile and refresh mechanism](https://github.com/JayminPatel007/attendance-tracking-system/issues/147)):
  the `last_commit` checkpoint + `git diff --name-status -M` + `source_paths[]` intersection is a
  complete, proven design that needs no infrastructure. The checkpoint-advances-only-on-full-coverage
  rule and the HEAD-only rule are the non-obvious parts. Also settled by evidence: read *all* of a
  page's sources on recompile, not just the changed ones.
- **Staleness contract** ([Decide the staleness and accuracy contract](https://github.com/JayminPatel007/attendance-tracking-system/issues/148)):
  five mechanisms are available and stack — mark-don't-delete, `status: stale`, per-section coverage
  tags, time-decay annotation, lint. The per-section coverage tag is the one aimed squarely at *our*
  consumer (a cold agent deciding whether to trust the page or open the file). Verify-on-doubt at
  query time is the safety net that makes the rest tolerable.
- **Session learnings** ([Decide how session learnings get written back into the wiki](https://github.com/JayminPatel007/attendance-tracking-system/issues/149)):
  Karpathy's Query operation already contains the answer in embryo — "good answers can be filed back
  into the wiki as new pages… these shouldn't disappear into chat history." nashsu built this as an
  explicit "Save to Wiki" action that archives an answer to `wiki/queries/` and then re-ingests it.
  Note the map rules transcript-mining out of scope; this is the non-transcript version of the same
  goal.
- **Seed page** ([Prototype one backend seed page and react to it](https://github.com/JayminPatel007/attendance-tracking-system/issues/146)):
  llm-wiki-compiler's codebase section template
  (`Purpose · Architecture · Talks To · API Surface · Data · Key Decisions · Gotchas · Sources`) is
  a ready-made straw man to react to, and its `auth-service` worked example shows the intended
  density. Constraints to hold it to: ~500 words, facts separated from inferences, `path:line`
  anchors, no pasted code blocks.
- **A gap none of them fill**, and it is ours specifically: every implementation assumes the wiki is
  the *only* synthesised layer. We already have `CONTEXT.md` (canonical glossary) and 29 immutable
  ADRs that the wiki must cite without rewriting. Nothing in this research addresses coexistence with
  a pre-existing hand-written knowledge layer — that boundary is ours to draw.
