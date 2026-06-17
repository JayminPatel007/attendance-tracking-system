package org.sabha.identity.applicationservice.appointment;

/**
 * Write port for {@code role_assignments} rows produced by appointment
 * (ADR-0011). The read side is served by {@link org.sabha.common.RoleAssignmentLookup}
 * / {@link org.sabha.common.SanyojakZoneLookup} / {@link AppointerAuthorityLookup};
 * this is the single write path. Each {@link RoleAppointmentRow} carries the
 * {@code appointedBy} / {@code appointedAt} audit the ADR mandates; scope columns
 * are populated per the appointed role (Sabha-scoped vs Kshetra / Zone / City +
 * demographic).
 */
public interface RoleAppointmentRepository {

    void save(RoleAppointmentRow row);
}
