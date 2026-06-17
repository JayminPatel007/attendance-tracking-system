package org.sabha.identity.applicationservice.appointment;

import java.util.UUID;

import org.sabha.identity.applicationservice.directory.AddPersonCommand;

/**
 * Request to appoint a User into a role (ADR-0011). The appointee is either an
 * existing Person picked from the Directory ({@link #existingPersonId}) or a new
 * Person created inline ({@link #newPerson}); exactly one is set. {@code username}
 * and {@code rawPassword} are the auto-suggested credentials, used only when the
 * resolved Person does not already hold a login.
 */
public record RoleAppointmentCommand(
        AppointmentScope scope,
        UUID existingPersonId,
        AddPersonCommand newPerson,
        String username,
        String rawPassword) {

    public static RoleAppointmentCommand forExistingPerson(
            AppointmentScope scope, UUID personId, String username, String rawPassword) {
        return new RoleAppointmentCommand(scope, personId, null, username, rawPassword);
    }

    public static RoleAppointmentCommand forNewPerson(
            AppointmentScope scope, AddPersonCommand newPerson, String username, String rawPassword) {
        return new RoleAppointmentCommand(scope, null, newPerson, username, rawPassword);
    }

    public boolean createsNewPerson() {
        return existingPersonId == null;
    }
}
