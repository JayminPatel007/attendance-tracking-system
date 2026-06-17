package org.sabha.identity.applicationservice.appointment;

import java.util.UUID;

/**
 * The scope of the role being filled in an appointment (ADR-0011). Which fields
 * are set depends on the {@link AppointableRole}:
 *
 * <ul>
 *   <li>Sanchalak / Sah-Sanchalak — a {@code sabhaId} (the Sabha encodes its own
 *       Kshetra, demographic, and track).</li>
 *   <li>Nirikshak / Sah-Nirdeshak / Nirdeshak — a {@code kshetraId} + {@code demographic}.</li>
 *   <li>Sanyojak — a {@code zoneId} + {@code demographic}.</li>
 *   <li>Regional Team member / Sant — a {@code cityId} + {@code demographic}.</li>
 * </ul>
 */
public record AppointmentScope(
        AppointableRole role,
        UUID sabhaId,
        UUID kshetraId,
        UUID zoneId,
        UUID cityId,
        String demographic) {

    public static AppointmentScope onSabha(AppointableRole role, UUID sabhaId) {
        return new AppointmentScope(role, sabhaId, null, null, null, null);
    }

    public static AppointmentScope onKshetra(AppointableRole role, UUID kshetraId, String demographic) {
        return new AppointmentScope(role, null, kshetraId, null, null, demographic);
    }

    public static AppointmentScope onZone(AppointableRole role, UUID zoneId, String demographic) {
        return new AppointmentScope(role, null, null, zoneId, null, demographic);
    }

    public static AppointmentScope onCity(AppointableRole role, UUID cityId, String demographic) {
        return new AppointmentScope(role, null, null, null, cityId, demographic);
    }
}
