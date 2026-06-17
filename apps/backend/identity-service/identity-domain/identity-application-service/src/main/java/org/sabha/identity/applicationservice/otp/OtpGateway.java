package org.sabha.identity.applicationservice.otp;

/**
 * Driven port over the SMS/notification channel that delivers OTPs (PRD-0001:
 * "Notifications/SMS gateway abstracted behind a port so a fake can be used in
 * tests"). The 8A adapter logs the code; a real provider lands later.
 */
public interface OtpGateway {

    void send(String mobile, String code);
}
