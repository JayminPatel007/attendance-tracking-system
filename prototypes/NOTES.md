# UI prototypes — chosen designs

Throwaway UI prototypes for all high-priority flows in CONTEXT.md, ADR-0001 through ADR-0013. Each prototype has converged on a single chosen design.

## How to run

```sh
cd prototypes
python3 -m http.server 8000
```

Open <http://localhost:8000/>.

---

## Mobile — day-to-day Sanchalak operations

### Attendance marking — today's Occurrence
**Route:** `/mobile-attendance/` · The 5-second-per-Person hot path.

Tap-toggle list with search and filter chips. Roster up front; search/filter chips (All · Unmarked · Present · Absent · Candidates) when the list gets long. FAB for Walk-in; Finalize shows present count.

### Add Walk-in
**Route:** `/mobile-walk-in/`

Search Directory for the visitor's name or mobile. Online → full Directory; offline → cached Roster only. Confirm sheet shows their Home Sabha (now-away) before queuing the Walk-in.

### Add Person to Directory
**Route:** `/mobile-add-person/` · Try `9820111122` to trigger the redirect.

Mobile-first entry. Match → forced redirect to existing-Person profile (de-dup-safe per ADR-0013). No match → step 2 (name / DOB / Home Sabha).

### Verified Home Sabha Transfer (OTP)
**Route:** `/mobile-home-sabha-transfer/` · Valid OTP for the prototype: `4827`.

Sanchalak-initiated, Person-verified by OTP to mobile. Steps: find Person → confirm direction (current Home → destination = this Sanchalak's Sabha) → OTP sent → enter → done. Resend cooldown built in.

### Manage current Occurrence
**Route:** `/mobile-occurrence-control/` · Sanchalak only (ADR-0001).

Three Sanchalak-only actions in collapsible cards: Reschedule (date/time), Venue override (this Occurrence only — doesn't touch standing venue), Cancel (reason required, reversible). Cancelled state shows revert options. Audit log accumulates inline.

### Sync &amp; offline
**Route:** `/mobile-sync-status/`

Online / offline pill (toggleable for testing). Pending action queue with per-item retry. Roster staleness indicator (block-at-7-days per ADR-0007). Manual sync trigger.

---

## Web — analytics, oversight, and structural admin

### Analytics — three sections
**Route:** `/web-dashboard/?variant=A`

Sidebar nav switches between:
- **A — Dashboard overview** · KPI strip + card grid · the re-engagement candidates headline (ADR-0010).
- **B — People analytics** · Directory-level filterable table.
- **C — Sabha analytics** · Zone → Kshetra → Sabha tree explorer with candidate counts at every level.

A **Viewing-as toggle** (Nirdeshak ↔ Sah-Nirdeshak) sits in each section's header. Sah-Nirdeshak gets the **same analytics** as the Nirdeshak but **read-only**: a ribbon plus every write action (Follow-up, drill links, export) disabled. (Its proxy/reopen powers live in the Occurrence flows, not here.)

### Role appointment + revocation — scope-based authority chain
**Route:** `/web-role-appointment/` · supersedes ADR-0011.

Reworked from a single appointment form into a per-actor **appoint + revoke** console. An actor switcher (MK · Regional Team · Sanyojak · Nirdeshak · Sah-Nirdeshak) drives the whole screen:

- **Holders-in-scope cards** for every role the actor may appoint, each row carrying a **Revoke role** button. Authority is *by scope, not by creator* — you can revoke assignments you didn't create.
- **Regional Team is self-replicating:** appoints & revokes peer RT members; the system blocks revoking the **last** member of a (City, demographic) (try it — the guard fires). RT also appoints the Sanyojak.
- **Sah-Nirdeshak is capped at 2** per (Kshetra, demographic) — the appoint button disables and the cap chip turns amber at 2/2. As an *actor*, Sah-Nirdeshak appoints nothing (read-only + proxy note instead of a form).
- Delete wording is **"revoke the role assignment"** (Person persists; structures & sub-appointees inherited by the next scope-holder). An inline authority log records every appoint/revoke.
- Inline **"+ Create new Person"** + auto-suggested username/password retained from the old design.

### My authority — per-actor create/delete matrix
**Route:** `/web-authority-matrix/` · new.

Answers "what can I create or delete here?" for each role in one screen. Actor switcher across all five tiers; two columns (Structures · Roles) with a create badge and the delete rule per item, plus a legend for the three delete kinds (**block-if-non-empty** geographic · **soft-retire** Sabha Kind · **revoke** role assignment). Sah-Nirdeshak shows its operational/proxy powers and an explicit "no create/delete" note.

### Occurrence reopen
**Route:** `/web-occurrence-reopen/` · Authority: Nirikshak / Nirdeshak / Sah-Nirdeshak.

Two-pane: Occurrences list on the left, detail on the right. For Finalized Occurrences, a reason-required reopen card. Reopened Occurrences carry a visible badge and the reason persists in the audit trail.

### Nirikshak Sanchalak-proxy mode
**Route:** `/web-sanchalak-proxy/`

Pick a Sabha in scope (availability hint shown but not gated). On enter, a prominent warning banner runs across the page: "You are acting as Sanchalak for X — all actions audit-logged as you." Toolkit links open the operational flows. Audit preview shows the proxy attribution format.

### Define a Sabha
**Route:** `/web-sabha-definition/` · Nirdeshak authority (ADR-0012).

Single form. Sabha kind + Kshetra at top, then schedule shape toggle:
- **Weekly-recurring** — fixed day/start/end; system auto-materializes Occurrences.
- **Monthly ad-hoc (BSS/YSS)** — Sanchalak creates each Occurrence manually.

Standing venue (free-text). Inline Sanchalak picker (directory-style); optional Sah-Sanchalak.

### Structural admin
**Route:** `/web-structural-create/` · supersedes ADR-0009.

Role-scoped tabs, **scope-based create + delete**:
- **MK** — Cities (delete = block-if-non-empty), Sabha Kinds (delete = **soft-retire**, drains, never hard-deleted). Sabha Kind builder enforces "Sanyukta is Regular-track only." **MK no longer creates Zones.**
- **Regional Team** — Zones within its City (delete = block-if-non-empty).
- **Sanyojak** — Kshetras within their Zone (delete = block-if-non-empty).

Block-if-non-empty delete buttons show the reason inline (e.g. "has 6 Kshetras") and only enable on empty entities. **Creating** uses a full-width list with an inline **"+ Add X"** row at the top that expands into editable fields in place (no side panel, no modal). Tabs outside the actor's scope are **hidden, not locked** — MK sees Cities + Sabha Kinds; RT and Sanyojak see a single list with no tab bar. Toggle the role-switcher chip in the banner to see the tab set + delete rules change by authority. An inline authority log records every create/delete/retire.

---

## What's still out of scope

- **Ad-hoc Sabha** — deferred per CONTEXT.md.
- Sant-specific landing (the dashboard already covers their read view; a Sant-default city picker is a minor add).

## Revised authority chain — question answered (2026-06-13)

**Question:** does the reworked *scope-based* creation/appointment/deletion chain (per the updated `CONTEXT.md` role glossary; ADRs 0009 + 0011 to be superseded) hold up as UI?

**Verdict (from the prototypes):** yes. The four flows above demonstrate it end-to-end:
- Authority reads as **by scope, not by creator** — every delete/revoke button sits on the holder/entity row, independent of who created it.
- The three delete kinds (block-if-non-empty · soft-retire · revoke) are visually distinct and self-explanatory with the legend.
- The two non-trivial guards — RT **last-one-out** and Sah-Nirdeshak **max-2** — are both enforceable and legible in-place.
- Zone creation moving MK → Regional Team is clean; MK's Zone tab simply locks.

**Still open (not decided here — design/glossary only, no backend):**
- ADRs superseding 0009 (structural creation) + 0011 (appointment), plus a new deletion-model ADR, are **offered but not yet approved/written**.
- Two carried assumptions to re-confirm: (1) MK cannot create Zones even as a fallback; (2) Nirikshak "assign the Sabha" = its mutable set of 3–4 Sabhas, not a 1:1 binding.

## Open thread

"Regional Team" is now used as a first-class actor in `web-structural-create`, `web-role-appointment`, and `web-authority-matrix`, but the label is still a **placeholder** (`canonical domain name TBD` in CONTEXT.md) — swap it everywhere once the canonical name is settled.
