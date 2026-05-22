package org.sabha.sharedkernel;

import java.util.Optional;
import java.util.UUID;

/**
 * Cross-context bridge from an authenticated Keycloak subject to the local
 * {@code users.id} that owns identity-tier concerns. Lives in shared-kernel so
 * that contexts other than {@code identity} (e.g. {@code attendance}) can
 * resolve "who is calling?" without depending on identity's domain types — per
 * ADR-0015, cross-context dependencies go through shared-kernel or domain
 * events.
 *
 * <p>The implementation lives in {@code identity-infrastructure} because the
 * {@code users} table is owned by the identity bounded context.</p>
 */
public interface CallerResolver {

    Optional<UUID> resolveUserId(UUID keycloakSubject);
}
