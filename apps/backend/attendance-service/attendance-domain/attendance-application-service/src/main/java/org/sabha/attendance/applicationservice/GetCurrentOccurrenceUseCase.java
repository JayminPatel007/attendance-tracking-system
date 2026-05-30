package org.sabha.attendance.applicationservice;

import java.util.Optional;
import java.util.UUID;

import org.sabha.common.CallerResolver;
import org.springframework.stereotype.Service;

@Service
public class GetCurrentOccurrenceUseCase {

    private final CallerResolver callerResolver;
    private final CurrentOccurrenceQuery query;

    public GetCurrentOccurrenceUseCase(CallerResolver callerResolver, CurrentOccurrenceQuery query) {
        this.callerResolver = callerResolver;
        this.query = query;
    }

    public Optional<CurrentOccurrence> execute(UUID keycloakSubject) {
        return callerResolver.resolveUserId(keycloakSubject)
                .flatMap(query::findShapeableForSanchalak);
    }
}
