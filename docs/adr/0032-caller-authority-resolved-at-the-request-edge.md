# Caller authority is resolved at the request edge: the facts consolidate, the policies do not

**Status**: accepted (issue [#133](https://github.com/JayminPatel007/attendance-tracking-system/issues/133), decided across the wayfinder map [#222](https://github.com/JayminPatel007/attendance-tracking-system/issues/222)).

**Correction (PR A of #133, issue [#237](https://github.com/JayminPatel007/attendance-tracking-system/issues/237))**: R1's population was recounted by the rule itself once it existed, and three figures below were wrong — the allowlist is **7 today and 6 post-fold, not 8 and 7**; `JdbcDashboardQueries` and `JdbcOccurrenceReopenQueries` carry javadoc only, while `CallerVisibility` (in `common-domain`, not a read-model) holds the SQL and was missed; and the mention/SQL split is 32/7, not 31/8. R1's predicate therefore keys off the **module**, not "an allowlisted CQRS read-model" — that phrase never described `CallerVisibility`. The measurements are corrected in place; the reasoning above them is untouched.

**Correction (PR D of #133, issue [#240](https://github.com/JayminPatel007/attendance-tracking-system/issues/240))**: the fold is implemented, and four counts below were wrong — all of them measurements, none of them reasoning. `CallerAuthority` carries **16** methods, not eleven: the Decision 2 fold table alone yields thirteen, and `isSant()`, `runsSabha()` and `userId()` complete it. `AppointmentAuthorization` goes **4 ports → 2**, not 3 → 1; both survivors resolve containment, which is what Decision 4's table meant. `WebSessionService.describe` lands at **three** statements, not two — the third is the `username()` re-fetch this ADR predicted and told the implementer to leave, so the stopping rule has now refused something for real. R1's allowlist did drop to **6**, as predicted, recounted against the rule itself. Two consequences the ADR did not anticipate: `MadhyasthaKaryalayaMembership.isMember` lost its last consumer and was deleted with the fold (the carve-out list already implied this by naming only `anyMemberExists` and `grantTo`), and `SanchalakLookup.sanchalakOf` had to drop its `default` body — on a one-method port a default leaves no abstract method at all, so no test could write it as a lambda. The measurements are corrected here; the decisions below are untouched.

**Supersedes §1, §5 and §6 of [ADR-0027](0027-no-shared-granted-scope-module-behind-the-authorization-engines.md)**, and corrects its §2 inventory. **§3 is preserved and quoted forward verbatim below; §4 stands untouched.** ADR-0027's *ruling* — no shared granted-scope module, no generic `AuthorizationEngine<T>`, no pre-resolved scope — also stands.

**Amends [ADR-0030](0030-caller-identity-resolved-at-the-http-edge.md)** by widening the type behind `@CurrentUser` from `UserId` to `CallerAuthority`. The annotation, the resolver's location, and ADR-0030's "resolve once at the edge" principle are unchanged.

**Gives [ADR-0029](0029-role-assignments-access-rule.md) its first enforcement** (rule R1 below). Extends [ADR-0021](0021-spring-scheduling-for-occurrence-cron.md)'s `TransitionActor` and [ADR-0019](0019-bounded-context-module-taxonomy.md)'s ring rules.

**This verdict covers the *caller*-keyed family only.** The sabha-owned lookup ports reach a deliberately different answer, recorded in [ADR-0033](0033-sabha-lookups-re-partition-by-subject.md). See [Where this verdict stops](#where-this-verdict-stops) before applying anything here to a port that is keyed by a target.

---

## Thesis

ADR-0027 rejected two different things in one breath and named only one of them:

- **A shared *policy* module** — one engine, or a pre-resolved *scope*, serving every authority question. **Rightly rejected.** Nothing here reverses it, and three separate design bake-offs on this map re-confirmed it.
- **A shared *fact* layer** — naming the caller's own role assignments once. **Wrongly rejected**, on reasons that have since failed against measurement.

**The policies do not consolidate; the facts do. ADR-0027 conflated them.**

ADR-0027's own revisit trigger — *"if a fifth and sixth engine ever appear with the same question shape and genuinely identical policy, revisit this decision then"* — has fired. The answer it produced is not the one the trigger anticipated: the fifth and sixth engines appeared with **no policy at all**, so the remedy is deletion, not a shared module.

### §3 is quoted forward, verbatim, because it is still right

> ### 3. The policies differ for real domain reasons — and #66 already proved the "same fact" is not actually shared
>
> The first spike question was whether the shared fact is genuinely common given the policies differ for domain reasons (Nirikshak proxy, reopen tiers, Sant universal read, MK override). They are not common, and the canonical evidence is in the read-model refactor that lands *next to* this: [#66](0019-bounded-context-module-taxonomy.md) (PR #94, `CallerVisibility` / `VisibilityTier`) had to **split the Nirikshak in two** — `NIRIKSHAK` (a Kshetra-tier reopen authority resolved through `role_assignments`) versus `NIRIKSHAK_PROXY` (an explicit, mutable Sabha set resolved through `nirikshak_sabha_assignments`) — precisely because the same role means two different things to two different consumers. A shared granted-scope layer would have to re-expose that split, and others like it (reopen tiers vs shaping authority; Sant universal read; MK override; appointment's "one tier up"), to every engine. The split *is* the policy; there is no policy-free "fact" underneath it worth naming once.

Every decision below obeys that paragraph. What moves is **rows**. Every engine keeps its own switch, its own tier sets, its own folds. Not one engine merges with another.

---

## The measured fact ADR-0027 did not have

`role_assignments` is one relation — `(user_id, role, sabha_id, kshetra_id, zone_id, city_id, demographic, appointed_by, revoked_at)`.

**Nine of the questions asked of it are the same statement:**

```sql
SELECT ... FROM role_assignments WHERE user_id = ? AND role = ? AND revoked_at IS NULL
```

They differ in exactly two ways: which scope column they project or compare, and `EXISTS` versus list. Six of these live in `common-domain` ports, two are identity-local (`AppointerAuthorityLookup`, `UserRolesLookup`) and one is analytics-local (`AuditScopeLookup`, which is three `SELECT DISTINCT`s over that same shape). The last three fell outside every earlier inventory, which is why ADR-0027's §2 port list is incomplete.

Against a population of 15 cross-context lookup ports holding **397 interface lines of which only 51 are executable**, 760 adapter lines and 42 test fakes — with two ports alone (`StructuralHierarchyLookup` 13, `RoleAssignmentLookup` 9) carrying 22 of the 42.

---

## Decision 1 — the seam is the request edge, and there is exactly one caller parameter

`@CurrentUser CallerAuthority caller` **replaces** `@CurrentUser UserId caller`. `UserId` stops being an edge type; you get it from `caller.userId()`.

- **The annotation is unchanged.** `CurrentUserArgumentResolver.supportsParameter` widens to accept `CallerAuthority`. This is not cosmetic: [#209](https://github.com/JayminPatel007/attendance-tracking-system/issues/209)'s `every_handler_resolves_its_caller` rule keys off the annotation, and so does R2 below. A new annotation would silently void both.
- **The edge gains a second port, not a second responsibility.** The resolver takes the existing `CallerResolver` plus one new `common-domain` port that loads the caller's `role_assignments` rows. Everything that can go wrong identifying a caller still goes wrong there, as a 403.
- **Two sequential queries at the edge** — `users` by Keycloak subject, then `role_assignments` by `users.id`. The `LEFT JOIN` collapse is **available and deliberately not taken**: it would fuse the two questions (*who is this?* and *what may they do?*) that ADR-0030 separated on purpose.
- **`TransitionActor.SignedIn` carries `CallerAuthority`**; `Cron()` stays a no-field record. ADR-0021's system-actor bypass stops being a javadoc claim and becomes a fact the compiler enforces.

### There is nothing to cache, and that is a consequence of the one-parameter rule

These two statements must be read together, because the second holds *only* because of the first.

The resolver runs once per **declared parameter**. Under one caller parameter type, a request resolves the caller's authority exactly once, and every engine in that request receives **the same instance**. Request-scoped caching is therefore a consequence of parameter passing, not a mechanism anyone builds: **no `@RequestScope` bean, no `ThreadLocal`, no memoization.** None exists in the backend today, and none is to be introduced for this.

The rejected two-parameter ("by signature") design is the only one that makes caching a real problem — both types would satisfy `supportsParameter`, so one handler could declare both and issue the `role_assignments` query twice in a single request. Caching here is **dissolved, not mechanised**.

### Why not lazy, and why not two parameter types

Measured over the handler population: **70 handlers, 53 caller-bound, 45 of which consult a caller-keyed authority port.** Of the remaining 8, six scope the caller *in SQL* (the [#66](https://github.com/JayminPatel007/attendance-tracking-system/issues/66) read side, out of scope here) and **exactly two ask nothing at all** — `GET /api/whoami` and `GET /bff/dashboard/thresholds`, the latter binding a caller on purpose to pay for a check it discards.

- **Lazy loses twice.** A deferred value is a function; a function injected into an engine is a port wearing a different hat, so the testability win is forfeit. Worse, the cron path (`OccurrenceWriter` via `AutoOpenScanner` / `AutoFinalizeScanner`) becomes a **2am runtime failure** where `TransitionActor.Cron` makes it a compile-time impossibility.
- **Two parameter types lose** on a permanent fork in the handler population — a decision for every future handler author and a check for every reviewer — bought for 2 handlers out of 53.

**Query arithmetic, honestly: `1 − N`.** A caller-bound handler costs `1 + N` today (edge resolve plus N authority port calls) and `1 + 1` after, for every handler regardless of N. So this **saves 3** on `WebSessionService.describe`, 1–2 on `AuditLogAccess` and `AppointmentAuthorization`, **zero on the ~41 handlers at N = 1**, and **costs 1** on the two at N = 0.

---

## Decision 2 — what `CallerAuthority` carries, and the stopping rule

`CallerAuthority` carries **exactly the caller's rows in `role_assignments`, and nothing else** — one relation, one key, one query — held **privately** as a `List<RoleAssignment>`, surfaced only as eleven named domain questions.

**That sentence is the stopping rule, and it is the load-bearing part of this ADR.** The danger in this decision is not the first fold; it is the tenth, when `CallerAuthority` has accreted every caller-adjacent fact in the system and *has* become the granted-scope module ADR-0027 rejected. "The caller's rows in one named relation" is a boundary a reviewer applies without judgement: a candidate either is a projection of that query or it is not.

The rows are private for a reason beyond discipline. The `role_assignments.role` column holds **nine** distinct wire values, and **four** overlapping enums partition it differently — `Role` (6), `AppointableRole` (8), `OversightRole` (2), `VisibilityTier` (8, plus the synthetic `NIRIKSHAK_PROXY`, and the only one carrying a written name-equals-column invariant). Keeping the rows private keeps that untyped string from reaching four contexts, so the taxonomy problem ([#232](https://github.com/JayminPatel007/attendance-tracking-system/issues/232)) stays sealed inside one class instead of becoming a precondition of this work.

### What folds in

Nine questions; **six adapters collapse into one**.

| Deleted | Becomes |
| --- | --- |
| `MadhyasthaKaryalayaLookup.isMember` | `isMadhyasthaKaryalaya()` |
| `NirdeshakScopeLookup.scopesOf` | `nirdeshakScopes()` |
| `SanyojakZoneLookup.zonesOf` (+ its `default` predicate) | `sanyojakZones()` / `isSanyojakOfZone(zoneId)` |
| `RegionalTeamCityLookup.citiesOf` (+ its `default` predicate) | `regionalTeamCities()` / `isRegionalTeamMemberOfCity(cityId)` |
| `RoleAssignmentLookup.rolesForUserOnSabha` | `rolesOnSabha(sabhaId)` |
| `RoleAssignmentLookup.rolesForUserOnKshetra` | `rolesOnKshetra(kshetraId, demographic)` |
| `UserRolesLookup.operationalRolesOf` (identity-local) | `operationalRoles()` |
| `AppointerAuthorityLookup.holds{Nirdeshak,Sanyojak,RegionalTeam}` (identity-local) | `holdsNirdeshakIn(k, d)` / `holdsSanyojakIn(z, d)` / `holdsRegionalTeamIn(c, d)` |
| `AuditScopeLookup.scopeOf` (analytics-local, 3 statements) | `kshetrasUnderOversight()` + reuses `sanyojakZones()` + `regionalTeamCities()` |

Adapters deleted: `JdbcNirdeshakScopeLookup`, `JdbcSanyojakZoneLookup`, `JdbcRegionalTeamCityLookup`, `JdbcUserRolesLookup`, `JdbcAppointerAuthorityLookup`, `JdbcAuditScopeLookup`. `JdbcRoleAssignmentLookup` slims to `sanchalakOf`. `JdbcMadhyasthaKaryalayaMembership` survives — it implements two interfaces and only one half folds.

**Note the pairs.** `sanyojakZones()` sits beside `holdsSanyojakIn(zoneId, demographic)`; `regionalTeamCities()` beside `holdsRegionalTeamIn(cityId, demographic)`; `nirdeshakScopes()` beside `kshetrasUnderOversight()`. Each pair is the same role read two ways, **deliberately** — ADR-0024 collapses the demographic away for geography, ADR-0023 drops it for the audit read, and appointment authority keeps it. Today those readings sit in different modules and the difference is invisible. As adjacent methods with a line of javadoc each, the asymmetry is *stated* rather than emergent. That is the single biggest legibility win here, and it is worth more than the deleted lines.

**If a future question needs the demographic *sometimes*, split the method — do not add the branch.** A method whose body omits a filter term is a rule written down; a branch inside one method is the fat interface §1 warned about.

### Correcting ADR-0027 §1

§1's objection is about **mismatched representations** forcing a fat interface or an over-fetch. It is not an argument about method count. Eleven one-line filters over **one relation, one key, one shape** do not meet its premise — and §1's over-fetch claim was already contradicted in production before this map began, by two `default` methods (`SanyojakZoneLookup.isSanyojakOfZone`, `RegionalTeamCityLookup.isRegionalTeamMemberOfCity`) that fetch the list and call `.contains()`. The repo chose over-fetch, twice, deliberately, and no one objected.

§1's *inverted* case survives and is honoured: `sanchalakOf` is carved out below.

---

## Decision 3 — what stays a port: seven carve-outs, for **two different reasons**

This list reads as one list with one rationale. It is not, and the distinction is the proof that the fold is about facts.

**Six are carved out because the subject is not the caller's rows in `role_assignments`:**

| Port | Why |
| --- | --- |
| `RoleAssignmentLookup.sanchalakOf(sabhaId)` — **rename the interface to `SanchalakLookup`** | Target-keyed (inverted). Once its other two methods are gone, "role assignment lookup" names a type that looks up one thing, and would collide conceptually with the new `RoleAssignment` row type (cf. [#217](https://github.com/JayminPatel007/attendance-tracking-system/issues/217)/[#218](https://github.com/JayminPatel007/attendance-tracking-system/issues/218)). |
| `ReissueAuthorityLookup.wasAppointedBy(target, appointer)` | Keyed on the **target's** `appointed_by` (ADR-0004 / ADR-0025). |
| `SahNirdeshakCountLookup.activeCount(kshetraId, demographic)` | Its SQL has **no `user_id` term at all** — a fact about a Kshetra, not about a caller. |
| `NirikshakAssignmentLookup` — **narrowed to `isAssignedTo`**; delete the dead `sabhasAssignedTo` | A different relation (`nirikshak_sabha_assignments`) with a different lifecycle — hard delete, **no `revoked_at` column** — and #66 split `NIRIKSHAK` from `NIRIKSHAK_PROXY` precisely to keep the proxy set distinct from the role row. Folding it would re-make that conflation in the name of tidiness, and charge every request a second query for a fact two call sites use. |
| `MadhyasthaKaryalayaMembership` (`anyMemberExists`, `grantTo`) | A global existence check and a write; neither is caller-keyed. |
| `SantLookup.isSant` | Folds at four call sites (`DashboardAccess` ×3, `AuditLogAccess`) but **not** at `PasswordReissueService.canReissue`, which asks it about the **target** — ADR-0004's rule that a Sant, having no appointer (ADR-0011), is reissued by an MK member instead. |

**One is carved out because it is not a fact at all:**

| Port | Why |
| --- | --- |
| `AuditReadAccess` | It fronts an **engine**, not a table. After the fold it is *fully* caller-keyed and issues zero queries — the most foldable-looking thing on this list — and it stays anyway, because what it fronts is a **policy**, and this ADR's whole thesis is that policies do not move. |

**The last zero-fake port survives for exactly the reason ADR-0027 was right about, in a document whose verdict is that ADR-0027 was mostly wrong.**

`AuditReadAccess` is re-signatured to `canRead(CallerAuthority)`, still in `common-domain`, issuing no queries. `AuditScope` gains `default boolean admitted() { return !(this instanceof Denied); }` so the one admission rule is read from one place by both the sidebar and the BFF. The boolean contract is **compile-enforced, not chosen**: handing back `AuditScope` would drag an analytics type into `identity-domain-core`, violating ADR-0019 and rule R4 below.

---

## Decision 4 — the engines: six become five, by **subtraction**

Recompute each engine's constructor once caller authority arrives as a parameter:

| Engine | Ports before | Ports after | Policy after |
| --- | ---: | ---: | --- |
| `AuthorizationEngine` (attendance) | 3 | **3** — all target-keyed | reopen-tier intersect, shaping disjunction, `onBehalfOf` |
| `AppointmentAuthorization` (identity) | 3 | **1** (containment) | six-arm switch, inverted rank, RT disjunction |
| `AuditLogAccess` (analytics) | 3 | **0** | MK/Sant fold, `empty ⇒ Denied` (ADR-0023) |
| `DashboardAccess` (analytics) | 3 | **1–2** | Sant ⇒ chosen City ⇒ `NoCity` fold |
| `SabhaDefinitionAuthorization` (identity) | 1 | **0** | **none** |
| `StructuralScopeAuthority` (sabha) | 4 | **0** | **none** |

**The rule: a class is an engine iff something is left after the fact layer is removed.**

1. **`SabhaDefinitionAuthorization` and `StructuralScopeAuthority` are deleted** — 103 lines whose entire executable content is delegation to a value the caller already holds. They are not thin engines; they are empty ones. Their predicates become named methods on `CallerAuthority`. `StructuralScopeAuthority`'s 13 tests move onto the value type and get *easier*, since they stop needing four fakes. Its tier table — the only place in the backend where create and delete authority are stated together — relocates into `CallerAuthority`'s javadoc and into this ADR; documentation relocates, a Spring bean does not relocate for free.
2. **Three of the four inline predicates stop being bypasses.** Under a caller parameter there is no port to bypass — reading it inline is reading a parameter. `HomeSabhaTransferService.initiate` and `SelectionService.nominate` become the **same single call**, `authority.runsSabha(sabhaId)`, which is the honest fix for the one piece of byte-for-byte duplication in the population; two new engine classes would have preserved the duplication behind two names. `SelectionService.requireNirdeshak` becomes `authority.isNirdeshakOf(...)` — the same method `AppointmentAuthorization` and the deleted `SabhaDefinitionAuthorization` call. **Four sites, two ports, one asserted invariant → four sites, one method, no invariant to assert.**
3. **The fourth is promoted, and the rule is what promotes it.** `PasswordReissueService.canReissue` reads **two** real ports (`ReissueAuthorityLookup.wasAppointedBy` and `SantLookup.isSant`), both keyed on the *target*. It becomes **`ReissueAuthorization`** — the backend's sole `appointed_by`-keyed authority check, the one that contradicts ADR-0025's current-scope rule under ADR-0004's licence, gets a documented home instead of a private method. The subtractive test, applied honestly, *promotes* the predicate flagged as most anomalous; that is what marks it as a rule rather than a rationalisation. It is now the one engine every port of which is about somebody other than the caller.
4. **`DashboardAccess.selectCity` is a defect of placement.** It is a command with a guard in front — it throws twice and writes through `defaultCity.choose(...)` — and `DashboardAccess` carries **zero** `@Transactional` anywhere, so that write is **untransacted** and #69's placement rule never had anything to fire on. It moves to a `SantCityPreferenceService` in the same ring, where throwing and mutating are the contract. `cityChip` also moves, to the query side; it assembles a view-model, not a decision. (`cityChip` is the softer call — only `selectCity` breaks the *stated* invariant, so if blast radius has to be cut, cut `cityChip` first.)

**Net: six engines → five.** Two deleted for having nothing to hide, one promoted for having something. **No two engines merge under any design considered.** That is subtraction, not consolidation.

### A portless engine is not an empty engine

`AuditLogAccess` ends the fold with **zero collaborators** and is **kept**. The deletion rule in 4.1 requires zero ports **and zero policy**; `AuditLogAccess` is portless and policy-full — ADR-0023's tier fold and `empty ⇒ Denied` are real policy. **This must be said explicitly, because a reader applying the deletion rule mechanically would delete it.** It keeps its `@Service` bean rather than becoming static, so it stays inside the constructor-injection convention and inside R3's reviewed list.

### Where a port implementation lives

**A port implementation lives in the ring that owns what it delegates to** — a database ⇒ `*-data-access`; an engine ⇒ `*-application-service`.

This retrodicts `AuditReadAccessAdapter`'s placement in `analytics-application-service` rather than excusing it: post-fold it makes **zero** queries, and putting it in `analytics-data-access` would force that module to depend on the application-service ring, inverting ADR-0019's ring order. #133's "layering oddity" is therefore **dissolved, not fixed**. The rule also covers `MadhyasthaKaryalayaMembership`'s adapter and any future engine-fronting port, so it is worth writing once rather than defending one file.

The adapter also stays a **separate class** from the engine it fronts: it is the only place carrying the *direction* of the dependency — identity asks, analytics answers — and its javadoc is where that contract is written. If a second cross-context audit question appears, that is the moment merging wins.

---

## Decision 5 — where the new types live

Decided by a compile-enforced rule, not by taste. ADR-0019 says `*-application-service` depends only on its own `*-domain-core` and `common-domain`, and explicitly **may not** depend on `common-application`. Every engine taking `CallerAuthority` lives in an `*-application-service` ring, across four contexts. `common-domain` is the only module all four can see — and it carries zero production dependencies, which a pure value type wants anyway.

Following the `CallerResolver` / `CurrentUserArgumentResolver` precedent hop for hop:

| Type | Ring |
| --- | --- |
| `CallerAuthority`, `RoleAssignment`, `NirdeshakScope` | **`common-domain`** |
| the one-method loader port | **`common-domain`**, beside `CallerResolver` |
| the edge resolver that populates it | **`common-application`**, beside `CurrentUserArgumentResolver` |
| the single adapter over `role_assignments` | **`identity-data-access`** (ADR-0029: identity owns that table) |

`RoleAssignment.role` stays the **wire string**, parsed to `Role` / `OversightRole` only where a method's return type needs it — which is what both surviving adapters already do.

---

## Decision 6 — the rules that hold the seam

Two invariants were proposed during this map and **both are rejected as stated.**

**"No class outside an engine may depend on an authority port" is false in production**, today and after the fold: `WebSessionService` holds `AuditReadAccess` and `SahNirdeshakCap` holds `SahNirdeshakCountLookup` — both legitimate carve-outs, neither an engine. (A third, `DashboardBffController` holding `MadhyasthaKaryalayaLookup`, evaporates in the fold — a REST controller holding an authority port.) A rule needing an exemption list covering a third of its own population on day one has the wrong predicate. Its true subject was never engines but `role_assignments` — re-read ADR-0029 clause 2 — so it is **replaced by R1**, not dropped.

**"No engine may hold a port that answers a question about the caller's own `role_assignments`" is true but inexpressible.** Caller-vs-target is a *meaning*, and nothing cheap reaches it. Two independent witnesses:

- *In SQL* — `JdbcReissueAuthorityLookup` reads `WHERE user_id = ? AND appointed_by = ?` where `user_id` is the **target**; one table away in `JdbcAuditScopeLookup`, `user_id` **is** the caller. The same lexeme denotes both.
- *In Java* — `boolean isSant(UUID userId)` is the same signature whether the argument is the caller (`DashboardAccess`) or the target (`PasswordReissueService`), and both readings are live in production one module apart.

ArchUnit, which sees strictly less than the SQL text does, is not close.

**This does not reopen Decision 4.** That invariant was wanted to stop a sixth service re-deriving a predicate from raw rows — and Decision 2 **removed the raw rows**. `CallerAuthority`'s private `List<RoleAssignment>` makes re-derivation a **compile** error, strictly stronger than a test. The sole residual path is a new adapter writing fresh `role_assignments` SQL below the port layer, and that path — only that path — is what R1 catches. So the invariant stands as **documentation, deliberately unenforced**, and this ADR says so in these terms because "the invariant has no test" is true and, read alone, misleading.

### What actually gets written

**R1 — `role_assignments` has one authority reader.** Outside `identity-data-access`, `role_assignments` may appear only in an allowlisted file, each entry carrying its ADR-0029-clause-1 justification. Three-way failure per house style (missing / exemption-now-a-lie / exemption-matches-nothing).

- **This is the first enforcement ADR-0029 has ever had.** ADR-0029 was written because the `role = 'SANT'` check had already been copy-pasted into three adapters across two contexts, and nothing has stopped a fourth since.
- **R1 cannot be an ArchUnit rule.** ArchUnit reads bytecode, and a SQL text block is a constant-pool entry it does not expose. R1 is a **source-text** check, and it must strip comments before matching: **32 non-test files outside `identity-data-access` mention `role_assignments`, but only 7 carry SQL** — the other 25 are javadoc, 10 of them in `common-domain`.
- **Today's allowlist is exactly 7 files**: analytics `JdbcAuditFeed`, `JdbcAuditScopeLookup`; attendance `JdbcCurrentRosterQuery`, `JdbcCurrentOccurrenceQuery`, `JdbcProxySabhaQueries`, `JdbcSanchalakSabhasQuery`; common `CallerVisibility`. **After the fold removes `JdbcAuditScopeLookup` it is 6** — recounted against R1's own predicate, not inherited. The surviving `JdbcSantLookup` does **not** raise that figure: it lives *inside* `identity-data-access`, where the table is permitted outright and no allowlist entry is needed.

**R2 — amend `every_handler_resolves_its_caller` rather than add to it.** Assert the `@CurrentUser` parameter's **type** is `CallerAuthority`. One predicate on an existing rule is the cheapest possible enforcement of the one-caller-parameter rule, and it is what makes a `UserId`-typed relapse fail instead of pass. #209's exemption map is unaffected and must not be rewritten.

**R3 — `authority_engines_return_a_decision_and_hold_no_transaction`.** A reviewed list of the five surviving engines: every public method returns `boolean`, a sealed type, or `Optional`; the class and its methods are not `@Transactional`. A reviewed list rather than a marker interface, because the repo's two existing markers earn their keep by carrying behaviour (`AggregateRoot`) or a dispatch contract (`DomainEvent`) — one carrying neither is the test leaking into the domain, which ADR-0019's ring discipline exists to prevent — and because a list **detects its own staleness** where a marker on a deleted class vanishes silently. Retrodiction: R3 catches `DashboardAccess.cityChip`, which returns a DTO record.

**R4 — `caller_authority_stays_out_of_the_inner_and_outer_rings`.** No `*-domain-core` and no `*-data-access` / `*-messaging` class may depend on `CallerAuthority` (ADR-0019). Plain ArchUnit — it keys off one named type, so it needs no marker and no list. Verified non-vacuous: the poms do not close this — `VisibleSections`, in `identity-domain-core`, already imports `org.sabha.common.Role`, so `common-domain` is visible from the innermost ring.

**"An engine never throws or mutates" stays javadoc.** R3 catches the DTO-returning half; a return-type whitelist cannot see a write. `DashboardAccess.selectCity` returns a sealed `DashboardScope` and would pass R3 while writing. The instrument that found it was human inventory, and the fix is relocation (Decision 4.4), not a rule.

**No `@Transactional` amendment is needed for `ReissueAuthorization`**: it lands in `identity-application-service`, which #69's rule already permits, and the `@Transactional` member of `PasswordReissueService` is the reissue *command*, which stays behind.

---

## Where this verdict stops

**Everything above is about the *caller*.** The discriminator is one question: *whose rows answer this?*

- **The caller's own rows in `role_assignments`** — one row-set per request, one key, one query. This folds to the edge, because there is exactly one caller per request and the snapshot is cheap, uniform and reusable.
- **Anything keyed by a target** — a Sabha, an appointee, a Kshetra — does **not**. A target is not a request-scoped singleton; one request may touch many. There is no snapshot to take at the edge, and taking one would be guessing.

The sabha-owned lookup family is the worked example on the other side of that line, and it reaches the opposite answer: it re-partitions **by subject** and stays a set of ports. See [ADR-0033](0033-sabha-lookups-re-partition-by-subject.md).

**The specific misreading this section exists to prevent:** *"`CallerAuthority` worked, so `SabhaFacts` should be resolved at the edge too."* It should not, and ADR-0033 gives the measurements. Six identity adapters ran one statement against one relation; the sabha five span **four tables and six key shapes**. The mechanism transferred only because the shape did.

---

## Consequences

- ADR-0027 keeps its ruling, its §3 and its §4; §1, §5 and §6 are superseded here, and its §2 inventory omitted `ReissueAuthorityLookup` and wrongly recorded that `DashboardAccess` reads no `role_assignments` (false since [#79](https://github.com/JayminPatel007/attendance-tracking-system/issues/79)).
- `@CurrentUser` resolves to `CallerAuthority` at all 53 caller-bound handlers. `GET /bff/dashboard/thresholds`'s javadoc must be **rewritten**, not amended: "binding it is what requires a resolved local User" is still true but now understates what binding costs.
- Six adapters become one. A dashboard request drops from **eight SQL statements to two**. Engine unit tests construct a `CallerAuthority` literal instead of building fakes; `StructuralScopeAuthority`'s four fakes go with it, and `AuditReadAccess`'s fake becomes the lambda `caller -> true`.
- **`WebSessionServiceTest` must be written.** `AuditReadAccess` appeared on the zero-fake list for a mundane reason — `WebSessionService` has no unit test at all — and the fold is what makes writing one cheap. The remediation for that finding was always "write the test", not "delete the port".
- **`describe` will re-fetch a `users` row the edge just read**, for `username()`. The tempting fix — putting `username` on `CallerAuthority` — is exactly what the stopping rule forbids. **Leave the re-fetch.** It is the first time the stopping rule costs something, and a rule that has never refused anything has not been tested.
- `docs/wiki/patterns/authorization.md`'s `Deviations` list must be **rewritten, not corrected**: its "deliberate duplication, so a tier's authority cannot drift" bullet defends a duplication that existed only because two ports existed.
- The four-enum `role` taxonomy is untouched and stays sealed behind `CallerAuthority`'s private rows ([#232](https://github.com/JayminPatel007/attendance-tracking-system/issues/232)).

### Corrections to the record

Three cost figures cited in #133 and on this map are wrong, and two of them are the **same error** — a field count read as a call count:

1. **`StructuralScopeAuthority` makes one round trip per request, not "up to four".** It *holds* four ports, but its callers take four mutually exclusive branches, one query each.
2. **`SabhaDefinitionAuthorization` is three pass-throughs, not four.**
3. In the other direction: **`WebSessionService.describe` issues eight SQL statements today, not four.** The four-port count misses that `canRead` fans out to five, `JdbcAuditScopeLookup` alone being three `SELECT DISTINCT`s. **Eight becomes two** — the best number on this map, and the one figure corrected upward.

Also corrected: #133's single-consumer claim covers **six** ports, not four; `JdbcNirikshakAssignmentLookup` did **not** "forget" a `revoked_at` filter — that table has no such column, revocation is row deletion; and `NirikshakAssignmentLookup.sabhasAssignedTo` has **zero** production consumers.

---

## What would change our mind

- **A caller-bound entry point that is not a request** — a queue consumer, a webhook replaying a user action. Today the only non-request path touching these ports takes the `Cron` branch. At two resolution sites, "resolve once at the edge" needs re-examining, though `TransitionActor` is already the shape that would absorb it.
- **The zero-authority handler population growing past ~15% of caller-bound handlers.** Two of 53 is noise; at 8 or 10 the two-parameter design becomes right, and the migration is a widened `supportsParameter`, not a redesign.
- **`CallerAuthority` outgrowing one query.** The entire cost case is that the fold is *one* query against *one* relation. If the stopping rule is ever breached and the edge loads two or three relations eagerly, the arithmetic above is void.
- **A question arrives that is caller-keyed but not a `role_assignments` projection, and folding it is obviously right anyway.** Then the stopping rule is wrong, not the fold — and it must be **re-drawn explicitly, not stretched silently.** Stretching it once is how this type becomes the module ADR-0027 rejected.
- **A future question needs the demographic conditionally.** Split the method; do not add the branch. If several such splits pile up and the pairs stop being legible side by side, two ports split by shape is the fallback.
- **The inline predicate reads drift.** If a sixth service re-derives Sanchalak-or-Sah-Sanchalak instead of calling `runsSabha`, inlining has failed empirically and promoting every predicate to an engine is the answer.
- **A second consumer of `AuditReadAccess` appears outside identity** — or three or more all outside analytics, at which point the audit tier rule has become shared vocabulary rather than analytics policy, and moving it becomes right.
- **A second `appointed_by`-keyed predicate never appears**, leaving `ReissueAuthorization` a one-method engine nobody wanted. That would not reverse the promotion, but it would mean the promoting rule should be restated — and "consult a port ⇒ engine, *unless the port is target-keyed and single-use*" is a worse rule, so watch for the second one.
- **R1's allowlist passing ~12 entries.** A line admitting that many exceptions has stopped being a line, and ADR-0029 needs re-deciding rather than re-listing.
- **The asymmetry with ADR-0033 proving unteachable.** If reviewers repeatedly try to apply this ADR's edge mechanism to the sabha ports despite [Where this verdict stops](#where-this-verdict-stops), the cost is in the documentation, not the design — and the remedy is to **merge the two ADRs into one document**, which was the live alternative when this split was chosen.
