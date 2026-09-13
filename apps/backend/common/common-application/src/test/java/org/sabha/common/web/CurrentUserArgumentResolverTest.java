package org.sabha.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.CallerResolver;
import org.sabha.common.CallerUnknownException;
import org.sabha.common.UserId;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

/**
 * The edge's whole contract: a caller is identified once, and every way that can
 * fail fails here as a 403 rather than several frames into a use case.
 */
class CurrentUserArgumentResolverTest {

    private static final UUID SUBJECT = UUID.randomUUID();
    private static final UUID LOCAL_USER = UUID.randomUUID();

    private final CurrentUserArgumentResolver resolver =
            new CurrentUserArgumentResolver(knownSubject(SUBJECT, LOCAL_USER));

    @Test
    void resolvesTheSubjectToTheLocalUserId() {
        assertThat(resolver.resolve(authenticated(SUBJECT.toString()))).isEqualTo(UserId.of(LOCAL_USER));
    }

    @Test
    void bothEdgesResolveTheSameWay() {
        // The web BFF (OIDC session) and the mobile resource server differ only in
        // how the Authentication is built; getName() is the subject either way.
        Authentication bff = new TestingAuthenticationToken(SUBJECT.toString(), "session");
        bff.setAuthenticated(true);
        assertThat(resolver.resolve(bff)).isEqualTo(UserId.of(LOCAL_USER));
    }

    @Test
    void unknownSubjectIsForbiddenNotEmpty() {
        UUID stranger = UUID.randomUUID();
        assertThatThrownBy(() -> resolver.resolve(authenticated(stranger.toString())))
                .isInstanceOf(CallerUnknownException.class);
    }

    @Test
    void aSubjectThatIsNotAUuidIsForbiddenRatherThanA500() {
        assertThatThrownBy(() -> resolver.resolve(authenticated("service-account-batch")))
                .isInstanceOf(CallerUnknownException.class)
                .hasMessageContaining("service-account-batch");
    }

    @Test
    void missingAuthenticationIsForbidden() {
        assertThatThrownBy(() -> resolver.resolve(null)).isInstanceOf(CallerUnknownException.class);
    }

    @Test
    void anonymousAuthenticationIsForbidden() {
        Authentication anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        anonymous.setAuthenticated(false);
        assertThatThrownBy(() -> resolver.resolve(anonymous)).isInstanceOf(CallerUnknownException.class);
    }

    @Test
    void onlyBindsUserIdParametersCarryingTheAnnotation() throws Exception {
        assertThat(supports("annotatedUserId")).isTrue();
        assertThat(supports("annotatedUuid")).isFalse();
        assertThat(supports("bareUserId")).isFalse();
    }

    private boolean supports(String method) throws Exception {
        return resolver.supportsParameter(
                new org.springframework.core.MethodParameter(
                        Signatures.class.getDeclaredMethod(method, methodParamType(method)), 0));
    }

    private static Class<?> methodParamType(String method) {
        return method.equals("annotatedUuid") ? UUID.class : UserId.class;
    }

    @SuppressWarnings("unused")
    static class Signatures {
        void annotatedUserId(@CurrentUser UserId caller) {
        }

        void annotatedUuid(@CurrentUser UUID caller) {
        }

        void bareUserId(UserId caller) {
        }
    }

    private static Authentication authenticated(String name) {
        TestingAuthenticationToken token = new TestingAuthenticationToken(name, "credentials", List.of());
        token.setAuthenticated(true);
        return token;
    }

    private static CallerResolver knownSubject(UUID subject, UUID userId) {
        return candidate -> subject.equals(candidate) ? Optional.of(userId) : Optional.empty();
    }
}
