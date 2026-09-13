package org.sabha.attendance.domain;

/**
 * The justification a User must give when cancelling or reopening an Occurrence
 * (ADR-0001). Stamped on the Occurrence's audit row and shown to anyone reading
 * its history.
 *
 * <p>The requirement lives in the type rather than in each operation that needs
 * one: {@code cancel} and {@code reopen} previously each restated "must not be
 * blank" in their own body, behind a {@code String} parameter that promised
 * nothing. A {@code Reason} that exists is valid, so every future operation
 * needing one inherits the rule instead of adding the next copy of the check.</p>
 *
 * <p>Text is stripped on the way in, so the stored audit reason never carries the
 * caller's incidental whitespace, and the length cap is measured against the
 * stripped text — padding alone cannot breach it. The cap exists because the
 * backing {@code occurrence_state_transitions.reason} column is unbounded
 * {@code TEXT} and the endpoints accept arbitrary request bodies.</p>
 */
public record Reason(String text) {

    /** Long enough for any real explanation; short enough that the column is not a dumping ground. */
    public static final int MAX_LENGTH = 500;

    public Reason {
        if (text == null || text.isBlank()) {
            throw new InvalidReasonException("A reason is required.");
        }
        text = text.strip();
        if (text.length() > MAX_LENGTH) {
            throw new InvalidReasonException(
                    "A reason must be at most " + MAX_LENGTH + " characters.");
        }
    }

    @Override
    public String toString() {
        return text;
    }
}
