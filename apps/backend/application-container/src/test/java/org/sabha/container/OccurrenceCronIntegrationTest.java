package org.sabha.container;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sabha.attendance.applicationservice.AutoFinalizeScanner;
import org.sabha.attendance.applicationservice.AutoOpenScanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Slice 3 integration test: drives the auto-Open and auto-Finalize scanners
 * across a simulated time advance. Substitutes the production {@link Clock}
 * bean with a {@link MutableClock} so the test can step from "before
 * scheduled start" through to "24h after scheduled end" without sleeping.
 */
@SpringBootTest
@Import(OccurrenceCronIntegrationTest.TestClockConfig.class)
@TestPropertySource(properties = {
        "sabha.cron.auto-open=-",
        "sabha.cron.auto-finalize=-"
})
@Testcontainers
class OccurrenceCronIntegrationTest {

    private static final ZoneId KOLKATA = ZoneId.of("Asia/Kolkata");

    // The seeded Sabha at slice-2/002-seed.sql: Sunday 19:00–20:00, sabha_id 002.
    private static final UUID SABHA_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    // 2026-05-24 was a Sunday — matches the seed's day_of_week=0.
    private static final LocalDate OCCURRENCE_DATE = LocalDate.of(2026, 5, 24);
    private static final UUID OCCURRENCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000777");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    JdbcClient jdbc;

    @Autowired
    AutoOpenScanner autoOpenScanner;

    @Autowired
    AutoFinalizeScanner autoFinalizeScanner;

    @Autowired
    MutableClock clock;

    @BeforeEach
    void seedOccurrence() {
        // Remove the seeded "today's occurrence" so it doesn't interfere with the scanners.
        jdbc.sql("DELETE FROM occurrence_state_transitions").update();
        jdbc.sql("DELETE FROM attendance_markings").update();
        jdbc.sql("DELETE FROM occurrences").update();

        jdbc.sql("""
                INSERT INTO occurrences (id, sabha_id, occurrence_date, state)
                VALUES (?, ?, ?, 'SCHEDULED')
                """)
                .param(OCCURRENCE_ID)
                .param(SABHA_ID)
                .param(OCCURRENCE_DATE)
                .update();
    }

    @AfterEach
    void cleanup() {
        jdbc.sql("DELETE FROM occurrence_state_transitions").update();
        jdbc.sql("DELETE FROM occurrences").update();
    }

    @Test
    void cronAdvancesOccurrenceThroughOpenAndThenFinalizedWithAuditTrail() {
        // ---- Step 1: clock just past the scheduled start (19:00 IST). ----
        clock.set(OCCURRENCE_DATE.atTime(19, 30).atZone(KOLKATA).toInstant());

        autoOpenScanner.scan();

        Map<String, Object> openRow = jdbc.sql(
                "SELECT state, version FROM occurrences WHERE id = ?")
                .param(OCCURRENCE_ID)
                .query()
                .singleRow();
        assertThat(openRow.get("state")).isEqualTo("OPEN_FOR_MARKING");

        Long transitionsAfterOpen = jdbc.sql(
                "SELECT count(*) FROM occurrence_state_transitions WHERE occurrence_id = ?")
                .param(OCCURRENCE_ID)
                .query(Long.class)
                .single();
        assertThat(transitionsAfterOpen).isEqualTo(1L);

        Map<String, Object> openTransition = jdbc.sql("""
                SELECT from_state, to_state, action, actor_kind, actor_user_id
                FROM occurrence_state_transitions
                WHERE occurrence_id = ?
                ORDER BY at_timestamp ASC
                LIMIT 1
                """)
                .param(OCCURRENCE_ID)
                .query()
                .singleRow();
        assertThat(openTransition.get("from_state")).isEqualTo("SCHEDULED");
        assertThat(openTransition.get("to_state")).isEqualTo("OPEN_FOR_MARKING");
        assertThat(openTransition.get("action")).isEqualTo("OPEN");
        assertThat(openTransition.get("actor_kind")).isEqualTo("SYSTEM");
        assertThat(openTransition.get("actor_user_id")).isNull();

        // ---- Step 2: clock past end (20:00) + 24h grace = 2026-05-25 20:00. ----
        clock.set(OCCURRENCE_DATE.plusDays(1).atTime(20, 30).atZone(KOLKATA).toInstant());

        autoFinalizeScanner.scan();

        Map<String, Object> finalizedRow = jdbc.sql(
                "SELECT state FROM occurrences WHERE id = ?")
                .param(OCCURRENCE_ID)
                .query()
                .singleRow();
        assertThat(finalizedRow.get("state")).isEqualTo("FINALIZED");

        Long transitionsAfterFinalize = jdbc.sql(
                "SELECT count(*) FROM occurrence_state_transitions WHERE occurrence_id = ?")
                .param(OCCURRENCE_ID)
                .query(Long.class)
                .single();
        assertThat(transitionsAfterFinalize).isEqualTo(2L);
    }

    @Test
    void autoOpenSkipsScheduledOccurrencesBeforeTheirStartTime() {
        clock.set(OCCURRENCE_DATE.atTime(18, 30).atZone(KOLKATA).toInstant());

        autoOpenScanner.scan();

        Map<String, Object> row = jdbc.sql(
                "SELECT state FROM occurrences WHERE id = ?")
                .param(OCCURRENCE_ID)
                .query()
                .singleRow();
        assertThat(row.get("state")).isEqualTo("SCHEDULED");

        Long count = jdbc.sql(
                "SELECT count(*) FROM occurrence_state_transitions WHERE occurrence_id = ?")
                .param(OCCURRENCE_ID)
                .query(Long.class)
                .single();
        assertThat(count).isZero();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestClockConfig {

        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(Instant.parse("2026-05-24T13:30:00Z"), KOLKATA);
        }

        /**
         * Stub JwtDecoder so the test doesn't need a Keycloak container — no
         * authenticated HTTP request is made in this test; the scanners run
         * in-process against the Spring context's beans directly.
         */
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

        // Compiler still treats Clock as having millis(); inherited default delegates to instant().
        @SuppressWarnings("unused")
        private static ZoneOffset never() { return ZoneOffset.UTC; }
    }
}
