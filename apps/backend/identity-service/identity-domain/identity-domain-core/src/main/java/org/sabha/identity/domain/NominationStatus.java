package org.sabha.identity.domain;

/**
 * Lifecycle of a BSS/YSS {@link SelectionNomination} (ADR-0006). A nomination is
 * opened {@code PENDING} by the Regular Sanchalak and resolved by the demographic
 * Nirdeshak to {@code APPROVED} (the Person gains the selective Home Sabha) or
 * {@code REJECTED} (with an optional reason). A later deselection moves an
 * {@code APPROVED} nomination to {@code DESELECTED}, the inverse that removes the
 * selective Home Sabha while preserving the audit trail.
 */
public enum NominationStatus {
    PENDING,
    APPROVED,
    REJECTED,
    DESELECTED
}
