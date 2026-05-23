/**
 * Attendance context — Use-case ring (Clean Architecture).
 *
 * <p>Four-step application services that orchestrate Occurrence aggregates
 * (load → mutate → save → publish) plus the driven-port interfaces they depend
 * on ({@link org.sabha.attendance.applicationservice.OccurrenceRepository},
 * {@link org.sabha.attendance.applicationservice.CurrentRosterQuery}) and the
 * application-tier exceptions raised by orchestration (caller resolution,
 * aggregate lookup, optimistic-lock give-up). Permitted Spring dependencies:
 * {@code spring-context} and {@code spring-tx} only. Per ADR-0019.</p>
 */
package org.sabha.attendance.applicationservice;
