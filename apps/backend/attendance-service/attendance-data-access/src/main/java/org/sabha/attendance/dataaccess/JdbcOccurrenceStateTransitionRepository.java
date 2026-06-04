package org.sabha.attendance.dataaccess;

import org.sabha.attendance.applicationservice.OccurrenceStateTransition;
import org.sabha.attendance.applicationservice.OccurrenceStateTransitionRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOccurrenceStateTransitionRepository implements OccurrenceStateTransitionRepository {

    private final JdbcClient jdbc;

    public JdbcOccurrenceStateTransitionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void append(OccurrenceStateTransition transition) {
        jdbc.sql("""
                INSERT INTO occurrence_state_transitions
                    (id, occurrence_id, from_state, to_state, action,
                     actor_kind, actor_user_id, on_behalf_of_user_id, reason, at_timestamp)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)
                .param(transition.id())
                .param(transition.occurrenceId())
                .param(transition.fromState().name())
                .param(transition.toState().name())
                .param(transition.action().name())
                .param(transition.actorKind().name())
                .param(transition.actorUserId())
                .param(transition.onBehalfOfUserId())
                .param(transition.reason())
                .param(java.sql.Timestamp.from(transition.at()))
                .update();
    }
}
