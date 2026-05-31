package org.sabha.identity.applicationservice;

import java.util.UUID;

/**
 * Raised when a user who is neither Sanchalak nor Sah-Sanchalak of the
 * destination Sabha tries to initiate a Verified Home Sabha Transfer (ADR-0002).
 * A transport-tier signal — sibling of the domain exceptions — mapped to HTTP 403.
 */
public class TransferNotAuthorizedException extends RuntimeException {

    public TransferNotAuthorizedException(UUID userId, UUID destinationSabhaId) {
        super("User " + userId + " is not authorized to initiate a transfer into Sabha "
                + destinationSabhaId);
    }
}
