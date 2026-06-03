package org.sabha.sabha.applicationservice;

import org.sabha.sabha.domain.Kshetra;

/** Driven port for persisting {@link Kshetra} aggregates. */
public interface KshetraRepository {

    void save(Kshetra kshetra);
}
