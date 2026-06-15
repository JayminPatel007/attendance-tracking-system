package org.sabha.common;

import java.util.UUID;

/**
 * Raised when a user attempts an action they are not authorized to perform
 * (ADR-0001). A sibling of {@link DomainException} rather than a subclass,
 * because authorization failures map to HTTP 403 — distinct from the 422 a
 * domain-rule violation produces.
 *
 * <p>It is the base of the whole 403 family (issue #78): authorization failures
 * that carry their own bespoke message — including {@link CallerUnknownException}
 * and the per-action refusals raised by feature contexts — extend it via the
 * message constructor, so the global handler maps this one base type to 403
 * without importing any feature exception.
 */
public class AuthorizationDeniedException extends RuntimeException {

    private final UUID userId;
    private final AuthorizedAction action;

    public AuthorizationDeniedException(UUID userId, AuthorizedAction action) {
        super("User " + userId + " is not authorized to " + action);
        this.userId = userId;
        this.action = action;
    }

    /**
     * For subclasses that describe the refused action in their own words rather
     * than as an {@link AuthorizedAction} the engine arbitrated; {@link #userId()}
     * and {@link #action()} are then {@code null}.
     */
    protected AuthorizationDeniedException(String message) {
        super(message);
        this.userId = null;
        this.action = null;
    }

    public UUID userId() {
        return userId;
    }

    public AuthorizedAction action() {
        return action;
    }
}
