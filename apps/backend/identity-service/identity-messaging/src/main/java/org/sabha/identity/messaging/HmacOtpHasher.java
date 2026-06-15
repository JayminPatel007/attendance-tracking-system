package org.sabha.identity.messaging;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.sabha.identity.domain.OtpHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * HMAC-SHA256 implementation of {@link OtpHasher} (issue #77). A 6-digit OTP has
 * only a million possibilities, so a bare digest is brute-forced offline in
 * moments; this hash is therefore <em>keyed</em> by a server-side secret
 * ({@code sabha.otp.hash-secret}) an attacker with only database/backup read
 * access does not hold, and <em>salted</em> per row with the challenge id so two
 * identical codes never share a digest. The secret lives outside the database; if
 * it is ever rotated, in-flight challenges fail to verify and the caller simply
 * re-requests an OTP (the rows are at most minutes old).
 *
 * <p>The keying is the whole defence, so a misconfigured secret must not pass
 * silently: the constructor refuses to start the context if the secret is unset,
 * still the shipped dev placeholder, or too short to be a real key. This turns a
 * forgotten {@code SABHA_OTP_HASH_SECRET} into a loud boot failure instead of a
 * deployment that quietly hashes with a value sitting in the repo.</p>
 */
@Component
public class HmacOtpHasher implements OtpHasher {

    private static final String ALGORITHM = "HmacSHA256";

    /** The placeholder shipped in {@code application.yml}; seeing it means the secret was never configured. */
    static final String INSECURE_PLACEHOLDER = "dev-otp-hash-secret";

    /** A real HMAC key should carry meaningful entropy, not a handful of characters. */
    static final int MIN_SECRET_LENGTH = 16;

    private final byte[] secret;

    public HmacOtpHasher(@Value("${sabha.otp.hash-secret}") String secret) {
        if (secret == null || secret.isBlank()
                || secret.equals(INSECURE_PLACEHOLDER)
                || secret.strip().length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "sabha.otp.hash-secret is unset, the dev placeholder, or shorter than "
                    + MIN_SECRET_LENGTH + " characters. Set SABHA_OTP_HASH_SECRET to a strong, "
                    + "environment-specific value so OTP codes are not hashed with a guessable key (issue #77).");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String hash(UUID challengeId, String code) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            // Per-row salt: the challenge id is mixed in before the code so the same
            // code under two challenges yields two different digests.
            mac.update(challengeId.toString().getBytes(StandardCharsets.UTF_8));
            byte[] digest = mac.doFinal(code.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("OTP hashing failed", e);
        }
    }
}
