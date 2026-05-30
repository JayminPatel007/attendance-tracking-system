package org.sabha.identity.domain;

/**
 * A Person's gender, used to filter eligibility for demographic Sabha kinds
 * (CONTEXT.md / ADR-0013). Mirrors the {@code persons.gender} CHECK constraint.
 */
public enum Gender {
    MALE,
    FEMALE
}
