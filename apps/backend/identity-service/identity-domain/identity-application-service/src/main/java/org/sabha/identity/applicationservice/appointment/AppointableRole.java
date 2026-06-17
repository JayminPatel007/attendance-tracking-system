package org.sabha.identity.applicationservice.appointment;

/**
 * The roles a User can be appointed into through the role-appointment flow
 * (ADR-0011). A superset of the operational {@link org.sabha.common.Role} enum:
 * it additionally names {@link #REGIONAL_TEAM} (the City-level oversight body,
 * working label) and {@link #SANT} (recorded administratively by the Madhyastha
 * Karyalaya, not "appointed" in the system sense). Madhyastha Karyalaya
 * membership itself is seeded at install time and is not appointable here.
 */
public enum AppointableRole {
    SANCHALAK,
    SAH_SANCHALAK,
    NIRIKSHAK,
    SAH_NIRDESHAK,
    NIRDESHAK,
    SANYOJAK,
    REGIONAL_TEAM,
    SANT
}
