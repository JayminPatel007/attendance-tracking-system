package org.sabha.identity.messaging;

import java.security.SecureRandom;

import org.sabha.identity.applicationservice.OtpCodeGenerator;
import org.springframework.stereotype.Component;

/**
 * Generates a uniformly random 6-digit OTP (PRD-0001), zero-padded so every code
 * is exactly six characters. Behind {@link OtpCodeGenerator} so tests inject a
 * deterministic code instead.
 */
@Component
public class SecureRandomOtpCodeGenerator implements OtpCodeGenerator {

    private static final int BOUND = 1_000_000;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        return String.format("%06d", random.nextInt(BOUND));
    }
}
