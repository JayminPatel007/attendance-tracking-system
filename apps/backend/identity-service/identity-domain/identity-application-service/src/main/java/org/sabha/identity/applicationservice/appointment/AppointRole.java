package org.sabha.identity.applicationservice.appointment;

import java.util.UUID;

/**
 * The role-appointment use case (ADR-0011), extracted so collaborators — notably
 * {@link org.sabha.identity.applicationservice.sabhadefinition.SabhaDefinitionService SabhaDefinitionService},
 * which appoints a Sabha's Sanchalak in the same
 * transaction as creating it (ADR-0012) — depend on the behaviour rather than the
 * concrete {@link RoleAppointmentService}. Keeps those orchestrations unit-testable
 * with a lightweight fake.
 */
public interface AppointRole {

    AppointmentResult appoint(UUID keycloakSubject, RoleAppointmentCommand command);
}
