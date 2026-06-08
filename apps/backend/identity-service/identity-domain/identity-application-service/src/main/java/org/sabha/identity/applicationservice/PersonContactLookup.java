package org.sabha.identity.applicationservice;

import java.util.Optional;
import java.util.UUID;

/**
 * Narrow driven port over the Directory's contact details, used by the
 * password-reset orchestrator (ADR-0004) to find the registered mobile the reset
 * OTP is sent to. Kept separate from the broad {@link PersonDirectory} so the
 * orchestrator depends only on the read it needs; the JDBC adapter implements
 * both ports on one class.
 */
public interface PersonContactLookup {

    /** The Person's registered own-mobile, empty when they have none on file. */
    Optional<String> mobileOf(UUID personId);
}
