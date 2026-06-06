package org.sabha.identity.applicationservice;

import java.util.UUID;

/**
 * Raised when a user who does not hold the demographic Nirdeshak tier for the
 * nomination's (Kshetra, demographic) tries to approve, reject, or deselect
 * (ADR-0006 — the Nirdeshak is track-shared across Regular and BSS/YSS). A
 * transport-tier signal mapped to HTTP 403.
 */
public class SelectionDecisionNotAuthorizedException extends RuntimeException {

    public SelectionDecisionNotAuthorizedException(UUID userId) {
        super("User " + userId + " is not the demographic Nirdeshak authorized to decide this selection");
    }
}
