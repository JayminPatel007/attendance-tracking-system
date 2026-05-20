# PRD-0001 — Attendance Tracking System (MVP)

**Status:** Ready for implementation
**Label:** ready-for-agent
**Domain reference:** [`CONTEXT.md`](../../CONTEXT.md)
**Architectural decisions:** [`docs/adr/0001`](../adr/0001-sabha-occurrence-lifecycle.md) through [`docs/adr/0013`](../adr/0013-directory-de-duplication-on-person-add.md)
**Prototypes:** [`prototypes/NOTES.md`](../../prototypes/NOTES.md)

This PRD synthesises a 24-question grilling session ([`CONTEXT.md`](../../CONTEXT.md) and ADRs 0001–0013) and a 12-prototype UI pass ([`prototypes/`](../../prototypes/)). Open questions raised by prototyping have been resolved in this document — see the *Implementation Decisions* section.

## Problem Statement

A religious organisation operates thousands of in-person gatherings (Sabhas) across multiple States in a deep hierarchy — State → City → Zone → Kshetra → Sabha — with tens of thousands of People attending weekly. Attendance is currently captured on paper or not at all. The organisation needs to know:

- **At the Sabha level:** who turned up, and (for the Sanchalak running the gathering) the ability to record it in seconds without internet access during the Sabha itself.
- **At every tier above the Sabha:** who is *drifting away* from their Home Sabha, so the responsible Karyakar can follow up personally before the Person is lost.

The current paper-based approach makes the second question structurally impossible to answer at any scale, and makes the first answer untrustworthy by the time it reaches higher tiers (rewrites, lost sheets, no audit).

## Solution

Two purpose-built apps that share one backend:

- **Mobile (Flutter, iOS/Android) — for Sanchalak and Sah-Sanchalak only.** Offline-capable attendance marking against a cached Roster. Person Directory management. Verified Home Sabha Transfer with OTP. Per-Occurrence operations (cancel, reschedule, venue override).
- **Web — for every other role (Nirikshak through Madhyastha Karyalaya, plus Sant).** Per-tier analytics dashboards (with the re-engagement candidate list as the headline). Role appointment flows with inline Person creation. Sabha definition. Structural admin. Sanchalak-proxy mode for Nirikshaks.

One Spring Boot backend, one Postgres database, single deployment across all States. Person identity is mobile-keyed and globally unique. The system encodes the organisation's ubiquitous language directly — no multi-tenant abstractions.

## User Stories

### Sanchalak (mobile)

1. As a Sanchalak, I want to mark each member of my Roster present or absent in a single tap, so that I can finish marking everyone within the time the gathering takes.
2. As a Sanchalak, I want to mark attendance even when my Sabha venue has no internet, so that connectivity does not push me back to paper.
3. As a Sanchalak, I want to see when my Roster was last synced with the server, so that I know my cached data is current before the gathering starts.
4. As a Sanchalak, I want the app to block me from marking with a Roster older than 7 days, so that I cannot record attendance against a stale list.
5. As a Sanchalak, I want to add a Walk-in (a Person attending my Sabha who is not on its Roster), so that visitors get counted without joining my standing Roster.
6. As a Sanchalak, I want to search the wider Directory by name or mobile when adding a Walk-in (online only), so that I find the existing Person rather than create a duplicate.
7. As a Sanchalak, I want to add a brand-new Person to the Directory (when I have connectivity), so that someone not yet in the system can be on my Roster going forward.
8. As a Sanchalak, I want the system to hard-block me if I enter a mobile number that matches an existing Person, so that I cannot create a duplicate.
9. As a Sanchalak, I want to initiate a Verified Home Sabha Transfer when a Person from elsewhere asks to join my Sabha, so that the Roster reflects reality with the Person's own OTP-verified consent.
10. As a Sanchalak, I want to cancel an Occurrence (with reason) ahead of time, so that analytics consumers know that absence wasn't a turnout problem.
11. As a Sanchalak, I want to revert a Cancelled Occurrence if the Sabha runs anyway, so that I can still record who attended.
12. As a Sanchalak, I want to reschedule a single Occurrence without affecting the Sabha's standing schedule, so that one-off shifts (festivals, hall closures) don't require reconfiguring the Sabha.
13. As a Sanchalak, I want to set a one-off venue override for an Occurrence, so that the record reflects the actual location when we meet somewhere else.
14. As the Sanchalak of a BSS or YSS Sabha, I want to manually create this month's Occurrence on a date I choose, so that the monthly cadence matches when the gathering can actually happen.
15. As a Sanchalak, I want to log in once and change my initial password, so that the appointer who set my credentials cannot continue to access my account.
16. As a Sanchalak, I want to recover access via OTP to my registered mobile if I forget my password, so that I do not need to wait for my appointer to reissue.

### Sah-Sanchalak (mobile)

17. As a Sah-Sanchalak, I want to share the marking workload on my assigned Sabha (attendance, Walk-ins, Directory adds, Home Sabha Transfers), so that the Sanchalak isn't the sole bottleneck on the day.
18. As a Sah-Sanchalak, I want the app to prevent me from cancelling, rescheduling, changing the venue, or modifying the Sabha's standing schedule, so that Sabha-shaping decisions stay with the Sanchalak.

### Nirikshak (web)

19. As a Nirikshak, I want to see an analytics dashboard covering the 3–4 Sabhas assigned to me, so that I can spot which are losing engagement.
20. As a Nirikshak, I want to reopen a Finalized Occurrence (with a written reason), so that the Sanchalak can correct attendance errors discovered after the 24-hour grace.
21. As a Nirikshak, I want to act as Sanchalak for any of my assigned Sabhas when its Sanchalak is unavailable, so that the gathering can still be operated and marked.
22. As a Nirikshak, I want every action I take in proxy mode to be audit-logged as me (not as the Sanchalak), so that accountability is preserved.

### Nirdeshak / Sah-Nirdeshak (web)

23. As a Nirdeshak, I want to see attendance roll-ups across all Sabhas in my (Kshetra, demographic) scope (Regular *and* BSS/YSS), so that I can identify which Sabhas need intervention.
24. As a Nirdeshak, I want to define a new Sabha — choosing weekly-recurring or monthly-ad-hoc cadence, standing venue, and Sanchalak — so that organisational expansion is self-serve.
25. As a Nirdeshak, I want to appoint a Sanchalak, Sah-Sanchalak, Nirikshak, or Sah-Nirdeshak within my scope and set their User credentials in the same flow, so that onboarding is a single action.
26. As a Nirdeshak, I want the appointment UI to search the Directory first and create a new Person record inline only if no match exists, so that I don't create duplicates and the new Karyakar's identity is unified.
27. As a Nirdeshak, I want to approve or reject a Regular Sanchalak's BSS/YSS nomination, so that the selective Sabhas grow with my judgment in the loop.
28. As a Nirdeshak, I want to reopen Finalized Occurrences, so that I can correct attendance issues escalated to me.

### Sanyojak (web)

29. As a Sanyojak, I want Zone × demographic analytics, so that I can spot which Kshetras within my Zone are off track.
30. As a Sanyojak, I want to create Kshetras within my Zone, so that organisational expansion is self-serve.
31. As a Sanyojak, I want to appoint Nirdeshaks for (Kshetra, demographic) within my Zone, so that new Kshetras can be made operational.

### Regional Team member (web)

32. As a Regional Team member, I want City × demographic analytics, so that I can spot which Zones in my City are off track.
33. As a Regional Team member, I want to appoint Sanyojaks within my (City, demographic) scope, so that the Zone tier stays staffed.

### Sant (web)

34. As a Sant, I want to view analytics for any City in the State — with my self-selected default City landing first — so that I can spiritually oversee the wider organisation without being limited to my formal assignment.

### Madhyastha Karyalaya member (web)

35. As a Madhyastha Karyalaya member, I want State-level analytics, so that the entire State's health is visible to the oversight body.
36. As a Madhyastha Karyalaya member, I want to create Cities, Zones, and Sabha Kinds, so that structural changes are recorded and attributed to the specific member who made them.
37. As a Madhyastha Karyalaya member, I want to add other Madhyastha Karyalaya members, so that the body grows without depending on the original bootstrap account.
38. As a Madhyastha Karyalaya member, I want to appoint Regional Team members for each (City, demographic), so that the City tier stays staffed.
39. As a Madhyastha Karyalaya member, I want to create Sant User records (setting their username and initial password), so that Sants can log in and access the dashboards.

### Person (subject — no login)

40. As a Person, I want to receive an OTP whenever my Home Sabha is being changed, so that no Sanchalak can pull me into their Roster without my consent.
41. As a Person whose mobile is on file, I want my participation in Sabhas to require no direct system action from me beyond responding to OTPs, so that I can focus on attending.

### System / cron

42. As the system, I want to materialize weekly Occurrences on a rolling forward window, so that the Sanchalak's app shows future weeks ready for marking.
43. As the system, I want to auto-Finalize each Occurrence 24 hours after its scheduled end time, so that analytics consumers see a stable historical record.
44. As the system, I want to surface a soft warning to a monthly-ad-hoc Sabha's Sanchalak (and visibility to the Nirdeshak) when the month is past its midpoint with no scheduled Occurrence, so that the monthly cadence rule isn't quietly violated.
45. As the system, I want every action by every actor (including proxy actions) to be audit-logged with actor User ID, timestamp, target, and (for proxy actions) the User being proxied for, so that accountability is traceable end-to-end.

### Bootstrap

46. As an installer, I want a one-off seed step that creates the first Madhyastha Karyalaya member for the deployed State, so that the system has a credentialled starting actor from whom every other role can be appointed.

## Implementation Decisions

### Backend modules

One Spring Boot application, one Postgres database, single deployment per ADR-0005 and ADR-0008. Four packages plus shared kernel. Cross-package communication goes through application services or domain events — no reach-ins.

#### `identity` — Persons, Users, Roles, Authorization

- Person Directory with mobile-keyed identity (ADR-0013). Mobile required at creation; unique system-wide; parent/guardian linking for children without their own phone.
- User account lifecycle (a Person becomes a User when assigned a role; loses User status when all roles revoked; Person record persists).
- Role assignments scoped per the role-scoping table in CONTEXT.md (Sanchalak/Sah-Sanchalak per `(Kshetra, demographic, track)`; Nirikshak Regular-only; Nirdeshak/Sah-Nirdeshak/Sanyojak/Regional Team per `(geographic level, demographic)`; Sant per `(City, demographics…)`; MK per State).
- Appointment workflow per ADR-0011 with the Person-create-at-appointment exception per ADR-0002.
- OTP infrastructure used by both Verified Home Sabha Transfer (ADR-0002) and password reset (ADR-0004).

**Deep extraction — Authorization Engine.** Public interface:
```
canUserDo(userId, action, target) → Decision
```
Hides role scoping, demographic-vs-track scope, proxy authority (Nirikshak-as-Sanchalak), tier hierarchy traversal, and the Sabha-shaping vs day-of marking permission split (Sah-Sanchalak excluded from `cancel | reschedule | venue-override | schedule-change`).

#### `sabha` — Sabhas, Occurrences, Schedules, Rosters

- Sabha aggregate with discriminated `scheduleShape` (`WEEKLY_RECURRING` | `MONTHLY_AD_HOC`) per ADR-0012.
- Roster derived from `Person.HomeSabhas`.
- Per-Occurrence venue override (Sanchalak-only authority per ADR-0001).
- BSS / YSS Sabhas have no Nirikshak and are monthly-ad-hoc — Sanchalak manually creates each Occurrence.
- Materialization job (cron): weekly Sabhas → Scheduled Occurrences in a rolling 8-week window. First Occurrence for a newly-created weekly Sabha requires ≥24h lead time; below that, skip to the following week.
- Auto-Finalize job (hourly): any `Open for Marking` Occurrence whose scheduled end time is ≥24h ago → Finalized.

**Deep extraction — Occurrence State Machine.** Public interface:
```
transition(occurrenceId, action, actor) → Result
```
State diagram (from CONTEXT.md and ADR-0001):

```
Scheduled ─cancel→ Cancelled ─revert→ Scheduled
    │
    ├─ reschedule → Rescheduled
    │
    ├─ open (auto on day) → Open for Marking
    │
                                Open for Marking
                                       │
                                       ├─ auto (24h after end) → Finalized
                                       │
Finalized ─reopen by {Nirikshak | Nirdeshak | Sah-Nirdeshak} with reason→ Open for Marking
```

Reversibility of `Cancelled` is bounded by the auto-Finalize cutoff — i.e., within the 24h grace; after that, only a reopen path is possible (and the Occurrence shows a "reopened" badge with reason).

**Deep extraction — Verified Home Sabha Transfer orchestrator.** Public interface:
```
initiate(personId, destinationSabhaId, initiatingUserId) → TransferId
confirm(transferId, otpCode) → Result
```
Encapsulates OTP send, consent receipt, Roster swap, and audit. The *initial* Home Sabha assignment when an appointer creates a new Person bypasses this orchestrator entirely — Person creation includes Home Sabha as a single transactional act per ADR-0011's exception.

#### `attendance` — Markings, sync protocol

- Attendance Markings with idempotent push from mobile.
- Last-write-wins per `(Occurrence, Person)` keyed on client `markedAt`; server arrival time is tiebreaker (ADR-0007).
- Server-side 7-day staleness gate — rejects markings where the client's roster-version is older than 7 days.
- Roster-member markings and Walk-in markings are distinct in the data so the re-engagement calculator can ignore Walk-ins (ADR-0010).

#### `analytics` — Read models, dashboards

- Projections from attendance events; never ad-hoc joins on transactional tables (ADR-0008).
- Per-tier roll-up: Sanchalak → Nirikshak (3–4 Sabhas) → Nirdeshak (Kshetra × demographic, both tracks) → Sanyojak (Zone × demographic) → Regional Team (City × demographic) → MK (State).
- Sant universal-read with self-selected default city.
- Three sections per the prototype: Dashboard overview (KPIs + candidate list), People analytics (filterable table), Sabha analytics (Zone → Kshetra → Sabha tree with candidate counts at every level).

**Deep extraction — Re-engagement Candidate Calculator.** Public interface:
```
candidatesFor(scope) → List<{personId, missedStreak, homeSabhaId}>
```
Encapsulates streak counting (3+ missed = candidate, 6+ = priority), Walk-in non-reset, Cancelled-doesn't-count, and independent streaks per Home Sabha. Thresholds tunable via config owned by MK.

#### Cross-cutting

- **Audit log** — every operation records `actingUserId`, `onBehalfOfUserId` (nullable; populated for Nirikshak-as-Sanchalak proxy actions and similar), timestamp, action, target. Proxy attribution is system-enforced (separate field, not a string convention) so analytics can filter on `onBehalfOfUserId IS NOT NULL`.
- **Notifications/SMS gateway** (used by `identity` for OTPs) abstracted behind a port so a fake can be used in tests.

### Schema additions captured during this design pass

- `Person.dateOfBirth: Date?` (optional, informational only — no age enforcement; per Q11)
- `Person.guardianFor: List<PersonId>` for the parent/child shared-mobile case (ADR-0013)
- `Occurrence` state-transition log table — preserves cancel + revert history per ADR-0001
- `Occurrence.venueOverride: String?`
- `RoleAssignment.appointedBy: UserId`, `appointedAt: Instant`
- `Sabha.scheduleShape: enum { WEEKLY_RECURRING, MONTHLY_AD_HOC }` discriminator with shape-specific fields nested

### Mobile app (Flutter)

Six screens, all matching the chosen prototype designs in `prototypes/NOTES.md`:

| Screen | Prototype route |
|---|---|
| Attendance marking | `mobile-attendance/` |
| Walk-in | `mobile-walk-in/` |
| Add Person | `mobile-add-person/` |
| Verified Home Sabha Transfer | `mobile-home-sabha-transfer/` |
| Manage Occurrence (cancel/reschedule/venue) | `mobile-occurrence-control/` |
| Sync & offline | `mobile-sync-status/` |

Local SQLite store: cached Roster, today's Occurrence, pending-sync queue. Sync protocol pushes idempotently.

**Roster staleness blocked state** (resolved here from prototype open-question #11): when the cached Roster is >7 days old, attendance toggles are disabled and a full-screen modal blocks marking with a "Sync now" CTA. On sync failure, the offline-blocked state persists.

### Web app

Framework choice deferred to implementation; should be a thin SPA over the backend's REST/RPC. Six screens, all matching the chosen prototype designs:

| Screen | Prototype route |
|---|---|
| Analytics dashboard (3 sections) | `web-dashboard/` |
| Role appointment + inline Person create | `web-role-appointment/` |
| Occurrence reopen | `web-occurrence-reopen/` |
| Sanchalak-proxy mode (Nirikshak) | `web-sanchalak-proxy/` |
| Sabha definition | `web-sabha-definition/` |
| Structural admin | `web-structural-create/` |

**Sant landing** (resolved from prototype open-question #8): no Sant-specific screen; add a city-picker chip to the existing dashboard's topnav. The chip persists the Sant's selection as their default.

### Open-question resolutions from prototyping (additional decisions)

The prototype handoff surfaced 12 open questions. Resolutions:

1. **Sanchalak "last seen" signal** — compute from latest of (login, sync, marking). Informational hint only in v1; no alerting to Nirikshak.
2. **Audit attribution format for proxy actions** — system-enforced: `actingUserId` + `onBehalfOfUserId` as separate fields, so queries can filter on proxy actions. Display format is UI concern.
3. **Sabha definition + Sanchalak appointment** — single transaction: Sabha shell + Sanchalak appointment + User credentials in one atomic operation.
4. **First-Occurrence materialization timing for a new weekly Sabha** — next calendar day-of-week matching the schedule, with ≥24h lead time; skip to the following week if creation is too late.
5. **OTP parameters** — 6 digits, 5-minute TTL, 30-second resend cooldown, 5 max attempts then 1-hour lockout, 3 OTP sends per mobile per hour. Same parameters for Home Sabha Transfer and password reset.
6. **Walk-in cross-demographic eligibility** — a Person may be a Walk-in at any Sabha not in their Home Sabhas, regardless of demographic. Cross-demographic Walk-ins valid (a Yuvak attending a Baal Sabha as a visitor) and recorded as Walk-ins.
7. **Cancel reversibility window** — Cancelled is reversible until the auto-Finalize cutoff (24h after scheduled end). After that, only the reopen path (Nirikshak/Nirdeshak/Sah-Nirdeshak with reason) restores editability.
8. **Sant default city + read-anywhere UX** — see *Web app* above.
9. **Regional Team canonical name** — still unresolved; placeholder label remains.
10. **Sabha-shaping vs day-of authority split** — already captured in ADR-0001. Permission model enumerates `SABHA_SHAPING_ACTIONS = {cancel, reschedule, venue-override, schedule-change}` excluded for Sah-Sanchalak.
11. **Roster staleness blocked state** — see *Mobile app* above.
12. **Re-engagement candidate follow-up workflow** — v1 ships read-only candidate list; "marked as followed up" toggle intentionally deferred until usage informs the shape.

## Testing Decisions

### What makes a good test for this codebase

- **Public-interface only.** Tests exercise a module's external surface — never reach into private state or duplicate the module's internal structure. If a test breaks when internals are refactored, it's the wrong shape of test.
- **Real Postgres for persistence-touching tests** (Testcontainers or equivalent). Mocked databases are explicitly forbidden by the project's testing posture — a prior class of incidents was masked when mocked tests passed and the real schema diverged.
- **Fakes for external systems** that the project cannot make hermetic (SMS gateway, time). Hexagonal architecture per ADR-0008 means ports + adapters; tests inject the fake adapter.
- **Pure functions tested as pure** — the Re-engagement Candidate Calculator and the Occurrence State Machine are largely pure given a state snapshot, and their tests should look like input → output cases.

### Modules tested in round one (user-confirmed)

- **Authorization Engine** — every role × action × target permutation that defines a permission boundary. Includes: Sanchalak vs Sah-Sanchalak Sabha-shaping split; Nirikshak proxy gaining Sanchalak permissions on assigned Sabhas only; reopen restricted to Nirikshak/Nirdeshak/Sah-Nirdeshak (Sanyojak/Sant/MK rejected); track-shared higher tiers (Nirdeshak handles both Regular and BSS Baal in the same Kshetra); structural creation matrix per ADR-0009 (Nirdeshak creates Sabhas only in their scope; Sanyojak creates Kshetras only in their Zone; MK creates Cities/Zones/Sabha Kinds).
- **Occurrence State Machine** — every transition in the state diagram, plus negative cases: Sah-Sanchalak cancel rejected; Sanyojak reopen rejected; Cancelled → Scheduled after auto-Finalize rejected; reopen without reason rejected.
- **Verified Home Sabha Transfer orchestrator** — happy path (initiate → OTP → consent → swap → audit); OTP expiry; OTP wrong; max-attempts exhausted; rate limit; initial-Home-at-Person-creation bypasses orchestrator entirely; Sah-Sanchalak initiating allowed; Nirikshak top-down reassignment skips OTP.
- **Re-engagement Candidate Calculator** — 3-streak → candidate, 6-streak → priority, Walk-in elsewhere doesn't reset, Cancelled Occurrence not counted as missed, independent streaks per Home Sabha, candidate present-at-Home-Sabha drops off immediately.

### Prior art

None — this is the first round of code. Tests for these four modules establish the patterns subsequent code follows.

## Out of Scope

- **Ad-hoc Sabha creation** (one-off gatherings outside the standing schedule — visiting Sant lectures, festival one-offs). Concept is in `CONTEXT.md` flagged as deferred.
- **Re-engagement candidate follow-up tracking** ("marked as followed up" toggle). v1 ships the read-only list; revisit after a quarter of usage.
- **Push notifications / alerts.** Including the "Nirikshak alerted when their Sanchalak hasn't opened the app for N days" idea — flagged as "revisit if operational pain emerges."
- **Multi-tenant / per-State deployment.** Single deployment covers every State per ADR-0005.
- **Selection criteria enforcement** for BSS/YSS. Pure human judgment per Q16.
- **Age cutoff enforcement.** DOB is optional and informational only; the system never blocks based on age.
- **SSO / external IdP.** Username + password only per ADR-0004; revisit later if a partner needs it.
- **Reporting exports** (PDF / CSV / Excel). Defer until consumers ask.
- **Person deactivation** (death, leaving the organisation). Out of v1; streaks will grow indefinitely for deactivated People — accepted as known issue.
- **Mobile number change flow.** v1 allows a Sanchalak to edit the field; the safer OTP-to-old-and-OTP-to-new path is deferred.
- **Web-based attendance marking.** ADR-0003 explicitly puts marking on mobile only. A Sanchalak without a smartphone is blocked — accepted constraint.

## Further Notes

- **Regional Team canonical name is still unsettled.** UI copy must use "Regional Team" as a placeholder and keep the label easily replaceable. The proxy and structural-create flows currently elide the role for that reason.
- **YSS expansion:** Yuvak Sevak Sabha (males) and Yuvati Sevak Sabha (females) share the YSS acronym in the organisation's usage, mirroring BSS's loose application to both Baal and Balika.
- **The Sant universal-read scope is load-bearing.** The dashboard's city picker must behave fundamentally differently for a Sant (any city) than for every other role (strict scope). Don't paper over the difference.
- **Mobile-keyed identity is foundational.** A v1 with optional mobile numbers would create dupes from day one — keep mobile required at Person creation. Children without their own phones link to a parent's mobile via `guardianFor`.
- **This PRD is the synthesis of two prior sessions:** a 24-question domain grilling with the domain expert ([`CONTEXT.md`](../../CONTEXT.md) and ADRs 0001–0013) and a 12-prototype UI pass ([`prototypes/`](../../prototypes/)). Implementation should treat the ADRs as the canonical source of architectural decisions and `CONTEXT.md` as the canonical source of language. The 12 prototype open questions have all been resolved in this PRD's *Implementation Decisions* section — do not re-litigate them.
- **Bootstrap:** the very first MK member for the deployed State is seeded by the installer, outside the normal appointment flow. After that, every credential and every Person record traces back to a User action with audit attribution.
