package org.sabha.sabha.applicationservice;

import org.sabha.sabha.domain.Demographic;
import org.sabha.sabha.domain.SabhaKind;
import org.sabha.sabha.domain.Track;

/** Driven port for persisting and de-duplicating {@link SabhaKind} aggregates. */
public interface SabhaKindRepository {

    void save(SabhaKind kind);

    boolean exists(Demographic demographic, Track track);
}
