package org.sabha.identity.applicationservice.appointment;

/**
 * Contact details surfaced by the "who appointed me?" lookup (ADR-0004) so a
 * locked-out User can reach the Karyakar who can reissue their password — their
 * appointer, or a Madhyastha Karyalaya member for Sants.
 */
public record AppointerContact(String name, String mobile) {
}
