/**
 * Attendance context — Entities ring (Clean Architecture).
 *
 * <p>Aggregate roots ({@link org.sabha.attendance.domain.Occurrence}), entities
 * ({@link org.sabha.attendance.domain.AttendanceMarking}), value objects, and
 * domain-rule exceptions. Per ADR-0019 + ADR-0020 this package is pure Java:
 * no Spring, no JPA, no driven-port interfaces. Mutations live as methods on
 * aggregates that validate invariants and register domain events.</p>
 */
package org.sabha.attendance.domain;
