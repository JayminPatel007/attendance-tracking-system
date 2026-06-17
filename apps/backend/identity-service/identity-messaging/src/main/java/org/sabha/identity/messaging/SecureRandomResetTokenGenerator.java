package org.sabha.identity.messaging;

import java.security.SecureRandom;
import java.util.Base64;

import org.sabha.identity.applicationservice.passwordreset.ResetTokenGenerator;
import org.springframework.stereotype.Component;

/**
 * Generates a high-entropy, URL-safe reset token (ADR-0004) — 256 bits of
 * {@link SecureRandom} Base64URL-encoded — handed to the client once its OTP is
 * verified and echoed on {@code complete}. Behind {@link ResetTokenGenerator} so
 * tests inject a deterministic value.
 */
@Component
public class SecureRandomResetTokenGenerator implements ResetTokenGenerator {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom random = new SecureRandom();
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    @Override
    public String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return encoder.encodeToString(bytes);
    }
}
