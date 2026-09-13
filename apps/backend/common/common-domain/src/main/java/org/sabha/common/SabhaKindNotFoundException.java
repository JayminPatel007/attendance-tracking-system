package org.sabha.common;

import java.util.UUID;

/**
 * No registered Sabha Kind carries the given id (ADR-0009). Lives in
 * common-domain beside the lookups that report it (issue #218): sabha-service
 * raises it from its own repository, identity from {@link SabhaProvisioning}
 * when a Sabha is defined against a kind that does not exist.
 *
 * <p>This is about the Sabha Kind itself, not its {@link SabhaKindCode}
 * encoding.</p>
 *
 * <p>Mapped to HTTP 404 through {@link NotFoundException}.</p>
 */
public class SabhaKindNotFoundException extends NotFoundException {

    public SabhaKindNotFoundException(UUID sabhaKindId) {
        super("No Sabha Kind with id " + sabhaKindId);
    }
}
