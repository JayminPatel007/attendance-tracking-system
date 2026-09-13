package org.sabha.common;

import java.util.UUID;

/**
 * An authenticated credential that maps to no local {@code users} row. Lives in
 * common-domain beside the port it belongs to (issue #78) so every bounded
 * context resolves "who is calling?" through the same type and the same HTTP
 * mapping.
 *
 * <p>Raised at the HTTP edge by the {@code @CurrentUser} argument resolver
 * (ADR-0030), and by {@link CallerResolver#requireUserId(UUID)} for the two
 * callers that are not request-bound.</p>
 *
 * <p>Extends {@link AuthorizationDeniedException} — a caller the system cannot
 * identify is forbidden (403), not a server error — so the global handler maps
 * it through the one 403 base type rather than a bespoke advice.
 */
public class CallerUnknownException extends AuthorizationDeniedException {

    private final String subject;

    public CallerUnknownException(UUID keycloakSubject) {
        this(String.valueOf(keycloakSubject), "No local user mapped to Keycloak subject " + keycloakSubject);
    }

    private CallerUnknownException(String subject, String message) {
        super(message);
        this.subject = subject;
    }

    /**
     * The credential carried no usable subject at all — absent, anonymous, or a
     * subject claim that is not a UUID. Before ADR-0030 the last of those threw
     * {@link IllegalArgumentException} out of {@code UUID.fromString} and
     * surfaced as a 500; it is a 403 like every other unidentifiable caller.
     */
    public static CallerUnknownException unusableSubject(String rawSubject) {
        return new CallerUnknownException(rawSubject, "Authenticated request carried no usable subject: " + rawSubject);
    }

    /** The subject as it arrived, for logging. Never parsed — it may not be a UUID. */
    public String subject() {
        return subject;
    }
}
