package org.sabha.attendance.applicationservice;

import java.time.Instant;
import java.util.UUID;

/**
 * One row of the proxy Sabha picker (Slice 14): a Sabha the Nirikshak may proxy,
 * with the informational "last seen" hint for its Sanchalak. {@code lastSeenAt} is
 * {@code null} when the Sanchalak has never been observed; the hint never gates
 * proxy entry.
 */
public record ProxySabhaListItem(
        UUID sabhaId,
        String sabhaLabel,
        UUID sanchalakUserId,
        String sanchalakName,
        Instant lastSeenAt) {
}
