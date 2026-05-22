/**
 * Attendance domain layer: AttendanceMarking aggregate, marking-type
 * discriminator (Roster vs Walk-in), sync conflict resolution rules
 * (last-write-wins per ADR-0007). Pure domain — no Spring, no JPA.
 */
package org.sabha.attendance.domain;
