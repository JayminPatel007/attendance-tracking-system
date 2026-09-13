package org.sabha.common;

import java.util.UUID;

/**
 * No City carries the given id. Lives in common-domain beside the lookups that
 * report it (issue #218) so every context names the same error the same way:
 * sabha-service raises it from its own repository, analytics from the City
 * directory it reads across the context boundary, and both mean the identical
 * thing to a client.
 *
 * <p>Mapped to HTTP 404 through {@link NotFoundException}.</p>
 */
public class CityNotFoundException extends NotFoundException {

    public CityNotFoundException(UUID cityId) {
        super("No City with id " + cityId);
    }
}
