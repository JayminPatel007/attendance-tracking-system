# Mobile App is Offline-Capable for Attendance Marking Only

The Sanchalak / Sah-Sanchalak mobile app supports offline attendance marking against a Roster cached on first sync, with markings queued locally and pushed to the backend when connectivity returns. All other mobile operations (creating ad-hoc Sabhas, adding People to the Directory, Verified Home Sabha Transfer) require live connectivity. Conflict resolution between Sanchalak and Sah-Sanchalak edits to the same Occurrence uses **last-write-wins per `(Occurrence, Person)`**. The app refuses to mark new attendance if the local Roster is older than **7 days**, forcing a sync first.

## Why not online-only

Sabha venues commonly have poor or no connectivity (community-hall basements, rural mandirs). Attendance marking happens in those exact moments. Forcing online-only would push Sanchalaks to paper and create a "type it in later" workflow that defeats the point of having the app.

## Why not fully offline-first

Operations beyond marking (transfers, directory edits, ad-hoc creation) are inherently coordination-heavy — Verified Home Sabha Transfer literally needs a network round-trip for the OTP — and almost never happen during the Sabha itself. Making them offline-capable would add multi-way conflict scenarios for very rare value.

## Why last-write-wins per (Occurrence, Person)

The conflict surface is one specific Person's presence at one specific Occurrence; both Karyakars (Sanchalak and Sah-Sanchalak) are typically intending to record the same factual presence. A conflict-resolution UI would demand more sophistication than the Sanchalak persona reliably has, for a class of conflict that is operationally rare and almost always non-substantive.

## Consequences

- Mobile app needs a local SQLite store (cached Roster, today's Occurrence, pending-sync queue) and a sync protocol with idempotent push.
- 7-day staleness cap is a server-enforced contract (server rejects markings where the client's roster-version is older than 7 days) plus a client-side guard.
- The backend needs to accept Attendance Markings out-of-order with deterministic last-write-wins semantics (use the client-side `markedAt` timestamp, server arrival time is a tiebreaker only).
- No support for the same Karyakar editing offline from two devices concurrently — accepted; treat as undefined behavior.
- A Walk-in who is *not yet* in the Directory cannot be marked during an offline Sabha. The Sanchalak captures contact details on paper, then post-Sabha runs the live "Add Person" flow (with full-Directory de-dup search and Home Sabha assignment). If the Occurrence has already auto-Finalized in the meantime, back-filling the Walk-in marking requires a Nirikshak/Nirdeshak reopen per ADR-0001 — acceptable cost for a rare case.
