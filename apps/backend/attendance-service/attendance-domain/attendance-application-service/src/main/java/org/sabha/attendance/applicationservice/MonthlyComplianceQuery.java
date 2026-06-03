package org.sabha.attendance.applicationservice;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import org.sabha.common.SabhaShapeLookup;
import org.springframework.stereotype.Service;

/**
 * The monthly compliance nudge (ADR-0012): a soft warning — not a block — that a
 * monthly-ad-hoc Sabha has no Occurrence scheduled this calendar month and the
 * month is past its midpoint. Surfaced to the Sanchalak (and later the Nirdeshak
 * dashboard, Slice 15).
 */
@Service
public class MonthlyComplianceQuery {

    private final SabhaShapeLookup sabhaShapes;
    private final OccurrenceCalendar calendar;

    public MonthlyComplianceQuery(SabhaShapeLookup sabhaShapes, OccurrenceCalendar calendar) {
        this.sabhaShapes = sabhaShapes;
        this.calendar = calendar;
    }

    public boolean needsOccurrence(UUID sabhaId, LocalDate asOf) {
        boolean monthly = sabhaShapes.scheduleShapeOf(sabhaId)
                .filter("MONTHLY_AD_HOC"::equals)
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
