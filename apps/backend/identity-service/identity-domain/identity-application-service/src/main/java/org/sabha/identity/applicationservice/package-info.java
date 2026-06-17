/**
 * Use-case ring (Clean Architecture) for the Identity context: application
 * services that orchestrate User aggregates plus driven-port interfaces
 * (UserRepository) implemented in identity-data-access. ADR-0019 + ADR-0020.
 *
 * <p>Grouped into feature subpackages in domain vocabulary — {@code directory},
 * {@code appointment}, {@code selection}, {@code passwordreset}, {@code otp},
 * {@code transfer}, {@code sabhadefinition}, {@code bootstrap}, {@code session}
 * — so a reader lands in the handful of files for one concern. A few genuinely
 * cross-cutting driven ports ({@code IdentityProviderGateway},
 * {@code UserRepository}) and the shared {@code UnknownUsernameException} stay
 * in this root package rather than being forced into one feature.
 */
package org.sabha.identity.applicationservice;
