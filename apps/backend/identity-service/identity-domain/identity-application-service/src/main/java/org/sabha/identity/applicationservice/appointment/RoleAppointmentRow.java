package org.sabha.identity.applicationservice.appointment;

import java.time.Instant;
import java.util.UUID;

/**
 * A {@code role_assignments} row to be written by an appointment (ADR-0011). The
 * {@link AppointmentScope} carries the role and the scope columns relevant to it
 * (Sabha-scoped roles set a Sabha; the Kshetra / Zone / City tiers set their id
 * plus a demographic; unused fields are {@code null}); the remaining fields are
 * the row identity and the {@code appointedBy} / {@code appointedAt} audit.
 */
public record RoleAppointmentRow(
        UUID id,
        UUID userId,
        AppointmentScope scope,
        UUID appointedBy,
        Instant appointedAt) {
}
