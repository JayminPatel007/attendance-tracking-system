package org.sabha.attendance.applicationservice;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import org.sabha.common.SabhaFact;
import org.sabha.common.SabhaFacts;
import org.springframework.stereotype.Service;

/**
 * The monthly compliance nudge (ADR-0012): a soft warning — not a block — that a
 * monthly-ad-hoc Sabha has no Occurrence scheduled this calendar month and the
 * month is past its midpoint. Surfaced to the Sanchalak (and later the Nirdeshak
 * dashboard, Slice 15).
 */
@Service
public class MonthlyComplianceQuery {

    private final SabhaFacts sabhas;
    private final OccurrenceCalendar calendar;

    public MonthlyComplianceQuery(SabhaFacts sabhas, OccurrenceCalendar calendar) {
        this.sabhas = sabhas;
        this.calendar = calendar;
    }

    public boolean needsOccurrence(UUID sabhaId, LocalDate asOf) {
        boolean monthly = sabhas.of(sabhaId)
                .filter(SabhaFact::isMonthlyAdHoc)
                .isPresent();
        if (!monthly || !pastMidpoint(asOf)) {
            return false;
        }
        return !calendar.existsInMonth(sabhaId, YearMonth.from(asOf));
    }

    /** Past the halfway point of the month — strictly after the middle day. */
    private static boolean pastMidpoint(LocalDate asOf) {
        return asOf.getDayOfMonth() * 2 > asOf.lengthOfMonth();
    }
}
