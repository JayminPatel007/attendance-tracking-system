package org.sabha.common;

import java.util.UUID;

/**
 * No Sabha carries the given id. Lives in common-domain beside the lookups that
 * report it (issue #218): sabha-service raises it from its own repository,
 * attendance and identity from the cross-context ports they read through.
 *
 * <p>Which field named the missing id — a Sabha to delete, the Sabha an
 * Occurrence is created against, a Person's Home Sabha — is in the request, not
 * in the error. Identity's {@code HomeSabhaNotFoundException} carried a
 * byte-identical message and is folded in here.</p>
 *
 * <p>Mapped to HTTP 404 through {@link NotFoundException}.</p>
 */
public class SabhaNotFoundException extends NotFoundException {

    public SabhaNotFoundException(UUID sabhaId) {
        super("No Sabha with id " + sabhaId);
    }
}
