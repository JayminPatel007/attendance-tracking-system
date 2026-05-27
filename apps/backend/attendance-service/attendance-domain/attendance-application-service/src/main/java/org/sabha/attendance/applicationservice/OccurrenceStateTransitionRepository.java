package org.sabha.attendance.applicationservice;

public interface OccurrenceStateTransitionRepository {

    void append(OccurrenceStateTransition transition);
}
