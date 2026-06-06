package org.sabha.identity.dataaccess;

import java.util.UUID;

import org.sabha.identity.applicationservice.SelectionRoster;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * JDBC adapter over {@code home_sabhas} for the BSS/YSS selection workflow
 * (ADR-0006). Membership is additive: approval inserts the selective Sabha
 * alongside the Person's existing Home Sabhas; deselection removes only that row,
 * leaving the Regular Home Sabha untouched. The insert is idempotent against the
 * {@code (person_id, sabha_id)} primary key.
 */
@Repository
public class JdbcSelectionRoster implements SelectionRoster {

    private final JdbcClient jdbc;

    public JdbcSelectionRoster(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean isOnRoster(UUID personId, UUID sabhaId) {
        return jdbc.sql("SELECT COUNT(*) AS c FROM home_sabhas WHERE person_id = ? AND sabha_id = ?")
                .param(personId)
                .param(sabhaId)
                .query((rs, n) -> rs.getInt("c"))
                .single() > 0;
    }

    @Override
    public void addHomeSabha(UUID personId, UUID sabhaId) {
        jdbc.sql("""
                INSERT INTO home_sabhas (person_id, sabha_id) VALUES (?, ?)
                ON CONFLICT (person_id, sabha_id) DO NOTHING
                """)
                .param(personId)
                .param(sabhaId)
                .update();
    }

    @Override
    public void removeHomeSabha(UUID personId, UUID sabhaId) {
        jdbc.sql("DELETE FROM home_sabhas WHERE person_id = ? AND sabha_id = ?")
                .param(personId)
                .param(sabhaId)
                .update();
    }
}
