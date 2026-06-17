package org.sabha.identity.messaging;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import org.sabha.identity.applicationservice.otp.OtpGateway;
import org.springframework.stereotype.Component;

/**
 * Stand-in OTP delivery adapter for v1 (mirrors {@code LoggingDomainEventPublisher}):
 * it logs the code instead of sending an SMS, so the flow is exercisable without a
 * real provider. PRD-0001 keeps the gateway behind {@link OtpGateway} precisely so
 * this can be swapped for a real SMS client later. The mobile is masked in the log.
 */
@Component
public class LoggingOtpGateway implements OtpGateway {

    private static final Logger LOG = System.getLogger(LoggingOtpGateway.class.getName());

    @Override
    public void send(String mobile, String code) {
        LOG.log(Level.INFO, "OTP for {0}: {1}", mask(mobile), code);
    }

    private static String mask(String mobile) {
        if (mobile == null || mobile.length() < 4) {
            return "****";
        }
        return "****" + mobile.substring(mobile.length() - 4);
    }
}
