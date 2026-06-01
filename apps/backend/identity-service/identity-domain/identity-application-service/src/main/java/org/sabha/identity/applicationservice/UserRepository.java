package org.sabha.identity.applicationservice;

import java.util.Optional;
import java.util.UUID;

import org.sabha.identity.domain.User;

public interface UserRepository {
    Optional<User> findByKeycloakUserId(UUID keycloakUserId);

    void save(User user);
}
