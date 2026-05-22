package org.sabha.attendance.application;

import java.util.Optional;
import java.util.UUID;

import org.sabha.attendance.domain.CurrentRoster;
import org.sabha.attendance.domain.CurrentRosterQuery;
import org.sabha.sharedkernel.CallerResolver;
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
