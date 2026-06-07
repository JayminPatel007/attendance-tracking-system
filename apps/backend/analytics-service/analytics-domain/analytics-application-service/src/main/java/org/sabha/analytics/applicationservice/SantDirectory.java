package org.sabha.analytics.applicationservice;

import java.util.UUID;

/**
 * Read port: whether a User holds the Sant oversight role (Slice 17). Sant is
 * recorded as a {@code role_assignments} row with {@code role = 'SANT'} and is
 * deliberately outside the operational {@link org.sabha.common.Role} enum — this
 * port keeps the membership read behind a named concept, mirroring the
 * Madhyastha Karyalaya lookup. The Sant's formal (City, demographic) scope is
 * irrelevant here: the dashboard grants a Sant universal read across the State.
 */
public interface SantDirectory {

    boolean isSant(UUID userId);
}
