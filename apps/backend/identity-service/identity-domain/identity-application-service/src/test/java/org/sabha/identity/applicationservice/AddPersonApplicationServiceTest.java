package org.sabha.identity.applicationservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.common.CallerResolver;
import org.sabha.common.DomainEvent;
import org.sabha.common.DomainEventPublisher;
import org.sabha.identity.domain.Gender;
import org.sabha.identity.domain.GuardianOrMobileRequiredException;
import org.sabha.identity.domain.MobileAlreadyRegisteredException;
import org.sabha.identity.domain.Person;
import org.sabha.identity.domain.PersonAddedOverDuplicateWarning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AddPersonApplicationServiceTest {

    private static final UUID KEYCLOAK_SUBJECT = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID ADDER_USER = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    private static final UUID HOME_SABHA = UUID.fromString("00000000-0000-0000-0000-0000000000a3");
    private static final UUID KSHETRA = UUID.fromString("00000000-0000-0000-0000-0000000000a4");
    private static final UUID OTHER_KSHETRA = UUID.fromString("00000000-0000-0000-0000-0000000000a5");

    @Test
    void addsAPersonWithAFreshMobile() {
        InMemoryDirectory directory = directoryAt(KSHETRA);
        RecordingPublisher publisher = new RecordingPublisher();
        AddPersonApplicationService service = service(directory, publisher);

        AddResult result = service.add(KEYCLOAK_SUBJECT, addCommand("Ravi Patel", "+919820111000"));

        assertThat(result.created()).isTrue();
        assertThat(directory.findById(result.personId())).isPresent();
        assertThat(publisher.events).isEmpty();
    }

    @Test
    void hardBlocksWhenMobileAlreadyRegistered() {
        InMemoryDirectory directory = directoryAt(KSHETRA);
        Person existing = Person.create(UUID.randomUUID(), "Ravi Patel", Gender.MALE, null, "+919820111000", null);
        directory.seed(existing);
        AddPersonApplicationService service = service(directory, new RecordingPublisher());

        assertThatThrownBy(() -> service.add(KEYCLOAK_SUBJECT, addCommand("Ravi P", "+919820111000")))
                .isInstanceOf(MobileAlreadyRegisteredException.class)
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(MobileAlreadyRegisteredException.class))
                .extracting(ex -> ex.existing().id())
                .isEqualTo(existing.id());
    }

    @Test
    void softWarnsOnCloseNameInSameKshetra() {
        InMemoryDirectory directory = directoryAt(KSHETRA);
        NameCandidate candidate = new NameCandidate(UUID.randomUUID(), "Ravi Patel", "Yuvak Sabha");
        directory.seedCandidates(KSHETRA, List.of(candidate));
        AddPersonApplicationService service = service(directory, new RecordingPublisher());

        AddResult result = service.add(KEYCLOAK_SUBJECT, addCommand("Ravee Patel", "+919820111999"));

        assertThat(result.softWarned()).isTrue();
        assertThat(result.candidates()).containsExactly(candidate);
        assertThat(directory.findByMobile("+919820111999")).isEmpty();
    }

    @Test
    void doesNotWarnWhenCloseNameIsInADifferentKshetra() {
        InMemoryDirectory directory = directoryAt(KSHETRA);
        directory.seedCandidates(OTHER_KSHETRA,
                List.of(new NameCandidate(UUID.randomUUID(), "Ravi Patel", "Yuvak Sabha")));
        AddPersonApplicationService service = service(directory, new RecordingPublisher());

        AddResult result = service.add(KEYCLOAK_SUBJECT, addCommand("Ravee Patel", "+919820112000"));

        assertThat(result.created()).isTrue();
    }

    @Test
    void overridePastCandidatesCreatesAndEmitsAuditEvent() {
        InMemoryDirectory directory = directoryAt(KSHETRA);
        NameCandidate candidate = new NameCandidate(UUID.randomUUID(), "Ravi Patel", "Yuvak Sabha");
        directory.seedCandidates(KSHETRA, List.of(candidate));
        RecordingPublisher publisher = new RecordingPublisher();
        AddPersonApplicationService service = service(directory, publisher);

        AddResult result = service.add(KEYCLOAK_SUBJECT, new AddPersonCommand(
                "Ravee Patel", Gender.MALE, null, "+919820112001", null, HOME_SABHA, true));

        assertThat(result.created()).isTrue();
        assertThat(publisher.events).hasSize(1);
        PersonAddedOverDuplicateWarning event = (PersonAddedOverDuplicateWarning) publisher.events.get(0);
        assertThat(event.aggregateId()).isEqualTo(result.personId());
        assertThat(event.adderUserId()).isEqualTo(ADDER_USER);
        assertThat(event.candidateIdsShown()).containsExactly(candidate.personId());
    }

    @Test
    void addsAGuardianLinkedChildWithoutOwnMobile() {
        InMemoryDirectory directory = directoryAt(KSHETRA);
        UUID parent = UUID.randomUUID();
        AddPersonApplicationService service = service(directory, new RecordingPublisher());

        AddResult result = service.add(KEYCLOAK_SUBJECT, new AddPersonCommand(
                "Child Patel", Gender.MALE, null, null, parent, HOME_SABHA, false));

        assertThat(result.created()).isTrue();
        Person child = directory.findById(result.personId()).orElseThrow();
        assertThat(child.mobile()).isNull();
        assertThat(child.guardianPersonId()).isEqualTo(parent);
    }

    @Test
    void rejectsAPersonWithNeitherMobileNorGuardian() {
        InMemoryDirectory directory = directoryAt(KSHETRA);
        AddPersonApplicationService service = service(directory, new RecordingPublisher());

        assertThatThrownBy(() -> service.add(KEYCLOAK_SUBJECT, new AddPersonCommand(
                "Nobody", Gender.MALE, null, null, null, HOME_SABHA, false)))
                .isInstanceOf(GuardianOrMobileRequiredException.class);
    }

    private static AddPersonCommand addCommand(String fullName, String mobile) {
        return new AddPersonCommand(fullName, Gender.MALE, null, mobile, null, HOME_SABHA, false);
    }

    private static InMemoryDirectory directoryAt(UUID kshetraId) {
        InMemoryDirectory directory = new InMemoryDirectory();
        directory.mapSabhaToKshetra(HOME_SABHA, kshetraId);
        return directory;
    }

    private AddPersonApplicationService service(PersonDirectory directory, DomainEventPublisher publisher) {
        CallerResolver caller = subject ->
                subject.equals(KEYCLOAK_SUBJECT) ? Optional.of(ADDER_USER) : Optional.empty();
        return new AddPersonApplicationService(caller, directory, publisher, java.time.Clock.systemUTC());
    }

    /** In-memory {@link PersonDirectory} fake driven entirely through the public port. */
    static final class InMemoryDirectory implements PersonDirectory {
        private final Map<UUID, Person> byId = new HashMap<>();
        private final Map<UUID, UUID> sabhaToKshetra = new HashMap<>();
        private final List<NameCandidate> seededCandidates = new ArrayList<>();
        private UUID candidateKshetra;

        void mapSabhaToKshetra(UUID sabhaId, UUID kshetraId) {
            sabhaToKshetra.put(sabhaId, kshetraId);
        }

        void seed(Person person) {
            byId.put(person.id(), person);
        }

        void seedCandidates(UUID kshetraId, List<NameCandidate> candidates) {
            this.candidateKshetra = kshetraId;
            this.seededCandidates.clear();
            this.seededCandidates.addAll(candidates);
        }

        @Override
        public Optional<Person> findByMobile(String mobile) {
            return byId.values().stream().filter(p -> mobile.equals(p.mobile())).findFirst();
        }

        @Override
        public Optional<Person> findById(UUID personId) {
            return Optional.ofNullable(byId.get(personId));
        }

        @Override
        public List<NameCandidate> findNameCandidates(UUID kshetraId, String fullName, int limit) {
            if (kshetraId.equals(candidateKshetra)) {
                return seededCandidates.stream().limit(limit).toList();
            }
            return List.of();
        }

        @Override
        public Optional<UUID> kshetraIdOfSabha(UUID sabhaId) {
            return Optional.ofNullable(sabhaToKshetra.get(sabhaId));
        }

        @Override
        public void add(Person person, UUID homeSabhaId) {
            byId.put(person.id(), person);
        }
    }

    static final class RecordingPublisher implements DomainEventPublisher {
        final List<DomainEvent> events = new ArrayList<>();

        @Override
        public void publishAll(List<? extends DomainEvent> toPublish) {
            events.addAll(toPublish);
        }
    }
}
