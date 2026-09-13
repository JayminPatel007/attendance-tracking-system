package org.sabha.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.List;

import org.junit.jupiter.api.Test;

class WhereClauseTest {

    @Test
    void emits_the_where_keyword_before_a_single_condition() {
        WhereClause where = WhereClause.create().and("a = 1");

        assertThat(where.sql()).isEqualTo("WHERE a = 1");
    }

    @Test
    void folds_to_a_tautology_when_no_conditions_were_added() {
        // An unrestricted/scope-only caller still needs a syntactically valid WHERE body.
        assertThat(WhereClause.create().sql()).isEqualTo("WHERE 1 = 1");
    }

    @Test
    void joins_conditions_with_and_in_the_order_they_were_added() {
        WhereClause where = WhereClause.create()
                .and("a = 1")
                .and("b = 2")
                .and("c = 3");

        assertThat(where.sql()).isEqualTo("WHERE a = 1 AND b = 2 AND c = 3");
    }

    @Test
    void binds_the_named_parameter_a_condition_references() {
        WhereClause where = WhereClause.create().and("a = :x", "x", 5);

        assertThat(where.sql()).isEqualTo("WHERE a = :x");
        assertThat(where.params()).containsExactly(entry("x", 5));
    }

    @Test
    void binds_a_standalone_parameter_for_a_condition_carrying_several_params() {
        // The audit feed's geography OR-group is one condition but binds an IN
        // list per non-empty scope set, so params are bound apart from the clause.
        WhereClause where = WhereClause.create()
                .and("(f.kshetra_id IN (:kshetraIds) OR f.zone_id IN (:zoneIds))")
                .param("kshetraIds", List.of("k1"))
                .param("zoneIds", List.of("z1"));

        assertThat(where.params())
                .containsOnly(entry("kshetraIds", List.of("k1")),
                        entry("zoneIds", List.of("z1")));
    }

    @Test
    void has_no_parameters_when_only_plain_conditions_were_added() {
        assertThat(WhereClause.create().and("x IS NOT NULL").params()).isEmpty();
    }
}
