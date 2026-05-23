package org.sabha.attendance.applicationservice;

import java.util.Optional;
import java.util.UUID;

import org.sabha.common.CallerResolver;
import org.springframework.stereotype.Service;

@Service
public class GetCurrentRosterUseCase {

    private final CallerResolver callerResolver;
    private final CurrentRosterQuery query;

    public GetCurrentRosterUseCase(CallerResolver callerResolver, CurrentRosterQuery query) {
        this.callerResolver = callerResolver;
        this.query = query;
    }

    public Optional<CurrentRoster> execute(UUID keycloakSubject) {
        return callerResolver.resolveUserId(keycloakSubject)
                .flatMap(query::findForSanchalak);
    }
}
