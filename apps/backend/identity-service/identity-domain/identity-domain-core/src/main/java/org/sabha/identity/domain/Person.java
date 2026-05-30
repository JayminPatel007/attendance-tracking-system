package org.sabha.identity.domain;

import java.time.LocalDate;
import java.util.UUID;

/**
 * A human known to the system, held in the central Directory (CONTEXT.md).
 *
 * <p>Identity is mobile-keyed (ADR-0013): a Person carries either their own
 * system-wide-unique {@code mobile} or, for a child without their own phone, a
 * {@code guardianPersonId} link to the parent whose mobile they share. Exactly
 * one of the two is present — enforced by {@link #create}.
 */
public record Person(
        UUID id,
        String fullName,
        Gender gender,
        LocalDate dateOfBirth,
        String mobile,
        UUID guardianPersonId) {

    public static Person create(
            UUID id,
            String fullName,
            Gender gender,
            LocalDate dateOfBirth,
            String mobile,
            UUID guardianPersonId) {
        boolean hasMobile = mobile != null && !mobile.isBlank();
        boolean hasGuardian = guardianPersonId != null;
        if (!hasMobile && !hasGuardian) {
            throw new GuardianOrMobileRequiredException();
        }
        return new Person(id, fullName, gender, dateOfBirth, hasMobile ? mobile : null, guardianPersonId);
    }

    public boolean isGuardianLinked() {
        return guardianPersonId != null;
    }
}
