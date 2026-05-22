/**
 * Attendance bounded context — outbound adapters. JDBC implementations of the
 * domain ports: {@code OccurrenceRepository}, {@code AttendanceMarkingRepository},
 * and the read-side {@code CurrentRosterQuery}. Inbound REST controllers live
 * in attendance-application per ADR-0017.
 */
package org.sabha.attendance.infrastructure;
