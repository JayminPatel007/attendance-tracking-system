package org.sabha.identity.messaging;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The OTP hashing scheme that keeps a 6-digit code out of the clear at rest
 * (issue #77). The 6-digit space is trivially brute-forced against a bare digest,
 * so these tests pin the two defences: the digest is keyed by a server secret and
 * salted per challenge, and it never reveals the plaintext. They also pin the
 * fail-fast guard that refuses to run with an unconfigured / weak key.
 */
class HmacOtpHasherTest {

    private static final UUID CHALLENGE = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID OTHER_CHALLENGE = UUID.fromString("00000000-0000-0000-0000-0000000000c2");
    private static final String CODE = "123456";
    private static final String SECRET = "a-strong-server-secret";
    private static final String OTHER_SECRET = "another-strong-server-secret";

    @Test
    void hashIsDeterministicForTheSameSecretChallengeAndCodeButNeverThePlaintext() {
        HmacOtpHasher hasher = new HmacOtpHasher(SECRET);

        String first = hasher.hash(CHALLENGE, CODE);
        String second = hasher.hash(CHALLENGE, CODE);

        assertThat(first).isEqualTo(second);
        assertThat(first).doesNotContain(CODE);
    }

    @Test
    void sameCodeUnderDifferentChallengesProducesDifferentDigests() {
        HmacOtpHasher hasher = new HmacOtpHasher(SECRET);

        assertThat(hasher.hash(CHALLENGE, CODE))
                .isNotEqualTo(hasher.hash(OTHER_CHALLENGE, CODE));
    }

    @Test
    void theSameCodeUnderDifferentSecretsProducesDifferentDigests() {
        HmacOtpHasher one = new HmacOtpHasher(SECRET);
        HmacOtpHasher two = new HmacOtpHasher(OTHER_SECRET);

        assertThat(one.hash(CHALLENGE, CODE))
                .isNotEqualTo(two.hash(CHALLENGE, CODE));
    }

    @Test
    void differentCodesUnderTheSameChallengeProduceDifferentDigests() {
        HmacOtpHasher hasher = new HmacOtpHasher(SECRET);

        assertThat(hasher.hash(CHALLENGE, "123456"))
                .isNotEqualTo(hasher.hash(CHALLENGE, "654321"));
    }

    @Test
    void refusesToStartWithABlankSecret() {
        assertThatThrownBy(() -> new HmacOtpHasher("  "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SABHA_OTP_HASH_SECRET");
    }

    @Test
    void refusesToStartWithTheShippedDevPlaceholder() {
        assertThatThrownBy(() -> new HmacOtpHasher(HmacOtpHasher.INSECURE_PLACEHOLDER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SABHA_OTP_HASH_SECRET");
    }

    @Test
    void refusesToStartWithASecretShorterThanTheMinimum() {
        assertThatThrownBy(() -> new HmacOtpHasher("tooshort"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SABHA_OTP_HASH_SECRET");
    }
}
