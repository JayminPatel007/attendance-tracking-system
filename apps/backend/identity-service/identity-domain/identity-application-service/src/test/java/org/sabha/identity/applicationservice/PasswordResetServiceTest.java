package org.sabha.identity.applicationservice;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.DomainEvent;
import org.sabha.common.DomainEventPublisher;
import org.sabha.identity.domain.PasswordReset;
import org.sabha.identity.domain.PasswordResetOtpSent;
import org.sabha.identity.domain.PasswordResetRequested;
import org.sabha.identity.domain.PasswordResetStatus;
import org.sabha.identity.domain.OtpAttemptsExhaustedException;
import org.sabha.identity.domain.OtpExpiredException;
import org.sabha.identity.domain.PasswordResetCompleted;
import org.sabha.identity.domain.PasswordResetVerified;
import org.sabha.identity.domain.ResetAlreadyCompletedException;
import org.sabha.identity.domain.ResetTokenExpiredException;
import org.sabha.identity.domain.User;
import org.sabha.identity.domain.WrongOtpException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordResetServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID PERSON_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c2");
    private static final UUID KEYCLOAK_USER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c3");
    private static final String USERNAME = "ravi.patel";
    private static final String MOBILE = "+919820100200";
    private static final String FIXED_OTP = "654321";
    private static final String FIXED_RESET_TOKEN = "reset-token-abc";

    @Test
    void requestSendsAnOtpToTheUsersRegisteredMobileAndOpensAPendingReset() {
        Fixture f = new Fixture();
        f.users.seed(new User(USER_ID, PERSON_ID, USERNAME, KEYCLOAK_USER_ID));
        f.contacts.seedMobile(PERSON_ID, MOBILE);

        UUID resetId = f.service().request(USERNAME);

        PasswordReset saved = f.resets.findById(resetId).orElseThrow();
        assertThat(saved.status()).isEqualTo(PasswordResetStatus.PENDING);
        assertThat(saved.userId()).isEqualTo(USER_ID);
        assertThat(saved.mobile()).isEqualTo(MOBILE);

        assertThat(f.gateway.sentTo).isEqualTo(MOBILE);
        assertThat(f.gateway.sentCode).isEqualTo(FIXED_OTP);

        assertThat(f.publisher.events).hasSize(2);
        assertThat(f.publisher.events.get(0)).isInstanceOf(PasswordResetRequested.class);
        PasswordResetRequested requested = (PasswordResetRequested) f.publisher.events.get(0);
        assertThat(requested.aggregateId()).isEqualTo(resetId);
        assertThat(requested.userId()).isEqualTo(USER_ID);
        assertThat(f.publisher.events.get(1)).isInstanceOf(PasswordResetOtpSent.class);
    }

    @Test
    void requestForAnUnknownUsernameIsRejectedAndSendsNoOtp() {
        Fixture f = new Fixture();

        assertThatThrownBy(() -> f.service().request("nobody"))
                .isInstanceOf(UnknownUsernameException.class);

        assertThat(f.gateway.sentCode).isNull();
        assertThat(f.publisher.events).isEmpty();
    }

    @Test
    void requestForAUserWithoutARegisteredMobileIsRejectedAndSendsNoOtp() {
        Fixture f = new Fixture();
        f.users.seed(new User(USER_ID, PERSON_ID, USERNAME, KEYCLOAK_USER_ID));
        // No mobile seeded for PERSON_ID.

        assertThatThrownBy(() -> f.service().request(USERNAME))
                .isInstanceOf(NoRegisteredMobileException.class);

        assertThat(f.gateway.sentCode).isNull();
        assertThat(f.publisher.events).isEmpty();
    }

    @Test
    void requestConsultsTheSendPolicyWithThisFlowsSendLogBeforeSending() {
        Fixture f = new Fixture();
        f.users.seed(new User(USER_ID, PERSON_ID, USERNAME, KEYCLOAK_USER_ID));
        f.contacts.seedMobile(PERSON_ID, MOBILE);
        RecordingOtpSendPolicy policy = new RecordingOtpSendPolicy();
        f.otpSendPolicy = policy;

        f.service().request(USERNAME);

        assertThat(policy.mobile).isEqualTo(MOBILE);
        assertThat(policy.log).isSameAs(f.resets);
        assertThat(policy.now).isEqualTo(f.clock.instant());
    }

    @Test
    void requestSendsNoOtpWhenTheSendPolicyDeniesTheSend() {
        Fixture f = new Fixture();
        f.users.seed(new User(USER_ID, PERSON_ID, USERNAME, KEYCLOAK_USER_ID));
        f.contacts.seedMobile(PERSON_ID, MOBILE);
        RecordingOtpSendPolicy policy = new RecordingOtpSendPolicy();
        policy.toThrow = new OtpRateLimitExceededException(MOBILE);
        f.otpSendPolicy = policy;

        assertThatThrownBy(() -> f.service().request(USERNAME))
                .isInstanceOf(OtpRateLimitExceededException.class);
        assertThat(f.gateway.sentCode).isNull();
    }

    @Test
    void verifyWithCorrectOtpMarksTheResetVerifiedAndIssuesAResetToken() {
        Fixture f = new Fixture();
        f.users.seed(new User(USER_ID, PERSON_ID, USERNAME, KEYCLOAK_USER_ID));
        f.contacts.seedMobile(PERSON_ID, MOBILE);
        PasswordResetService service = f.service();
        UUID resetId = service.request(USERNAME);
        f.publisher.events.clear();

        String resetToken = service.verify(resetId, FIXED_OTP);

        assertThat(resetToken).isEqualTo(FIXED_RESET_TOKEN);
        PasswordReset saved = f.resets.findById(resetId).orElseThrow();
        assertThat(saved.status()).isEqualTo(PasswordResetStatus.VERIFIED);

        assertThat(f.publisher.events).hasSize(1);
        assertThat(f.publisher.events.get(0)).isInstanceOf(PasswordResetVerified.class);
        assertThat(f.publisher.events.get(0).aggregateId()).isEqualTo(resetId);
    }

    @Test
    void verifyWithWrongOtpIsRejectedAndDoesNotVerify() {
        Fixture f = new Fixture();
        f.users.seed(new User(USER_ID, PERSON_ID, USERNAME, KEYCLOAK_USER_ID));
        f.contacts.seedMobile(PERSON_ID, MOBILE);
        PasswordResetService service = f.service();
        UUID resetId = service.request(USERNAME);

        assertThatThrownBy(() -> service.verify(resetId, "000000"))
                .isInstanceOf(WrongOtpException.class);

        assertThat(f.resets.findById(resetId).orElseThrow().status())
                .isEqualTo(PasswordResetStatus.PENDING);
    }

    @Test
    void verifyAfterTtlExpiresIsRejected() {
        Fixture f = new Fixture();
        f.users.seed(new User(USER_ID, PERSON_ID, USERNAME, KEYCLOAK_USER_ID));
        f.contacts.seedMobile(PERSON_ID, MOBILE);
        PasswordResetService service = f.service();
        UUID resetId = service.request(USERNAME);

        f.clock.advance(Duration.ofMinutes(6));

        assertThatThrownBy(() -> service.verify(resetId, FIXED_OTP))
                .isInstanceOf(OtpExpiredException.class);
        assertThat(f.resets.findById(resetId).orElseThrow().status())
                .isEqualTo(PasswordResetStatus.EXPIRED);
    }

    @Test
    void verifyLocksAfterFiveWrongAttemptsAndIgnoresLaterCorrectOtp() {
        Fixture f = new Fixture();
        f.users.seed(new User(USER_ID, PERSON_ID, USERNAME, KEYCLOAK_USER_ID));
        f.contacts.seedMobile(PERSON_ID, MOBILE);
        PasswordResetService service = f.service();
        UUID resetId = service.request(USERNAME);

        for (int attempt = 0; attempt < 4; attempt++) {
            assertThatThrownBy(() -> service.verify(resetId, "000000"))
                    .isInstanceOf(WrongOtpException.class);
        }
        assertThatThrownBy(() -> service.verify(resetId, "000000"))
                .isInstanceOf(OtpAttemptsExhaustedException.class);
        assertThat(f.resets.findById(resetId).orElseThrow().status())
                .isEqualTo(PasswordResetStatus.LOCKED);

        assertThatThrownBy(() -> service.verify(resetId, FIXED_OTP))
                .isInstanceOf(OtpAttemptsExhaustedException.class);
        assertThat(f.resets.findById(resetId).orElseThrow().status())
                .isEqualTo(PasswordResetStatus.LOCKED);
    }

    @Test
    void completeWithAValidTokenSetsTheNewPasswordAndCompletesTheReset() {
        Fixture f = new Fixture();
        f.users.seed(new User(USER_ID, PERSON_ID, USERNAME, KEYCLOAK_USER_ID));
        f.contacts.seedMobile(PERSON_ID, MOBILE);
        PasswordResetService service = f.service();
        UUID resetId = service.request(USERNAME);
        String resetToken = service.verify(resetId, FIXED_OTP);
        f.publisher.events.clear();

        service.complete(resetToken, "new-secret-9");

        PasswordReset saved = f.resets.findById(resetId).orElseThrow();
        assertThat(saved.status()).isEqualTo(PasswordResetStatus.COMPLETED);

        assertThat(f.identityProvider.lastKeycloakUserId).isEqualTo(KEYCLOAK_USER_ID);
        assertThat(f.identityProvider.lastRawPassword).isEqualTo("new-secret-9");
        assertThat(f.identityProvider.lastRequirePasswordChange).isFalse();

        assertThat(f.publisher.events).hasSize(1);
        assertThat(f.publisher.events.get(0)).isInstanceOf(PasswordResetCompleted.class);
        assertThat(f.publisher.events.get(0).aggregateId()).isEqualTo(resetId);
    }

    @Test
    void completeWithAnUnknownTokenIsRejectedAndChangesNoPassword() {
        Fixture f = new Fixture();

        assertThatThrownBy(() -> f.service().complete("not-a-real-token", "new-secret-9"))
                .isInstanceOf(InvalidResetTokenException.class);

        assertThat(f.identityProvider.lastRawPassword).isNull();
    }

    @Test
    void completeAfterTheResetTokenExpiresIsRejectedAndChangesNoPassword() {
        Fixture f = new Fixture();
        f.users.seed(new User(USER_ID, PERSON_ID, USERNAME, KEYCLOAK_USER_ID));
        f.contacts.seedMobile(PERSON_ID, MOBILE);
        PasswordResetService service = f.service();
        UUID resetId = service.request(USERNAME);
        String resetToken = service.verify(resetId, FIXED_OTP);

        f.clock.advance(Duration.ofMinutes(6));

        assertThatThrownBy(() -> service.complete(resetToken, "new-secret-9"))
                .isInstanceOf(ResetTokenExpiredException.class);

        assertThat(f.identityProvider.lastRawPassword).isNull();
        assertThat(f.resets.findById(resetId).orElseThrow().status())
                .isEqualTo(PasswordResetStatus.VERIFIED);
    }

    @Test
    void completeTwiceWithTheSameTokenIsRejectedAndChangesThePasswordOnlyOnce() {
        Fixture f = new Fixture();
        f.users.seed(new User(USER_ID, PERSON_ID, USERNAME, KEYCLOAK_USER_ID));
        f.contacts.seedMobile(PERSON_ID, MOBILE);
        PasswordResetService service = f.service();
        UUID resetId = service.request(USERNAME);
        String resetToken = service.verify(resetId, FIXED_OTP);
        service.complete(resetToken, "first-secret");

        assertThatThrownBy(() -> service.complete(resetToken, "attacker-secret"))
                .isInstanceOf(ResetAlreadyCompletedException.class);

        assertThat(f.identityProvider.lastRawPassword).isEqualTo("first-secret");
        assertThat(f.resets.findById(resetId).orElseThrow().status())
                .isEqualTo(PasswordResetStatus.COMPLETED);
    }

    // ---- test fixtures -------------------------------------------------------

    /** Wires the orchestrator against in-memory fakes driven through its ports. */
    static final class Fixture {
        final InMemoryUserRepository users = new InMemoryUserRepository();
        final InMemoryPersonContactLookup contacts = new InMemoryPersonContactLookup();
        final InMemoryPasswordResetRepository resets = new InMemoryPasswordResetRepository();
        final RecordingOtpGateway gateway = new RecordingOtpGateway();
        final RecordingIdentityProvider identityProvider = new RecordingIdentityProvider();
        final RecordingPublisher publisher = new RecordingPublisher();
        final MutableClock clock = new MutableClock(Instant.parse("2026-06-07T10:00:00Z"));

        OtpSendPolicy otpSendPolicy = new OtpSendPolicy();

        PasswordResetService service() {
            return new PasswordResetService(
                    users, contacts, resets, gateway,
                    new FixedOtpCodeGenerator(FIXED_OTP), otpSendPolicy,
                    new FixedResetTokenGenerator(FIXED_RESET_TOKEN),
                    identityProvider, publisher, clock);
        }
    }

    static final class InMemoryUserRepository implements UserRepository {
        private final Map<UUID, User> byId = new HashMap<>();

        void seed(User user) {
            byId.put(user.id(), user);
        }

        @Override
        public Optional<User> findByKeycloakUserId(UUID keycloakUserId) {
            return byId.values().stream()
                    .filter(u -> keycloakUserId.equals(u.keycloakUserId())).findFirst();
        }

        @Override
        public Optional<User> findByPersonId(UUID personId) {
            return byId.values().stream()
                    .filter(u -> personId.equals(u.personId())).findFirst();
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return byId.values().stream()
                    .filter(u -> username.equals(u.username())).findFirst();
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

    static final class InMemoryPersonContactLookup implements PersonContactLookup {
        private final Map<UUID, String> mobiles = new HashMap<>();

        void seedMobile(UUID personId, String mobile) {
            mobiles.put(personId, mobile);
        }

        @Override
        public Optional<String> mobileOf(UUID personId) {
            return Optional.ofNullable(mobiles.get(personId));
        }
    }

    static final class InMemoryPasswordResetRepository implements PasswordResetRepository {
        private final Map<UUID, PasswordReset> byId = new HashMap<>();

        @Override
        public void save(PasswordReset reset) {
            byId.put(reset.id(), reset);
        }

        @Override
        public Optional<PasswordReset> findById(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public Optional<PasswordReset> findByResetToken(String resetToken) {
            return byId.values().stream()
                    .filter(r -> resetToken.equals(r.resetToken()))
                    .findFirst();
        }

        @Override
        public Optional<Instant> lastInitiatedAt(String mobile) {
            return byId.values().stream()
                    .filter(r -> mobile.equals(r.mobile()))
                    .map(PasswordReset::initiatedAt)
                    .max(Instant::compareTo);
        }

        @Override
        public int sendCountSince(String mobile, Instant since) {
            return (int) byId.values().stream()
                    .filter(r -> mobile.equals(r.mobile()))
                    .filter(r -> !r.initiatedAt().isBefore(since))
                    .count();
        }
    }

    static final class RecordingOtpGateway implements OtpGateway {
        String sentTo;
        String sentCode;

        @Override
        public void send(String mobile, String code) {
            this.sentTo = mobile;
            this.sentCode = code;
        }
    }

    /** Captures what the service hands the policy, and optionally vetoes the send. */
    static final class RecordingOtpSendPolicy extends OtpSendPolicy {
        String mobile;
        OtpSendLog log;
        Instant now;
        RuntimeException toThrow;

        @Override
        public void enforce(String mobile, OtpSendLog log, Instant now) {
            this.mobile = mobile;
            this.log = log;
            this.now = now;
            if (toThrow != null) {
                throw toThrow;
            }
        }
    }

    static final class FixedOtpCodeGenerator implements OtpCodeGenerator {
        private final String code;

        FixedOtpCodeGenerator(String code) {
            this.code = code;
        }

        @Override
        public String generate() {
            return code;
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
    }

    static final class FixedResetTokenGenerator implements ResetTokenGenerator {
        private final String token;

        FixedResetTokenGenerator(String token) {
            this.token = token;
        }

        @Override
        public String generate() {
            return token;
        }
    }

    static final class RecordingPublisher implements DomainEventPublisher {
        final List<DomainEvent> events = new ArrayList<>();

        @Override
        public void publishAll(List<? extends DomainEvent> toPublish) {
            events.addAll(toPublish);
        }
    }

    /** A {@link Clock} whose instant can be advanced to cross the OTP TTL / cooldown windows. */
    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration by) {
            this.now = now.plus(by);
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
