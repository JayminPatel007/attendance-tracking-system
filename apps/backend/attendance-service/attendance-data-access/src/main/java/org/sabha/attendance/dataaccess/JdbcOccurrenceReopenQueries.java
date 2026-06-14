package org.sabha.attendance.dataaccess;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.sabha.attendance.applicationservice.OccurrenceReopenQueries;
import org.sabha.attendance.applicationservice.ReopenListItem;
import org.sabha.common.CallerVisibility;
import org.sabha.common.Role;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Read-side projection for the web Occurrence-reopen screen (Slice 13). Joins the
 * Occurrence to its Sabha and Kshetra and keeps only rows the caller may reopen:
 * the canonical {@link CallerVisibility} predicate granted the {@link
 * Role#REOPEN_TIERS} authorities (Nirikshak, Nirdeshak, Sah-Nirdeshak over the
 * Sabha's {@code (kshetra, demographic)} — ADR-0001, ADR-0011). Here the Nirikshak
 * resolves through {@code role_assignments} like a Nirdeshak, not the proxy-Sabha
 * assignment the dashboard read model uses. The "reopened" badge and last reason
 * come from the state-transition audit log — there is no denormalized column.
 *
 * <p>This is a CQRS read projection over the single physical schema (ADR-0008):
 * it reads tables owned by other modules directly rather than fanning out through
 * cross-context ports, which the write paths use to keep the seams compile-clean.
 * Results are capped at {@link #MAX_ROWS} most-recent rows so the payload stays
 * bounded as history accumulates — reopen targets recent corrections (ADR-0001).</p>
 */
@Repository
public class JdbcOccurrenceReopenQueries implements OccurrenceReopenQueries {

    /** Upper bound on the list payload; the screen only ever needs recent Occurrences. */
    private static final int MAX_ROWS = 200;

    private static final String REOPEN_SCOPE = CallerVisibility.predicate(
            new CallerVisibility.Aliases("s", "k", "z"), CallerVisibility.tiersFor(Role.REOPEN_TIERS));

    private static final String LIST_SQL = """
            SELECT o.id,
                   COALESCE(o.rescheduled_date, o.occurrence_date) AS effective_date,
                   o.state,
                   k.name AS kshetra_name,
                   s.sabha_kind,
                   COALESCE(o.venue_override, s.standing_venue) AS venue,
                   EXISTS (SELECT 1 FROM occurrence_state_transitions t
                           WHERE t.occurrence_id = o.id AND t.action = 'REOPEN') AS reopened,
                   (SELECT t.reason FROM occurrence_state_transitions t
                    WHERE t.occurrence_id = o.id AND t.action = 'REOPEN'
                    ORDER BY t.at_timestamp DESC LIMIT 1) AS last_reopen_reason
            FROM occurrences o
            JOIN sabhas s ON s.id = o.sabha_id
            JOIN kshetras k ON k.id = s.kshetra_id
            WHERE %s
              AND (o.state = 'FINALIZED'
                   OR EXISTS (SELECT 1 FROM occurrence_state_transitions t
                              WHERE t.occurrence_id = o.id AND t.action = 'REOPEN'))
            ORDER BY effective_date DESC, o.id
            LIMIT :maxRows
            """.formatted(REOPEN_SCOPE);

    private final JdbcClient jdbc;

    public JdbcOccurrenceReopenQueries(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ReopenListItem> listForReopener(UUID userId) {
        return jdbc.sql(LIST_SQL)
                .param(CallerVisibility.USER_PARAM, userId)
                .param("maxRows", MAX_ROWS)
                .query((rs, n) -> new ReopenListItem(
                        rs.getObject("id", UUID.class),
                        rs.getObject("effective_date", LocalDate.class),
                        rs.getString("state"),
                        rs.getString("kshetra_name"),
                        rs.getString("sabha_kind"),
                        rs.getString("venue"),
                        rs.getBoolean("reopened"),
                        rs.getString("last_reopen_reason")))
                .list();
    }
}
