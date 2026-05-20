# Single Bounded Context, with Internal Package Seams

The system is built as one bounded context inside one Spring Boot application backed by one database, with the ubiquitous language defined in `CONTEXT.md` as the single source of truth. Internally the code is organized into packages — `identity`, `sabha`, `attendance`, `analytics` — that talk to each other through narrow application-service or domain-event interfaces, *not* through cross-package reach-ins. The intent is to keep the option of extracting any of these into its own context (most likely `analytics`) without rewriting the domain.

## Why not multi-context now

Multi-context buys two things: team independence (different teams own different contexts and deploy on their own cadence) and scaling decoupling (the noisy write-side doesn't drag down read-side dashboards, or vice versa). Neither is currently the bottleneck — single organization, presumably small team, modest concurrent load. Paying the multi-context overhead (separate apps, event infrastructure, duplicated reference types) up front means inventing problems we don't have.

## Why the internal seams matter

Coupling that's invisible during single-context life becomes a year-long migration when extraction is finally needed. By treating package boundaries as quasi-contracts from day one — `attendance` doesn't import from `analytics`, `sabha` doesn't reach into `identity`'s aggregates — we preserve the ability to lift `analytics` (the most natural extraction candidate) into its own service when it earns one.

## Consequences

- Hexagonal architecture applies *within* this single context: domain at the core, application services as use cases, adapters at the edges (REST, JPA, SMS, push).
- Analytics read models should be built as projections (event-driven or background-refresh), not as ad-hoc joins on transactional tables — this keeps dashboard performance defensible at scale and pre-builds the seam if Analytics ever needs to leave.
- Cross-package communication uses application services or domain events. Reach-ins (e.g., an attendance service grabbing a Person's repository directly) are prohibited by code review, not enforced by tooling — accepted tradeoff for now.
