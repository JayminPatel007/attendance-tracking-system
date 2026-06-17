package org.sabha.identity.applicationservice.appointment;

import java.util.UUID;

import org.sabha.identity.applicationservice.directory.AddPersonCommand;

/**
 * The Sanchalak (or Sah-Sanchalak) named in a
 * {@link org.sabha.identity.applicationservice.sabhadefinition.SabhaDefinitionCommand SabhaDefinitionCommand}: an
 * existing Person picked from the Directory, or a new Person created inline, plus
 * the auto-suggested credentials. Mirrors the appointee half of
 * {@link RoleAppointmentCommand} so the definition flow can reuse Slice 11's
 * appointment machinery ({@link AppointRole}).
 */
public record Appointee(UUID existingPersonId, AddPersonCommand newPerson, String username, String rawPassword) {

    public static Appointee existing(UUID personId, String username, String rawPassword) {
        return new Appointee(personId, null, username, rawPassword);
    }

    public static Appointee newPerson(AddPersonCommand newPerson, String username, String rawPassword) {
        return new Appointee(null, newPerson, username, rawPassword);
    }

    public RoleAppointmentCommand toCommand(AppointmentScope scope) {
        return existingPersonId != null
                ? RoleAppointmentCommand.forExistingPerson(scope, existingPersonId, username, rawPassword)
                : RoleAppointmentCommand.forNewPerson(scope, newPerson, username, rawPassword);
    }
}
