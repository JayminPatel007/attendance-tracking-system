package org.sabha.attendance.applicationservice;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.sabha.common.UserId;
import org.springframework.stereotype.Service;

/**
 * Lists the monthly-ad-hoc Sabhas the calling Sanchalak presides over, each
 * tagged with its compliance nudge (ADR-0012). The mobile uses this both to pick
 * a Sabha to create this month's Occurrence against and to surface the soft
 * "no Occurrence yet this month" warning. An empty list means the caller
 * presides over no monthly Sabha — an unidentifiable caller was already rejected
 * at the edge (ADR-0030).
 */
@Service
public class ListSanchalakMonthlySabhasUseCase {

    private final SanchalakSabhasQuery sabhas;
    private final MonthlyComplianceQuery compliance;
    private final Clock clock;

    public ListSanchalakMonthlySabhasUseCase(
            SanchalakSabhasQuery sabhas,
            MonthlyComplianceQuery compliance,
            Clock clock) {
        this.sabhas = sabhas;
        this.compliance = compliance;
        this.clock = clock;
    }

    public List<MonthlySabha> execute(UserId caller) {
        return listFor(caller.value());
    }

    private List<MonthlySabha> listFor(UUID sanchalakUserId) {
        LocalDate today = LocalDate.now(clock);
        return sabhas.monthlyAdHocFor(sanchalakUserId).stream()
                .map(s -> new MonthlySabha(
                        s.sabhaId(),
                        s.sabhaKind(),
                        s.standingVenue(),
                        compliance.needsOccurrence(s.sabhaId(), today)))
                .toList();
    }
}
