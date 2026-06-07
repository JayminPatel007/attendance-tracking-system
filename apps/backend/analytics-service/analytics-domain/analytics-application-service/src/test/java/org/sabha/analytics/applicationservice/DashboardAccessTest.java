package org.sabha.analytics.applicationservice;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit behaviours of the dashboard Authorization Engine (Slice 17): a Sant reads
 * any City in the State (the universal-read exception that Slice 15 denied other
 * roles), filtered to their chosen City; every other role keeps the
 * role-scoped view. The chosen City is the persisted default.
 */
class DashboardAccessTest {

    private static final UUID SANT = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID NIRDESHAK = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    private static final UUID CITY_A = UUID.fromString("00000000-0000-0000-0000-0000000000c1");

    private final FakeSantDirectory sants = new FakeSantDirectory();
    private final FakeDefaultCity defaults = new FakeDefaultCity();
    private final FakeCityDirectory cities = new FakeCityDirectory();
    private final DashboardAccess access = new DashboardAccess(sants, defaults, cities);

    @Test
    void aNonSantCallerKeepsTheRoleScopedView() {
        assertThat(access.viewFor(NIRDESHAK)).isEqualTo(new DashboardScope.RoleScoped(NIRDESHAK));
    }

    @Test
    void aSantLandsOnTheirChosenCity() {
        sants.add(SANT);
        defaults.choose(SANT, CITY_A);

        assertThat(access.viewFor(SANT)).isEqualTo(new DashboardScope.CityScoped(CITY_A));
    }

    @Test
    void aSantWhoHasNotChosenAcityYetSeesNothing() {
        sants.add(SANT);

        assertThat(access.viewFor(SANT)).isEqualTo(new DashboardScope.NoCity());
    }

    @Test
    void aSantPickingACityScopesToItAndPersistsItAsTheDefault() {
        sants.add(SANT);
        cities.add(CITY_A);

        DashboardScope scope = access.selectCity(SANT, CITY_A);

        assertThat(scope).isEqualTo(new DashboardScope.CityScoped(CITY_A));
        assertThat(defaults.defaultCityOf(SANT)).contains(CITY_A);
    }

    @Test
    void aNonSantCannotPickACityAndNothingIsPersisted() {
        cities.add(CITY_A);

        assertThatThrownBy(() -> access.selectCity(NIRDESHAK, CITY_A))
                .isInstanceOf(NotASantException.class);
        assertThat(defaults.defaultCityOf(NIRDESHAK)).isEmpty();
    }

    @Test
    void pickingAnUnknownCityIsRejectedAndNothingIsPersisted() {
        sants.add(SANT);

        assertThatThrownBy(() -> access.selectCity(SANT, CITY_A))
                .isInstanceOf(CityNotFoundException.class);
        assertThat(defaults.defaultCityOf(SANT)).isEmpty();
    }

    @Test
    void aSantsChipOffersEveryCityAndHighlightsTheirCurrentChoice() {
        sants.add(SANT);
        cities.add(CITY_A);
        defaults.choose(SANT, CITY_A);

        DashboardAccess.CityChip chip = access.cityChip(SANT);

        assertThat(chip.sant()).isTrue();
        assertThat(chip.selectedCityId()).isEqualTo(CITY_A);
        assertThat(chip.cities()).extracting(CityOption::id).containsExactly(CITY_A);
    }

    @Test
    void aNonSantGetsAnInertChipWithNoCities() {
        cities.add(CITY_A);

        DashboardAccess.CityChip chip = access.cityChip(NIRDESHAK);

        assertThat(chip.sant()).isFalse();
        assertThat(chip.selectedCityId()).isNull();
        assertThat(chip.cities()).isEmpty();
    }

    private static final class FakeSantDirectory implements SantDirectory {
        private final Set<UUID> sants = new HashSet<>();

        void add(UUID userId) {
            sants.add(userId);
        }

        @Override
        public boolean isSant(UUID userId) {
            return sants.contains(userId);
        }
    }

    private static final class FakeCityDirectory implements CityDirectory {
        private final Map<UUID, String> cities = new HashMap<>();

        void add(UUID id) {
            cities.put(id, "City " + id);
        }

        @Override
        public boolean exists(UUID cityId) {
            return cities.containsKey(cityId);
        }

        @Override
        public java.util.List<CityOption> allCities() {
            return cities.entrySet().stream()
                    .map(e -> new CityOption(e.getKey(), e.getValue()))
                    .toList();
        }
    }

    private static final class FakeDefaultCity implements SantDefaultCity {
        private final Map<UUID, UUID> defaults = new HashMap<>();

        @Override
        public Optional<UUID> defaultCityOf(UUID userId) {
            return Optional.ofNullable(defaults.get(userId));
        }

        @Override
        public void choose(UUID userId, UUID cityId) {
            defaults.put(userId, cityId);
        }
    }
}
