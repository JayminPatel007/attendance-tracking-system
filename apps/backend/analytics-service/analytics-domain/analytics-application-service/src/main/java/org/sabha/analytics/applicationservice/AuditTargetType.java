package org.sabha.analytics.applicationservice;

/**
 * The kind of entity an {@link AuditEntry} concerns (ADR-0023, Slice 19). A
 * closed set the viewer filters on — unlike the extensible {@code sabha_kind}
 * token — each value backed by one or more UNION branches in the audit feed:
 *
 * <ul>
 *   <li>{@link #OCCURRENCE} — Occurrence lifecycle transitions (the only kind
 *       that can carry a proxy attribution, Slice 14).</li>
 *   <li>{@link #SABHA} — Sabha definition (Slice 12).</li>
 *   <li>{@link #ROLE_ASSIGNMENT} — role appointment (ADR-0011).</li>
 *   <li>{@link #STRUCTURAL} — City / Zone / Kshetra / Sabha-kind creation
 *       (ADR-0009).</li>
 *   <li>{@link #PERSON} — BSS/YSS selection nominate and decide (Slice 16).</li>
 * </ul>
 */
public enum AuditTargetType {
    OCCURRENCE,
    SABHA,
    ROLE_ASSIGNMENT,
    STRUCTURAL,
    PERSON
}
