package org.sabha.common;

/**
 * The oversight tiers that sit <em>outside</em> the operational {@link Role}
 * hierarchy. {@code Role} holds only the Karyakar tiers that can carry a scoped
 * {@code role_assignments} entry (ADR-0005); Sant and Madhyastha Karyalaya are
 * State-level oversight bodies recorded as {@code role_assignments} rows with
 * null operational scope, deliberately kept out of that enum.
 *
 * <p>This is the single home for those two role strings and for the caveat that
 * used to be repeated across every Sant/MK adapter and port javadoc: the wire
 * value written to (and matched against) {@code role_assignments.role} is the
 * enum constant name. Authority and membership checks for these tiers go through
 * the identity-owned common-domain ports {@link SantLookup} and
 * {@link MadhyasthaKaryalayaLookup}, never hand-written {@code role = 'SANT'}
 * SQL in foreign data-access modules (ADR-0029).</p>
 */
public enum OversightRole {

    /** A Sant — universal-read oversight with no operational scope or appointer. */
    SANT,

    /** Madhyastha Karyalaya — State-level oversight body (single-organization, ADR-0005). */
    MADHYASTHA_KARYALAYA;

    /** The string stored in {@code role_assignments.role} for this oversight tier. */
    public String wireValue() {
        return name();
    }
}
