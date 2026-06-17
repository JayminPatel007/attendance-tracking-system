package org.sabha.identity.applicationservice.transfer;

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
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.CallerResolver;
import org.sabha.common.DomainEvent;
import org.sabha.common.DomainEventPublisher;
import org.sabha.common.Role;
import org.sabha.common.RoleAssignmentLookup;
import org.sabha.identity.applicationservice.otp.OtpCodeGenerator;
import org.sabha.identity.applicationservice.otp.OtpGateway;
import org.sabha.identity.applicationservice.otp.OtpRateLimitExceededException;
import org.sabha.identity.applicationservice.otp.OtpSendLog;
import org.sabha.identity.applicationservice.otp.OtpSendPolicy;
import org.sabha.identity.domain.Gender;
import org.sabha.identity.domain.HomeSabhaRef;
import org.sabha.identity.domain.HomeSabhaSwapped;
import org.sabha.identity.domain.HomeSabhaTransfer;
import org.sabha.identity.domain.HomeSabhaTransferInitiated;
import org.sabha.identity.domain.NoMatchingHomeSabhaException;
import org.sabha.identity.domain.OtpAttemptsExhaustedException;
import org.sabha.identity.domain.OtpExpiredException;
import org.sabha.identity.domain.OtpHasher;
import org.sabha.identity.domain.Person;
import org.sabha.identity.domain.PersonHasNoMobileException;
import org.sabha.identity.domain.TransferOtpConfirmed;
import org.sabha.identity.domain.TransferOtpSent;
import org.sabha.identity.domain.TransferStatus;
import org.sabha.identity.domain.WrongOtpException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HomeSabhaTransferServiceTest {

    private static final UUID KEYCLOAK_SUBJECT = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID INITIATOR_USER = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final UUID PERSON = UUID.fromString("00000000-0000-0000-0000-0000000000b3");
    private static final UUID DESTINATION_SABHA = UUID.fromString("00000000-0000-0000-0000-0000000000b4");
    private static final UUID OLD_YUVAK_SABHA = UUID.fromString("00000000-0000-0000-0000-0000000000b5");
    private static final UUID SANYUKTA_SABHA = UUID.fromString("00000000-0000-0000-0000-0000000000b6");
    private static final String PERSON_MOBILE = "+919820100200";
    private static final String FIXED_OTP = "123456";
    private static final String YUVAK_KIND = "REGULAR_YUVAK";
    private static final String SANYUKTA_KIND = "REGULAR_SANYUKTA";

    @Test
    void initiateGeneratesAndSendsAnOtpToThePersonsMobile() {
        Fixture f = new Fixture();
        f.directory.seedPerson(person(PERSON, PERSON_MOBILE));

        UUID transferId = f.service().initiate(KEYCLOAK_SUBJECT, PERSON, DESTINATION_SABHA);

        HomeSabhaTransfer saved = f.transfers.findById(transferId).orElseThrow();
        assertThat(saved.status()).isEqualTo(TransferStatus.PENDING);
        assertThat(saved.personId()).isEqualTo(PERSON);
        assertThat(saved.destinationSabhaId()).isEqualTo(DESTINATION_SABHA);
        assertThat(saved.initiatingUserId()).isEqualTo(INITIATOR_USER);

        assertThat(f.gateway.sentTo).isEqualTo(PERSON_MOBILE);
        assertThat(f.gateway.sentCode).isEqualTo(FIXED_OTP);

        assertThat(f.publisher.events).hasSize(2);
        assertThat(f.publisher.events.get(0)).isInstanceOf(HomeSabhaTransferInitiated.class);
        HomeSabhaTransferInitiated initiated = (HomeSabhaTransferInitiated) f.publisher.events.get(0);
        assertThat(initiated.aggregateId()).isEqualTo(transferId);
        assertThat(initiated.personId()).isEqualTo(PERSON);
        assertThat(initiated.destinationSabhaId()).isEqualTo(DESTINATION_SABHA);
        assertThat(initiated.initiatingUserId()).isEqualTo(INITIATOR_USER);
        assertThat(f.publisher.events.get(1)).isInstanceOf(TransferOtpSent.class);
    }

    @Test
    void confirmWithCorrectOtpSwapsTheHomeSabhaForThatDemographic() {
        Fixture f = new Fixture();
        f.directory.seedPerson(person(PERSON, PERSON_MOBILE));
        f.directory.seedSabhaKind(DESTINATION_SABHA, YUVAK_KIND);
        f.directory.seedHomeSabhas(PERSON, List.of(
                new HomeSabhaRef(OLD_YUVAK_SABHA, YUVAK_KIND),
                new HomeSabhaRef(SANYUKTA_SABHA, SANYUKTA_KIND)));
        HomeSabhaTransferService service = f.service();
        UUID transferId = service.initiate(KEYCLOAK_SUBJECT, PERSON, DESTINATION_SABHA);
        f.publisher.events.clear();

        service.confirm(transferId, FIXED_OTP);

        List<UUID> homeSabhaIds = f.directory.homeSabhasOf(PERSON).stream()
                .map(HomeSabhaRef::sabhaId).toList();
        assertThat(homeSabhaIds).containsExactlyInAnyOrder(DESTINATION_SABHA, SANYUKTA_SABHA);
        assertThat(f.transfers.findById(transferId).orElseThrow().status())
                .isEqualTo(TransferStatus.CONFIRMED);

        assertThat(f.publisher.events).hasSize(2);
        assertThat(f.publisher.events.get(0)).isInstanceOf(TransferOtpConfirmed.class);
        HomeSabhaSwapped swapped = (HomeSabhaSwapped) f.publisher.events.get(1);
        assertThat(swapped.aggregateId()).isEqualTo(transferId);
        assertThat(swapped.personId()).isEqualTo(PERSON);
        assertThat(swapped.previousSabhaId()).isEqualTo(OLD_YUVAK_SABHA);
        assertThat(swapped.destinationSabhaId()).isEqualTo(DESTINATION_SABHA);
    }

    @Test
    void confirmWithWrongOtpIsRejectedAndDoesNotSwap() {
        Fixture f = new Fixture();
        f.directory.seedPerson(person(PERSON, PERSON_MOBILE));
        f.directory.seedSabhaKind(DESTINATION_SABHA, YUVAK_KIND);
        f.directory.seedHomeSabhas(PERSON, List.of(new HomeSabhaRef(OLD_YUVAK_SABHA, YUVAK_KIND)));
        HomeSabhaTransferService service = f.service();
        UUID transferId = service.initiate(KEYCLOAK_SUBJECT, PERSON, DESTINATION_SABHA);

        assertThatThrownBy(() -> service.confirm(transferId, "000000"))
                .isInstanceOf(WrongOtpException.class);

        List<UUID> homeSabhaIds = f.directory.homeSabhasOf(PERSON).stream()
                .map(HomeSabhaRef::sabhaId).toList();
        assertThat(homeSabhaIds).containsExactly(OLD_YUVAK_SABHA);
        assertThat(f.transfers.findById(transferId).orElseThrow().status())
                .isEqualTo(TransferStatus.PENDING);
    }

    @Test
    void confirmAfterTtlExpiresIsRejectedAndDoesNotSwap() {
        Fixture f = new Fixture();
        f.directory.seedPerson(person(PERSON, PERSON_MOBILE));
        f.directory.seedSabhaKind(DESTINATION_SABHA, YUVAK_KIND);
        f.directory.seedHomeSabhas(PERSON, List.of(new HomeSabhaRef(OLD_YUVAK_SABHA, YUVAK_KIND)));
        HomeSabhaTransferService service = f.service();
        UUID transferId = service.initiate(KEYCLOAK_SUBJECT, PERSON, DESTINATION_SABHA);

        f.clock.advance(Duration.ofMinutes(6));

        assertThatThrownBy(() -> service.confirm(transferId, FIXED_OTP))
                .isInstanceOf(OtpExpiredException.class);
        assertThat(f.directory.homeSabhasOf(PERSON).stream().map(HomeSabhaRef::sabhaId).toList())
                .containsExactly(OLD_YUVAK_SABHA);
        assertThat(f.transfers.findById(transferId).orElseThrow().status())
                .isEqualTo(TransferStatus.EXPIRED);
    }

    @Test
    void confirmWithNoHomeSabhaOfTheDestinationKindIsRejectedAndDoesNotSwap() {
        Fixture f = new Fixture();
        f.directory.seedPerson(person(PERSON, PERSON_MOBILE));
        f.directory.seedSabhaKind(DESTINATION_SABHA, YUVAK_KIND);
        // The Person holds only a SANYUKTA Home Sabha — nothing of the
        // destination's REGULAR_YUVAK kind to swap. The OTP is valid, so the
        // rejection comes from the swap phase, after the OTP is consumed.
        f.directory.seedHomeSabhas(PERSON, List.of(new HomeSabhaRef(SANYUKTA_SABHA, SANYUKTA_KIND)));
        HomeSabhaTransferService service = f.service();
        UUID transferId = service.initiate(KEYCLOAK_SUBJECT, PERSON, DESTINATION_SABHA);

        assertThatThrownBy(() -> service.confirm(transferId, FIXED_OTP))
                .isInstanceOf(NoMatchingHomeSabhaException.class);

        // A swap-phase failure must not leave a half-applied swap behind.
        assertThat(f.directory.homeSabhasOf(PERSON).stream().map(HomeSabhaRef::sabhaId).toList())
                .containsExactly(SANYUKTA_SABHA);
    }

    @Test
    void initiateForAPersonWithoutAMobileIsRejected() {
        Fixture f = new Fixture();
        Person child = Person.create(PERSON, "Child Patel", Gender.MALE, null, null, UUID.randomUUID());
        f.directory.seedPerson(child);

        assertThatThrownBy(() -> f.service().initiate(KEYCLOAK_SUBJECT, PERSON, DESTINATION_SABHA))
                .isInstanceOf(PersonHasNoMobileException.class);
        assertThat(f.gateway.sentCode).isNull();
    }

    @Test
    void initiateBySomeoneWithoutDestinationAuthorityIsDenied() {
        Fixture f = new Fixture();
        f.directory.seedPerson(person(PERSON, PERSON_MOBILE));
        f.roleAssignments.revoke(INITIATOR_USER, DESTINATION_SABHA);

        assertThatThrownBy(() -> f.service().initiate(KEYCLOAK_SUBJECT, PERSON, DESTINATION_SABHA))
                .isInstanceOf(TransferNotAuthorizedException.class);
        assertThat(f.gateway.sentCode).isNull();
    }

    @Test
    void sahSanchalakOfDestinationMayInitiate() {
        Fixture f = new Fixture();
        f.directory.seedPerson(person(PERSON, PERSON_MOBILE));
        f.roleAssignments.grant(INITIATOR_USER, DESTINATION_SABHA, Role.SAH_SANCHALAK);

        UUID transferId = f.service().initiate(KEYCLOAK_SUBJECT, PERSON, DESTINATION_SABHA);

        assertThat(f.transfers.findById(transferId)).isPresent();
        assertThat(f.gateway.sentCode).isEqualTo(FIXED_OTP);
    }

    @Test
    void initiateConsultsTheSendPolicyWithThisFlowsSendLogBeforeSending() {
        Fixture f = new Fixture();
        f.directory.seedPerson(person(PERSON, PERSON_MOBILE));
        RecordingOtpSendPolicy policy = new RecordingOtpSendPolicy();
        f.otpSendPolicy = policy;

        f.service().initiate(KEYCLOAK_SUBJECT, PERSON, DESTINATION_SABHA);

        assertThat(policy.mobile).isEqualTo(PERSON_MOBILE);
        assertThat(policy.log).isSameAs(f.transfers);
        assertThat(policy.now).isEqualTo(f.clock.instant());
    }

    @Test
    void initiateSendsNoOtpWhenTheSendPolicyDeniesTheSend() {
        Fixture f = new Fixture();
        f.directory.seedPerson(person(PERSON, PERSON_MOBILE));
        RecordingOtpSendPolicy policy = new RecordingOtpSendPolicy();
        policy.toThrow = new OtpRateLimitExceededException(PERSON_MOBILE);
        f.otpSendPolicy = policy;

        assertThatThrownBy(() -> f.service().initiate(KEYCLOAK_SUBJECT, PERSON, DESTINATION_SABHA))
                .isInstanceOf(OtpRateLimitExceededException.class);
        assertThat(f.gateway.sentCode).isNull();
    }

    @Test
    void confirmLocksAfterFiveWrongAttemptsAndIgnoresLaterCorrectOtp() {
        Fixture f = new Fixture();
        f.directory.seedPerson(person(PERSON, PERSON_MOBILE));
        f.directory.seedSabhaKind(DESTINATION_SABHA, YUVAK_KIND);
        f.directory.seedHomeSabhas(PERSON, List.of(new HomeSabhaRef(OLD_YUVAK_SABHA, YUVAK_KIND)));
        HomeSabhaTransferService service = f.service();
        UUID transferId = service.initiate(KEYCLOAK_SUBJECT, PERSON, DESTINATION_SABHA);

        for (int attempt = 0; attempt < 4; attempt++) {
            assertThatThrownBy(() -> service.confirm(transferId, "000000"))
                    .isInstanceOf(WrongOtpException.class);
        }
        assertThatThrownBy(() -> service.confirm(transferId, "000000"))
                .isInstanceOf(OtpAttemptsExhaustedException.class);
        assertThat(f.transfers.findById(transferId).orElseThrow().status())
                .isEqualTo(TransferStatus.LOCKED);

        assertThatThrownBy(() -> service.confirm(transferId, FIXED_OTP))
                .isInstanceOf(OtpAttemptsExhaustedException.class);
        assertThat(f.directory.homeSabhasOf(PERSON).stream().map(HomeSabhaRef::sabhaId).toList())
                .containsExactly(OLD_YUVAK_SABHA);
    }

    // ---- test fixtures -------------------------------------------------------

    private static Person person(UUID id, String mobile) {
        return Person.create(id, "Ravi Patel", Gender.MALE, null, mobile, null);
    }

    /** Wires the orchestrator against in-memory fakes driven through its ports. */
    static final class Fixture {
        final InMemoryHomeSabhaDirectory directory = new InMemoryHomeSabhaDirectory();
        final InMemoryTransferRepository transfers = new InMemoryTransferRepository();
        final RecordingOtpGateway gateway = new RecordingOtpGateway();
        final RecordingPublisher publisher = new RecordingPublisher();
        final InMemoryRoleAssignments roleAssignments = new InMemoryRoleAssignments();
        final MutableClock clock = new MutableClock(Instant.parse("2026-05-31T10:00:00Z"));

        Fixture() {
            // Default: the initiator is the destination Sabha's Sanchalak, so the
            // authority gate passes unless a test overrides it.
            roleAssignments.grant(INITIATOR_USER, DESTINATION_SABHA, Role.SANCHALAK);
        }

        CallerResolver caller() {
            return subject -> subject.equals(KEYCLOAK_SUBJECT)
                    ? Optional.of(INITIATOR_USER) : Optional.empty();
        }

        OtpSendPolicy otpSendPolicy = new OtpSendPolicy();

        HomeSabhaTransferService service() {
            return new HomeSabhaTransferService(
                    caller(), roleAssignments, directory, transfers, gateway,
                    new FixedOtpCodeGenerator(FIXED_OTP), new SaltedTestOtpHasher(),
                    otpSendPolicy, publisher, clock);
        }
    }

    static final class InMemoryRoleAssignments implements RoleAssignmentLookup {
        private final Map<String, Set<Role>> roles = new HashMap<>();

        void grant(UUID userId, UUID sabhaId, Role... granted) {
            roles.put(userId + "|" + sabhaId, Set.of(granted));
        }

        void revoke(UUID userId, UUID sabhaId) {
            roles.remove(userId + "|" + sabhaId);
        }

        @Override
        public Set<Role> rolesForUserOnSabha(UUID userId, UUID sabhaId) {
            return roles.getOrDefault(userId + "|" + sabhaId, Set.of());
        }

        @Override
        public Set<Role> rolesForUserOnKshetra(UUID userId, UUID kshetraId, String demographic) {
            return Set.of();
        }
    }

    static final class InMemoryHomeSabhaDirectory implements HomeSabhaDirectory {
        private final Map<UUID, Person> persons = new HashMap<>();
        private final Map<UUID, List<HomeSabhaRef>> homeSabhas = new HashMap<>();
        private final Map<UUID, String> sabhaKinds = new HashMap<>();

        void seedPerson(Person person) {
            persons.put(person.id(), person);
        }

        void seedHomeSabhas(UUID personId, List<HomeSabhaRef> refs) {
            homeSabhas.put(personId, new ArrayList<>(refs));
        }

        void seedSabhaKind(UUID sabhaId, String kind) {
            sabhaKinds.put(sabhaId, kind);
        }

        @Override
        public Optional<Person> findById(UUID personId) {
            return Optional.ofNullable(persons.get(personId));
        }

        @Override
        public List<HomeSabhaRef> homeSabhasOf(UUID personId) {
            return homeSabhas.getOrDefault(personId, List.of());
        }

        @Override
        public Optional<String> kindOf(UUID sabhaId) {
            return Optional.ofNullable(sabhaKinds.get(sabhaId));
        }

        @Override
        public void replaceHomeSabha(UUID personId, UUID previousSabhaId, UUID destinationSabhaId) {
            List<HomeSabhaRef> refs = homeSabhas.getOrDefault(personId, new ArrayList<>());
            refs.removeIf(ref -> ref.sabhaId().equals(previousSabhaId));
            refs.add(new HomeSabhaRef(destinationSabhaId, kindOf(destinationSabhaId).orElse(null)));
            homeSabhas.put(personId, refs);
        }
    }

    static final class InMemoryTransferRepository implements HomeSabhaTransferRepository {
        private final Map<UUID, HomeSabhaTransfer> byId = new HashMap<>();

        @Override
        public void save(HomeSabhaTransfer transfer) {
            byId.put(transfer.id(), transfer);
        }

        @Override
        public Optional<HomeSabhaTransfer> findById(UUID transferId) {
            return Optional.ofNullable(byId.get(transferId));
        }

        @Override
        public int sendCountSince(String mobile, Instant since) {
            return (int) byId.values().stream()
                    .filter(t -> mobile.equals(t.mobile()))
                    .filter(t -> !t.initiatedAt().isBefore(since))
                    .count();
        }

        @Override
        public Optional<Instant> lastInitiatedAt(String mobile) {
            return byId.values().stream()
                    .filter(t -> mobile.equals(t.mobile()))
                    .map(HomeSabhaTransfer::initiatedAt)
                    .max(Instant::compareTo);
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

    /** Deterministic salted stand-in for the production HMAC hasher. */
    static final class SaltedTestOtpHasher implements OtpHasher {
        @Override
        public String hash(UUID challengeId, String code) {
            return "digest(" + challengeId + ":" + code + ")";
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
