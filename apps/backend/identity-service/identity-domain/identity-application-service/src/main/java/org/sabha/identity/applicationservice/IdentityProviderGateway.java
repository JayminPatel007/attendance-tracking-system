package org.sabha.identity.applicationservice;

import java.util.UUID;

/**
 * Driven port (ADR-0019) onto the external identity provider that owns the
 * credential store (ADR-0016). Creating a User there is the first step of any
 * provisioning act — the install-time MK bootstrap here, and the assigner flow
 * in later role-appointment slices.
 *
 * <p>The application ring names the capability, not the vendor: which identity
 * provider backs this (Keycloak today) is an infrastructure detail that lives in
 * the adapter under {@code identity-messaging}.</p>
 */
public interface IdentityProviderGateway {

    /**
     * Creates a user with the given username and an initial password the user is
     * forced to change on first login.
     *
     * @return the new user's id in the identity provider (the JWT {@code sub})
     */
    UUID createUserRequiringPasswordChange(String username, String rawPassword);
}
