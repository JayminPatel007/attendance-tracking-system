package org.sabha.identity.applicationservice;

import org.sabha.common.DomainException;

/**
 * The User exists but their Person has no registered mobile, so a self-service
 * reset OTP cannot be delivered (ADR-0004). Such a User must use the
 * assigner-reissue fallback instead. Mapped to HTTP 422.
 */
public class NoRegisteredMobileException extends DomainException {

    public NoRegisteredMobileException(String username) {
        super("User '" + username + "' has no registered mobile to receive a reset OTP");
    }
}
