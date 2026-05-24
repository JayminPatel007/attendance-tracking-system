package org.sabha.identity.domain;

import java.util.UUID;

public record User(UUID id, UUID personId, String username, UUID keycloakUserId) {
}
