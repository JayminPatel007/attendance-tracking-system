package org.sabha.identity.messaging;

import java.util.List;
import java.util.UUID;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.sabha.identity.applicationservice.IdentityProviderGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.ws.rs.core.Response;

/**
 * {@link IdentityProviderGateway} adapter over Keycloak's Admin REST API (ADR-0016).
 * Authenticates to the {@code master} realm with the configured admin
 * credentials (the same {@code sabha.keycloak.*} settings used elsewhere) and
 * creates Users in the {@code sabha} realm with the {@code UPDATE_PASSWORD}
 * required-action so the first login forces a password change.
 */
@Component
public class KeycloakAdminRestClient implements IdentityProviderGateway {

    private final String adminBaseUrl;
    private final String realm;
    private final String adminUsername;
    private final String adminPassword;

    public KeycloakAdminRestClient(
            @Value("${sabha.keycloak.admin-base-url}") String adminBaseUrl,
            @Value("${sabha.keycloak.realm}") String realm,
            @Value("${sabha.keycloak.admin-username}") String adminUsername,
            @Value("${sabha.keycloak.admin-password}") String adminPassword) {
        this.adminBaseUrl = adminBaseUrl;
        this.realm = realm;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public UUID createUserRequiringPasswordChange(String username, String rawPassword) {
        try (Keycloak admin = adminClient()) {
            RealmResource realmResource = admin.realm(realm);
            try (Response response = realmResource.users().create(userRepresentation(username, rawPassword))) {
                if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                    throw new KeycloakProvisioningException(
                            "Keycloak rejected user creation for '" + username + "' (HTTP "
                                    + response.getStatus() + ")");
                }
                return UUID.fromString(CreatedResponseUtil.getCreatedId(response));
            }
        }
    }

    private Keycloak adminClient() {
        return KeycloakBuilder.builder()
                .serverUrl(adminBaseUrl)
                .realm("master")
                .clientId("admin-cli")
                .grantType(OAuth2Constants.PASSWORD)
                .username(adminUsername)
                .password(adminPassword)
                .build();
    }

    private static UserRepresentation userRepresentation(String username, String rawPassword) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(rawPassword);
        credential.setTemporary(true);

        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEnabled(true);
        user.setCredentials(List.of(credential));
        user.setRequiredActions(List.of("UPDATE_PASSWORD"));
        return user;
    }
}
