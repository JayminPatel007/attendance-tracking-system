package org.sabha.identity.applicationservice.directory;

import java.util.List;
import java.util.UUID;

/**
 * Outcome of an add attempt: either the Person was {@link #created(UUID)} (with
 * the new id) or the name soft-warn fired and the adder must choose among
 * {@link #softWarn(List) candidates} or override. A mobile hard block is not an
 * AddResult — it is signalled by {@link MobileAlreadyRegisteredException}.
 */
public record AddResult(UUID personId, List<NameCandidate> candidates) {

    public static AddResult created(UUID personId) {
        return new AddResult(personId, List.of());
    }

    public static AddResult softWarn(List<NameCandidate> candidates) {
        return new AddResult(null, List.copyOf(candidates));
    }

    public boolean created() {
        return personId != null;
    }

    public boolean softWarned() {
        return personId == null;
    }
}
