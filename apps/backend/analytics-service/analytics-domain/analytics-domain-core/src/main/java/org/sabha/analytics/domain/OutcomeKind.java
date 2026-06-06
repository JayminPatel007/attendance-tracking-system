package org.sabha.analytics.domain;

/**
 * One event in a Person's chronological stream against a single Home Sabha, as
 * the Re-engagement Candidate Calculator (ADR-0010) sees it.
 *
 * <ul>
 *   <li>{@link #PRESENT} — marked present at the Home Sabha; resets the streak.</li>
 *   <li>{@link #ABSENT} — a Home Sabha Occurrence the Person missed; extends the streak.</li>
 *   <li>{@link #CANCELLED} — a Cancelled Home Sabha Occurrence; never expected attendance,
 *       so it neither extends nor resets the streak (ADR-0001 + ADR-0010).</li>
 *   <li>{@link #WALK_IN_ELSEWHERE} — the Person walked in at another Sabha; engagement with
 *       the wider organisation does <em>not</em> reset their drift from this Home Sabha.</li>
 * </ul>
 */
public enum OutcomeKind {
    PRESENT,
    ABSENT,
    CANCELLED,
    WALK_IN_ELSEWHERE
}
