# Single-Organization System, Not Multi-Tenant SaaS

The system is built for one specific religious organization, with its hierarchy (State → City → Zone → Kshetra → Sabha), its named role tiers (Sanchalak → Sah-Sanchalak → Nirikshak → Nirdeshak → Sah-Nirdeshak → Sanyojak → Sant → Madhyastha Karyalaya), and its Sabha kinds (Baal, Balika, Yuvak, Yuvati, Sanyukta — possibly extended by BSS) baked in as first-class domain concepts. No tenant abstraction exists.

## Why this and not multi-tenant SaaS

The ubiquitous language is the whole point of using DDD here. Multi-tenancy would force the role names, the geographic tiers, and the Sabha kinds to become tenant-configured strings — collapsing "Sanchalak" and "Nirdeshak" into "role_id_1" / "role_id_2" and losing the domain precision the whole project depends on. The cost of building one rich domain model is dramatically less than the cost of building a generic configurable framework.

## Consequences

- All schema, code, and UI can reference the named domain entities directly (e.g., `SanchalakRepository`, `BaalSabhaScheduler`) without indirection through a tenant config.
- If a different organization later wants the system, the right move is a fork-and-rename, not retrofitting tenancy.
- Scale is still meaningful (the single organization spans States → Cities → Zones → Kshetras with thousands of Sabhas and tens of thousands of People), so the system must still be built for serious load — single-tenancy is a *modeling* decision, not a "small system" decision.
- **One deployment across all States.** Despite each State having its own Madhyastha Karyalaya, the system runs as a single deployment with a single database, covering every State the organisation operates in. Cross-State Person moves are a normal Verified Home Sabha Transfer with a destination in another State; Person identity (mobile-keyed per ADR-0013) is system-wide; MK analytics default to their own State but can include cross-State comparisons since the data is org-wide. The State boundary is an *authorization* and *roll-up* concept, not a deployment boundary.
