package org.sabha.identity.applicationservice;

import java.util.UUID;

/**
 * Driven port: whether a User holds the Sant oversight role (Slice 17/19). Sant
 * is recorded as a {@code role_assignments} row with {@code role = 'SANT'},
 * deliberately outside the operational {@link org.sabha.common.Role} enum —
 * mirroring {@link MadhyasthaKaryalayaMembership}. The web shell needs it to
 * unlock the Audit-log section for a Sant (ADR-0023), whose read access is
 * universal across the State. Adapter in {@code identity-data-access}.
 */
public interface SantMembership {

    boolean isSant(UUID userId);
}
