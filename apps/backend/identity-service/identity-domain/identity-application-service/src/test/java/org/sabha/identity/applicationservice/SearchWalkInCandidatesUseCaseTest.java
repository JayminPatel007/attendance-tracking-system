package org.sabha.identity.applicationservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sabha.identity.domain.Person;

import static org.assertj.core.api.Assertions.assertThat;

class SearchWalkInCandidatesUseCaseTest {

    private static final UUID SABHA = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID KSHETRA = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

    @Test
    void aNameQueryReturnsKshetraScopedCandidatesWithAllTheirHomeSabhas() {
        InMemoryDirectory directory = new InMemoryDirectory();
        directory.mapSabhaToKshetra(SABHA, KSHETRA);
        // A typical Person has more than one Home Sabha (their demographic kind +
        // the universal Sanyukta), so the candidate must carry all of them rather
        // than collapsing to one (CONTEXT.md).
        NameCandidate match = new NameCandidate(
                UUID.randomUUID(), "Ramesh Shah", List.of("REGULAR_BAAL", "REGULAR_SANYUKTA"));
        directory.seedNameCandidates(KSHETRA, List.of(match));
        SearchWalkInCandidatesUseCase useCase = new SearchWalkInCandidatesUseCase(directory);

        List<WalkInCandidate> results = useCase.search(SABHA, "Ramish Shah");

        assertThat(results).singleElement().satisfies(c -> {
            assertThat(c.personId()).isEqualTo(match.personId());
            assertThat(c.fullName()).isEqualTo("Ramesh Shah");
            assertThat(c.homeSabhas()).containsExactly("REGULAR_BAAL", "REGULAR_SANYUKTA");
        });
    }

    @Test
    void aMobileQueryReturnsTheExactPersonWithAllTheirHomeSabhas() {
        InMemoryDirectory directory = new InMemoryDirectory();
        directory.mapSabhaToKshetra(SABHA, KSHETRA);
        WalkInCandidate byMobile = new WalkInCandidate(
                UUID.randomUUID(), "Ramesh Shah", List.of("REGULAR_BAAL", "REGULAR_SANYUKTA"));
        directory.seedMobileMatch("+910000000110", byMobile);
        SearchWalkInCandidatesUseCase useCase = new SearchWalkInCandidatesUseCase(directory);

        List<WalkInCandidate> results = useCase.search(SABHA, "+910000000110");

        assertThat(results).containsExactly(byMobile);
    }

    @Test
    void aMobileQueryWithNoMatchReturnsEmpty() {
        InMemoryDirectory directory = new InMemoryDirectory();
        directory.mapSabhaToKshetra(SABHA, KSHETRA);
        SearchWalkInCandidatesUseCase useCase = new SearchWalkInCandidatesUseCase(directory);

        assertThat(useCase.search(SABHA, "+919999999999")).isEmpty();
    }

    @Test
    void anUnknownSabhaReturnsEmpty() {
        InMemoryDirectory directory = new InMemoryDirectory();
        SearchWalkInCandidatesUseCase useCase = new SearchWalkInCandidatesUseCase(directory);

        assertThat(useCase.search(SABHA, "Ramesh")).isEmpty();
    }

    /** In-memory {@link PersonDirectory} fake driven through the public port. */
    static final class InMemoryDirectory implements PersonDirectory {
        private final Map<UUID, UUID> sabhaToKshetra = new HashMap<>();
        private final Map<String, WalkInCandidate> mobileMatches = new HashMap<>();
        private final List<NameCandidate> nameCandidates = new ArrayList<>();
        private UUID candidateKshetra;

        void mapSabhaToKshetra(UUID sabhaId, UUID kshetraId) {
            sabhaToKshetra.put(sabhaId, kshetraId);
        }

        void seedMobileMatch(String mobile, WalkInCandidate candidate) {
            mobileMatches.put(mobile, candidate);
        }

        void seedNameCandidates(UUID kshetraId, List<NameCandidate> candidates) {
            this.candidateKshetra = kshetraId;
            this.nameCandidates.clear();
            this.nameCandidates.addAll(candidates);
        }

        @Override
        public Optional<Person> findByMobile(String mobile) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Person> findById(UUID personId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<NameCandidate> findNameCandidates(UUID kshetraId, String fullName, int limit) {
            return kshetraId.equals(candidateKshetra) ? nameCandidates.stream().limit(limit).toList() : List.of();
        }

        @Override
        public Optional<WalkInCandidate> findByMobileForWalkIn(String mobile) {
            return Optional.ofNullable(mobileMatches.get(mobile));
        }

        @Override
        public Optional<UUID> kshetraIdOfSabha(UUID sabhaId) {
            return Optional.ofNullable(sabhaToKshetra.get(sabhaId));
        }

        @Override
        public void add(Person person, UUID homeSabhaId) {
            throw new UnsupportedOperationException();
        }
    }
}
