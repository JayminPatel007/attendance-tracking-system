# Role Appointment Authority

Every Karyakar role and oversight role in the hierarchy has exactly one source of appointment. The chain is:

| Role (appointee) | Appointee's scope | Appointed by | Appointer's scope |
|---|---|---|---|
| **Sanchalak** | (Kshetra, demographic, track) | Nirdeshak | (Kshetra, demographic) |
| **Sah-Sanchalak** | (Kshetra, demographic, track) | Nirdeshak | (Kshetra, demographic) |
| **Nirikshak** | (Kshetra, demographic) — Regular track only | Nirdeshak | (Kshetra, demographic) |
| **Sah-Nirdeshak** | (Kshetra, demographic) | Nirdeshak (the principal themselves) | (Kshetra, demographic) |
| **Nirdeshak** | (Kshetra, demographic) | Sanyojak | (Zone, demographic) |
| **Sanyojak** | (Zone, demographic) | Regional Team | (City, demographic) |
| **Regional Team member** | (City, demographic) | Madhyastha Karyalaya | State |
| **Sant** | (City, [demographic…]) — flexible per Sant | *not appointed* — MK creates the User record administratively | — |
| **Madhyastha Karyalaya member** | State | Other existing MK members (with a one-off install-time bootstrap for the first) | State |

The **track** dimension (Regular / BSS / YSS) only differentiates the Sanchalak / Sah-Sanchalak tier. Every role above is per-demographic and track-shared, and Nirikshak exists only for the Regular track. See ADR-0006 for why higher tiers are track-shared.

This is ADR-0011's companion to **ADR-0009 (structural creation authority)**: 0009 covers *who creates the geographic / structural entities*, 0011 covers *who appoints the role-holders that operate them*.

## Why Nirdeshak appoints four roles below them (not just Sanchalak)

The Kshetra × kind tier is where most operational decisions about who should run what happen. Concentrating the appointment of Sanchalak, Sah-Sanchalak, and Nirikshak with the Nirdeshak — rather than splitting them across tiers — keeps the chain of accountability tight: when a Sabha goes wrong, there is one Nirdeshak who chose the team and is answerable for it.

## Why Nirdeshak appoints their own Sah-Nirdeshak (the surprise)

A deputy is normally appointed by the tier *above* the principal. Here, the Nirdeshak appoints their own Sah-Nirdeshak. The reason: the Sah-Nirdeshak's value is in being the Nirdeshak's chosen backstop for vacations, pilgrimages, or sudden absence — somebody the Nirdeshak personally trusts to run their (Kshetra, Sabha kind). Routing this through the Sanyojak would slow the decision and produce a deputy the Nirdeshak hadn't picked. The Sanyojak retains the higher lever — replacing the Nirdeshak outright if the choice of deputy reflects bad judgment.

## Why a Regional Team between MK and Sanyojak

A direct MK → Sanyojak appointment line was the obvious alternative, and was rejected for two reasons:
- A State has many Cities, each with many Zones, each with many Sabha kinds — the MK would be appointing dozens to hundreds of Sanyojaks across the State, far from the day-to-day operational pulse.
- The Regional Team sits at the City × Sabha-kind level, close enough to evaluate Sanyojak candidates on real evidence ("how is this person running their Zone in our City?"), and matches the Sant's natural scope (so Sant–Regional Team coordination on city-level decisions is structurally easy).

## Why Sants aren't "appointed" by the system

A Sant's religious appointment happens entirely outside any software. The system models it as an *administrative recording* by a Madhyastha Karyalaya member rather than an "appointment" — the credentials issued (per ADR-0004) give the Sant a login, nothing more. Treating this as an appointment would falsely imply the system grants a religious office that already exists.

## Consequences

- Every Karyakar role record carries `appointedBy` (User ID) and `appointedAt` (timestamp) for the audit trail.
- Sah-Nirdeshak record additionally carries the appointing Nirdeshak's User ID. If that Nirdeshak is later removed, the Sah-Nirdeshak appointment doesn't auto-revoke — the replacement Nirdeshak inherits the standing Sah and may choose to replace them.
- A Regional Team member record carries `appointedBy` (MK member User ID), enabling MK-level accountability for the City-tier oversight.
- The bootstrap problem (who appoints the first MK member?) is solved by an explicit install-time seed step — one-off, outside the normal appointment flow.
- **Person creation as part of appointment.** A new Karyakar may not yet have a Person record in the Directory (e.g., moved in from another State). The appointer's UI integrates a Person search first; if no match exists, the appointer creates the Person record, assigns an initial Home Sabha, *and* creates the User account (per ADR-0004) in one flow. This initial Home Sabha assignment is the **single exception to ADR-0002's OTP requirement** — the Person is being created right now by the appointer, and their consent to be in the Directory is the same act as their acceptance of the role. Subsequent Home Sabha changes follow the normal Verified Home Sabha Transfer rule.
