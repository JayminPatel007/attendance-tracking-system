# Persistence stays on JdbcClient (no JPA); aggregate lifecycles stay in-aggregate (no Spring State Machine)

**Status**: accepted. Records the outcome of the framework-improvements review (2026-06-10), items R1 + R2; rejects two technology adoptions. Documentation only — no code changes.

Two recurring "should we adopt framework X" suggestions keep surfacing in architecture reviews: convert the hand-written `JdbcClient` repositories to JPA, and externalize the Occurrence lifecycle into Spring State Machine. Both were evaluated and rejected. This ADR records the load-bearing reasons and the explicit revisit conditions so future reviews do not re-litigate them. Neither rejection is a blanket rule — [ADR-0019](0019-bounded-context-module-taxonomy.md) permits JPA in `*-data-access`; these are judgments about *this* codebase as it stands today.

## 1. JPA conversion of the JdbcClient repositories — rejected

The driven-side repository adapters in each `*-data-access` module are hand-shaped `JdbcClient` lambdas. The suggestion is to replace them with JPA entities + a `JpaRepository`-style layer. Rejected.

### Why reject

**It fights the pure-Java aggregate.** [ADR-0020](0020-aggregate-root-and-domain-events.md) keeps `*-domain-core` pure Java: aggregates are classes (not records) with `final` fields, private constructors, mutation methods that enforce invariants and `registerEvent(...)`, and explicit `rehydrate()` factories — no Spring, no JPA, no persistence annotations. JPA wants entities it can instantiate reflectively, mutate field-by-field through a no-arg constructor, and track via a dirty-checking proxy. Reconciling that with the ADR-0020 shape means **a parallel set of JPA entity classes** plus two-way mapping between them and the aggregates — a new shallow layer whose only job is translation, exactly the kind of low-depth-per-line code [the deep-module discipline](0027-no-shared-granted-scope-module-behind-the-authorization-engines.md) tells us to avoid. The alternative — annotating the aggregates directly — would drag JPA into `*-domain-core` and break the "Entities ring: pure Java, none" rule in ADR-0019's Clean-Architecture mapping.

**Everything JPA would give us is already solved, leaner.** Optimistic locking — JPA's headline feature here — is already provided by `AggregateRoot.version` plus the explicit `UPDATE ... WHERE id = ? AND version = ?` / `rowsAffected == 0` check and the bounded retry loop in the application service (ADR-0020). That retry is deliberately a visible `for` loop in the use case, not a framework-managed `@Version` exception; adopting JPA's `@Version` would *hide* the very control flow ADR-0020 chose to keep explicit. Row mapping is a handful of `JdbcClient` lambdas per repository — small, obvious, and co-located with the SQL.

**Half the data-access layer is exactly where JPA is worst.** A large share of the SQL is hand-shaped CQRS read-model query: the UNION audit feed that resolves geography per branch ([ADR-0023](0023-audit-log-read-model-and-viewer-authority.md)), role-scope predicates, and the analytics projections that [ADR-0008](0008-single-bounded-context-with-internal-seams.md) mandates be projections rather than ad-hoc joins. JPA/JPQL is at its weakest on UNION-of-heterogeneous-tables and bespoke scoped predicates; those would drop to native queries anyway, so JPA would cover only the easy half while complicating the hard half.

### Revisit condition

If repository boilerplate becomes *genuine, measured* pain (e.g. write-side repositories proliferate and the row-mapping lambdas become a real maintenance cost), evaluate **Spring Data JDBC first** — not JPA. Spring Data JDBC keeps the no-dirty-checking, save-the-whole-aggregate model that matches ADR-0020's aggregate-as-unit-of-write far better than JPA's entity-graph model does. JPA remains the last option, not the first.

## 2. Spring State Machine for the Occurrence lifecycle — rejected

The Occurrence lifecycle ([ADR-0001](0001-sabha-occurrence-lifecycle.md)) is the suggested home for Spring State Machine (SSM): model `Scheduled ↔ (Rescheduled | Cancelled) → Open for Marking → Finalized` as an SSM configuration with states, transitions, and guards. Rejected.

### Why reject

**The transition guards *are* the business rules, and they belong on the aggregate.** The Occurrence's five states are governed by domain invariants that are anything but generic plumbing: who may cancel or reschedule (Sanchalak or Nirikshak-proxy only, Sah-Sanchalak excluded — ADR-0001), the 24-hour grace window for retroactive cancel and auto-finalize, and which Kshetra tiers (Nirikshak / Nirdeshak / Sah-Nirdeshak) may reopen a Finalized Occurrence. [ADR-0020](0020-aggregate-root-and-domain-events.md) places exactly these invariants inside the aggregate's mutation methods (`open()`, `cancel()`, `reschedule()`, `finalize()`), each validating preconditions and emitting a domain event.

**SSM would hollow the aggregate into a state holder.** SSM externalizes transitions into builder configuration and runtime machine instances. Adopting it would move the guard logic out of the aggregate and into SSM config, leaving `Occurrence` as a passive state field — re-introducing precisely the anemic shape ADR-0020 was written to forbid. Tests would then have to stand up and cross a state-machine framework instead of calling a method on the aggregate and asserting on the result; the rich-domain testability ADR-0020 buys would be traded for framework ceremony.

**The lifecycle isn't shaped like SSM's strengths.** SSM earns its keep on hierarchical, parallel, or asynchronous/event-driven state machines. The Occurrence lifecycle is a flat, five-state, synchronous machine driven by user actions and one scheduled job (auto-finalize, [ADR-0021](0021-spring-scheduling-for-occurrence-cron.md)). A `switch`/guard on an enum inside the aggregate is the right-sized tool.

### Revisit condition

Revisit if the lifecycle becomes **hierarchical** (nested sub-states) or **asynchronous** (transitions driven by external events rather than user actions / the one finalize cron) — none of which exists today.

## Consequences

- `*-data-access` repositories stay on `JdbcClient`; aggregates keep their ADR-0020 pure-Java shape with `AggregateRoot.version` + application-service retry as the locking mechanism.
- The Occurrence lifecycle stays in-aggregate; `Occurrence`'s mutation methods remain the single home of its transition guards.
- ADR-0019's permission to use JPA in `*-data-access` is unchanged — this ADR is a judgment for the current codebase, not a prohibition.
- A future review proposing JPA or SSM should engage the revisit conditions above (measured boilerplate pain → Spring Data JDBC first; hierarchical/async lifecycle for SSM) rather than re-opening the general question.
