package org.sabha.common;

import java.util.Objects;
import java.util.UUID;

/**
 * The local {@code users.id} of a signed-in caller — the identity the domain
 * actually works in, and the one {@code CONTEXT.md} names (a <em>User</em> is a
 * Person who can log into the system).
 *
 * <p>Resolved once at the HTTP edge from the credential's Keycloak subject and
 * passed down; no module below the edge should ever see a subject again
 * (ADR-0030). The wrapper exists so a caller cannot be silently swapped with the
 * other {@code UUID}s that flow through the same signatures — a {@code personId},
 * a {@code sabhaId} — which the type system could not tell apart while every id
 * was a bare {@code UUID}.</p>
 *
 * <p>Cross-context lookup ports still take bare {@code UUID}s; call {@link
 * #value()} at that boundary. Typing those too is issue #204.</p>
 */
public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "UserId value must not be null");
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
