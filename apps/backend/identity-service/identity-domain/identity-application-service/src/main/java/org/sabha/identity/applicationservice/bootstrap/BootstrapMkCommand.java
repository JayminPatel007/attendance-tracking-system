package org.sabha.identity.applicationservice.bootstrap;

import org.sabha.identity.domain.Gender;

/**
 * The install-time inputs for seeding the first Madhyastha Karyalaya member
 * (ADR-0011's one-off bootstrap, outside the normal appointment flow). Supplied
 * by the operator via environment variables and parsed in the application
 * container.
 */
public record BootstrapMkCommand(
        String fullName,
        Gender gender,
        String mobile,
        String username,
        String rawPassword) {
}
