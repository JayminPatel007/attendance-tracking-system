# Sabha Schedule Shapes and Occurrence Materialization

A Sabha has one of two schedule shapes:

- **Weekly recurring** (Regular track) — a fixed `(day-of-week, start-time, end-time)` slot. The system auto-materializes future Occurrences on a rolling window (e.g., the next 8 weeks) from this schedule.
- **Monthly ad-hoc** (BSS / YSS tracks) — *no* standing day/time. The organisational rule is one Occurrence per calendar month. The Sanchalak creates each Occurrence manually a few days before its date.

The Sabha entity carries a `scheduleShape` discriminator; the weekly variant carries the slot fields, the monthly-ad-hoc variant carries nothing schedule-wise (only the standing venue and Sanchalak).

## Why not one unified recurrence model

A unified RRULE-style recurrence rule (used in calendar systems) could express both "weekly on Sunday" and "first Sunday of every month" in one field. We rejected it for two reasons:

- BSS / YSS Sabhas genuinely have **no fixed monthly date** — the Sanchalak picks each month based on member availability, venue conflicts, and religious calendar events. An RRULE would force a fictional pattern (e.g., "first Sunday") and the actual Occurrences would always be reschedules of phantom dates.
- The two shapes have different *lifecycle entry points*: weekly Occurrences exist as `Scheduled` before anyone touches them; monthly-ad-hoc Occurrences only exist when the Sanchalak creates them. Modeling these as the same entity-with-different-rules obscures the fact that the Sanchalak has a different responsibility in each case.

A discriminated shape makes both intentions explicit in the data model and the UI.

## Why monthly-ad-hoc Sabhas don't auto-materialize

Auto-materializing a phantom `Scheduled` Occurrence each month (with the date placeholder-set to, say, the first of the month) would create work the Sanchalak has to immediately reschedule. Worse, it would conflate "the Sabha hasn't decided on a date yet" with "the Sabha is scheduled for the 1st." Leaving the calendar empty until the Sanchalak schedules an Occurrence keeps the data model honest about what is actually planned.

## Compliance nudge for monthly Sabhas

The "one Occurrence per calendar month" rule is an organisational expectation, not a hard system constraint. The system surfaces a soft warning to the Sanchalak (and visibility for the Nirdeshak) when a BSS/YSS Sabha has no Occurrence scheduled and the month is past its midpoint — a nudge, not a block.

## Consequences

- The Occurrence materialization job only iterates weekly-recurring Sabhas; monthly-ad-hoc Sabhas are excluded.
- The mobile app's Roster cache (per ADR-0007) needs both schedule shapes — a Sanchalak running a BSS Sabha still benefits from offline marking against the cached Roster.
- Per-Occurrence venue override (mentioned in CONTEXT.md) applies to both shapes uniformly — the override is a property of the Occurrence, not the schedule.
- A future need for richer cadences (fortnightly, quarterly) becomes a new shape on the discriminator, not a retrofit of a generic rule engine.
