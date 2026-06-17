package org.sabha.identity.applicationservice.directory;

import java.util.UUID;

import org.sabha.common.NotFoundException;

/**
 * The Home Sabha chosen for a new Person does not exist, so its Kshetra (the
 * name soft-warn scope) cannot be resolved. Mapped to HTTP 404.
 */
public class HomeSabhaNotFoundException extends NotFoundException {

    public HomeSabhaNotFoundException(UUID sabhaId) {
        super("No Sabha with id " + sabhaId);
    }
}
