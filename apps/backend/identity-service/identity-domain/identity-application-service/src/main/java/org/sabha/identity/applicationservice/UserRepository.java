package org.sabha.identity.applicationservice;

import java.util.Optional;
import java.util.UUID;

import org.sabha.identity.domain.User;

public interface UserRepository {
    Optional<User> findByKeycloakUserId(UUID keycloakUserId);

    /** The User backing a Person, if that Person already holds a login. */
    Optional<User> findByPersonId(UUID personId);

    /** The User with the given login name — drives the password-reset lookups. */
    Optional<User> findByUsername(String username);

    /** The User by local id — drives the assigner-reissue's identity-provider call. */
    Optional<User> findById(UUID userId);

    /** Whether a username is already taken — enforced before appointment commit. */
    boolean existsByUsername(String username);

    void save(User user);
}
