package org.sabha.identity.applicationservice.appointment;

import java.util.UUID;

/**
 * Identity-local read port over {@code role_assignments}: how many active
 * Sah-Nirdeshaks a (Kshetra, demographic) currently holds. {@link RoleAppointmentService}
 * consults it to enforce the cap of two per (Kshetra, demographic) (ADR-0025 §3) —
 * the threshold itself lives in the service, this port only counts. Demographic is
 * matched as a string token to stay aligned with {@link org.sabha.common.SabhaScope}
 * across the bounded-context seam, mirroring {@link AppointerAuthorityLookup}.
 *
 * <p>"Active" excludes revoked assignments; role-assignment revocation is not yet a
 * modelled state, so today every matching row counts.</p>
 */
public interface SahNirdeshakCountLookup {

    int activeCount(UUID kshetraId, String demographic);
}
