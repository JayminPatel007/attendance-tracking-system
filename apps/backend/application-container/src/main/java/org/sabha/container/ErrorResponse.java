package org.sabha.container;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * HTTP error body shape (ADR-0019). Returned by {@link GlobalExceptionHandler}
 * for every mapped exception so the wire format is uniform across the API.
 * {@code code} is an optional machine-readable discriminator the client can
 * branch on (e.g., {@code ROSTER_STALE} so the mobile can prompt a sync).
 * {@code existingPersonId} is set only on the Directory mobile hard block
 * ({@code MOBILE_ALREADY_REGISTERED}) so the client can redirect to that
 * Person's profile (ADR-0013).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp, int status, String error, String message, String code, UUID existingPersonId) {

    static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(Instant.now(), status, error, message, null, null);
    }

    static ErrorResponse of(int status, String error, String message, String code) {
        return new ErrorResponse(Instant.now(), status, error, message, code, null);
    }

    static ErrorResponse mobileConflict(String message, UUID existingPersonId) {
        return new ErrorResponse(
                Instant.now(), 409, "Conflict", message, "MOBILE_ALREADY_REGISTERED", existingPersonId);
    }
}
