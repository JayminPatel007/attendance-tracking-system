package org.sabha.identity.applicationservice;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request to add a Person to the Directory. Either {@code mobile} (their own) or
 * {@code guardianPersonId} (a child sharing a parent's mobile) must be set, per
 * ADR-0013. {@code overrideDuplicateWarning} carries the adder's "none of these
 * — create new" decision past the name soft-warn.
 */
public record AddPersonCommand(
        String fullName,
        org.sabha.identity.domain.Gender gender,
        LocalDate dateOfBirth,
        String mobile,
        UUID guardianPersonId,
        UUID homeSabhaId,
        boolean overrideDuplicateWarning) {
}
