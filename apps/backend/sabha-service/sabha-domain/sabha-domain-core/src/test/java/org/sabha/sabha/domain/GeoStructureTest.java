package org.sabha.sabha.domain;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeoStructureTest {

    private static final UUID CREATOR = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final UUID CITY_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID ZONE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

    @Test
    void cityCarriesItsNameAndCreator() {
        City city = City.create("Surat", CREATOR);

        assertThat(city.id()).isNotNull();
        assertThat(city.name()).isEqualTo("Surat");
        assertThat(city.createdBy()).isEqualTo(CREATOR);
    }

    @Test
    void cityRejectsABlankName() {
        assertThatThrownBy(() -> City.create("  ", CREATOR))
                .isInstanceOf(StructuralNameRequiredException.class);
    }

    @Test
    void zoneBelongsToItsCityAndCarriesCreator() {
        Zone zone = Zone.create(CITY_ID, "Mumbai South", CREATOR);

        assertThat(zone.id()).isNotNull();
        assertThat(zone.cityId()).isEqualTo(CITY_ID);
        assertThat(zone.name()).isEqualTo("Mumbai South");
        assertThat(zone.createdBy()).isEqualTo(CREATOR);
    }

    @Test
    void zoneRejectsABlankName() {
        assertThatThrownBy(() -> Zone.create(CITY_ID, "", CREATOR))
                .isInstanceOf(StructuralNameRequiredException.class);
    }

    @Test
    void kshetraBelongsToItsZoneAndCarriesCreator() {
        Kshetra kshetra = Kshetra.create(ZONE_ID, "Goregaon-2", CREATOR);

        assertThat(kshetra.id()).isNotNull();
        assertThat(kshetra.zoneId()).isEqualTo(ZONE_ID);
        assertThat(kshetra.name()).isEqualTo("Goregaon-2");
        assertThat(kshetra.createdBy()).isEqualTo(CREATOR);
    }

    @Test
    void kshetraRejectsABlankName() {
        assertThatThrownBy(() -> Kshetra.create(ZONE_ID, "   ", CREATOR))
                .isInstanceOf(StructuralNameRequiredException.class);
    }
}
