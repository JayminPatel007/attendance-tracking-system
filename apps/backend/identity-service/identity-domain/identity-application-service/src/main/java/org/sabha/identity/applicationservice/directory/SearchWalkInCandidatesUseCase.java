package org.sabha.identity.applicationservice.directory;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

/**
 * Walk-in Directory search (Slice 7, issue #8) backing the mobile-walk-in flow.
 * Given the Occurrence's Sabha, derives the Kshetra and searches the full
 * Directory by mobile (exact) or name (phonetic / edit-distance, Kshetra-scoped),
 * returning each match with their current Home Sabha. Online-only (ADR-0007) —
 * the offline path searches the cached Roster on the device instead.
 */
@Service
public class SearchWalkInCandidatesUseCase {

    private static final int MAX_RESULTS = 5;

    /** A mobile-shaped query: an optional leading {@code +} then at least 3 digits. */
    private static final Pattern MOBILE = Pattern.compile("\\+?\\d{3,}");

    private final PersonDirectory directory;

    public SearchWalkInCandidatesUseCase(PersonDirectory directory) {
        this.directory = directory;
    }

    public List<WalkInCandidate> search(UUID sabhaId, String query) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }
        return directory.kshetraIdOfSabha(sabhaId)
                .map(kshetraId -> searchWithin(kshetraId, trimmed))
                .orElseGet(List::of);
    }

    private List<WalkInCandidate> searchWithin(UUID kshetraId, String query) {
        if (MOBILE.matcher(query).matches()) {
            return directory.findByMobileForWalkIn(query).map(List::of).orElseGet(List::of);
        }
        return directory.findNameCandidates(kshetraId, query, MAX_RESULTS).stream()
                .map(c -> new WalkInCandidate(c.personId(), c.fullName(), c.homeSabhas()))
                .toList();
    }
}
