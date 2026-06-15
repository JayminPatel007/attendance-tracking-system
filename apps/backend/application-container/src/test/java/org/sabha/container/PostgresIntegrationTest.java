package org.sabha.container;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared singleton Postgres for the integration suite (issue #72).
 *
 * <p>One container starts per JVM and is reused by every subclass, instead of
 * each test class booting (and tearing down) its own via {@code @Testcontainers}.
 * The container is deliberately never stopped: it lives for the JVM and is
 * reclaimed at shutdown (Ryuk is disabled in the surefire config — see the module
 * {@code pom.xml}).
 *
 * <p>Because every subclass now shares one database, test isolation comes from
 * {@code @Transactional}: each test runs in a transaction that rolls back, so the
 * Liquibase-seeded baseline stays pristine and the suite passes in any order. The
 * few tests that must verify real commit/rollback behaviour opt out per-method.
 */
abstract class PostgresIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            // The Spring TestContext cache keeps many application contexts alive at once
            // (one per distinct test config), each with its own connection pool against this
            // single shared server. Raise the server's ceiling so concurrent cached contexts
            // don't trip "too many clients" (default max_connections is 100).
            .withCommand("postgres", "-c", "max_connections=300");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Keep each cached context's pool small and let idle contexts release connections,
        // so the shared server's connection budget isn't consumed by dormant contexts.
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "4");
        registry.add("spring.datasource.hikari.minimum-idle", () -> "0");
        // A real OTP hash key (issue #77): HmacOtpHasher fails fast on a blank / placeholder /
        // too-short secret, so the suite supplies a proper one instead of the dev tripwire.
        registry.add("sabha.otp.hash-secret", () -> "integration-test-otp-hash-secret");
        // Disable the business cron jobs across the suite. They fire on scheduler threads
        // (outside any test's transaction) — auto-open runs every minute — so on the shared
        // DB they would non-deterministically commit state into other contexts' view. The
        // scanner tests invoke the scanners directly; nothing else needs them. ("-" disables
        // a @Scheduled cron.)
        registry.add("sabha.cron.auto-open", () -> "-");
        registry.add("sabha.cron.auto-finalize", () -> "-");
        registry.add("sabha.cron.weekly-materialization", () -> "-");
        registry.add("sabha.cron.reengagement-projection", () -> "-");
    }
}
