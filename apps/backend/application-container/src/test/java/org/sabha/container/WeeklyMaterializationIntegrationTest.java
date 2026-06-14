package org.sabha.container;

import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sabha.attendance.applicationservice.WeeklyMaterializationScanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Slice 12 integration test: drives the weekly materialization scanner against
 * real Postgres, proving the JDBC insert path, the weekly-Sabha catalog, and
 * idempotency on re-run. The seeded Sabha (slice-2/002-seed.sql) is a Sunday
 * 19:00–20:00 weekly Sabha. A {@link MutableClock} fixes "now" without sleeping.
 */
@SpringBootTest
@Import(WeeklyMaterializationIntegrationTest.TestClockConfig.class)
@Transactional
class WeeklyMaterializationIntegrationTest extends PostgresIntegrationTest {

    private static final ZoneId KOLKATA = ZoneId.of("Asia/Kolkata");
    private static final UUID SEEDED_WEEKLY_SABHA = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Autowired
    JdbcClient jdbc;

    @Autowired
    WeeklyMaterializationScanner scanner;

    @Autowired
    MutableClock clock;

    @BeforeEach
    void clearOccurrences() {
        jdbc.sql("DELETE FROM occurrence_state_transitions").update();
        jdbc.sql("DELETE FROM attendance_markings").update();
        jdbc.sql("DELETE FROM occurrences").update();
        // Wed 2026-06-03 12:00 IST — the next Sunday slot (06-07) is ~4 days out.
        clock.set(LocalDate.of(2026, 6, 3).atTime(12, 0).atZone(KOLKATA).toInstant());
    }

    @Test
    void materializesTheEightWeekWindowForTheWeeklySabhaAndIsIdempotent() {
        scanner.scan();

        List<LocalDate> dates = sundayDates();
        assertThat(dates).hasSize(8);
        assertThat(dates.get(0)).isEqualTo(LocalDate.of(2026, 6, 7));
        assertThat(dates).allSatisfy(d -> assertThat(d.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY));
        assertThat(scheduledCount()).isEqualTo(8L);

        scanner.scan(); // second run must not duplicate
        assertThat(sundayDates()).hasSize(8);
    }

    private List<LocalDate> sundayDates() {
        return jdbc.sql("""
                SELECT occurrence_date FROM occurrences
                WHERE sabha_id = ? ORDER BY occurrence_date
                """)
                .param(SEEDED_WEEKLY_SABHA)
                .query((rs, n) -> rs.getObject("occurrence_date", LocalDate.class))
                .list();
    }

    private Long scheduledCount() {
        return jdbc.sql("SELECT count(*) FROM occurrences WHERE sabha_id = ? AND state = 'SCHEDULED'")
                .param(SEEDED_WEEKLY_SABHA)
                .query(Long.class)
                .single();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestClockConfig {

        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(Instant.parse("2026-06-03T06:30:00Z"), KOLKATA);
        }

        @Bean
        @Primary
        JwtDecoder testJwtDecoder() {
            return token -> { throw new UnsupportedOperationException("JWT decoding not exercised in this test"); };
        }
    }

    static final class MutableClock extends Clock {

        private final AtomicReference<Instant> now;
        private final ZoneId zone;

        MutableClock(Instant initial, ZoneId zone) {
            this.now = new AtomicReference<>(initial);
            this.zone = zone;
        }

        void set(Instant instant) {
            this.now.set(instant);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId newZone) {
            return new MutableClock(now.get(), newZone);
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    }
}
