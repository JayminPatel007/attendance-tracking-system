package org.sabha.identity.applicationservice.appointment;

import java.util.UUID;

/**
 * A currently-active {@code role_assignments} row resolved for revocation: its
 * identity, the User who holds it (so login can be withdrawn when it was their
 * last role), and the {@link AppointmentScope} it sits at. The scope drives the
 * scope-based authority check — reusing {@link AppointmentAuthorization} — and
 * names the role for the Regional Team last-one-out guard.
 */
public record RevokableAssignment(UUID id, UUID userId, AppointmentScope scope) {
}
