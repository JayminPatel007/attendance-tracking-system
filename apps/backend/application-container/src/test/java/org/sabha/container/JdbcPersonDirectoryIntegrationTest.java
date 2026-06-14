package org.sabha.container;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.identity.applicationservice.AddPersonApplicationService;
import org.sabha.identity.applicationservice.AddPersonCommand;
import org.sabha.identity.applicationservice.AddResult;
import org.sabha.identity.applicationservice.NameCandidate;
import org.sabha.identity.applicationservice.PersonDirectory;
import org.sabha.identity.domain.Gender;
import org.sabha.identity.domain.MobileAlreadyRegisteredException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Drives the Directory dedup port against a real Postgres so the slice-6
 * migration is exercised end-to-end: the system-wide unique mobile constraint,
 * the {@code dmetaphone}/{@code levenshtein} Kshetra-scoped name search, and the
 * add → persist → read path through {@link AddPersonApplicationService}.
 */
@SpringBootTest
@Import(JdbcPersonDirectoryIntegrationTest.NoAuthConfig.class)
@Transactional
class JdbcPersonDirectoryIntegrationTest extends PostgresIntegrationTest {

    // Seeded by slice-2/002-seed.sql and slice-6/001-person-directory.sql.
    private static final UUID KSHETRA_TRACER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SABHA_TRACER = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SEEDED_RAMESH = UUID.fromString("00000000-0000-0000-0000-000000000110");
    private static final String RAMESH_MOBILE = "+910000000110";
    private static final UUID SANCHALAK_KEYCLOAK = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID OTHER_KSHETRA = UUID.fromString("00000000-0000-0000-0000-0000000009ff");

    @Autowired
    JdbcClient jdbc;

    @Autowired
    PersonDirectory directory;

    @Autowired
    AddPersonApplicationService addPerson;

    @Test
    void mobileUniqueConstraintRejectsADuplicateInsert() {
        assertThatThrownBy(() -> jdbc.sql("""
                INSERT INTO persons (id, full_name, gender, mobile)
                VALUES (?, 'Mobile Twin', 'MALE', ?)
                """)
                .param(UUID.randomUUID())
                .param(RAMESH_MOBILE)
                .update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findsACloseNameInTheSameKshetra() {
        List<NameCandidate> candidates = directory.findNameCandidates(KSHETRA_TRACER, "Ramish Shah", 5);

        assertThat(candidates).extracting(NameCandidate::personId).contains(SEEDED_RAMESH);
    }

    @Test
    void excludesCloseNamesFromOtherKshetras() {
        assertThat(directory.findNameCandidates(OTHER_KSHETRA, "Ramish Shah", 5)).isEmpty();
    }

    @Test
    void addsAFreshPersonThatIsThenRetrievable() {
        AddResult result = addPerson.add(SANCHALAK_KEYCLOAK, new AddPersonCommand(
                "Brand New Karyakar", Gender.MALE, null, "+919999000111", null, SABHA_TRACER, false));

        assertThat(result.created()).isTrue();
        assertThat(directory.findById(result.personId()))
                .get()
                .extracting(p -> p.mobile())
                .isEqualTo("+919999000111");
    }

    @Test
    void hardBlocksAnAddWhoseMobileAlreadyExists() {
        assertThatThrownBy(() -> addPerson.add(SANCHALAK_KEYCLOAK, new AddPersonCommand(
                "Ramesh Duplicate", Gender.MALE, null, RAMESH_MOBILE, null, SABHA_TRACER, false)))
                .isInstanceOf(MobileAlreadyRegisteredException.class);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class NoAuthConfig {

        // No JWT is decoded in this test; the services are exercised in-process.
        @Bean
        @Primary
        JwtDecoder testJwtDecoder() {
            return token -> {
                throw new UnsupportedOperationException("JWT decoding not exercised");
            };
        }
    }
}
