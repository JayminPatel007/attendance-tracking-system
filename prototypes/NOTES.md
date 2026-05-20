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

### Role appointment + inline Person create
**Route:** `/web-role-appointment/`

Single long form. Person picked from an always-visible directory-style list with avatars and role chips, or "+ Create new Person" inline (ADR-0011). Username + initial password auto-suggested.

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
**Route:** `/web-structural-create/` · Authority per ADR-0009.

Role-scoped tabs:
- **MK** — Cities, Zones, Sabha Kinds. Sabha Kind builder enforces "Sanyukta is Regular-track only."
- **Sanyojak** — Kshetras within their Zone.

Toggle the role-switcher chip in the banner to see how the tab set changes by authority.

---

## What's still out of scope

- **Ad-hoc Sabha** — deferred per CONTEXT.md.
- Sant-specific landing (the dashboard already covers their read view; a Sant-default city picker is a minor add).

## Open thread

"Regional Team" placeholder label is not yet used in any flow — pick it up when the canonical name is settled. The proxy and structural-create flows are good places to surface it once named.
