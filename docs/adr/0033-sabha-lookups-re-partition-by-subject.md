# Sabha-owned lookups re-partition by subject — and do **not** move to the request edge

**Status**: accepted (issue [#133](https://github.com/JayminPatel007/attendance-tracking-system/issues/133), decided across the wayfinder map [#222](https://github.com/JayminPatel007/attendance-tracking-system/issues/222)).

**Supersedes nothing.** It **bends [ADR-0027](0027-no-shared-granted-scope-module-behind-the-authorization-engines.md) §1** (see below), leaves ADR-0027 §3 completely untouched, and adds a second failure of its §5. Follows [ADR-0019](0019-bounded-context-module-taxonomy.md)'s port and ring rules unchanged.

**Companion to [ADR-0032](0032-caller-authority-resolved-at-the-request-edge.md), and deliberately asymmetric to it.** Read the next section before reading anything else here.

---

## Why this family did **not** move to the edge

ADR-0032 folded nine caller-keyed questions into a `CallerAuthority` value resolved once per request. #133 assumed the sabha-owned lookup ports would "consolidate similarly". **They do not, and the asymmetry is the finding — not a failure to reach symmetry.**

ADR-0032 turned on one measured fact: **six identity adapters run the same statement against the same relation, keyed on `user_id`**, differing only in which scope column they project. That is what makes an edge-resolved snapshot possible — one caller per request, one query, one value.

The sabha five have no such shape. Measured from the adapters:

| Read | Table | Key |
| --- | --- | --- |
| `sabhaScope`, `scheduleShapeOf`, `findSchedule`, `isSabhaKindRetired` | `sabhas` (last joins `sabha_kinds`) | `id = ?` |
| `selectiveSabhaIn` | `sabhas` | `(kshetra_id, sabha_kind)` |
| `findAllWeekly` | `sabhas` | unkeyed, filtered on shape |
| `zoneOfKshetra` | `kshetras` | `id = ?` |
| `cityOfZone` | `zones` | `id = ?` |
| `demographicOfKind`, `isKindRetired` | `sabha_kinds` | `id = ?` |
| `createWeekly`, `createMonthlyAdHoc` | `sabhas` | INSERT |

**Four tables, six key shapes, and a write.** There is no one-query fold to make.

**And a Sabha is a *target*, not a request-scoped singleton.** One request may touch many Sabhas, or none. Even where a snapshot would be right, the edge is the wrong place to build it — you would be guessing which Sabha to pre-load.

> **The discriminator, stated once:** *whose rows answer this?* The caller's own rows in `role_assignments` fold to the edge. Anything keyed by a target stays a port. ADR-0032's mechanism transferred to the identity family only because the **shape** transferred; it does not transfer here, and applying it to `SabhaFacts` would be a misreading of both documents.

What these ports *do* want is consolidation on a different axis: **by subject**, staying ports.

---

## Five facts the decision turned on

1. **Four projections of one row, spread across three ports.** `sabhaScope` (kshetra_id, sabha_kind), `scheduleShapeOf` (schedule_shape), `findSchedule` (day_of_week, start_time, end_time) and `isSabhaKindRetired` (join on `retired_at`) all read `sabhas WHERE id = ?`. This is the real sabha-side analogue of ADR-0032's finding — keyed on the target, not the caller.
2. **`WeeklySabhaCatalog` is `SabhaScheduleLookup`'s projection minus the id predicate.** `SELECT id, day_of_week, start_time, end_time FROM sabhas WHERE schedule_shape = 'WEEKLY_RECURRING'` versus the same columns `WHERE id = ? AND schedule_shape = 'WEEKLY_RECURRING'`. Same columns, two ports, **two value types** (`WeeklySabhaRef`, `SabhaSchedule`).
3. **The soft-retire rule is written twice, one hop apart.** `StructuralHierarchyLookup.isSabhaKindRetired(sabhaId)` (a join) and `SabhaProvisioning.isKindRetired(sabhaKindId)` (a direct `EXISTS`). Both `default`-bodied, both fake-driven, both from [#87](https://github.com/JayminPatel007/attendance-tracking-system/issues/87).
4. **The hierarchy chain is not walked.** `AppointmentAuthorization` calls `sabhaScope`, `zoneOfKshetra` and `cityOfZone` in **three different branches of one switch**, one hop each. No production caller takes two hops, so folding the chain into a JOIN saves **zero** round trips. This is the strongest point for the status quo, and it survives into the decision: the chain stays its own port.
5. **`StructuralHierarchyLookup` is the worst port in the population** — 5 methods, 3 subjects, **13 fakes across 12 test classes** (the single largest fake burden of the 15), two of its methods `default`-bodied purely so fakes need not implement them.

Two premises in #133 are corrected: `SabhaShapeLookup` has **two** consumers, not one; and `SabhaProvisioning`'s single consumer is in **identity** (`SabhaDefinitionService`), not attendance.

---

## Decision — re-partition by subject into three ports

Each port covers **one table family and one key**:

- **`SabhaFacts`** — the `sabhas` table, read-only: `of(sabhaId)`, `allWeekly()`, `selectiveIn(kshetraId, demographic, track)`.
- **`StructuralParentage`** — the geography chain: `zoneOfKshetra`, `cityOfZone`.
- **`SabhaProvisioning`** — the command: creates a Sabha; reads `sabha_kinds` for the demographic and the retired mark.

### Per-port verdicts

| Port | Verdict |
| --- | --- |
| `StructuralHierarchyLookup` | **Deleted.** Splits: `sabhaScope` + `isSabhaKindRetired` + `selectiveSabhaIn` → `SabhaFacts`; `zoneOfKshetra` + `cityOfZone` → `StructuralParentage`. |
| `SabhaShapeLookup` | **Deleted** — `schedule_shape` is a field on `SabhaFacts`. |
| `SabhaScheduleLookup` | **Deleted** — the standing slot is a field on `SabhaFacts`. |
| `WeeklySabhaCatalog` | **Deleted** — becomes `SabhaFacts.allWeekly()`; `WeeklySabhaRef` dies with it. |
| `SabhaProvisioning` | **Kept as-is, name included.** It is a cross-context **command**, not a lookup, and commands do not consolidate with reads. The name already names an act; its javadoc gains a line saying so. The confusion came from #133 grouping it with the lookups, not from the name. |

**Why by subject rather than by the hot cluster alone.** Keeping five ports, or merging only the four projections of `sabhas WHERE id = ?`, both organise these ports the way **history** left them — by which consumer asked first. Re-partitioning organises them the way the **data is shaped**. It is the only option under which the soft-retire duplication (fact 3) becomes **structurally impossible** rather than merely fixed once: *"is this Sabha's Type retired"* belongs to `SabhaFacts`; *"is this Sabha Type retired"* belongs to the command that creates **by Type**; those are honestly two questions from two callers. Merging only the hot cluster also leaves the duplicate schedule value type (`WeeklySabhaRef`, fact 2) in place.

### The shared value surface

`SabhaFacts`' record may carry Kshetra, Sabha Kind code, demographic, track, schedule shape, standing slot and the retired mark — **plain text, enums already in `common-domain`, and numbers only**. Sabha's own `ScheduleShape` enum still does not cross the seam, so `SabhaProvisioning`'s recorded reason for having two create methods survives intact.

### Two axes that were expected to separate the options and did not

- **Round trips: zero saved under every option**, because of fact 4. This is the mirror image of ADR-0032's collapsed objection — there, checking the objection *made* the case; here it collapses *against* the merge, and the merge wins on other grounds anyway.
- **Over-fetch: not a differentiator.** The merged read is one narrow row by primary key, and this repo already chose over-fetch deliberately twice (`SanyojakZoneLookup.isSanyojakOfZone`, `RegionalTeamCityLookup.isRegionalTeamMemberOfCity`) with no objection.

With both gone, the choice reduced to one question: re-partition on the axis the data has, or keep the axis history left?

---

## Engaging ADR-0027

- **§1 — "the question shapes don't share a shape": bends, and this is the honest amendment.** §1's concern is a point predicate and a collection query forced into one interface, making the point caller over-fetch. `SabhaFacts` does hold two shapes — `of(id)` and `allWeekly()` — but they read the **same table and return the same type**, so no caller over-fetches to serve another's shape. §1's warning is about *mismatched* shapes over *different* data; it does not reach two windows on one relation.
- **§3 — "the split is the policy": untouched.** Nothing here moves a decision. `SabhaFacts` answers *"what is this Sabha"*, never *"may this caller act on it"*. Authorization stays exactly where it is, and ADR-0032's thesis — the facts consolidate, the policies do not — is obeyed on this side too.
- **§5 — "per-port fixtures are the price of engine isolation": fails again.** 13 of the population's 42 fakes belong to one port that is three subjects in a trenchcoat. That price bought no isolation; it bought `StructuralHierarchyLookup` being hard to stub.

---

## Consequences

- Five ports become three; `StructuralHierarchyLookup`, `SabhaShapeLookup`, `SabhaScheduleLookup`, `WeeklySabhaCatalog` and the `WeeklySabhaRef` value type are deleted.
- The largest single fake burden in the port population (13 fakes across 12 test classes) goes with `StructuralHierarchyLookup`.
- The [#87](https://github.com/JayminPatel007/attendance-tracking-system/issues/87) soft-retire rule stops being written twice, and the shape that allowed the duplication is removed.
- **Zero round trips are saved, and that is expected.** The case for this change is legibility and drift-resistance, not cost. Anyone re-opening it on performance grounds is arguing against a claim this ADR does not make.
- `SabhaProvisioning` keeps its name and its two create methods.

---

## What would change our mind

- **`SabhaFacts.of()` grows caller-specific branching or conditionally-meaningful fields.** If a caller needs a column meaningful only for one schedule shape, the record starts carrying emptiness that means different things to different callers, and it has become the fat interface §1 warns about. Fall back to merging only the hot cluster.
- **`allWeekly()` needs a different projection from `of()`.** The whole §1 rebuttal rests on the two windows returning the same type over the same table. If the weekly-materialization cron ever needs a join the by-id read does not, the shared type breaks and `WeeklySabhaCatalog` should come back as its own port.
- **A caller appears that walks two or more hops of the geography chain.** That would not reverse this — it would *extend* it, by justifying a folded `parentageOf(sabhaId)` on `StructuralParentage`. Worth recording because fact 4 is load-bearing for keeping the chain separate, and it is the fact most likely to change.
- **The asymmetry with ADR-0032 proves unteachable.** If reviewers repeatedly try to apply the `CallerAuthority` edge mechanism to `SabhaFacts` despite [Why this family did not move to the edge](#why-this-family-did-not-move-to-the-edge), the cost is in the documentation rather than the design — and the remedy is to **merge this ADR and ADR-0032 into one document**, which was the live alternative when the two-document split was chosen.
