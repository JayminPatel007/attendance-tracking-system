# Directory De-duplication on Person Add

When a Person is added to the Directory — by a Sanchalak/Sah-Sanchalak post-Sabha (Walk-in cleanup, new Roster member) or by an appointer onboarding a new Karyakar (ADR-0011) — the system runs a two-signal de-duplication check:

1. **Mobile number — hard block.** If the mobile being entered matches an existing Person's mobile, the add is *blocked*. The UI surfaces the existing Person (name, Home Sabha, last attendance) and forces the adder to either select that Person and proceed with their intent, or cancel.
2. **Name — soft warning.** If no mobile match but the entered name is close (phonetic + edit-distance) to existing Persons within the same City, the UI shows up to 5 candidates and a "none of these — create new" option. The adder can override; the override is logged with their User ID and timestamp.

Mobile number is a **required field** at Person creation. Children without their own phones are recorded with a parent/guardian's mobile.

## Why mobile-as-primary-key

Three reasons:
- The organisation's culture treats mobile number as the stable personal identifier (people change Kshetras, names get spelled differently, but mobiles persist).
- Mobile is already required infrastructure for Verified Home Sabha Transfer (ADR-0002).
- Mobile is globally unique in practice — collision is essentially impossible, unlike name or even name + DOB.

Alternatives considered:
- **Name + DOB:** Fails because DOB is optional (per the Person glossary entry); also name spelling varies across transliterations.
- **National ID:** Privacy concerns, no existing organisational practice of collecting it.
- **System-generated code:** Doesn't help with de-dup; people don't carry it with them.

## Why hard-block on mobile but soft-warn on name

Mobile is a precise identifier — a match is almost certainly the same Person, so blocking is correct (the adder probably *meant* to find the existing Person). Name is approximate — common names, transliteration drift, and parent/child name reuse all generate false positives. A hard block on name would frustrate legitimate adds; a soft warning lets the human catch the obvious dupes without grinding workflow.

## Why the name search is scoped to the City

A full-Directory name search across a State of hundreds of thousands of People would surface too many false positives ("Ravi Patel" is dozens of People). City-level scope captures the realistic "this Person attended our Sabha and might already be in our area's records" case while keeping the candidate list manageable.

## Consequences

- The Person entity has a `mobile` field that is unique **system-wide** — the system is a single deployment covering every State the organisation operates in (see ADR-0005), so a mobile number can identify a Person regardless of which State they currently live in. Cross-State Verified Home Sabha Transfers reuse this identity rather than creating a new Person record.
- Parent/guardian-mobile linking for children must be supported as a first-class concept on the Person record — multiple Persons can share a mobile *if and only if* one is a guardian relationship. This is an exception to the unique constraint and needs explicit modeling (e.g., a `guardianFor` link rather than a duplicated mobile field).
- The phonetic/edit-distance name index is rebuilt incrementally on Person create/update. Latency on the add screen needs to stay tight (< 500ms for the candidate list) — server-side fuzzy match with appropriate indexing.
- Override events (adder chose "create new" despite a name candidate list) are a useful audit signal: high override rates may indicate the candidate matcher is too aggressive.
- Mobile-required at creation interacts with ADR-0007's offline mode: since adding a Person requires connectivity anyway, the de-dup check is always online — no offline fallback needed.
