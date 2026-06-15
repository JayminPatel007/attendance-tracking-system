package org.sabha.common;

/**
 * Base type for the 409 family (issue #78): an otherwise-valid request that
 * collides with current state — a duplicate, a taken name, a stale snapshot — so
 * the caller must adjust and retry rather than fix malformed input (which would
 * be a 422 {@link DomainException}).
 *
 * <p>Carries an optional machine-readable {@code code} that the global handler
 * surfaces as the RFC 9457 {@code code} extension so clients keep a stable thing
 * to branch on. Mapped to HTTP 409 by the one handler that maps this base type;
 * subclasses supply the message and, where they have one, the code.
 */
public class ConflictException extends RuntimeException {

    private final String code;

    protected ConflictException(String message) {
        this(message, null);
    }

    protected ConflictException(String message, String code) {
        super(message);
        this.code = code;
    }

    /** The machine-readable discriminator, or {@code null} when the message alone suffices. */
    public String code() {
        return code;
    }
}
