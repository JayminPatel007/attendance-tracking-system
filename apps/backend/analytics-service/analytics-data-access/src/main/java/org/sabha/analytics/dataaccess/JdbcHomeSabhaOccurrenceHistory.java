package org.sabha.analytics.dataaccess;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.sabha.analytics.applicationservice.HomeSabhaOccurrenceHistory;
import org.sabha.analytics.domain.HomeSabhaHistory;
import org.sabha.analytics.domain.OutcomeKind;
import org.sabha.analytics.domain.Scope;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Read-side adapter feeding the Re-engagement Candidate Calculator (ADR-0010) the
 * chronological outcome stream per (Person, Home Sabha) within a Scope.
 *
 * <p>It stays deliberately dumb: it classifies raw facts — a concluded Home Sabha
 * Occurrence as {@code PRESENT}/{@code ABSENT}/{@code CANCELLED}, and the Person's
 * Walk-ins elsewhere as {@code WALK_IN_ELSEWHERE} — and hands them over in date
 * order. The streak rule (which outcomes count, which reset) lives on
 * {@link HomeSabhaHistory}, not in this SQL. Only Occurrences from a Person's
 * membership window onward are facts for them, so a freshly-transferred Person
 * (Slice 8) does not inherit a missed streak at their new Home Sabha.
 */
@Repository
public class JdbcHomeSabhaOccurrenceHistory implements HomeSabhaOccurrenceHistory {

    private static final String SQL = """
            SELECT person_id, home_sabha_id, outcome
            FROM (
                SELECT hs.person_id,
                       hs.sabha_id AS home_sabha_id,
                       o.occurrence_date AS event_date,
                       o.created_at AS event_seq,
                       CASE
                           WHEN o.state = 'CANCELLED' THEN 'CANCELLED'
                           WHEN am.present IS TRUE     THEN 'PRESENT'
                           ELSE 'ABSENT'
                       END AS outcome
                FROM home_sabhas hs
                JOIN occurrences o
                       ON o.sabha_id = hs.sabha_id
                      AND o.state IN ('FINALIZED', 'CANCELLED')
                      AND o.occurrence_date >= hs.assigned_at::date
                LEFT JOIN attendance_markings am
                       ON am.occurrence_id = o.id
                      AND am.person_id = hs.person_id
                      AND am.marking_type = 'ROSTER'
                WHERE hs.sabha_id IN (:sabhaIds)

                UNION ALL

                SELECT hs.person_id,
                       hs.sabha_id AS home_sabha_id,
                       wo.occurrence_date AS event_date,
                       wo.created_at AS event_seq,
                       'WALK_IN_ELSEWHERE' AS outcome
                FROM home_sabhas hs
                JOIN attendance_markings wm
                       ON wm.person_id = hs.person_id
                      AND wm.marking_type = 'WALK_IN'
                JOIN occurrences wo
                       ON wo.id = wm.occurrence_id
                WHERE hs.sabha_id IN (:sabhaIds)
            ) ev
            ORDER BY person_id, home_sabha_id, event_date, event_seq
            """;

    private final JdbcClient jdbc;

    public JdbcHomeSabhaOccurrenceHistory(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<HomeSabhaHistory> within(Scope scope) {
        Set<UUID> sabhaIds = resolveSabhaIds(scope);
        if (sabhaIds.isEmpty()) {
            return List.of();
        }

        Map<Stream, List<OutcomeKind>> outcomesByStream = new LinkedHashMap<>();
        jdbc.sql(SQL)
                .param("sabhaIds", sabhaIds)
                .query((rs, n) -> new Row(
                        rs.getObject("person_id", UUID.class),
                        rs.getObject("home_sabha_id", UUID.class),
                        OutcomeKind.valueOf(rs.getString("outcome"))))
                .list()
                .forEach(row -> outcomesByStream
                        .computeIfAbsent(new Stream(row.personId(), row.homeSabhaId()), k -> new ArrayList<>())
                        .add(row.outcome()));

        return outcomesByStream.entrySet().stream()
                .map(e -> new HomeSabhaHistory(e.getKey().personId(), e.getKey().homeSabhaId(), e.getValue()))
                .toList();
    }

    private Set<UUID> resolveSabhaIds(Scope scope) {
        return switch (scope) {
            case Scope.OfSabhas ofSabhas -> ofSabhas.sabhaIds();
            case Scope.Everything ignored -> Set.copyOf(jdbc.sql("SELECT id FROM sabhas").query(UUID.class).list());
        };
    }

    private record Row(UUID personId, UUID homeSabhaId, OutcomeKind outcome) {
    }

    private record Stream(UUID personId, UUID homeSabhaId) {
    }
}
