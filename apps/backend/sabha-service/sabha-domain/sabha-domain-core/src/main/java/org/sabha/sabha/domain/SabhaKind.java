package org.sabha.sabha.domain;

import java.util.UUID;

/**
 * A registered Sabha Kind — a {@code (demographic, track)} combination the
 * Madhyastha Karyalaya has added to the system (ADR-0009). The set of kinds is
 * extensible data rather than a hardcoded enum; each carries a {@code createdBy}
 * audit field naming the MK member who registered it.
 *
 * <p>Invariant: a Sanyukta kind may exist only on the Regular track
 * ({@link SanyuktaMustBeRegularTrackException}).</p>
 */
public record SabhaKind(UUID id, Demographic demographic, Track track, UUID createdBy) {

    /** Registers a new kind, enforcing the Sanyukta-Regular-only invariant. */
    public static SabhaKind register(Demographic demographic, Track track, UUID createdBy) {
        if (demographic == Demographic.SANYUKTA && track != Track.REGULAR) {
            throw new SanyuktaMustBeRegularTrackException(track);
        }
        return new SabhaKind(UUID.randomUUID(), demographic, track, createdBy);
    }
}
