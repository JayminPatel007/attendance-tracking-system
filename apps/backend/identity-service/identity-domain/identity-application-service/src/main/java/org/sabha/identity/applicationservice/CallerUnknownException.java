package org.sabha.identity.applicationservice;

import java.util.UUID;

public class CallerUnknownException extends RuntimeException {

    private final UUID keycloakSubject;

    public CallerUnknownException(UUID keycloakSubject) {
        super("No local user mapped to Keycloak subject " + keycloakSubject);
        this.keycloakSubject = keycloakSubject;
    }

    public UUID keycloakSubject() {
        return keycloakSubject;
    }
}
