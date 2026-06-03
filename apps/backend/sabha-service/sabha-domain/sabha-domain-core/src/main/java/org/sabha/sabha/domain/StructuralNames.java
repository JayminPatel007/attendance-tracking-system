package org.sabha.sabha.domain;

/** Shared validation for the names of structural entities (ADR-0009). */
final class StructuralNames {

    private StructuralNames() {
    }

    /** Returns {@code name} when non-blank, else throws for the named entity. */
    static String require(String name, String entity) {
        if (name == null || name.isBlank()) {
            throw new StructuralNameRequiredException(entity);
        }
        return name;
    }
}
