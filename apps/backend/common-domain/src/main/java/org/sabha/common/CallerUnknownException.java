package org.sabha.common;

import java.util.UUID;

/**
 * The failure mode of {@link CallerResolver#requireUserId(UUID)}: an
 * authenticated Keycloak subject maps to no local {@code users} row. Lives in
 * common-domain beside the port it belongs to (issue #78) so every bounded
 * context resolves "who is calling?" through the same type and the same HTTP
 * mapping.
 *
 * <p>Extends {@link AuthorizationDeniedException} — a caller the system cannot
 * identify is forbidden (403), not a server error — so the global handler maps
 * it through the one 403 base type rather than a bespoke advice.
 */
public class CallerUnknownException extends AuthorizationDeniedException {

    private final UUID keycloakSubject;

    public CallerUnknownException(UUID keycloakSubject) {
        super("No local user mapped to Keycloak subject " + keycloakSubject);
        this.keycloakSubject = keycloakSubject;
    }

    public UUID keycloakSubject() {
        return keycloakSubject;
    }
}
