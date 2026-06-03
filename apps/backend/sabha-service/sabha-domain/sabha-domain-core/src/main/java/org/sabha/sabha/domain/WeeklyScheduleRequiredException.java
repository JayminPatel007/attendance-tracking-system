package org.sabha.sabha.domain;

import org.sabha.common.DomainException;

/**
 * Thrown when a weekly-recurring Sabha is defined without a complete, valid
 * standing slot — a day-of-week, a start time, and an end time strictly after
 * the start (ADR-0012). Mapped to HTTP 422.
 */
public class WeeklyScheduleRequiredException extends DomainException {

    public WeeklyScheduleRequiredException() {
        super("A weekly-recurring Sabha needs a day-of-week and a start time before its end time");
    }
}
