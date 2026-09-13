package org.sabha.analytics.applicationservice;

import org.sabha.analytics.domain.Thresholds;
import org.sabha.common.UserId;

/**
 * Write port for the MK-owned re-engagement {@link Thresholds} (ADR-0010). The
 * caller (MK authority) is recorded against the change for audit.
 */
public interface ThresholdAdmin {

    void update(Thresholds thresholds, UserId updatedBy);
}
