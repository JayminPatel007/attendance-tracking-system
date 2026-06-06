package org.sabha.identity.domain;

/**
 * A top-level section of the Angular web shell (Slice 9). The set a given user
 * may see is decided by {@link VisibleSections}; later slices fill each section
 * with screens. The web app renders sidebar nav from the visible set.
 */
public enum Section {
    DASHBOARD,
    ROLE_APPOINTMENT,
    STRUCTURAL_ADMIN,
    SABHA_DEFINITION,
    OCCURRENCE_REOPEN,
    SANCHALAK_PROXY,
    SELECTION
}
