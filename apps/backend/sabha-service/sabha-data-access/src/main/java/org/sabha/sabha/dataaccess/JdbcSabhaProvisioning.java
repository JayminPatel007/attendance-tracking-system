package org.sabha.sabha.dataaccess;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import org.sabha.common.SabhaProvisioning;
import org.sabha.sabha.domain.Sabha;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * JDBC implementation of the cross-context {@link SabhaProvisioning} port
 * (ADR-0012, ADR-0019). Builds the {@link Sabha} aggregate (which enforces the
 * schedule-shape invariants) and inserts it into the {@code sabhas} table. The
 * denormalized {@code sabha_kind} token is written as {@code TRACK_DEMOGRAPHIC}
 * to match {@link JdbcStructuralHierarchyLookup}, alongside the typed
 * {@code sabha_kind_id} FK introduced in Slice 12.
 */
@Repository
public class JdbcSabhaProvisioning implements SabhaProvisioning {

    private final JdbcClient jdbc;

    public JdbcSabhaProvisioning(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<String> demographicOfKind(UUID sabhaKindId) {
        return jdbc.sql("SELECT demographic FROM sabha_kinds WHERE id = ?")
                .param(sabhaKindId)
                .query(String.class)
                .optional();
    }

    @Override
    public UUID createWeekly(UUID kshetraId, UUID sabhaKindId, DayOfWeek dayOfWeek,
                             LocalTime startTime, LocalTime endTime, String standingVenue, UUID createdBy) {
        Sabha sabha = Sabha.weekly(kshetraId, sabhaKindId, dayOfWeek, startTime, endTime, standingVenue, createdBy);
        insert(sabha);
        return sabha.id();
    }

    @Override
    public UUID createMonthlyAdHoc(UUID kshetraId, UUID sabhaKindId, String standingVenue, UUID createdBy) {
        Sabha sabha = Sabha.monthlyAdHoc(kshetraId, sabhaKindId, standingVenue, createdBy);
        insert(sabha);
        return sabha.id();
    }

    private void insert(Sabha sabha) {
        jdbc.sql("""
                INSERT INTO sabhas (id, kshetra_id, sabha_kind, sabha_kind_id, schedule_shape,
                                    day_of_week, start_time, end_time, standing_venue, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)
                .param(sabha.id())
                .param(sabha.kshetraId())
                .param(kindToken(sabha.sabhaKindId()))
                .param(sabha.sabhaKindId())
                .param(sabha.scheduleShape().name())
                .param(sabha.dayOfWeek() == null ? null : storedDayOfWeek(sabha.dayOfWeek()))
                .param(sabha.startTime())
                .param(sabha.endTime())
                .param(sabha.standingVenue())
                .param(sabha.createdBy())
                .update();
    }

    /** The {@code TRACK_DEMOGRAPHIC} token denormalized into {@code sabhas.sabha_kind}. */
    private String kindToken(UUID sabhaKindId) {
        return jdbc.sql("SELECT track || '_' || demographic FROM sabha_kinds WHERE id = ?")
                .param(sabhaKindId)
                .query(String.class)
                .single();
    }

    /** Stores 0–6 with Sunday=0, matching the existing schema and lookup. */
    private static short storedDayOfWeek(DayOfWeek dayOfWeek) {
        return (short) (dayOfWeek == DayOfWeek.SUNDAY ? 0 : dayOfWeek.getValue());
    }
}
