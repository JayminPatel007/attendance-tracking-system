package org.sabha.identity.applicationservice.sabhadefinition;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

import org.sabha.identity.applicationservice.appointment.Appointee;

/**
 * Request to define a Sabha and appoint its Sanchalak in one transaction
 * (ADR-0012). The {@code weekly} discriminator selects the schedule shape: a
 * weekly-recurring Sabha carries the {@code (dayOfWeek, startTime, endTime)} slot;
 * a monthly-ad-hoc Sabha carries none. A {@code sanchalak} is mandatory; a
 * {@code sahSanchalak} is optional.
 */
public record SabhaDefinitionCommand(
        UUID kshetraId,
        UUID sabhaKindId,
        boolean weekly,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        String standingVenue,
        Appointee sanchalak,
        Appointee sahSanchalak) {

    public static SabhaDefinitionCommand weekly(
            UUID kshetraId, UUID sabhaKindId, DayOfWeek dayOfWeek,
            LocalTime startTime, LocalTime endTime, String standingVenue,
            Appointee sanchalak, Appointee sahSanchalak) {
        return new SabhaDefinitionCommand(kshetraId, sabhaKindId, true,
                dayOfWeek, startTime, endTime, standingVenue, sanchalak, sahSanchalak);
    }

    public static SabhaDefinitionCommand monthlyAdHoc(
            UUID kshetraId, UUID sabhaKindId, String standingVenue,
            Appointee sanchalak, Appointee sahSanchalak) {
        return new SabhaDefinitionCommand(kshetraId, sabhaKindId, false,
                null, null, null, standingVenue, sanchalak, sahSanchalak);
    }

    public boolean hasSahSanchalak() {
        return sahSanchalak != null;
    }
}
