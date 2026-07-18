package org.sabha.identity.applicationservice.appointment;

import java.util.UUID;

/**
 * The role-revocation use case (ADR-0025 §1-2, ADR-0026): "deleting" a
 * role-holder revokes that one assignment as a state change, never a row
 * deletion. Authority is by the appointer scope the assignment sits under, not by
 * {@code appointedBy}, so a successor may revoke an assignment their predecessor
 * made; revocation does not cascade to the holder's appointees or created
 * structures. Extracted as an interface to mirror {@link AppointRole}.
 */
public interface RevokeRole {

    void revoke(UUID keycloakSubject, UUID assignmentId);
}
