package org.sabha.sabha.dataaccess;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.sabha.common.SabhaFact;
import org.sabha.common.SabhaFacts;
import org.sabha.common.SabhaKindCode;
import org.sabha.common.SabhaSchedule;
import org.sabha.common.SabhaScope;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * JDBC implementation of the cross-context {@link SabhaFacts} port (ADR-0019,
 * ADR-0033). Three windows on the sabha-owned {@code sabhas} table sharing one
 * projection and one row mapper, so every window returns a fully-populated
 * {@link SabhaFact} and the three differ only in their WHERE clause.
 *
 * <p>A Sabha's {@code (demographic, track)} is carried denormalized in
 * {@code sabhas.sabha_kind} as {@code TRACK_DEMOGRAPHIC} (e.g.
 * {@code REGULAR_YUVAK}); {@link SabhaKindCode} is the one definition of that
 * encoding the appointment engine matches against.</p>
 */
@Repository
public class JdbcSabhaFacts implements SabhaFacts {

    /**
     * The registry is joined with a LEFT JOIN, not an inner one: {@code sabha_kind_id}
     * is nullable by design (Slice 12 added it, and the pre-existing seed Sabhas carry
     * only the denormalized {@code sabha_kind} token). An inner join would drop those
     * rows entirely and make every read here empty for them. {@code retired_at IS NOT NULL}
     * is false — never null — for an unmatched row, which is the soft-retire default
     * ADR-0026 wants.
     */
    private static final String PROJECTION = """
            SELECT s.id, s.kshetra_id, s.sabha_kind, s.schedule_shape,
                   s.day_of_week, s.start_time, s.end_time,
                   (sk.retired_at IS NOT NULL) AS kind_retired
            FROM sabhas s
            LEFT JOIN sabha_kinds sk ON sk.id = s.sabha_kind_id
            """;

    private final JdbcClient jdbc;

    public JdbcSabhaFacts(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<SabhaFact> of(UUID sabhaId) {
        return jdbc.sql(PROJECTION + "WHERE s.id = ?")
                .param(sabhaId)
                .query(JdbcSabhaFacts::toFact)
                .optional();
    }

    @Override
    public List<SabhaFact> allWeekly() {
        return jdbc.sql(PROJECTION + "WHERE s.schedule_shape = '" + SabhaFact.WEEKLY_RECURRING + "'")
                .query(JdbcSabhaFacts::toFact)
                .list();
    }

    @Override
    public Optional<SabhaFact> selectiveIn(UUID kshetraId, String demographic, String track) {
        return jdbc.sql(PROJECTION + "WHERE s.kshetra_id = ? AND s.sabha_kind = ?")
                .param(kshetraId)
                .param(SabhaKindCode.encode(track, demographic))
                .query(JdbcSabhaFacts::toFact)
                .optional();
    }

    private static SabhaFact toFact(ResultSet rs, int rowNum) throws SQLException {
        SabhaKindCode kind = SabhaKindCode.parse(rs.getString("sabha_kind"));
        SabhaScope scope = new SabhaScope(
                rs.getObject("kshetra_id", UUID.class), kind.demographic(), kind.track());
        UUID id = rs.getObject("id", UUID.class);
        boolean kindRetired = rs.getBoolean("kind_retired");
        return SabhaFact.WEEKLY_RECURRING.equals(rs.getString("schedule_shape"))
                ? SabhaFact.weekly(id, scope, standingSlot(rs), kindRetired)
                : SabhaFact.monthlyAdHoc(id, scope, kindRetired);
    }

    private static SabhaSchedule standingSlot(ResultSet rs) throws SQLException {
        return new SabhaSchedule(
                toDayOfWeek(rs.getShort("day_of_week")),
                rs.getObject("start_time", LocalTime.class),
                rs.getObject("end_time", LocalTime.class));
    }

    private static DayOfWeek toDayOfWeek(short stored) {
        // Database stores 0–6 (Sunday=0). DayOfWeek values are MONDAY=1..SUNDAY=7.
        return stored == 0 ? DayOfWeek.SUNDAY : DayOfWeek.of(stored);
    }
}
