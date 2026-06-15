package org.sabha.analytics.application;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.sabha.analytics.applicationservice.AuditEntry;
import org.sabha.analytics.applicationservice.AuditFilter;
import org.sabha.analytics.applicationservice.AuditLogAccess;
import org.sabha.analytics.applicationservice.AuditLogQueries;
import org.sabha.analytics.applicationservice.AuditScope;
import org.sabha.analytics.applicationservice.AuditTargetType;
import org.sabha.common.CallerResolver;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Audit-log viewer BFF for the Angular web shell (ADR-0023, ADR-0022, Slice 19).
 * Cookie/session authenticated, so the caller is the Keycloak subject in
 * {@link Authentication#getName()}; an authenticated subject with no local User is
 * unauthorized (403). The {@link AuditLogAccess} engine resolves the caller's
 * {@link AuditScope}; a {@link AuditScope.Denied} — every tier below Nirdeshak —
 * is a 403, and the feed is never queried.
 *
 * <p>Filters arrive as query parameters; {@code from} / {@code to} are calendar
 * dates (the {@code to} day is included by translating it to the exclusive start
 * of the next day, in UTC). {@code proxyOnly} keeps only on-behalf-of entries
 * (Slice 14). The unused-by-default {@code targetType} + {@code targetId} pair is
 * how an entity detail screen drills into one entity's history.</p>
 */
@RestController
public class AuditLogBffController {

    /** Hard cap on a page regardless of the requested window (the feed is unbounded). */
    private static final int MAX_LIMIT = 500;

    private final AuditLogAccess access;
    private final AuditLogQueries queries;
    private final CallerResolver callers;

    public AuditLogBffController(AuditLogAccess access, AuditLogQueries queries, CallerResolver callers) {
        this.access = access;
        this.queries = queries;
        this.callers = callers;
    }

    @GetMapping("/bff/audit-log")
    public ResponseEntity<List<AuditEntry>> list(
            @RequestParam(required = false) AuditTargetType targetType,
            @RequestParam(required = false) UUID targetId,
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "false") boolean proxyOnly,
            Authentication authentication) {

        UUID subject = UUID.fromString(authentication.getName());
        AuditScope scope = access.scopeFor(callers.requireUserId(subject));
        if (scope instanceof AuditScope.Denied) {
            return ResponseEntity.status(403).build();
        }
        AuditFilter filter = new AuditFilter(
                targetType,
                targetId,
                actorUserId,
                blankToNull(action),
                startOfDay(from),
                startOfNextDay(to),
                proxyOnly,
                Math.min(AuditFilter.DEFAULT_LIMIT, MAX_LIMIT));
        return ResponseEntity.ok(queries.find(scope, filter));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static Instant startOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /** The {@code to} date is inclusive for the user, so the exclusive bound is the next day. */
    private static Instant startOfNextDay(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
