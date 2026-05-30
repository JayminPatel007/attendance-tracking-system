package org.sabha.container;

import org.sabha.attendance.applicationservice.StaleRosterException;
import org.sabha.common.AuthorizationDeniedException;
import org.sabha.common.ConcurrentModificationException;
import org.sabha.common.DomainException;
import org.sabha.common.NotFoundException;
import org.sabha.identity.domain.MobileAlreadyRegisteredException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global HTTP error mapping (ADR-0019). Lives in application-container as the
 * single declared exception to its "no feature code" rule, because the wire
 * format is a deployment-tier concern. More specific exception subclasses must
 * be declared before their parents so Spring picks the most-specific handler.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(NotFoundException ex) {
        return ResponseEntity.status(404)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> forbidden(AuthorizationDeniedException ex) {
        return ResponseEntity.status(403)
                .body(ErrorResponse.of(403, "Forbidden", ex.getMessage()));
    }

    @ExceptionHandler(StaleRosterException.class)
    public ResponseEntity<ErrorResponse> staleRoster(StaleRosterException ex) {
        return ResponseEntity.status(409)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage(), "ROSTER_STALE"));
    }

    @ExceptionHandler(ConcurrentModificationException.class)
    public ResponseEntity<ErrorResponse> conflict(ConcurrentModificationException ex) {
        return ResponseEntity.status(409)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(MobileAlreadyRegisteredException.class)
    public ResponseEntity<ErrorResponse> mobileAlreadyRegistered(MobileAlreadyRegisteredException ex) {
        return ResponseEntity.status(409)
                .body(ErrorResponse.mobileConflict(ex.getMessage(), ex.existing().id()));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> domain(DomainException ex) {
        return ResponseEntity.status(422)
                .body(ErrorResponse.of(422, "Unprocessable Entity", ex.getMessage()));
    }
}
