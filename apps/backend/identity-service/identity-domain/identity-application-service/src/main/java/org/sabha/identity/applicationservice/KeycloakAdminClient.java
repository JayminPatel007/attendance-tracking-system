package org.sabha.identity.applicationservice;

import java.util.UUID;

/**
 * Driven port (ADR-0019) onto Keycloak's Admin REST API. Keycloak is the
 * credential store (ADR-0016); creating a User there is the first step of any
 * provisioning act — the install-time MK bootstrap here, and the assigner flow
 * in later role-appointment slices.
 *
 * <p>The adapter lives in {@code identity-messaging} (an outbound integration).</p>
 */
public interface KeycloakAdminClient {

    /**
     * Creates a Keycloak user with the given username and an initial password
     * the user is forced to change on first login (the {@code UPDATE_PASSWORD}
     * required-action, per ADR-0016).
     *
     * @return the new Keycloak user's id (the JWT {@code sub})
     */
    UUID createUserRequiringPasswordChange(String username, String rawPassword);
}
