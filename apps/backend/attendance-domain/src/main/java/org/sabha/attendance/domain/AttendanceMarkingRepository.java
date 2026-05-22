package org.sabha.attendance.domain;

public interface AttendanceMarkingRepository {

    /**
     * Insert or update the marking for ({@code occurrenceId}, {@code personId}).
     * Latest write wins; the timestamp and {@code markedByUserId} on the row are
     * updated to reflect the most recent submission.
     */
    void upsert(AttendanceMarking marking);
}
