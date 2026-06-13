# Attendance Tracking — Sabha

A system for recording who attended in-person religious gatherings ("Sabhas"). Attendance is captured by the Sabha's organizer (Sanchalak) marking attendees on their device. Roll-up reporting flows through a multi-level organizational hierarchy.

## Language

### The gathering

**Sabha**:
A standing recurring in-person religious gathering. Identified by its Sabha Type and Kshetra (e.g., "the Yuvak Sabha at Kshetra X"), it has a fixed Sanchalak, a derived Roster, a **standing venue** (free-text address), and one of two **schedule shapes**:

- **Weekly recurring** — for Regular-track Sabhas. A fixed `(day-of-week, start-time, end-time)` slot (e.g., Sunday 7–8:30pm). Occurrences are auto-materialized from this schedule by the system.
- **Monthly ad-hoc** — for BSS / YSS Sabhas. The Sabha has *no* standing day/time. The organisational rule is that it must hold one Occurrence per calendar month, but the specific date is decided by the Sanchalak each month and the Occurrence is created manually.

The Sabha itself is the durable thing; each actual gathering on a specific date is a **Sabha Occurrence**. Individual Occurrences may be rescheduled (date/time) or relocated (per-occurrence venue override) without altering the Sabha's standing schedule or venue.
_Avoid_: Meeting, event, session

**Sabha Occurrence**:
A single dated instance of a Sabha. For **weekly-recurring** Sabhas, Occurrences are pre-materialized from the Sabha's schedule by a system job. For **monthly-ad-hoc** Sabhas (BSS / YSS), each Occurrence is created manually by the Sanchalak before its date. Each Occurrence has its own Attendance Markings, moves through a defined lifecycle (see Occurrence States below), and may be **rescheduled** to a different date/time without changing the Sabha's underlying schedule.
_Avoid_: Session, instance

**Occurrence States**:

- **Scheduled** — auto-created from the Sabha's schedule; awaiting its date.
- **Rescheduled** — a Scheduled Occurrence whose date/time has been changed by the Sanchalak.
- **Cancelled** — no attendance can be recorded while in this state. **Reversible**: the Sanchalak (or Nirikshak acting as proxy) can revert a Cancelled Occurrence back to Scheduled or directly to Open for Marking. The audit log preserves both the cancellation and the reversion so the history isn't erased.
- **Open for Marking** — attendance is being recorded by the Sanchalak.
- **Finalized** — attendance is frozen. Edits require a higher-tier Karyakar to reopen.

**Sabha Type** (aka **Sabha Kind**):
A categorization of a Sabha along two dimensions: (a) **demographic** (one of the named types below) and (b) **track** (Regular or BSS — see Bal Sevak Sabha below). The combination uniquely identifies a Sabha Type, e.g., "BSS Baal Sabha" and "Regular Baal Sabha" are distinct. **The list of Sabha Types is extensible** — new ones are registered by the Madhyastha Karyalaya — not a hardcoded enum. The five demographic types below are the ones currently known.
_Avoid_: Category, group

**Named demographic types:**

- **Baal Sabha** — for male children
- **Balika Sabha** — for female children
- **Yuvak Sabha** — for male youth
- **Yuvati Sabha** — for female youth
- **Sanyukta Sabha** — for everyone: all ages (children through elderly), all genders. Sits alongside the four demographic Sabhas rather than replacing any of them. **Regular-track only** — no BSS/YSS Sanyukta exists (the selective concept doesn't apply to the all-encompassing kind). Every Person in the Kshetra's Directory has the Sanyukta Sabha at their Kshetra as one of their Home Sabhas by default — the Roster is essentially everyone. The Sanyukta Sanchalak is a distinct role assignment in the system, though in practice the same Person may hold a Sanyukta Sanchalak role *and* a demographic Sanchalak role in the same Kshetra.

_Avoid_: "Male Children Sabha", "Female Youth Sabha" (English descriptions) — use the canonical names above.

**Bal Sevak Sabha (BSS)**:
A selective program track for children chosen from the Regular Baal / Balika Sabha population based on certain criteria. Selected children become part of **BSS Baal Sabha** (boys) or **BSS Balika Sabha** (girls). BSS is a *parallel additive* track: selected children continue to attend their Regular Baal/Balika Sabha *and* additionally attend the corresponding BSS Sabha. The BSS Sabha has its own **Sanchalak and Sah-Sanchalak** (distinct from the Regular Sabha's), but the **Nirdeshak, Sah-Nirdeshak, Sanyojak, and Regional Team are shared with the Regular track** for the same demographic (e.g., the Baal Nirdeshak handles both Regular Baal and BSS Baal Sabhas in the Kshetra). **No Nirikshak exists for BSS Sabhas** — the BSS Sanchalak reports directly to the demographic Nirdeshak.
_Avoid_: BSS (acronym only; use the full name once per document)

**Yuvak Sevak Sabha (YSS)** / **Yuvati Sevak Sabha** _(same acronym YSS used loosely for both, mirroring BSS's pattern)_:
The selective program for the youth tier, structurally identical to BSS — parallel additive track. Selected youths gain an additional **YSS Yuvak Sabha** (males) or **YSS Yuvati Sabha** (females) Home Sabha alongside their Regular one. Same role-scoping shape: distinct Sanchalak / Sah-Sanchalak per (Kshetra, demographic, YSS); Nirdeshak / Sah-Nirdeshak / Sanyojak / Regional Team shared with Regular for the same demographic; no Nirikshak.

**Selection (BSS / YSS)**:
A child or youth joins the selective program via a two-step flow: their **Regular Sanchalak nominates**, and the **demographic Nirdeshak approves** (the same Nirdeshak who oversees both Regular and the selective track for that demographic). Approval adds the Person to the selective Sabha's Roster — a new Home Sabha in the `(demographic, BSS)` or `(demographic, YSS)` kind. Deselection is the inverse and removes that additional Home Sabha; the Regular Home Sabha is unaffected. **Criteria are not system-enforced** — the Sanchalak and Nirdeshak apply their own judgment about spiritual readiness and engagement; the system facilitates the workflow and audits who nominated and approved, but doesn't constrain who is eligible.

### Geographic hierarchy (top-down)

**State** → **City** → **Zone** → **Kshetra** → **Sabha**

**Kshetra**:
A sub-region within a Zone. Contains roughly 10–20 Sabhas. Overseen by a Nirdeshak.
_Avoid_: Sub-region, area


### Roles (each tier has its own role)

**Karyakar**:
The supertype for any Person who holds an *operational* organizational role in the Sabha hierarchy. Includes exactly: Sanchalak, Sah-Sanchalak, Nirikshak, Nirdeshak, Sah-Nirdeshak, and Sanyojak. **Does not include** Sants (City) or Madhyastha Karyalaya members (State) — those are oversight roles of a different nature.
_Avoid_: Volunteer, official, staff

**Role Scoping (important):**

A Sabha kind has two dimensions — **demographic** (Baal/Balika/Yuvak/Yuvati/Sanyukta) and **track** (Regular/BSS/YSS). Roles scope along these dimensions differently:

- **Per (geographic-level, demographic, track)** — fully differentiated across both dimensions: **Sanchalak, Sah-Sanchalak.** Distinct people for Regular vs BSS even within the same Kshetra and demographic.
- **Per (geographic-level, demographic), Regular track only** — exists only for Regular Sabhas, no BSS/YSS counterpart: **Nirikshak.** A BSS or YSS Sanchalak reports directly to the demographic Nirdeshak with no Nirikshak tier in between.
- **Per (geographic-level, demographic), track-shared** — the same person serves Regular and BSS/YSS for that demographic: **Nirdeshak, Sah-Nirdeshak** (Kshetra-level), **Sanyojak** (Zone-level), **Regional Team** (City-level).
- **Flexible per demographic**: **Sant.** A City has at least one Sant; a Sant covers one or more demographics within their City. (Sant *read access* is universal — see Sant entry — but the formal assignment is still per demographic.)
- **Unified across all demographics and tracks**: **Madhyastha Karyalaya.** Exactly one per State.

The geographic structure itself (State → City → Zone → Kshetra → Sabha) is always shared — a Kshetra is a Kshetra regardless of demographic or track.

The *geography* is always shared — a Kshetra is a Kshetra regardless of Sabha kind.


**Sanchalak** — owns a **Sabha**. Organizes the Sabha and marks attendance.
_Avoid_: Organizer, host

**Sah-Sanchalak** — co-Sanchalak (deputy / assistant for a Sabha). Shares the Sanchalak's *day-of* operational authority on their assigned Sabha: marks/edits attendance, adds People to the Directory, pulls People into the Sabha via verified Home Sabha transfer. **Does not** share the Sanchalak's *Sabha-shaping* authority: cannot cancel an Occurrence, reschedule an Occurrence, or change the Sabha's standing schedule — those remain Sanchalak-only.

**Nirikshak** — oversees 3–4 **Sabhas** within a **Kshetra**. Sits between Sanchalak and Nirdeshak in the chain. The 3–4 Sabhas are *assigned* to the Nirikshak by their Nirdeshak; the assignment is mutable and the group has no name or identity beyond "the Sabhas currently assigned to Nirikshak X." **Sanchalak-proxy capability:** on any Sabha assigned to them, the Nirikshak can exercise the full Sanchalak operational toolkit when the Sanchalak is unavailable — mark/edit attendance, cancel or reschedule an Occurrence, change the Sabha's standing schedule, register walk-ins, add People to the Directory. Audit records reflect that the Nirikshak (not the Sanchalak) performed the action.
_Avoid_: Inspector, supervisor

**Nirdeshak** — owns the Sabhas of a single demographic (track-shared) within a Kshetra (so each Kshetra has one Nirdeshak per demographic: a Baal Nirdeshak, a Yuvak Nirdeshak, etc.). **Creates new Sabhas** within their (Kshetra, demographic) scope, and **appoints (and may revoke)** the Sanchalak, Sah-Sanchalak, Nirikshak, and up to two Sah-Nirdeshaks in that scope. May delete a Sabha only while it has no recorded Occurrences/attendance (block-if-non-empty); deleting a role-holder revokes that role assignment (the Person persists; their structures and sub-appointees are inherited by the next holder of the scope).
_Avoid_: Director

**Sah-Nirdeshak** — deputy for the same (Kshetra, demographic) scope, appointed by the Nirdeshak. Retains the Nirdeshak's **operational/proxy** powers (e.g. reopening a Finalized Occurrence, acting on the Kshetra's Sabhas during the Nirdeshak's absence) and the **same analytics view** as the Nirdeshak. **Currently holds no appointment, structural-creation, or deletion authority** ("for now"). **At most 2 per (Kshetra, demographic).**

**Sanyojak** — owns the Sabhas of a single demographic (track-shared) within a Zone (so each Zone has one Sanyojak per demographic). **Creates new Kshetras** within their Zone, and **appoints (and may revoke) the Nirdeshak** for each (Kshetra, demographic) in the Zone. May delete a Kshetra only while it has no live children (block-if-non-empty).
_Avoid_: Coordinator, organizer

**Sant** — religious figure overseeing a **City**. A Sant's oversight is *formally* scoped to one or more Sabha kinds within that City (so the City × Sabha-kind hierarchy stays populated for roll-up), but **read access is universal**: a Sant can view analytics for *any* City and Sabha kind, not only their assigned ones. Each Sant picks their own **default city** for the dashboard's landing view. **Operationally read-only** otherwise — no write authority over Occurrences, attendance, Directory, or role assignments (the one exception is Ad-hoc Sabha creation; any User can create one). **Not "appointed" by anyone** in the system sense — the religious position exists outside the system. Madhyastha Karyalaya members create the Sant's User record administratively (per ADR-0004 they set the initial username + password); daily login is not expected.

**Regional Team** _(canonical domain name TBD)_ — a City-level oversight body, scoped per **City × Sabha kind** (mirroring Sant's scope shape). A **body of multiple Users**, each with their own login, all carrying the "Regional Team member of (City X, demographic Y)" role (track-shared). Not a Karyakar role. **Creates new Zones** within their City, **appoints (and may revoke) Sanyojaks** within their (Zone, demographic) scope, and consumes analytics rolled up for that scope. **Self-replicating:** any member may create *and* delete peer Regional Team members in the same (City, demographic) — the system only blocks deleting the *last* remaining member of a (City, demographic). The first member of a (City, demographic) is created by the Madhyastha Karyalaya. Does *not* create Cities or Sabha Kinds — those remain Madhyastha Karyalaya authority.

**Madhyastha Karyalaya** — the state-level oversight body. **Exactly one per State, unified across all Sabha kinds.** A **body of multiple Users**, each with their own individual login; each member carries the "Madhyastha Karyalaya member" role assignment for the State. Not a Karyakar role. **Creates new Cities and Sabha Kinds** (Zone creation has moved down to the Regional Team), with each creation attributed to the specific member User who performed it. **Creates the first Regional Team member per (City, demographic) and creates Sant User records** as administrative acts, and **may delete the same**. May delete a City only while it has no Zones (block-if-non-empty); a Sabha Kind is **soft-retired** (marked inactive so no new Sabhas/roles of that kind can be created, while existing ones drain) rather than hard-deleted. The first MK member is seeded at install time (one-off bootstrap); subsequent members are added by any existing MK member.
_Avoid_: Central office (English literal).

### Attendance

**Attendance Marking**:
The act of the Sanchalak indicating that a Person was present at a specific Sabha occurrence.
_Avoid_: Check-in, sign-in

**Person**:
A human known to the system, held in a central Directory. Stores **gender** (used to filter eligibility for demographic kinds) and **date of birth (optional)** — DOB is captured when available but not required, and is informational only; the system does *not* enforce age-based eligibility for demographic kinds (the assigning Sanchalak vouches for fit). Has one **Home Sabha per Sabha kind** they are eligible for: one of the demographic kinds (Baal / Balika / Yuvak / Yuvati, conventionally chosen by gender + age) plus Sanyukta. So a typical Person has 2 Home Sabhas: e.g., the Yuvak Sabha at their Kshetra *and* the Sanyukta Sabha at their Kshetra. Each Home Sabha is independently mutable (Verified Home Sabha Transfer). A Person may *additionally* be a **User** if they hold any role.
_Avoid_: User (a User is a Person with role(s); not all People are Users), member, attendee

**User**:
A Person who can log into the system. A Person becomes a User when assigned at least one role (Karyakar role, Sant, or Madhyastha Karyalaya membership) and loses User status when all roles are revoked. Their underlying Person record persists. Authentication is by **custom username + password**, both set by the assigning Karyakar at role assignment time and handed to the new User as credentials. The User is **required to change the password on first login**. A User's permissions are the union of all their role assignments.
_Avoid_: Account, login (verb)

**Home Sabha**:
A Sabha a Person is registered to attend regularly. A Person has one Home Sabha *per Sabha kind* they qualify for (their demographic kind + Sanyukta). Used to derive the Roster: the Roster of a Sabha is the set of People whose Home Sabha (for that kind) is currently that Sabha. A Person attending any Sabha that is not one of their Home Sabhas is a Walk-in there.
_Avoid_: Primary sabha, base sabha

**Verified Home Sabha Transfer**:
The act of a Sanchalak (or Sah-Sanchalak) changing a Person's Home Sabha. **Always Person-initiated**: the Person approaches the Sanchalak of the destination Sabha and asks to be transferred; the Sanchalak initiates the operation in the system; an out-of-band verification step (OTP to the Person's mobile number) confirms consent before the change commits. Covers two cases: (1) **lateral transfer** — moving to a Sabha of the same demographic kind at a different Kshetra (e.g., the Person moved into the area); (2) **level transition** — moving to the next demographic level Sabha (e.g., Baal → Yuvak as the child ages up). The two cases share the same mechanics; the demographic kind change in (2) is incidental. Distinct from a top-down reassignment by a Nirikshak / Nirdeshak, which does not require Person verification.
_Avoid_: Reassignment (a Nirdeshak does that without consent), promotion

**Roster**:
The set of People whose **Home Sabha** is this Sabha. Derived, not maintained per occurrence. Changes only when People are added, removed, or have their Home Sabha re-assigned.
_Avoid_: Invitee list

**Ad-hoc Sabha** _(deferred — likely not in v1)_:
A one-off Sabha with no standing schedule, created for a specific event (e.g., a visiting Sant's lecture, a special festival gathering). May be anchored at any geographic level — Kshetra, Zone, City, or State. Any User (any role assignment) can create one; the creator becomes its Sanchalak for marking purposes. Roster is empty by default; attendance is recorded as Walk-ins drawn from the wider Directory. Standard Occurrence lifecycle applies (Open for Marking → Finalized) on a single date. Not currently planned for the first release — flagged here to keep the language consistent if/when added.

**Walk-in** (colloquially: **Visitor**):
A Person marked present at a Sabha that is not their **Home Sabha**. The Person must already exist in the Directory — Walk-in is about presence at the *wrong* Sabha, not about being unknown to the system. Expected to be rare per Person (one or two occurrences, e.g., when visiting another area).
_Avoid_: Guest (implies they may be unknown to the Directory)

**Re-engagement candidate**:
A Person whose recent attendance pattern at their **Home Sabha** suggests they are drifting away from the gathering. The system surfaces these People in a per-tier list scoped to each role's assignments, with People-level detail (not just counts) so the responsible Karyakar can follow up directly. The exact threshold and the rule for whether Walk-in attendance affects the candidate status are decisions in ADR-0010.
_Avoid_: Inactive person, lapsed member

## Relationships (provisional)

- A **Sabha** has a **Sabha Type**, belongs to a **Kshetra**, is owned by one **Sanchalak** (and possibly **Sah-Sanchalaks**), and is overseen by one **Nirikshak**.
- A **Sabha** has many **Sabha Occurrences** (one per scheduled date).
- A **Kshetra** belongs to a **Zone** and has, *per Sabha kind*, one **Nirdeshak** (and possibly **Sah-Nirdeshaks**) and several **Nirikshaks** (each covering 3–4 Sabhas of that kind).
- A **Zone** belongs to a **City** and has, *per Sabha kind*, one **Sanyojak**.
- A **City** belongs to a **State** and is overseen by **Sant(s)**.
- A **State** is overseen by the **Madhyastha Karyalaya**.
- An **Attendance Marking** ties a **Person** to a specific **Sabha Occurrence**.

## Example dialogue

> **Sanchalak:** "I missed marking Ravi as present at last Sunday's Yuvak Sabha. Can I still fix it?"
> **Nirdeshak:** "Was the Occurrence Finalized? If not, just open it and add him. If it was Finalized, ask your Nirikshak to reopen it."

> **Nirdeshak:** "Yuvati Sabha at Kshetra-3 has dropped from 80% to 50% attendance this quarter — pull up the re-engagement candidates and we'll talk to those People."

> **Sanchalak (of Yuvak Sabha at Kshetra-7):** "A new family moved into my area; their son Ravi wants this to be his Home Sabha."
> **System:** [sends OTP to Ravi's mobile] — Verified Home Sabha Transfer flow.

## Analytics direction (not yet detailed)

The web app's analytics follow a **per-tier roll-up spine**: each role sees a dashboard scoped to their assignments, with drill-down. Time-series is the default presentation. The single most operationally valuable analytic is the **re-engagement candidate list** — People who haven't attended recently. Full detail deferred.

## Flagged ambiguities (open)

- **Regional Team domain name.** The organisation does not yet have a canonical name for the City-level oversight body that appoints Sanyojaks — "Regional Team" is the working label.
