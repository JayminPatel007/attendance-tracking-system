# Re-engagement Candidate Definition

A Person becomes a **re-engagement candidate** when they have missed **3 or more consecutive Occurrences of their Home Sabha**. A second tier of **priority** kicks in at **6 or more consecutive missed Occurrences**. Only Home-Sabha Occurrences count toward the missed-Occurrence streak — Walk-in attendance at other Sabhas does *not* reset the counter. The candidate list is visible to every tier with scope (Sanchalak through Madhyastha Karyalaya), with People-level detail and the same hierarchy roll-up as every other analytic.

## Why "consecutive missed Occurrences" rather than a percentage or calendar window

- A **percentage threshold** ("attendance dropped below 50%") requires the denominator (the Roster as it stood for each historical Occurrence) to be stable enough to be meaningful, and confuses Sanchalaks who think in terms of "Ravi hasn't come for a few weeks" rather than "Ravi is at 47%."
- A **calendar window** ("hasn't attended in 4 weeks") sounds simpler but breaks when a Sabha's schedule shifts (festival break, Sanchalak travel) — the People who got "missed" by a schedule pause appear identical to People who voluntarily stopped coming.
- **Consecutive missed Occurrences** ties to the same thing the Sanchalak experiences week to week. Three in a row is decision-actionable ("call them"), six in a row is escalation-actionable ("the Nirikshak should know").

## Why Walk-ins don't reset the counter

The operational intent of the list is "who is drifting away from the Sabha they're *supposed* to be part of?" A Person who consistently shows up as a Walk-in elsewhere is engaging with the wider organisation, but their Home Sabha — and the Sanchalak responsible for them — is still seeing them drift. The right corrective in that case is a Verified Home Sabha Transfer (ADR-0002) to align the Home Sabha with reality, not a silent counter reset that hides the drift.

## Consequences

- The thresholds (3 and 6) are tunable system parameters, not hardcoded. Madhyastha Karyalaya owns the config.
- A Cancelled Occurrence (after the ADR-0001 reversibility update) does not count as a missed Occurrence — it was never expected to have attendance.
- The streak resets the moment the Person is marked present at their Home Sabha. Going from candidate → off-list → candidate again is a normal pattern; the analytic should show recent transitions so follow-up calls aren't repeated immediately.
- Cross-Home-Sabha case: a Person with multiple Home Sabhas (e.g., Yuvak + Sanyukta, or Baal + BSS-Baal) has an independent streak per Home Sabha. They can be a re-engagement candidate on one and not the other — and the candidate list is owned by the Sanchalak of whichever Home Sabha they're drifting from.
