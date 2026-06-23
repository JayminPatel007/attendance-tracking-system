package org.sabha.sabha.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A registered Sabha Kind — a {@code (demographic, track)} combination the
 * Madhyastha Karyalaya has added to the system (ADR-0009). The set of kinds is
 * extensible data rather than a hardcoded enum; each carries a {@code createdBy}
 * audit field naming the MK member who registered it.
 *
 * <p>Invariant: a Sanyukta kind may exist only on the Regular track
 * ({@link SanyuktaMustBeRegularTrackException}).</p>
 *
 * <p>A Sabha Kind is reference data the whole type system hangs off, so it is
 * never hard-deleted — it is <em>soft-retired</em> instead (ADR-0026): the MK
 * marks it inactive ({@code retiredAt}/{@code retiredBy}) so no new Sabhas,
 * roles, or Home Sabhas of that kind can be created while existing ones drain.
 * Retirement is reversible via {@link #reactivate()}. A {@code null} {@code
 * retiredAt} means active.</p>
 */
public record SabhaKind(UUID id, Demographic demographic, Track track, UUID createdBy,
                        Instant retiredAt, UUID retiredBy) {

    /** Registers a new, active kind, enforcing the Sanyukta-Regular-only invariant. */
    public static SabhaKind register(Demographic demographic, Track track, UUID createdBy) {
        if (demographic == Demographic.SANYUKTA && track != Track.REGULAR) {
            throw new SanyuktaMustBeRegularTrackException(track);
        }
        return new SabhaKind(UUID.randomUUID(), demographic, track, createdBy, null, null);
    }

    /** True when the kind has been retired and no longer accepts new usage. */
    public boolean isRetired() {
        return retiredAt != null;
    }

    /** Retires an active kind, attributing the act to the MK member who performed it. */
    public SabhaKind retire(UUID retiredBy, Instant retiredAt) {
        if (isRetired()) {
            throw new SabhaKindAlreadyRetiredException(id);
        }
        return new SabhaKind(id, demographic, track, createdBy, retiredAt, retiredBy);
    }

    /** Reactivates a retired kind, returning it to active service. */
    public SabhaKind reactivate() {
        if (!isRetired()) {
            throw new SabhaKindNotRetiredException(id);
        }
        return new SabhaKind(id, demographic, track, createdBy, null, null);
    }
}
