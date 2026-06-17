package org.sabha.identity.applicationservice.appointment;

import java.util.List;
import java.util.UUID;

import org.sabha.identity.applicationservice.directory.NameCandidate;

/**
 * Outcome of an appointment attempt: either the role was {@link #appointed} (with
 * the resolved Person, User, and new RoleAssignment ids) or — only on the inline
 * new-Person path — the name soft-warn fired and the appointer must choose among
 * {@link #softWarn(List) candidates} or override (Slice 6). A mobile hard block
 * and an authorization denial are signalled by exceptions, not an
 * AppointmentResult.
 */
public record AppointmentResult(UUID personId, UUID userId, UUID assignmentId, List<NameCandidate> candidates) {

    public static AppointmentResult appointed(UUID personId, UUID userId, UUID assignmentId) {
        return new AppointmentResult(personId, userId, assignmentId, List.of());
    }

    public static AppointmentResult softWarn(List<NameCandidate> candidates) {
        return new AppointmentResult(null, null, null, List.copyOf(candidates));
    }

    public boolean appointed() {
        return assignmentId != null;
    }

    public boolean softWarned() {
        return assignmentId == null;
    }
}
