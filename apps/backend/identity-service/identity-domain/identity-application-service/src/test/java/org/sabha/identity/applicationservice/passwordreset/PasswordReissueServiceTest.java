package org.sabha.identity.applicationservice.passwordreset;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.AuthorizationDeniedException;
import org.sabha.common.CallerResolver;
import org.sabha.common.MadhyasthaKaryalayaLookup;
import org.sabha.common.SantLookup;
import org.sabha.identity.applicationservice.IdentityProviderGateway;
import org.sabha.identity.applicationservice.UserRepository;
import org.sabha.identity.domain.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordReissueServiceTest {

    private static final UUID APPOINTER_SUBJECT = UUID.fromString("00000000-0000-0000-0000-0000000000d1");
    private static final UUID APPOINTER_USER = UUID.fromString("00000000-0000-0000-0000-0000000000d2");
    private static final UUID TARGET_USER = UUID.fromString("00000000-0000-0000-0000-0000000000d3");
    private static final UUID TARGET_PERSON = UUID.fromString("00000000-0000-0000-0000-0000000000d4");
    private static final UUID TARGET_KEYCLOAK = UUID.fromString("00000000-0000-0000-0000-0000000000d5");
    private static final UUID MK_SUBJECT = UUID.fromString("00000000-0000-0000-0000-0000000000d6");
    private static final UUID MK_USER = UUID.fromString("00000000-0000-0000-0000-0000000000d7");

    @Test
    void originalAppointerCanReissueWithAForceChangePasswordAndAuditsTheAct() {
        Fixture f = new Fixture();
        f.users.seed(new User(TARGET_USER, TARGET_PERSON, "target.user", TARGET_KEYCLOAK));
        f.authority.recordAppointment(TARGET_USER, APPOINTER_USER);

        f.service().reissue(APPOINTER_SUBJECT, TARGET_USER, "fresh-secret-1");

        assertThat(f.identityProvider.lastKeycloakUserId).isEqualTo(TARGET_KEYCLOAK);
        assertThat(f.identityProvider.lastRawPassword).isEqualTo("fresh-secret-1");
        assertThat(f.identityProvider.lastRequirePasswordChange).isTrue();

        assertThat(f.audit.reissues).hasSize(1);
        InMemoryReissueAuditLog.ReissueAudit audit = f.audit.reissues.get(0);
        assertThat(audit.targetUserId()).isEqualTo(TARGET_USER);
        assertThat(audit.actorUserId()).isEqualTo(APPOINTER_USER);
        assertThat(audit.reissuedAt()).isNotNull();
    }

    @Test
    void madhyasthaKaryalayaMemberCanReissueForASant() {
        Fixture f = new Fixture();
        f.users.seed(new User(TARGET_USER, TARGET_PERSON, "sant.user", TARGET_KEYCLOAK));
        f.sants.markSant(TARGET_USER);
        f.mkMembership.grant(MK_USER);

        f.service().reissue(MK_SUBJECT, TARGET_USER, "fresh-secret-2");

        assertThat(f.identityProvider.lastKeycloakUserId).isEqualTo(TARGET_KEYCLOAK);
        assertThat(f.identityProvider.lastRequirePasswordChange).isTrue();
        assertThat(f.audit.reissues).hasSize(1);
        assertThat(f.audit.reissues.get(0).actorUserId()).isEqualTo(MK_USER);
    }

    @Test
    void aCallerWhoIsNeitherTheAppointerNorMkIsDeniedAndChangesNoPassword() {
        Fixture f = new Fixture();
        f.users.seed(new User(TARGET_USER, TARGET_PERSON, "target.user", TARGET_KEYCLOAK));
        // APPOINTER_SUBJECT did not appoint TARGET_USER, and TARGET_USER is not a Sant.

        assertThatThrownBy(() -> f.service().reissue(APPOINTER_SUBJECT, TARGET_USER, "fresh-secret-3"))
                .isInstanceOf(AuthorizationDeniedException.class);

        assertThat(f.identityProvider.lastRawPassword).isNull();
        assertThat(f.audit.reissues).isEmpty();
    }

    @Test
    void reissuingASantWithoutMkMembershipIsDenied() {
        Fixture f = new Fixture();
        f.users.seed(new User(TARGET_USER, TARGET_PERSON, "sant.user", TARGET_KEYCLOAK));
        f.sants.markSant(TARGET_USER);
        // APPOINTER_SUBJECT (APPOINTER_USER) is not an MK member.

        assertThatThrownBy(() -> f.service().reissue(APPOINTER_SUBJECT, TARGET_USER, "fresh-secret-4"))
                .isInstanceOf(AuthorizationDeniedException.class);

        assertThat(f.identityProvider.lastRawPassword).isNull();
        assertThat(f.audit.reissues).isEmpty();
    }

    // ---- test fixtures -------------------------------------------------------

    static final class Fixture {
        final InMemoryUserRepository users = new InMemoryUserRepository();
        final InMemoryReissueAuthority authority = new InMemoryReissueAuthority();
        final InMemorySantLookup sants = new InMemorySantLookup();
        final InMemoryMkMembership mkMembership = new InMemoryMkMembership();
        final InMemoryReissueAuditLog audit = new InMemoryReissueAuditLog();
        final RecordingIdentityProvider identityProvider = new RecordingIdentityProvider();
        final MutableClock clock = new MutableClock(Instant.parse("2026-06-07T10:00:00Z"));

        CallerResolver caller() {
            return subject -> {
                if (subject.equals(APPOINTER_SUBJECT)) {
                    return Optional.of(APPOINTER_USER);
                }
                if (subject.equals(MK_SUBJECT)) {
                    return Optional.of(MK_USER);
                }
                return Optional.empty();
            };
        }

        PasswordReissueService service() {
            return new PasswordReissueService(
                    caller(), authority, sants, mkMembership, users, identityProvider, audit, clock);
        }
    }

    static final class InMemoryUserRepository implements UserRepository {
        private final Map<UUID, User> byId = new HashMap<>();

        void seed(User user) {
            byId.put(user.id(), user);
        }

        @Override
        public Optional<User> findByKeycloakUserId(UUID keycloakUserId) {
            return byId.values().stream().filter(u -> keycloakUserId.equals(u.keycloakUserId())).findFirst();
        }

        @Override
        public Optional<User> findByPersonId(UUID personId) {
            return byId.values().stream().filter(u -> personId.equals(u.personId())).findFirst();
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return byId.values().stream().filter(u -> username.equals(u.username())).findFirst();
        }

        @Override
        public Optional<User> findById(UUID userId) {
            return Optional.ofNullable(byId.get(userId));
        }

        @Override
        public boolean existsByUsername(String username) {
            return findByUsername(username).isPresent();
        }

        @Override
        public void save(User user) {
            byId.put(user.id(), user);
        }
    }

    static final class InMemoryReissueAuthority implements ReissueAuthorityLookup {
        private final Map<UUID, UUID> appointers = new HashMap<>();

        void recordAppointment(UUID targetUserId, UUID appointerUserId) {
            appointers.put(targetUserId, appointerUserId);
        }

        @Override
        public boolean wasAppointedBy(UUID targetUserId, UUID appointerUserId) {
            return appointerUserId.equals(appointers.get(targetUserId));
        }
    }

    static final class InMemorySantLookup implements SantLookup {
        private final Set<UUID> sants = new HashSet<>();

        void markSant(UUID userId) {
            sants.add(userId);
        }

        @Override
        public boolean isSant(UUID userId) {
            return sants.contains(userId);
        }
    }

    static final class InMemoryMkMembership implements MadhyasthaKaryalayaLookup {
        private final Set<UUID> members = new HashSet<>();

        void grant(UUID userId) {
            members.add(userId);
        }

        @Override
        public boolean isMember(UUID userId) {
            return members.contains(userId);
        }
    }

    static final class InMemoryReissueAuditLog implements PasswordReissueAuditLog {
        final java.util.List<ReissueAudit> reissues = new java.util.ArrayList<>();

        record ReissueAudit(UUID id, UUID targetUserId, UUID actorUserId, Instant reissuedAt) {
        }

        @Override
        public void recordReissue(UUID id, UUID targetUserId, UUID actorUserId, Instant reissuedAt) {
            reissues.add(new ReissueAudit(id, targetUserId, actorUserId, reissuedAt));
        }
    }

    static final class RecordingIdentityProvider implements IdentityProviderGateway {
        UUID lastKeycloakUserId;
        String lastRawPassword;
        Boolean lastRequirePasswordChange;

        @Override
        public UUID createUserRequiringPasswordChange(String username, String rawPassword) {
            return UUID.randomUUID();
        }

        @Override
        public void resetPassword(UUID keycloakUserId, String rawPassword, boolean requirePasswordChange) {
            this.lastKeycloakUserId = keycloakUserId;
            this.lastRawPassword = rawPassword;
            this.lastRequirePasswordChange = requirePasswordChange;
        }

        @Override
        public void disableUser(UUID keycloakUserId) {
            throw new UnsupportedOperationException("not used by password reissue");
        }
    }

    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
