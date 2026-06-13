# Structural Creation Authority Lives at the Tier Above

**Status**: accepted; **Zone row superseded by [ADR-0024](0024-zone-creation-moves-to-regional-team.md)** (Zone creation moved MK → Regional Team). The tier-above principle and all other rows below still stand. Deletion semantics for these entities are defined in [ADR-0026](0026-deletion-model.md).

Each structural entity in the hierarchy is created by the role that owns the tier *above* it (or, for Madhyastha Karyalaya, owns one or two tiers above). Concretely:

| Created entity | Created by |
|---|---|
| **Sabha** | Nirdeshak (within their Kshetra × Sabha kind scope) |
| **Kshetra** | Sanyojak (within their Zone) |
| **Zone** | Madhyastha Karyalaya (within their State) |
| **City** | Madhyastha Karyalaya (within their State) |
| **Sabha Kind** | Madhyastha Karyalaya (system-wide registration) |

## Why each tier creates downward, not upward or by ops

Tier-above creation makes the org structure self-serve: as the organization expands (new Kshetra opens, a Sanchalak gets a Sabha approved), the people closest to the decision make it directly, without an ops-team round-trip. The model also keeps the audit story clean — every structural change is attributable to a known User with the appropriate scope, no shared admin account.

## Implication for Sabha Kind

The "Sabha Kind" list is **extensible** (not a hardcoded enum) because Madhyastha Karyalaya can register new ones — e.g., when the youth-level selective program (currently TBD-name) gets its canonical name, or if a new demographic category emerges. This refines ADR-0005's framing ("Sabha kinds baked in as first-class domain concepts"): the kinds *are* first-class — each has a name and an identity in the model — but the set of kinds is data, not code. Adding a new kind doesn't require a code change; it requires a Madhyastha Karyalaya action, which then triggers the rest of the system (parallel role assignments, Home Sabha eligibility) to incorporate it.

## Consequences

- Cities, Zones, Kshetras, Sabhas, and Sabha Kinds are all aggregates (or aggregate-referenced reference data) with a `createdBy` audit field pointing to the User who created them.
- The system needs onboarding flows for each tier — e.g., when a Sanyojak creates a Kshetra, the immediate next step is assigning Nirdeshaks for each Sabha kind (otherwise the Kshetra is structurally incomplete).
- Sabha Kind registration is rare but high-impact: every existing geographic node (Kshetra, Zone) becomes eligible to host a new Sabha of that kind, which means the parallel role structure now has an empty slot per node. UI should make this visible.
