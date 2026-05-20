# Platform Split: Mobile for Sabha-Level Operations, Web for Everything Else

The mobile (Flutter, iOS/Android) app is used exclusively by Sanchalak and Sah-Sanchalak, for People-Directory registration and Attendance Marking. Every other role (Nirikshak, Nirdeshak, Sah-Nirdeshak, Sanyojak, Sant, Madhyastha Karyalaya members) uses the web application, which handles analytics and role assignments / unassignments.

## Why this split rather than a single universal app

Sabha-level Karyakars need a fast, offline-tolerant capture surface that lives in their pocket during a gathering — that's the mobile sweet spot. Higher tiers do read-heavy analytics and occasional structural changes (assigning a Nirikshak, reopening a Finalized Occurrence) — that's the web sweet spot. Forcing one platform to serve both would compromise both: the mobile app would carry analytics weight it doesn't need, and the web app would carry a capture flow that almost never gets used there.

## Consequences

- Mobile app permission model is narrow: own Sabha(s) only, attendance + directory ops.
- Web app exposes the full authorization matrix (Q15) for everything else.
- A Karyakar who is *both* a Sanchalak and (say) a Nirikshak uses both apps — one identity, two surfaces.
- Attendance marking does not need to be implemented on web at all (which means a Sanchalak without a smartphone is blocked — flag as a known constraint).
