package org.sabha.identity.applicationservice.selection;

import java.time.Clock;
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
import org.sabha.common.CallerAuthority;
import org.sabha.common.DomainEvent;
import org.sabha.common.DomainEventPublisher;
import org.sabha.common.Role;
import org.sabha.identity.applicationservice.Callers;
import org.sabha.common.SabhaScope;
import org.sabha.common.SabhaFact;
import org.sabha.common.SabhaFacts;
import org.sabha.identity.domain.NoSelectiveSabhaException;
import org.sabha.identity.domain.NoSelectiveTrackException;
import org.sabha.identity.domain.NominationStatus;
import org.sabha.identity.domain.PersonNotOnRosterException;
import org.sabha.identity.domain.SelectionApproved;
import org.sabha.identity.domain.SelectionNominated;
import org.sabha.identity.domain.SelectionNomination;
import org.sabha.identity.domain.SelectionRejected;
import org.sabha.identity.domain.SelectionRevoked;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SelectionServiceTest {

    private static final UUID SANCHALAK_USER = UUID.fromString("00000000-0000-0000-0000-0000000000c2");
    private static final UUID PERSON = UUID.fromString("00000000-0000-0000-0000-0000000000c3");
    private static final UUID REGULAR_SABHA = UUID.fromString("00000000-0000-0000-0000-0000000000c4");
    private static final UUID SELECTIVE_SABHA = UUID.fromString("00000000-0000-0000-0000-0000000000c5");
    private static final UUID KSHETRA = UUID.fromString("00000000-0000-0000-0000-0000000000c6");
    private static final UUID NIRDESHAK_USER = UUID.fromString("00000000-0000-0000-0000-0000000000c8");
    private static final String DEMOGRAPHIC = "YUVAK";

    /**
     * The two callers this workflow distinguishes, as rows rather than fakes. Both
     * gates read the caller directly now: {@code runsSabha} for nomination — the
     * same single call the home-Sabha transfer makes — and {@code holdsNirdeshakIn}
     * for the decision (ADR-0032).
     */
    private static final CallerAuthority SANCHALAK =
            Callers.of(SANCHALAK_USER).sanchalakOf(REGULAR_SABHA).build();
    private static final CallerAuthority NIRDESHAK =
            Callers.of(NIRDESHAK_USER).nirdeshakOf(KSHETRA, DEMOGRAPHIC).build();

    @Test
    void nominateCreatesPendingNominationForARosterPersonWithTheDerivedSelectiveSabha() {
        Fixture f = new Fixture();

        UUID nominationId = f.service().nominate(SANCHALAK, PERSON, REGULAR_SABHA);

        SelectionNomination saved = f.nominations.findById(nominationId).orElseThrow();
        assertThat(saved.status()).isEqualTo(NominationStatus.PENDING);
        assertThat(saved.personId()).isEqualTo(PERSON);
        assertThat(saved.regularSabhaId()).isEqualTo(REGULAR_SABHA);
        assertThat(saved.selectiveSabhaId()).isEqualTo(SELECTIVE_SABHA);
        assertThat(saved.kshetraId()).isEqualTo(KSHETRA);
        assertThat(saved.demographic()).isEqualTo(DEMOGRAPHIC);
        assertThat(saved.track()).isEqualTo("YSS");
        assertThat(saved.nominatedBy()).isEqualTo(SANCHALAK_USER);

        assertThat(f.publisher.events).hasSize(1);
        assertThat(f.publisher.events.get(0)).isInstanceOf(SelectionNominated.class);
        SelectionNominated event = (SelectionNominated) f.publisher.events.get(0);
        assertThat(event.aggregateId()).isEqualTo(nominationId);
        assertThat(event.personId()).isEqualTo(PERSON);
        assertThat(event.selectiveSabhaId()).isEqualTo(SELECTIVE_SABHA);
        assertThat(event.nominatedBy()).isEqualTo(SANCHALAK_USER);
    }

    @Test
    void nominateBySomeoneWhoIsNotTheRegularSabhasSanchalakIsDenied() {
        Fixture f = new Fixture();
        CallerAuthority notSanchalak = Callers.noRoles(SANCHALAK_USER);

        assertThatThrownBy(() -> f.service().nominate(notSanchalak, PERSON, REGULAR_SABHA))
                .isInstanceOf(NominationNotAuthorizedException.class);
        assertThat(f.publisher.events).isEmpty();
    }

    @Test
    void sahSanchalakOfTheRegularSabhaMayNominate() {
        Fixture f = new Fixture();
        CallerAuthority sahSanchalak =
                Callers.of(SANCHALAK_USER).sahSanchalakOf(REGULAR_SABHA).build();

        UUID nominationId = f.service().nominate(sahSanchalak, PERSON, REGULAR_SABHA);

        assertThat(f.nominations.findById(nominationId)).isPresent();
    }

    @Test
    void nominateAPersonWhoIsNotOnTheRegularSabhasRosterIsRejected() {
        Fixture f = new Fixture();
        f.roster.clear();

        assertThatThrownBy(() -> f.service().nominate(SANCHALAK, PERSON, REGULAR_SABHA))
                .isInstanceOf(PersonNotOnRosterException.class);
        assertThat(f.publisher.events).isEmpty();
    }

    @Test
    void nominateFromADemographicWithoutASelectiveTrackIsRejected() {
        Fixture f = new Fixture();
        f.sabhaFacts.seedScope(REGULAR_SABHA, new SabhaScope(KSHETRA, "SANYUKTA", "REGULAR"));

        assertThatThrownBy(() -> f.service().nominate(SANCHALAK, PERSON, REGULAR_SABHA))
                .isInstanceOf(NoSelectiveTrackException.class);
        assertThat(f.publisher.events).isEmpty();
    }

    @Test
    void nominateWhenNoSelectiveSabhaExistsInTheKshetraIsRejected() {
        Fixture f = new Fixture();
        f.sabhaFacts.clearSelective();

        assertThatThrownBy(() -> f.service().nominate(SANCHALAK, PERSON, REGULAR_SABHA))
                .isInstanceOf(NoSelectiveSabhaException.class);
        assertThat(f.publisher.events).isEmpty();
    }

    @Test
    void aSecondPendingNominationForTheSamePersonAndTrackIsRejected() {
        Fixture f = new Fixture();
        SelectionService service = f.service();
        service.nominate(SANCHALAK, PERSON, REGULAR_SABHA);
        f.publisher.events.clear();

        assertThatThrownBy(() -> service.nominate(SANCHALAK, PERSON, REGULAR_SABHA))
                .isInstanceOf(DuplicateNominationException.class);
        assertThat(f.publisher.events).isEmpty();
    }

    @Test
    void nominateAPersonAlreadyOnTheSelectiveRosterIsRejected() {
        Fixture f = new Fixture();
        f.roster.add(PERSON, SELECTIVE_SABHA);

        assertThatThrownBy(() -> f.service().nominate(SANCHALAK, PERSON, REGULAR_SABHA))
                .isInstanceOf(AlreadySelectedException.class);
        assertThat(f.publisher.events).isEmpty();
    }

    @Test
    void approveAddsTheSelectiveHomeSabhaLeavesTheRegularOneAndRecordsTheDecider() {
        Fixture f = new Fixture();
        SelectionService service = f.service();
        UUID nominationId = service.nominate(SANCHALAK, PERSON, REGULAR_SABHA);
        f.publisher.events.clear();

        service.approve(NIRDESHAK, nominationId);

        assertThat(f.roster.isOnRoster(PERSON, SELECTIVE_SABHA)).isTrue();
        assertThat(f.roster.isOnRoster(PERSON, REGULAR_SABHA)).isTrue();
        SelectionNomination saved = f.nominations.findById(nominationId).orElseThrow();
        assertThat(saved.status()).isEqualTo(NominationStatus.APPROVED);
        assertThat(saved.decidedBy()).isEqualTo(NIRDESHAK_USER);
        assertThat(saved.decidedAt()).isEqualTo(f.clock.instant());

        assertThat(f.publisher.events).hasSize(1);
        assertThat(f.publisher.events.get(0)).isInstanceOf(SelectionApproved.class);
        SelectionApproved event = (SelectionApproved) f.publisher.events.get(0);
        assertThat(event.personId()).isEqualTo(PERSON);
        assertThat(event.selectiveSabhaId()).isEqualTo(SELECTIVE_SABHA);
        assertThat(event.approvedBy()).isEqualTo(NIRDESHAK_USER);
    }

    @Test
    void approveBySomeoneWhoIsNotTheDemographicNirdeshakIsDeniedAndAddsNoHomeSabha() {
        Fixture f = new Fixture();
        SelectionService service = f.service();
        UUID nominationId = service.nominate(SANCHALAK, PERSON, REGULAR_SABHA);
        // The Sanchalak is a valid caller but holds no Nirdeshak authority.
        f.publisher.events.clear();

        assertThatThrownBy(() -> service.approve(SANCHALAK, nominationId))
                .isInstanceOf(SelectionDecisionNotAuthorizedException.class);
        assertThat(f.roster.isOnRoster(PERSON, SELECTIVE_SABHA)).isFalse();
        assertThat(f.nominations.findById(nominationId).orElseThrow().status())
                .isEqualTo(NominationStatus.PENDING);
        assertThat(f.publisher.events).isEmpty();
    }

    @Test
    void rejectRecordsTheReasonAndDeciderAndAddsNoHomeSabha() {
        Fixture f = new Fixture();
        SelectionService service = f.service();
        UUID nominationId = service.nominate(SANCHALAK, PERSON, REGULAR_SABHA);
        f.publisher.events.clear();

        service.reject(NIRDESHAK, nominationId, "Not yet ready");

        SelectionNomination saved = f.nominations.findById(nominationId).orElseThrow();
        assertThat(saved.status()).isEqualTo(NominationStatus.REJECTED);
        assertThat(saved.rejectionReason()).isEqualTo("Not yet ready");
        assertThat(saved.decidedBy()).isEqualTo(NIRDESHAK_USER);
        assertThat(saved.decidedAt()).isEqualTo(f.clock.instant());
        assertThat(f.roster.isOnRoster(PERSON, SELECTIVE_SABHA)).isFalse();

        assertThat(f.publisher.events).hasSize(1);
        assertThat(f.publisher.events.get(0)).isInstanceOf(SelectionRejected.class);
        SelectionRejected event = (SelectionRejected) f.publisher.events.get(0);
        assertThat(event.personId()).isEqualTo(PERSON);
        assertThat(event.rejectedBy()).isEqualTo(NIRDESHAK_USER);
        assertThat(event.reason()).isEqualTo("Not yet ready");
    }

    @Test
    void deselectRemovesOnlyTheSelectiveHomeSabhaAndRecordsTheDecider() {
        Fixture f = new Fixture();
        SelectionService service = f.service();
        UUID nominationId = service.nominate(SANCHALAK, PERSON, REGULAR_SABHA);
        service.approve(NIRDESHAK, nominationId);
        f.publisher.events.clear();

        service.deselect(NIRDESHAK, PERSON, SELECTIVE_SABHA);

        assertThat(f.roster.isOnRoster(PERSON, SELECTIVE_SABHA)).isFalse();
        assertThat(f.roster.isOnRoster(PERSON, REGULAR_SABHA)).isTrue();
        SelectionNomination saved = f.nominations.findById(nominationId).orElseThrow();
        assertThat(saved.status()).isEqualTo(NominationStatus.DESELECTED);
        assertThat(saved.decidedBy()).isEqualTo(NIRDESHAK_USER);

        assertThat(f.publisher.events).hasSize(1);
        assertThat(f.publisher.events.get(0)).isInstanceOf(SelectionRevoked.class);
        SelectionRevoked event = (SelectionRevoked) f.publisher.events.get(0);
        assertThat(event.personId()).isEqualTo(PERSON);
        assertThat(event.selectiveSabhaId()).isEqualTo(SELECTIVE_SABHA);
        assertThat(event.revokedBy()).isEqualTo(NIRDESHAK_USER);
    }

    @Test
    void deselectBySomeoneWhoIsNotTheDemographicNirdeshakIsDenied() {
        Fixture f = new Fixture();
        SelectionService service = f.service();
        UUID nominationId = service.nominate(SANCHALAK, PERSON, REGULAR_SABHA);
        service.approve(NIRDESHAK, nominationId);
        f.publisher.events.clear();

        assertThatThrownBy(() -> service.deselect(SANCHALAK, PERSON, SELECTIVE_SABHA))
                .isInstanceOf(SelectionDecisionNotAuthorizedException.class);
        assertThat(f.roster.isOnRoster(PERSON, SELECTIVE_SABHA)).isTrue();
        assertThat(f.publisher.events).isEmpty();
    }

    // ---- test fixtures -------------------------------------------------------

    /** Wires the orchestrator against in-memory fakes driven through its ports. */
    static final class Fixture {
        final InMemorySelectionRepository nominations = new InMemorySelectionRepository();
        final InMemoryRoster roster = new InMemoryRoster();
        final InMemorySabhaFacts sabhaFacts = new InMemorySabhaFacts();
        final RecordingPublisher publisher = new RecordingPublisher();
        final Clock clock = Clock.fixed(Instant.parse("2026-06-06T10:00:00Z"), ZoneOffset.UTC);

        Fixture() {
            // Default: the Person is on the Regular Sabha's Roster and a YSS selective
            // Sabha exists in the Kshetra, so the happy paths pass unless a test
            // overrides one. Who the caller is no longer lives here — it is the
            // argument, so a test that varies authority varies the argument.
            roster.add(PERSON, REGULAR_SABHA);
            sabhaFacts.seedScope(REGULAR_SABHA, new SabhaScope(KSHETRA, DEMOGRAPHIC, "REGULAR"));
            sabhaFacts.seedScope(SELECTIVE_SABHA, new SabhaScope(KSHETRA, DEMOGRAPHIC, "YSS"));
            sabhaFacts.seedSelective(KSHETRA, DEMOGRAPHIC, "YSS", SELECTIVE_SABHA);
        }

        SelectionService service() {
            return new SelectionService(roster, sabhaFacts, nominations, publisher, clock);
        }
    }


    static final class InMemorySelectionRepository implements SelectionRepository {
        private final Map<UUID, SelectionNomination> byId = new HashMap<>();

        @Override
        public void save(SelectionNomination nomination) {
            byId.put(nomination.id(), nomination);
        }

        @Override
        public Optional<SelectionNomination> findById(UUID nominationId) {
            return Optional.ofNullable(byId.get(nominationId));
        }

        @Override
        public boolean hasPendingFor(UUID personId, String track) {
            return byId.values().stream().anyMatch(n ->
                    n.personId().equals(personId) && n.track().equals(track)
                            && n.status() == NominationStatus.PENDING);
        }

        @Override
        public Optional<SelectionNomination> findApproved(UUID personId, UUID selectiveSabhaId) {
            return byId.values().stream()
                    .filter(n -> n.personId().equals(personId)
                            && n.selectiveSabhaId().equals(selectiveSabhaId)
                            && n.status() == NominationStatus.APPROVED)
                    .findFirst();
        }
    }


    static final class InMemoryRoster implements SelectionRoster {
        private final Map<UUID, List<UUID>> homeSabhas = new HashMap<>();

        void add(UUID personId, UUID sabhaId) {
            homeSabhas.computeIfAbsent(personId, k -> new ArrayList<>()).add(sabhaId);
        }

        void clear() {
            homeSabhas.clear();
        }

        @Override
        public boolean isOnRoster(UUID personId, UUID sabhaId) {
            return homeSabhas.getOrDefault(personId, List.of()).contains(sabhaId);
        }

        @Override
        public void addHomeSabha(UUID personId, UUID sabhaId) {
            List<UUID> sabhas = homeSabhas.computeIfAbsent(personId, k -> new ArrayList<>());
            if (!sabhas.contains(sabhaId)) {
                sabhas.add(sabhaId);
            }
        }

        @Override
        public void removeHomeSabha(UUID personId, UUID sabhaId) {
            homeSabhas.getOrDefault(personId, new ArrayList<>()).remove(sabhaId);
        }
    }

    static final class InMemorySabhaFacts implements SabhaFacts {
        private final Map<UUID, SabhaScope> scopes = new HashMap<>();
        private final Map<String, UUID> selective = new HashMap<>();

        void seedScope(UUID sabhaId, SabhaScope scope) {
            scopes.put(sabhaId, scope);
        }

        void seedSelective(UUID kshetraId, String demographic, String track, UUID sabhaId) {
            selective.put(kshetraId + "|" + demographic + "|" + track, sabhaId);
        }

        void clearSelective() {
            selective.clear();
        }

        @Override
        public Optional<SabhaFact> of(UUID sabhaId) {
            return Optional.ofNullable(scopes.get(sabhaId))
                    .map(scope -> SabhaFact.monthlyAdHoc(sabhaId, scope, false));
        }

        @Override
        public List<SabhaFact> allWeekly() {
            return List.of();
        }

        /** Both windows return the same type, so the selective Sabha is resolved through {@code of}. */
        @Override
        public Optional<SabhaFact> selectiveIn(UUID kshetraId, String demographic, String track) {
            return Optional.ofNullable(selective.get(kshetraId + "|" + demographic + "|" + track))
                    .flatMap(this::of);
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
