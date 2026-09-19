package org.sabha.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.CallerAuthority;
import org.sabha.common.CallerAuthorityLookup;
import org.sabha.common.CallerResolver;
import org.sabha.common.CallerUnknownException;
import org.sabha.common.RoleAssignment;
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

    private static final UUID KSHETRA = UUID.randomUUID();

    private final CurrentUserArgumentResolver resolver =
            new CurrentUserArgumentResolver(knownSubject(SUBJECT, LOCAL_USER), oneNirdeshakRow());

    @Test
    void resolvesTheSubjectToTheLocalUserId() {
        assertThat(resolver.resolve(authenticated(SUBJECT.toString())).userId())
                .isEqualTo(UserId.of(LOCAL_USER));
    }

    @Test
    void loadsTheCallersAuthorityInTheSamePass() {
        // The point of the widening: one resolution yields both answers, so no
        // engine below has to ask a port for the caller's own rows (ADR-0032).
        CallerAuthority caller = resolver.resolve(authenticated(SUBJECT.toString()));

        assertThat(caller.holdsNirdeshakIn(KSHETRA, "YUVAK")).isTrue();
        assertThat(caller.holdsNirdeshakIn(KSHETRA, "BALAK")).isFalse();
    }

    @Test
    void aCallerHoldingNoRoleResolvesRatherThanFailing() {
        // Identity and authority are separate questions: the first is a 403 when it
        // fails, the second simply comes back empty. Most Karyakars hold no role.
        UUID subject = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        CurrentUserArgumentResolver bare =
                new CurrentUserArgumentResolver(knownSubject(subject, user), userId -> List.of());

        CallerAuthority caller = bare.resolve(authenticated(subject.toString()));

        assertThat(caller.userId()).isEqualTo(UserId.of(user));
        assertThat(caller.operationalRoles()).isEmpty();
        assertThat(caller.isMadhyasthaKaryalaya()).isFalse();
    }

    @Test
    void bothEdgesResolveTheSameWay() {
        // The web BFF (OIDC session) and the mobile resource server differ only in
        // how the Authentication is built; getName() is the subject either way.
        Authentication bff = new TestingAuthenticationToken(SUBJECT.toString(), "session");
        bff.setAuthenticated(true);
        assertThat(resolver.resolve(bff).userId()).isEqualTo(UserId.of(LOCAL_USER));
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
    void onlyBindsCallerAuthorityParametersCarryingTheAnnotation() throws Exception {
        assertThat(supports("annotatedAuthority")).isTrue();
        assertThat(supports("annotatedUuid")).isFalse();
        assertThat(supports("bareAuthority")).isFalse();
        // The relapse ADR-0032's rule R2 also guards: a @CurrentUser UserId no
        // longer resolves, so the one-caller-parameter rule fails fast here as well
        // as in the build.
        assertThat(supports("annotatedUserId")).isFalse();
    }

    private boolean supports(String method) throws Exception {
        return resolver.supportsParameter(
                new org.springframework.core.MethodParameter(
                        Signatures.class.getDeclaredMethod(method, methodParamType(method)), 0));
    }

    private static Class<?> methodParamType(String method) {
        return switch (method) {
            case "annotatedUuid" -> UUID.class;
            case "annotatedUserId" -> UserId.class;
            default -> CallerAuthority.class;
        };
    }

    @SuppressWarnings("unused")
    static class Signatures {
        void annotatedAuthority(@CurrentUser CallerAuthority caller) {
        }

        void annotatedUuid(@CurrentUser UUID caller) {
        }

        void annotatedUserId(@CurrentUser UserId caller) {
        }

        void bareAuthority(CallerAuthority caller) {
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

    /** The loader is a lambda now — one method over one relation (ADR-0032). */
    private static CallerAuthorityLookup oneNirdeshakRow() {
        return userId -> List.of(
                new RoleAssignment("NIRDESHAK", null, KSHETRA, null, null, "YUVAK"));
    }
}
