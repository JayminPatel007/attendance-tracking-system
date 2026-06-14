package org.sabha.container;

import org.sabha.analytics.applicationservice.NotASantException;
import org.sabha.attendance.applicationservice.CallerUnknownException;
import org.sabha.attendance.applicationservice.StaleRosterException;
import org.sabha.common.AuthorizationDeniedException;
import org.sabha.common.ConcurrentModificationException;
import org.sabha.common.DomainException;
import org.sabha.common.NotFoundException;
import org.sabha.identity.applicationservice.AlreadySelectedException;
import org.sabha.identity.applicationservice.DuplicateNominationException;
import org.sabha.identity.applicationservice.NominationNotAuthorizedException;
import org.sabha.identity.applicationservice.OtpRateLimitExceededException;
import org.sabha.identity.applicationservice.OtpResendCooldownException;
import org.sabha.identity.applicationservice.SelectionDecisionNotAuthorizedException;
import org.sabha.identity.applicationservice.TransferNotAuthorizedException;
import org.sabha.identity.applicationservice.UsernameAlreadyTakenException;
import org.sabha.identity.domain.MobileAlreadyRegisteredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global HTTP error mapping (ADR-0019). Lives in application-container as the
 * single declared exception to its "no feature code" rule, because the wire
 * format is a deployment-tier concern. More specific exception subclasses must
 * be declared before their parents so Spring picks the most-specific handler.
 *
 * <p>Errors are rendered as RFC 9457 Problem Details: returning a
 * {@link ProblemDetail} makes Spring serialize {@code type/title/status/detail}
 * as {@code application/problem+json} with its {@code status} driving the HTTP
 * status. The machine-readable {@code code} discriminator (and, for the
 * Directory hard block, {@code existingPersonId}) is carried as an RFC 9457
 * extension property that Jackson unwraps to a top-level field, so clients keep
 * a stable thing to branch on.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail notFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ProblemDetail forbidden(AuthorizationDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, ex.getMessage(), null);
    }

    /**
     * An authenticated caller whose subject maps to no local User is forbidden,
     * not a server error — the canonical mapping so endpoints need not pre-check.
     */
    @ExceptionHandler(CallerUnknownException.class)
    public ProblemDetail callerUnknown(CallerUnknownException ex) {
        return problem(HttpStatus.FORBIDDEN, ex.getMessage(), null);
    }

    @ExceptionHandler(StaleRosterException.class)
    public ProblemDetail staleRoster(StaleRosterException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), "ROSTER_STALE");
    }

    @ExceptionHandler(ConcurrentModificationException.class)
    public ProblemDetail conflict(ConcurrentModificationException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(MobileAlreadyRegisteredException.class)
    public ProblemDetail mobileAlreadyRegistered(MobileAlreadyRegisteredException ex) {
        ProblemDetail problem = problem(HttpStatus.CONFLICT, ex.getMessage(), "MOBILE_ALREADY_REGISTERED");
        problem.setProperty("existingPersonId", ex.existing().id());
        return problem;
    }

    @ExceptionHandler(TransferNotAuthorizedException.class)
    public ProblemDetail transferNotAuthorized(TransferNotAuthorizedException ex) {
        return problem(HttpStatus.FORBIDDEN, ex.getMessage(), null);
    }

    @ExceptionHandler(UsernameAlreadyTakenException.class)
    public ProblemDetail usernameAlreadyTaken(UsernameAlreadyTakenException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), "USERNAME_TAKEN");
    }

    @ExceptionHandler({NominationNotAuthorizedException.class, SelectionDecisionNotAuthorizedException.class})
    public ProblemDetail selectionNotAuthorized(RuntimeException ex) {
        return problem(HttpStatus.FORBIDDEN, ex.getMessage(), null);
    }

    @ExceptionHandler(NotASantException.class)
    public ProblemDetail notASant(NotASantException ex) {
        return problem(HttpStatus.FORBIDDEN, ex.getMessage(), null);
    }

    @ExceptionHandler({DuplicateNominationException.class, AlreadySelectedException.class})
    public ProblemDetail nominationConflict(RuntimeException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler({OtpRateLimitExceededException.class, OtpResendCooldownException.class})
    public ProblemDetail tooManyOtpRequests(RuntimeException ex) {
        return problem(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), null);
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail domain(DomainException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), null);
    }

    /**
     * Build the RFC 9457 body. {@code title} and {@code type} default from the
     * status ("about:blank" + the status reason phrase), so the visible titles
     * stay exactly as before. {@code code}, when present, becomes the stable
     * machine-readable extension property.
     */
    private static ProblemDetail problem(HttpStatus status, String detail, String code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        if (code != null) {
            problem.setProperty("code", code);
        }
        return problem;
    }
}
