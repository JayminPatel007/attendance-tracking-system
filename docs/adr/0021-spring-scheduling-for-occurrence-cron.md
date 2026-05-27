# Spring Scheduling for Occurrence cron jobs

**Status**: accepted.

Slice 3 adds two time-driven Occurrence state transitions: auto-Open (per-minute) and auto-Finalize (hourly, 24h after scheduled end per [ADR-0001](0001-sabha-occurrence-lifecycle.md)). The issue specifies the choice of scheduling framework — Spring Scheduling or Quartz — must be captured in an ADR.

## Decision

Use **Spring Scheduling** (`@EnableScheduling` + `@Scheduled`).

## Why not Quartz

Quartz's distinguishing capabilities — clustered job persistence, misfire policies, distributed locking — are valuable only when a single logical job must run exactly once across multiple replicas. Today the system has a single fat-jar deployable (`application-container`, [ADR-0019](0019-bounded-context-module-taxonomy.md)) and runs as one process; multi-instance deployment is not on the near roadmap. Adopting Quartz now means:

- A non-trivial extra dependency surface (its own tables, configuration, persistence wiring) we'd carry from day one.
- Operational concepts (job stores, listeners, triggers) the team would need to learn for a problem we don't have.
- A migration cost at the moment we *do* scale to multiple replicas anyway — but at that point we'll know what we actually need.

Spring Scheduling, by contrast, ships with `spring-context` (already on the classpath via the application-container's starters) and has zero additional setup. Cron expressions are first-class. The two jobs in this slice are stateless scanners — each pass re-derives the work from current Occurrence state, so a missed firing is self-healing on the next tick.

## What this commits us to

- Cron methods live on `application-container` (per [ADR-0019](0019-bounded-context-module-taxonomy.md): scheduling is a deployment-tier composition concern, not a bounded-context feature). The scanners themselves are application services in `attendance-application-service`; the container holds only the `@Scheduled`-annotated entry points.
- Cron expressions are externalised via configuration (`sabha.cron.auto-open`, `sabha.cron.auto-finalize`) with sane defaults baked into the annotation, so they can be overridden per environment without a code change.
- The scanner application services accept a `java.time.Clock` (default bean: `Clock.system(...)` with a configurable zone). Integration tests substitute a mutable clock to simulate time advance without `Thread.sleep` — a key affordance for the cron integration test promised by Slice 3.
- Each `@Scheduled` method catches and logs `RuntimeException` so one failing scan doesn't poison the scheduler thread for later firings.

## When we'd revisit

The moment we run more than one instance of `application-container` against the same Postgres. At that point the two scanners would race — both replicas would attempt the same transition, lose the optimistic-lock contention, and one would retry needlessly. The fix at that point is either Quartz with a clustered job store, ShedLock against the same Postgres, or moving the cron into a sidecar that's deployed singleton. Capture the choice in a follow-up ADR when the multi-instance shape is concrete.
