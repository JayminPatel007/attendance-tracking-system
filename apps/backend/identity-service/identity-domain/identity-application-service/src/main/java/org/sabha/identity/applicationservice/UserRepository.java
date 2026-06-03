package org.sabha.identity.applicationservice;

import java.util.Optional;
import java.util.UUID;

import org.sabha.identity.domain.User;

public interface UserRepository {
    Optional<User> findByKeycloakUserId(UUID keycloakUserId);

    /** The User backing a Person, if that Person already holds a login. */
    Optional<User> findByPersonId(UUID personId);

    /** Whether a username is already taken — enforced before appointment commit. */
    boolean existsByUsername(String username);

    void save(User user);
}
