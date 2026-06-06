package org.sabha.container;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.analytics.applicationservice.HomeSabhaOccurrenceHistory;
import org.sabha.analytics.domain.HomeSabhaHistory;
import org.sabha.analytics.domain.Scope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sabha.analytics.domain.OutcomeKind.ABSENT;
import static org.sabha.analytics.domain.OutcomeKind.CANCELLED;
import static org.sabha.analytics.domain.OutcomeKind.PRESENT;
import static org.sabha.analytics.domain.OutcomeKind.WALK_IN_ELSEWHERE;

/**
 * Pins {@code JdbcHomeSabhaOccurrenceHistory} to the contract the Re-engagement
 * Calculator (ADR-0010) depends on: per (Person, Home Sabha) it produces the
 * chronological outcome stream, classifying concluded Occurrences as
 * PRESENT/ABSENT/CANCELLED, interleaving the Person's Walk-ins elsewhere, skipping
 * not-yet-concluded Occurrences, and ignoring Occurrences from before the Person's
 * membership began.
 */
@SpringBootTest
@Import(JdbcHomeSabhaOccurrenceHistoryIntegrationTest.NoAuthConfig.class)
@Testcontainers
@Transactional
class JdbcHomeSabhaOccurrenceHistoryIntegrationTest {

    private static final UUID KSHETRA = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MARKED_BY = UUID.fromString("00000000-0000-0000-0000-000000000004");

    private static final UUID HOME_SABHA = UUID.fromString("00000000-0000-0000-0000-0000000f0001");
    private static final UUID OTHER_SABHA = UUID.fromString("00000000-0000-0000-0000-0000000f0002");
    private static final UUID RAVI = UUID.fromString("00000000-0000-0000-0000-0000000f0101");
    private static final UUID NEWCOMER = UUID.fromString("00000000-0000-0000-0000-0000000f0102");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    JdbcClient jdbc;

    @Autowired
    HomeSabhaOccurrenceHistory history;

    @Test
    void buildsAChronologicalTimelineInterleavingHomeOutcomesAndWalkInsElsewhere() {
        sabha(HOME_SABHA);
        sabha(OTHER_SABHA);
        person(RAVI, "+910000000901");
        homeSabha(RAVI, HOME_SABHA, LocalDate.of(2026, 1, 1));

        // Home Sabha timeline: present, then absent, then a cancelled Occurrence,
        // then absent (never marked), then a still-open Occurrence that has not concluded.
        occurrence(HOME_SABHA, LocalDate.of(2026, 1, 7), "FINALIZED");
        rosterMark(HOME_SABHA, LocalDate.of(2026, 1, 7), RAVI, true);
        occurrence(HOME_SABHA, LocalDate.of(2026, 1, 14), "FINALIZED");
        rosterMark(HOME_SABHA, LocalDate.of(2026, 1, 14), RAVI, false);
        occurrence(HOME_SABHA, LocalDate.of(2026, 1, 28), "CANCELLED");
        occurrence(HOME_SABHA, LocalDate.of(2026, 2, 4), "FINALIZED"); // Ravi never marked -> ABSENT
        occurrence(HOME_SABHA, LocalDate.of(2026, 2, 11), "OPEN_FOR_MARKING"); // not concluded -> no event

        // A Walk-in elsewhere, dated between the two January Occurrences.
        occurrence(OTHER_SABHA, LocalDate.of(2026, 1, 20), "FINALIZED");
        walkInMark(OTHER_SABHA, LocalDate.of(2026, 1, 20), RAVI);

        assertThat(streamFor(RAVI).outcomes())
                .containsExactly(PRESENT, ABSENT, WALK_IN_ELSEWHERE, CANCELLED, ABSENT);
    }

    @Test
    void ignoresOccurrencesFromBeforeThePersonJoinedTheHomeSabha() {
        sabha(HOME_SABHA);
        person(NEWCOMER, "+910000000902");
        // Two concluded Occurrences exist, but the Person only joined before the second.
        occurrence(HOME_SABHA, LocalDate.of(2026, 1, 7), "FINALIZED");
        occurrence(HOME_SABHA, LocalDate.of(2026, 2, 4), "FINALIZED");
        homeSabha(NEWCOMER, HOME_SABHA, LocalDate.of(2026, 1, 20));

        assertThat(streamFor(NEWCOMER).outcomes()).containsExactly(ABSENT);
    }

    private HomeSabhaHistory streamFor(UUID personId) {
        return history.within(new Scope.OfSabhas(Set.of(HOME_SABHA))).stream()
                .filter(s -> s.personId().equals(personId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no stream for person " + personId));
    }

    private void sabha(UUID id) {
        jdbc.sql("""
                INSERT INTO sabhas (id, kshetra_id, sabha_kind, schedule_shape, standing_venue)
                VALUES (?, ?, 'REGULAR_YUVAK', 'WEEKLY_RECURRING', 'Test Venue')
                """).params(id, KSHETRA).update();
    }

    private void person(UUID id, String mobile) {
        jdbc.sql("INSERT INTO persons (id, full_name, gender, mobile) VALUES (?, 'Test Person', 'MALE', ?)")
                .params(id, mobile).update();
    }

    private void homeSabha(UUID personId, UUID sabhaId, LocalDate assignedAt) {
        jdbc.sql("INSERT INTO home_sabhas (person_id, sabha_id, assigned_at) VALUES (?, ?, ?)")
                .params(personId, sabhaId, assignedAt.atStartOfDay()).update();
    }

    private void occurrence(UUID sabhaId, LocalDate date, String state) {
        jdbc.sql("INSERT INTO occurrences (id, sabha_id, occurrence_date, state) VALUES (?, ?, ?, ?)")
                .params(UUID.randomUUID(), sabhaId, date, state).update();
    }

    private void rosterMark(UUID sabhaId, LocalDate date, UUID personId, boolean present) {
        mark(sabhaId, date, personId, present, "ROSTER");
    }

    private void walkInMark(UUID sabhaId, LocalDate date, UUID personId) {
        mark(sabhaId, date, personId, true, "WALK_IN");
    }

    private void mark(UUID sabhaId, LocalDate date, UUID personId, boolean present, String type) {
        UUID occurrenceId = jdbc.sql("SELECT id FROM occurrences WHERE sabha_id = ? AND occurrence_date = ?")
                .params(sabhaId, date)
                .query(UUID.class)
                .single();
        jdbc.sql("""
                INSERT INTO attendance_markings
                    (id, occurrence_id, person_id, present, marked_by_user_id, marking_type, client_marked_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """).params(UUID.randomUUID(), occurrenceId, personId, present, MARKED_BY, type,
                        date.atStartOfDay()).update();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class NoAuthConfig {
        @Bean
        @Primary
        JwtDecoder testJwtDecoder() {
            return token -> {
                throw new UnsupportedOperationException("JWT decoding not exercised");
            };
        }
    }
}
