package org.sabha.analytics.applicationservice;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The {@code AuditReadAccess} seam the identity web shell consults to gate the
 * Audit-log section (issue #80). It must answer "yes" for exactly the callers
 * {@link AuditLogAccess} admits and "no" for the rest, so the sidebar can never
 * drift from the BFF engine.
 *
 * <p>This is also the test ADR-0032 predicted would become trivial: the adapter
 * and the engine behind it now hold no ports between them, so the whole wiring is
 * two constructor calls with nothing to fake.</p>
 */
class AuditReadAccessAdapterTest {

    private static final UUID MK = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID REGIONAL_TEAM = UUID.fromString("00000000-0000-0000-0000-0000000000a6");
    private static final UUID SANCHALAK = UUID.fromString("00000000-0000-0000-0000-0000000000a5");
    private static final UUID SABHA = UUID.fromString("00000000-0000-0000-0000-0000000000b5");
    private static final UUID CITY = UUID.fromString("00000000-0000-0000-0000-0000000000b3");

    private final AuditReadAccessAdapter adapter = new AuditReadAccessAdapter(new AuditLogAccess());

    @Test
    void admitsAnUnrestrictedOversightBody() {
        assertThat(adapter.canRead(Callers.madhyasthaKaryalaya(MK))).isTrue();
    }

    @Test
    void admitsAScopedRegionalTeamMember() {
        // The Regional Team is not an operational Role, and is admitted anyway —
        // the gate folds on resolved geography, not on a tier list (issue #80).
        assertThat(adapter.canRead(Callers.regionalTeamIn(REGIONAL_TEAM, CITY, "YUVAK"))).isTrue();
    }

    @Test
    void deniesACallerWhoResolvesToNoScope() {
        assertThat(adapter.canRead(Callers.sanchalakOf(SANCHALAK, SABHA))).isFalse();
    }

    @Test
    void deniesACallerHoldingNothingAtAll() {
        assertThat(adapter.canRead(Callers.noRoles(UUID.randomUUID()))).isFalse();
    }
}
