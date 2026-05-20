# Sanchalak-Initiated Home Sabha Transfer Requires Person Verification

A Sanchalak (or Sah-Sanchalak) can pull a Person into their own Sabha as the Person's new Home Sabha, but the operation only completes after the Person confirms it out-of-band (OTP to their registered mobile number, or equivalent). Higher-tier Karyakars (Nirikshak / Nirdeshak / Sanyojak) can reassign Home Sabhas within their scope *without* Person verification.

## Why this and not "only Nirdeshaks can change Home Sabhas"

A pure top-down model creates friction for the common, legitimate case (Person moves to a new neighborhood, attends the local Sabha, wants their Home Sabha to reflect reality). Routing every such change through the Nirdeshak makes attendance records perpetually stale.

## Why verification rather than letting Sanchalaks act freely

Without verification, a Sanchalak could silently inflate their Sabha's Roster (or, more realistically, capture a Person who attended once as a Walk-in and never agreed to switch). The OTP step makes the Person an explicit participant in the decision, preserving the integrity of the Roster — and therefore of every analytics number derived from it.

## Consequences worth flagging

- Requires a verified mobile number on every Person in the Directory, and a working SMS path.
- Needs a fallback for People without mobile numbers (children, elderly) — likely the top-down path through their Nirdeshak.
- **One exception, documented in ADR-0011**: the *initial* Home Sabha assignment when an appointer creates a new Person record as part of a role appointment does not require OTP — the Person is brought into the Directory and accepts the role at the same moment.
