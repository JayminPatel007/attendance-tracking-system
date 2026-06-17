package org.sabha.identity.applicationservice.passwordreset;

/**
 * Driven port generating the opaque, short-lived token handed to the client once
 * a reset OTP is verified (ADR-0004). The client echoes it on the
 * {@code complete} call to set the new password, so the OTP itself is never
 * replayed. Behind a port so tests can inject a deterministic value.
 */
public interface ResetTokenGenerator {

    String generate();
}
