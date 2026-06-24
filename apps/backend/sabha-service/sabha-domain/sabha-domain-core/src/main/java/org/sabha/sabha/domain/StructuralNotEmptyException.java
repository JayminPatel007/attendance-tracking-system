package org.sabha.sabha.domain;

import org.sabha.common.ConflictException;

/**
 * Thrown when a geographic entity (City, Zone, Kshetra, Sabha) is deleted while
 * it still has live children or recorded Occurrences beneath it (ADR-0026,
 * block-if-non-empty). Attendance history is never destroyed, so the delete is
 * rejected rather than cascaded — a state collision, hence HTTP 409.
 *
 * <p>The message is the human-readable blocking reason the web surfaces inline
 * (e.g. {@code "has 6 Kshetras"}); the factory methods keep that wording the one
 * canonical place it is defined.</p>
 */
public class StructuralNotEmptyException extends ConflictException {

    public static final String CODE = "STRUCTURE_NOT_EMPTY";

    private StructuralNotEmptyException(String reason) {
        super(reason, CODE);
    }

    public static StructuralNotEmptyException cityHasZones(int count) {
        return new StructuralNotEmptyException(reason(count, "Zone"));
    }

    public static StructuralNotEmptyException zoneHasKshetras(int count) {
        return new StructuralNotEmptyException(reason(count, "Kshetra"));
    }

    public static StructuralNotEmptyException kshetraHasSabhas(int count) {
        return new StructuralNotEmptyException(reason(count, "Sabha"));
    }

    public static StructuralNotEmptyException sabhaHasOccurrences(int count) {
        return new StructuralNotEmptyException(reason(count, "Occurrence"));
    }

    private static String reason(int count, String noun) {
        return "has " + count + " " + noun + (count == 1 ? "" : "s");
    }
}
