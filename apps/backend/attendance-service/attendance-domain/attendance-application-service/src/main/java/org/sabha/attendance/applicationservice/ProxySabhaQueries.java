package org.sabha.attendance.applicationservice;

import java.util.List;
import java.util.UUID;

/**
 * Read-side port for the proxy Sabha picker (Slice 14): the Sabhas currently
 * assigned to a Nirikshak, each with its Sanchalak and the informational "last
 * seen" hint. Like the reopen projection (ADR-0008 CQRS read), the derived
 * "last seen" — the latest of the Sanchalak's login / sync / marking — is computed
 * in SQL and the BFF controller consults this port directly.
 */
public interface ProxySabhaQueries {

    List<ProxySabhaListItem> assignedSabhas(UUID nirikshakUserId);

    /**
     * The recent Occurrences of {@code sabhaId} the Nirikshak may shape in proxy
     * mode, returned only when the Sabha is actually assigned to them (so an
     * unassigned caller sees an empty list rather than another Sabha's
     * Occurrences). Authorization of the shaping mutations themselves remains the
     * {@link AuthorizationEngine}'s job.
     */
    List<ProxyOccurrenceItem> proxyOccurrences(UUID nirikshakUserId, UUID sabhaId);
}
