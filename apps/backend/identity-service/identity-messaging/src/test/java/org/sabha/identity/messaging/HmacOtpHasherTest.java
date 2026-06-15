package org.sabha.identity.messaging;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The OTP hashing scheme that keeps a 6-digit code out of the clear at rest
 * (issue #77). The 6-digit space is trivially brute-forced against a bare digest,
 * so these tests pin the two defences: the digest is keyed by a server secret and
 * salted per challenge, and it never reveals the plaintext.
 */
class HmacOtpHasherTest {

    private static final UUID CHALLENGE = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID OTHER_CHALLENGE = UUID.fromString("00000000-0000-0000-0000-0000000000c2");
    private static final String CODE = "123456";

    @Test
    void hashIsDeterministicForTheSameSecretChallengeAndCodeButNeverThePlaintext() {
        HmacOtpHasher hasher = new HmacOtpHasher("server-secret");

        String first = hasher.hash(CHALLENGE, CODE);
        String second = hasher.hash(CHALLENGE, CODE);

        assertThat(first).isEqualTo(second);
        assertThat(first).doesNotContain(CODE);
    }

    @Test
    void sameCodeUnderDifferentChallengesProducesDifferentDigests() {
        HmacOtpHasher hasher = new HmacOtpHasher("server-secret");

        assertThat(hasher.hash(CHALLENGE, CODE))
                .isNotEqualTo(hasher.hash(OTHER_CHALLENGE, CODE));
    }

    @Test
    void theSameCodeUnderDifferentSecretsProducesDifferentDigests() {
        HmacOtpHasher one = new HmacOtpHasher("secret-one");
        HmacOtpHasher two = new HmacOtpHasher("secret-two");

        assertThat(one.hash(CHALLENGE, CODE))
                .isNotEqualTo(two.hash(CHALLENGE, CODE));
    }

    @Test
    void differentCodesUnderTheSameChallengeProduceDifferentDigests() {
        HmacOtpHasher hasher = new HmacOtpHasher("server-secret");

        assertThat(hasher.hash(CHALLENGE, "123456"))
                .isNotEqualTo(hasher.hash(CHALLENGE, "654321"));
    }
}
