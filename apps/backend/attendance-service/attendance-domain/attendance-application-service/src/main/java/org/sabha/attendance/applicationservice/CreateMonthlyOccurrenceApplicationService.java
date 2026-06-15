package org.sabha.attendance.applicationservice;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.sabha.attendance.domain.Occurrence;
import org.sabha.common.AuthorizationDeniedException;
import org.sabha.common.AuthorizedAction;
import org.sabha.common.CallerResolver;
import org.sabha.common.SabhaShapeLookup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manual monthly-ad-hoc Occurrence creation (ADR-0012): a BSS/YSS Sanchalak
 * creates this month's Occurrence on a date they pick, since monthly Sabhas have
 * no standing schedule to materialize from.
 *
 * <ol>
 *   <li>Resolve the caller via {@link CallerResolver}.</li>
 *   <li>Guard the Sabha is monthly-ad-hoc via {@link SabhaShapeLookup} — weekly
 *       Sabhas materialize automatically and reject manual creation (422); an
 *       unknown Sabha is a 404.</li>
 *   <li>Authorize the caller as the Sabha's Sanchalak via the
 *       {@link AuthorizationEngine} (Sah-Sanchalak excluded, ADR-0001).</li>
 *   <li>Insert a {@code Scheduled} Occurrence carrying the picked date, time, and
 *       venue; it then follows the same state machine as any other Occurrence.</li>
 * </ol>
 */
@Service
public class CreateMonthlyOccurrenceApplicationService {

    private final CallerResolver callerResolver;
    private final AuthorizationEngine authz;
    private final SabhaShapeLookup sabhaShapes;
    private final OccurrenceInsert occurrences;

    public CreateMonthlyOccurrenceApplicationService(
            CallerResolver callerResolver,
            AuthorizationEngine authz,
            SabhaShapeLookup sabhaShapes,
            OccurrenceInsert occurrences) {
        this.callerResolver = callerResolver;
        this.authz = authz;
        this.sabhaShapes = sabhaShapes;
        this.occurrences = occurrences;
    }

    @Transactional
    public UUID create(UUID keycloakSubject, UUID sabhaId, LocalDate date,
                       LocalTime startTime, LocalTime endTime, String venue) {
        UUID caller = callerResolver.requireUserId(keycloakSubject);

        String shape = sabhaShapes.scheduleShapeOf(sabhaId)
                .orElseThrow(() -> new SabhaNotFoundException(sabhaId));
        if (!"MONTHLY_AD_HOC".equals(shape)) {
            throw new NotMonthlyAdHocException(sabhaId);
        }

        if (!authz.canUserDo(caller, AuthorizedAction.CREATE_OCCURRENCE, sabhaId)) {
            throw new AuthorizationDeniedException(caller, AuthorizedAction.CREATE_OCCURRENCE);
        }

        Occurrence occurrence = Occurrence.scheduledAt(UUID.randomUUID(), sabhaId, date, startTime, endTime, venue);
        occurrences.add(occurrence);
        return occurrence.id();
    }
}
