package org.sabha.identity.applicationservice;

import java.util.List;
import java.util.UUID;

/**
 * Outcome of a Sabha-definition attempt (ADR-0012): either the Sabha was
 * {@link #created} (with the new Sabha id and the Sanchalak / optional
 * Sah-Sanchalak assignment ids), or — only on an inline new-Person path — the
 * name soft-warn fired during an appointment and the whole act is rolled back
 * for the Nirdeshak to resolve among {@link #softWarn(List) candidates} (Slice 6).
 * An authorization denial is signalled by an exception, not a result.
 */
public record SabhaDefinitionResult(
        UUID sabhaId, UUID sanchalakAssignmentId, UUID sahSanchalakAssignmentId, List<NameCandidate> candidates) {

    public static SabhaDefinitionResult created(
            UUID sabhaId, UUID sanchalakAssignmentId, UUID sahSanchalakAssignmentId) {
        return new SabhaDefinitionResult(sabhaId, sanchalakAssignmentId, sahSanchalakAssignmentId, List.of());
    }

    public static SabhaDefinitionResult softWarn(List<NameCandidate> candidates) {
        return new SabhaDefinitionResult(null, null, null, List.copyOf(candidates));
    }

    public boolean created() {
        return sabhaId != null;
    }

    public boolean softWarned() {
        return sabhaId == null;
    }
}
