package org.sabha.sabha.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SabhaTest {

    private static final UUID KSHETRA = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID KIND = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    private static final UUID CREATOR = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    @Test
    void weeklyRecurringSabhaCarriesItsStandingSlot() {
        Sabha sabha = Sabha.weekly(KSHETRA, KIND, DayOfWeek.SUNDAY,
                LocalTime.of(9, 0), LocalTime.of(10, 30), "Goregaon Mandir", CREATOR);

        assertThat(sabha.id()).isNotNull();
        assertThat(sabha.kshetraId()).isEqualTo(KSHETRA);
        assertThat(sabha.sabhaKindId()).isEqualTo(KIND);
        assertThat(sabha.scheduleShape()).isEqualTo(ScheduleShape.WEEKLY_RECURRING);
        assertThat(sabha.dayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
        assertThat(sabha.startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(sabha.endTime()).isEqualTo(LocalTime.of(10, 30));
        assertThat(sabha.standingVenue()).isEqualTo("Goregaon Mandir");
        assertThat(sabha.createdBy()).isEqualTo(CREATOR);
    }

    @Test
    void monthlyAdHocSabhaCarriesNoStandingSlot() {
        Sabha sabha = Sabha.monthlyAdHoc(KSHETRA, KIND, "Andheri Hall", CREATOR);

        assertThat(sabha.id()).isNotNull();
        assertThat(sabha.scheduleShape()).isEqualTo(ScheduleShape.MONTHLY_AD_HOC);
        assertThat(sabha.dayOfWeek()).isNull();
        assertThat(sabha.startTime()).isNull();
        assertThat(sabha.endTime()).isNull();
        assertThat(sabha.standingVenue()).isEqualTo("Andheri Hall");
        assertThat(sabha.createdBy()).isEqualTo(CREATOR);
    }

    @Test
    void weeklyRejectsAMissingDayOrTime() {
        assertThatThrownBy(() -> Sabha.weekly(KSHETRA, KIND, null,
                LocalTime.of(9, 0), LocalTime.of(10, 30), "Goregaon Mandir", CREATOR))
                .isInstanceOf(WeeklyScheduleRequiredException.class);
    }

    @Test
    void weeklyRejectsAStartNotBeforeEnd() {
        assertThatThrownBy(() -> Sabha.weekly(KSHETRA, KIND, DayOfWeek.SUNDAY,
                LocalTime.of(10, 30), LocalTime.of(10, 30), "Goregaon Mandir", CREATOR))
                .isInstanceOf(WeeklyScheduleRequiredException.class);
    }
}
