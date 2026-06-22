package org.sabha.sabha.applicationservice;

import java.util.Optional;
import java.util.UUID;

import org.sabha.sabha.domain.Demographic;
import org.sabha.sabha.domain.SabhaKind;
import org.sabha.sabha.domain.Track;

/** Driven port for persisting and de-duplicating {@link SabhaKind} aggregates. */
public interface SabhaKindRepository {

    void save(SabhaKind kind);

    boolean exists(Demographic demographic, Track track);

    /** Loads a kind for a lifecycle transition (retire/reactivate). */
    Optional<SabhaKind> findById(UUID id);

    /** Persists a retire/reactivate state change to an existing kind. */
    void update(SabhaKind kind);
}
