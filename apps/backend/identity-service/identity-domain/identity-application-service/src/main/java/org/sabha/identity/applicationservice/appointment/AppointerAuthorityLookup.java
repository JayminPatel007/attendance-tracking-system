package org.sabha.identity.applicationservice.appointment;

import java.util.UUID;

/**
 * Identity-local read port over {@code role_assignments}: whether a user already
 * holds an appointing tier at a given scope. The appointment Authorization Engine
 * (ADR-0011) consults it to decide whether the appointer is entitled to fill the
 * role beneath them. Demographic is matched as a string token to stay aligned
 * with {@link org.sabha.common.SabhaScope} across the bounded-context seam.
 *
 * <p>Madhyastha Karyalaya membership is read separately through
 * {@link org.sabha.common.MadhyasthaKaryalayaLookup} — it is not a scoped tier.</p>
 */
public interface AppointerAuthorityLookup {

    boolean holdsNirdeshak(UUID userId, UUID kshetraId, String demographic);

    boolean holdsSanyojak(UUID userId, UUID zoneId, String demographic);

    boolean holdsRegionalTeam(UUID userId, UUID cityId, String demographic);
}
