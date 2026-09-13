package org.sabha.common;

import java.util.Optional;
import java.util.UUID;

/**
 * The one mapping from an authenticated Keycloak subject to the local
 * {@code users.id} the domain works in. Lives in common-domain so that contexts
 * other than {@code identity} can resolve "who is calling?" without depending on
 * identity's domain types (ADR-0019); the implementation lives in
 * identity-data-access, because the {@code users} table is identity's.
 *
 * <p>Since ADR-0030 this port has exactly two callers, and no application service
 * among them: the {@code @CurrentUser} argument resolver, which runs it once per
 * request at the HTTP edge, and {@code LoginActivityListener}, which is
 * event-driven rather than request-bound and so holds a subject rather than a
 * resolved caller. Everything below the edge takes a {@link UserId} and never
 * sees a subject at all.</p>
 */
public interface CallerResolver {

    Optional<UUID> resolveUserId(UUID keycloakSubject);
}
