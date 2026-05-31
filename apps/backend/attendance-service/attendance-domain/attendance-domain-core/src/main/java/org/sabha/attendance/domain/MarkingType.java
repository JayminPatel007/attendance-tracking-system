package org.sabha.attendance.domain;

/**
 * Discriminates how an {@link AttendanceMarking} was recorded. {@code ROSTER}
 * is the default — a Person marked against their Home Sabha's Roster.
 * {@code WALK_IN} is a Person attending a Sabha that is not one of their Home
 * Sabhas; it never changes their Home Sabha and is excluded from missed-Occurrence
 * streak analytics (re-engagement, Slice 15).
 */
public enum MarkingType {
    ROSTER,
    WALK_IN
}
