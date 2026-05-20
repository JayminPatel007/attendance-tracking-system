# Sabha Occurrence Lifecycle

A Sabha Occurrence moves through an explicit state machine: `Scheduled ↔ (Rescheduled | Cancelled) → Open for Marking → Finalized`. Attendance Markings are only valid when the Occurrence is `Open for Marking`. `Cancelled` is **reversible** — the Sanchalak (or Nirikshak proxy) may revert it back to Scheduled or directly to Open for Marking if the gathering ends up happening. `Finalized` is auto-set **24 hours after the Occurrence's scheduled end time** and reopening it is restricted to **Nirikshak, Nirdeshak, or Sah-Nirdeshak** — explicitly *not* Sanyojak, Sant, or Madhyastha Karyalaya members.

## Grace period: 24 hours

Tight enough that analytics stabilize within a day (most weekly Sabhas are Sunday — by Monday evening the prior week is locked), generous enough to cover the realistic "I forgot to mark Ravi" case the morning after.

## Why the reopen permission stops at Sah-Nirdeshak

Sanyojak, Sant, and Madhyastha Karyalaya are *oversight* roles, deliberately kept out of the data-edit path. Letting them reopen Occurrences would mean state-level or city-level edits to a single Sabha's history — the opposite of the trust boundary the Finalize state exists to create. The Kshetra-level tiers (Nirikshak, Nirdeshak, Sah-Nirdeshak) sit close enough to the Sabha to legitimately judge whether a reopen request is real.

## Audit obligations on reopen

Every reopen records: reopener User, timestamp, free-text reason. The Occurrence carries a visible "reopened" badge in analytics so consumers know that line moved after Finalize.

## Cancel and Reschedule: Sanchalak-only (with Nirikshak proxy)

Cancel and Reschedule of a Scheduled Occurrence — and the higher-stakes operation of changing the Sabha's standing schedule — are restricted to the Sanchalak of that Sabha. **Sah-Sanchalak is explicitly excluded** from these operations; their authority is the day-of marking/directory toolkit only. **The Nirikshak that the Sabha is assigned to may exercise these Sanchalak-only operations as a proxy when the Sanchalak is unavailable**; audit logs always reflect the proxying Nirikshak as the actor.

Tier-skipping (Nirdeshak, Sanyojak, Sant, MK directly cancelling/rescheduling a specific Sabha's Occurrence) is explicitly *not* a path — those tiers route through the Nirikshak proxy if intervention is needed.

## Why Sah-Sanchalak is excluded from cancel/reschedule

Sah-Sanchalak exists to share the marking workload on the day, not to make decisions about whether the Sabha happens. Granting them cancel/reschedule authority would create a second decision-maker for a question (does the Sabha run this week?) that has exactly one right answer, generating coordination overhead the role wasn't designed for.

## Retroactive cancel

Allowed only within the 24-hour grace window above. If a Sanchalak forgets to cancel and the date passes with no markings, they may still cancel the next morning; after the grace window the Occurrence locks as an unattended Finalized record and cannot be retroactively cancelled.

## Per-Occurrence venue override

A Sanchalak (or Nirikshak proxy) may set a one-off venue override on an Occurrence without changing the Sabha's standing venue. Same authority as cancel/reschedule — Sah-Sanchalak excluded, consistent with the Sabha-shaping line. Allowed up until the Occurrence enters Open for Marking.

## Why this and not "always editable"

The alternative — letting any Sanchalak edit any past attendance forever — was rejected because it makes higher-tier analytics untrustworthy: every historical number can change silently. The Finalized state plus a higher-tier reopen gives Sanchalaks freedom to correct on-the-day mistakes while giving analytics consumers a stable baseline.

## Why Cancelled is reversible (not terminal)

The organisation operates this way in practice: a Sabha gets cancelled in advance ("nobody's going to come because of the festival"), then plans shift and it runs anyway. Forcing a separate "ad-hoc" record for that case would create two competing concepts (the cancelled one and the substitute one) for what the Sanchalak sees as one event. Allowing a revert keeps the model honest about the *one* gathering that actually happened.

The audit-trail concern that motivated a terminal-Cancelled is preserved by *appending* state-transition records — the cancellation and the subsequent reversion are both visible in the Occurrence's history — rather than by freezing the state. Analytics consumers can see "this was Cancelled and then reverted" if they care.
