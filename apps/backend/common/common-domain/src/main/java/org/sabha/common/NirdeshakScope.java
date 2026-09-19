package org.sabha.common;

import java.util.UUID;

/**
 * A single Nirdeshak ownership scope: a Kshetra and the demographic within it. A
 * Nirdeshak appointment carries both (Slice 11, ADR-0011), so neither half means
 * anything alone.
 *
 * <p>Was nested in the deleted {@code NirdeshakScopeLookup}; it outlived that
 * port because the sabha context's "my Sabhas" listing still takes a list of
 * these across the bounded-context seam (ADR-0026). It now comes from {@link
 * CallerAuthority#nirdeshakScopes()} rather than a query.</p>
 */
public record NirdeshakScope(UUID kshetraId, String demographic) {
}
