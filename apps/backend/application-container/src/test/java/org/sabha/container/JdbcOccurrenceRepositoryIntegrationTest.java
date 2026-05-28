package org.sabha.container;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sabha.attendance.applicationservice.OccurrenceRepository;
import org.sabha.attendance.domain.Occurrence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@code JdbcOccurrenceRepository.save()} to the dirty-tracking contract
 * on {@link Occurrence}: a save with one pending marking writes exactly that
 * row and leaves untouched rehydrated markings alone (their {@code marked_at}
 * does not advance). Catches the O(n·m) regression where every save re-upserts
 * every marking in the aggregate.
 */
@SpringBootTest
@Import(JdbcOccurrenceRepositoryIntegrationTest.NoAuthConfig.class)
@Testcontainers
class JdbcOccurrenceRepositoryIntegrationTest {

    // Seeded by slice-2/002-seed.sql.
    private static final UUID OCCURRENCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID SABHA_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID MARKED_BY = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID PERSON_A = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID PERSON_B = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID PERSON_C = UUID.fromString("00000000-0000-0000-0000-000000000103");
    private static final Instant SEEDED_MARKED_AT = Instant.parse("2026-05-20T19:00:00Z");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    JdbcClient jdbc;

    @Autowired
    OccurrenceRepository occurrences;

    @BeforeEach
    void seedExistingMarkings() {
        jdbc.sql("DELETE FROM attendance_markings WHERE occurrence_id = ?")
                .param(OCCURRENCE_ID).update();
        insertMarking(PERSON_A, true);
        insertMarking(PERSON_B, false);
    }

    @Test
    void savingAnOccurrenceWithOnePendingMarkingDoesNotRewriteUntouchedRehydratedRows() {
        Occurrence occurrence = occurrences.findById(OCCURRENCE_ID).orElseThrow();
        occurrence.mark(PERSON_C, true, MARKED_BY, Instant.parse("2026-05-28T19:30:00Z"));

        occurrences.save(occurrence);

        Instant markedAtA = readMarkedAt(PERSON_A);
        Instant markedAtB = readMarkedAt(PERSON_B);
        Instant markedAtC = readMarkedAt(PERSON_C);
        assertThat(markedAtA).isEqualTo(SEEDED_MARKED_AT);
        assertThat(markedAtB).isEqualTo(SEEDED_MARKED_AT);
        assertThat(markedAtC).isAfter(SEEDED_MARKED_AT);
    }

    private void insertMarking(UUID personId, boolean present) {
        jdbc.sql("""
                INSERT INTO attendance_markings
                    (id, occurrence_id, person_id, present, marked_at, marked_by_user_id, client_marked_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """)
                .param(UUID.randomUUID())
                .param(OCCURRENCE_ID)
                .param(personId)
                .param(present)
                .param(Timestamp.from(SEEDED_MARKED_AT))
                .param(MARKED_BY)
                .param(Timestamp.from(SEEDED_MARKED_AT))
                .update();
    }

    private Instant readMarkedAt(UUID personId) {
        return jdbc.sql("SELECT marked_at FROM attendance_markings WHERE occurrence_id = ? AND person_id = ?")
                .param(OCCURRENCE_ID)
                .param(personId)
                .query((rs, n) -> rs.getTimestamp("marked_at").toInstant())
                .single();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class NoAuthConfig {

        // No JWT is decoded in this test; the repository is exercised in-process.
        @Bean
        @Primary
        JwtDecoder testJwtDecoder() {
            return token -> { throw new UnsupportedOperationException("JWT decoding not exercised"); };
        }
    }
}
