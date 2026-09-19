package org.sabha.analytics.applicationservice;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.UserId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit behaviours of the dashboard Authorization Engine (Slice 17): a Sant reads
 * any City in the State (the universal-read exception that Slice 15 denied other
 * roles), filtered to their chosen City; every other role keeps the role-scoped
 * view.
 *
 * <p>The City pick and the chip moved out with ADR-0032, and so did their tests
 * ({@link SantCityPreferenceServiceTest}, {@link CityChipQueryTest}). What is
 * left here is the whole of the engine's surface — one question, one decision.
 */
class DashboardAccessTest {

    private static final UUID SANT = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID NIRDESHAK = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    private static final UUID CITY_A = UUID.fromString("00000000-0000-0000-0000-0000000000c1");

    private final FakeSantLookup sants = new FakeSantLookup();
    private final FakeSantDefaultCity defaults = new FakeSantDefaultCity();
    private final DashboardAccess access = new DashboardAccess(sants, defaults);

    @Test
    void aNonSantCallerKeepsTheRoleScopedView() {
        assertThat(access.viewFor(UserId.of(NIRDESHAK))).isEqualTo(new DashboardScope.RoleScoped(NIRDESHAK));
    }

    @Test
    void aSantLandsOnTheirChosenCity() {
        sants.add(SANT);
        defaults.choose(SANT, CITY_A);

        assertThat(access.viewFor(UserId.of(SANT))).isEqualTo(new DashboardScope.CityScoped(CITY_A));
    }

    @Test
    void aSantWhoHasNotChosenAcityYetSeesNothing() {
        sants.add(SANT);

        assertThat(access.viewFor(UserId.of(SANT))).isEqualTo(new DashboardScope.NoCity());
    }
}
