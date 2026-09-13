package org.sabha.attendance.domain;

import org.sabha.common.DomainException;

/**
 * Raised when the text offered for a {@link Reason} cannot be one — it is absent,
 * blank, or too long. Mapped to HTTP 422 by the global handler's
 * {@link DomainException} mapping, which is the status the two exceptions this
 * type replaced (cancellation- and reopen-specific) already produced.
 *
 * <p>The message is user-facing copy: the mobile client renders the ProblemDetail
 * {@code detail} verbatim. It deliberately names no Occurrence — a {@link Reason}
 * is constructed before anything ties it to one, and a UUID in the text served no
 * reader.</p>
 */
public class InvalidReasonException extends DomainException {

    InvalidReasonException(String message) {
        super(message);
    }
}
