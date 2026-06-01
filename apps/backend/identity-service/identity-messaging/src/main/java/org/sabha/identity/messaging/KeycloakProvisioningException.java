package org.sabha.identity.messaging;

/**
 * Raised when Keycloak's Admin REST API refuses a provisioning request (e.g. a
 * username collision or a transport failure). Surfaces as a startup failure for
 * the install-time bootstrap and as a rolled-back appointment in later slices.
 */
public class KeycloakProvisioningException extends RuntimeException {

    public KeycloakProvisioningException(String message) {
        super(message);
    }
}
