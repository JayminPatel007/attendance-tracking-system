package org.sabha.attendance.applicationservice;

import java.util.UUID;

import org.sabha.common.DomainException;

/**
 * Thrown when an Occurrence is manually created against a Sabha that is not
 * monthly-ad-hoc (ADR-0012): weekly-recurring Sabhas materialize their
 * Occurrences via the cron, never by hand. Mapped to HTTP 422.
 */
public class NotMonthlyAdHocException extends DomainException {

    public NotMonthlyAdHocException(UUID sabhaId) {
        super("Sabha " + sabhaId + " is not monthly-ad-hoc; Occurrences are materialized automatically");
    }
}
