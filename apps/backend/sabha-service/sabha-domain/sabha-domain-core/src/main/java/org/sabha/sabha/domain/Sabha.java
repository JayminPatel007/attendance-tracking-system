package org.sabha.sabha.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

/**
 * A Sabha — a standing congregation in a Kshetra, of a registered
 * {@link SabhaKind} (demographic + track), with one of two schedule shapes
 * (ADR-0012). The shape is a discriminator: the {@link ScheduleShape#WEEKLY_RECURRING}
 * variant carries the standing {@code (dayOfWeek, startTime, endTime)} slot; the
 * {@link ScheduleShape#MONTHLY_AD_HOC} variant carries no schedule fields (they are
 * {@code null}). Both carry a standing venue and a {@code createdBy} audit field.
 *
 * <p>The Sanchalak who runs the Sabha is recorded as a role assignment in the
 * identity context, not on this record.</p>
 */
public record Sabha(
        UUID id,
        UUID kshetraId,
        UUID sabhaKindId,
        ScheduleShape scheduleShape,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        String standingVenue,
        UUID createdBy) {

    /** Defines a weekly-recurring Sabha with its fixed standing slot. */
    public static Sabha weekly(UUID kshetraId, UUID sabhaKindId, DayOfWeek dayOfWeek,
                               LocalTime startTime, LocalTime endTime,
                               String standingVenue, UUID createdBy) {
        if (dayOfWeek == null || startTime == null || endTime == null
                || !startTime.isBefore(endTime)) {
            throw new WeeklyScheduleRequiredException();
        }
        return new Sabha(UUID.randomUUID(), kshetraId, sabhaKindId,
                ScheduleShape.WEEKLY_RECURRING, dayOfWeek, startTime, endTime,
                StructuralNames.require(standingVenue, "Standing venue"), createdBy);
    }

    /** Defines a monthly-ad-hoc Sabha — no standing slot, only its venue. */
    public static Sabha monthlyAdHoc(UUID kshetraId, UUID sabhaKindId,
                                     String standingVenue, UUID createdBy) {
        return new Sabha(UUID.randomUUID(), kshetraId, sabhaKindId,
                ScheduleShape.MONTHLY_AD_HOC, null, null, null,
                StructuralNames.require(standingVenue, "Standing venue"), createdBy);
    }
}
