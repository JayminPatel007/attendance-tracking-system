package org.sabha.container;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.sabha.identity.applicationservice.otp.OtpGateway;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test double for the OTP send port. With codes now hashed at rest (issue #77) the
 * persisted {@code otp_code} is a digest, so OTP-flow integration tests can no
 * longer read the live code from the database; they capture the plaintext here
 * instead, exactly where the application dispatches it. Overrides the real
 * {@code LoggingOtpGateway} via {@link Primary}.
 */
@TestConfiguration
class RecordingOtpGatewayConfig {

    @Bean
    @Primary
    RecordingOtpGateway recordingOtpGateway() {
        return new RecordingOtpGateway();
    }

    static final class RecordingOtpGateway implements OtpGateway {
        private final Map<String, String> lastByMobile = new ConcurrentHashMap<>();

        @Override
        public void send(String mobile, String code) {
            lastByMobile.put(mobile, code);
        }

        /** The most recent plaintext OTP dispatched to {@code mobile}. */
        String lastCodeFor(String mobile) {
            return lastByMobile.get(mobile);
        }
    }
}
