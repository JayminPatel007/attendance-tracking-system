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
 */
@Component
public class HmacOtpHasher implements OtpHasher {

    private static final String ALGORITHM = "HmacSHA256";

    private final byte[] secret;

    public HmacOtpHasher(@Value("${sabha.otp.hash-secret}") String secret) {
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
