package org.sabha.common;

import java.util.Optional;
import java.util.UUID;

/**
 * Cross-context bridge from an authenticated Keycloak subject to the local
 * {@code users.id} that owns identity-tier concerns. Lives in common-domain so
 * that contexts other than {@code identity} (e.g. {@code attendance}) can
 * resolve "who is calling?" without depending on identity's domain types — per
 * ADR-0019, cross-context dependencies go through common-domain or domain
 * events.
 *
 * <p>The implementation lives in identity-data-access because the
 * {@code users} table is owned by the identity bounded context.</p>
 */
public interface CallerResolver {

    Optional<UUID> resolveUserId(UUID keycloakSubject);
}
