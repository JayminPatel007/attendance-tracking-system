package org.sabha.identity.applicationservice;

/**
 * Driven port generating the 6-digit OTP code (PRD-0001). Behind a port so tests
 * can inject a deterministic code instead of a random one.
 */
public interface OtpCodeGenerator {

    String generate();
}
