package org.sabha.common;

import java.util.UUID;

/**
 * Raised when a user attempts an action they are not authorized to perform
 * (ADR-0001). A sibling of {@link DomainException} rather than a subclass,
 * because authorization failures map to HTTP 403 — distinct from the 422 a
 * domain-rule violation produces.
 */
public class AuthorizationDeniedException extends RuntimeException {

    private final UUID userId;
    private final AuthorizedAction action;

    public AuthorizationDeniedException(UUID userId, AuthorizedAction action) {
        super("User " + userId + " is not authorized to " + action);
        this.userId = userId;
        this.action = action;
    }

    public UUID userId() {
        return userId;
    }

    public AuthorizedAction action() {
        return action;
    }
}
