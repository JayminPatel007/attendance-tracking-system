package org.sabha.identity.applicationservice.otp;

import java.time.Clock;

import org.sabha.common.DomainEventPublisher;
import org.sabha.identity.domain.OtpHasher;

/**
 * The one way a unit test stands up an {@link OtpGuardedFlow}: a fixed code, a
 * gateway that remembers what it was handed, and a deterministic hasher. Both
 * OTP features' service tests drive their real flow through this, so the shared
 * module's wiring is described once here rather than re-assembled beside each
 * feature.
 */
public final class OtpFlowFixture {

    /** Deterministic salted stand-in for the production HMAC hasher. */
    public static final OtpHasher HASHER = (challengeId, code) -> "digest(" + challengeId + ":" + code + ")";

    private OtpFlowFixture() {
    }

    /**
     * A flow under the live send budget that always generates {@code fixedCode}
     * and delivers it through {@code gateway}.
     */
    public static OtpGuardedFlow sending(String fixedCode, OtpGateway gateway,
                                         DomainEventPublisher events, Clock clock) {
        return new OtpGuardedFlow(
                new WindowedOtpSendPolicy(), () -> fixedCode, gateway, HASHER, events, clock);
    }

    /** An {@link OtpGateway} that remembers the last code it was asked to deliver. */
    public static final class RecordingOtpGateway implements OtpGateway {

        public String sentTo;
        public String sentCode;

        @Override
        public void send(String mobile, String code) {
            this.sentTo = mobile;
            this.sentCode = code;
        }
    }
}
