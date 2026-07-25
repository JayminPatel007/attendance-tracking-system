package org.sabha.identity.application;

import java.time.LocalDate;
import java.util.UUID;

import org.sabha.identity.domain.Gender;
import org.sabha.identity.domain.Person;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A Person's Directory profile on the wire — the response of the mobile lookup
 * and of the Person detail endpoint, on both the {@code /api} and {@code /bff}
 * chains (ADR-0022), which is why it sits beside the two controllers rather than
 * inside either.
 *
 * <p>Identity and gender are always present; a Person's date of birth is optional
 * (captured when available, informational only) and exactly one of mobile /
 * guardian is set for a given Person (ADR-0013), so all three stay nullable.
 * Saying so in the document (issue #104) is what lets the generated clients hand
 * callers a non-null id, name and gender instead of three seam asserts.
 */
public record PersonResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fullName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Gender gender,
        LocalDate dateOfBirth,
        String mobile,
        UUID guardianPersonId) {

    static PersonResponse of(Person person) {
        return new PersonResponse(person.id(), person.fullName(), person.gender(),
                person.dateOfBirth(), person.mobile(), person.guardianPersonId());
    }
}
