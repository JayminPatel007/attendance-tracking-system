package org.sabha.container;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Adds a shared singleton Keycloak (with the {@code sabha} realm imported) on top
 * of the singleton Postgres for tests that exercise the OIDC/auth surface (issue
 * #72). Like its parent, the container is JVM-lifetime and reclaimed at shutdown.
 *
 * <p>Registers the keycloak-backed properties every auth test used to repeat.
 * Subclasses needing extra context properties (e.g. an MK bootstrap user) add
 * their own {@code @DynamicPropertySource} method — Spring runs all of them
 * across the hierarchy. The {@link #KEYCLOAK} field is exposed for the few tests
 * that call the token endpoint directly.
 */
abstract class KeycloakIntegrationTest extends PostgresIntegrationTest {

    static final KeycloakContainer KEYCLOAK = new KeycloakContainer()
            .withRealmImportFile("/realm-sabha.json");

    static {
        KEYCLOAK.start();
    }

    @DynamicPropertySource
    static void keycloakProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> KEYCLOAK.getAuthServerUrl() + "/realms/sabha");
        registry.add("sabha.keycloak.issuer-uri", () -> KEYCLOAK.getAuthServerUrl() + "/realms/sabha");
        registry.add("sabha.keycloak.admin-base-url", KEYCLOAK::getAuthServerUrl);
        registry.add("sabha.keycloak.realm", () -> "sabha");
        registry.add("sabha.keycloak.admin-username", KEYCLOAK::getAdminUsername);
        registry.add("sabha.keycloak.admin-password", KEYCLOAK::getAdminPassword);
    }

    /** The single MK identity every MK-dependent test shares — see {@link #registerMkBootstrap}. */
    protected static final String MK_USERNAME = "mk-bootstrap";
    protected static final String MK_FULL_NAME = "Bootstrap MK Member";
    protected static final String MK_MOBILE = "+919820111222";
    protected static final String MK_PASSWORD = "changeme123!";

    /**
     * Seeds the {@code sabha.bootstrap.mk.*} properties so {@code MkBootstrapRunner}
     * installs the MK member at boot. {@code MkBootstrapService} is a no-op once any MK
     * exists (one MK per org), so on the shared Keycloak + Postgres only the first
     * MK-needing context installs it and the rest reuse it. Every MK test therefore
     * shares this one canonical identity — distinct per-class usernames would simply
     * never be created.
     */
    protected static void registerMkBootstrap(DynamicPropertyRegistry registry) {
        registry.add("sabha.bootstrap.mk.username", () -> MK_USERNAME);
        registry.add("sabha.bootstrap.mk.password", () -> MK_PASSWORD);
        registry.add("sabha.bootstrap.mk.full-name", () -> MK_FULL_NAME);
        registry.add("sabha.bootstrap.mk.gender", () -> "MALE");
        registry.add("sabha.bootstrap.mk.mobile", () -> MK_MOBILE);
    }
}
