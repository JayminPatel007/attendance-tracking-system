/**
 * Interface-adapters ring (Clean Architecture) for the Identity context:
 * outbound messaging/notification adapters. Holds the OTP delivery adapters
 * (Slice 8) — a logging stand-in {@code OtpGateway} and the
 * {@code OtpCodeGenerator} — behind the ports declared in
 * identity-application-service (ADR-0019). Also holds the Keycloak Admin REST
 * adapter (Slice 9) that provisions Users behind the {@code KeycloakAdminClient}
 * port.
 */
package org.sabha.identity.messaging;
