package org.sabha.container;

import java.time.Instant;

/**
 * HTTP error body shape (ADR-0019). Returned by {@link GlobalExceptionHandler}
 * for every mapped exception so the wire format is uniform across the API.
 */
public record ErrorResponse(Instant timestamp, int status, String error, String message) {

    static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(Instant.now(), status, error, message);
    }
}
