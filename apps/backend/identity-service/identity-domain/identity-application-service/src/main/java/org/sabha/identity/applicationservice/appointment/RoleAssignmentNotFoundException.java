package org.sabha.identity.applicationservice.appointment;

import java.util.UUID;

import org.sabha.common.NotFoundException;

/**
 * Thrown when the assignment named for revocation does not exist or is already
 * revoked. Revocation is idempotent only in the sense that an already-revoked
 * row is no longer active, so re-revoking it is a 404 rather than a no-op.
 */
public class RoleAssignmentNotFoundException extends NotFoundException {

    public RoleAssignmentNotFoundException(UUID assignmentId) {
        super("No active role assignment with id " + assignmentId);
    }
}
