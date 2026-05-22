package org.sabha.identity.domain;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findByKeycloakUserId(UUID keycloakUserId);
}
