package org.sabha.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Assembles a dynamic SQL {@code WHERE} clause for the CQRS read adapters: a list of
 * AND-joined condition strings together with the named parameters they reference.
 *
 * <p>The point of this type is that {@link #sql()} owns the {@code WHERE} keyword and
 * the single space before the predicate. No call site ever writes {@code "WHERE " +
 * predicate}, so the Java text-block trailing-whitespace trap — a text block silently
 * strips the space, yielding {@code WHEREpredicate} — is unrepresentable rather than
 * something each adapter has to remember to dodge. A composing query interpolates the
 * whole clause through a bare {@code %s} slot ({@link String#formatted}) and binds
 * {@link #params()}.</p>
 *
 * <p>With no conditions the clause folds to {@code WHERE 1 = 1}, so a scope-only or
 * unrestricted read still has a syntactically valid body.</p>
 */
public final class WhereClause {

    private final List<String> conditions = new ArrayList<>();
    private final Map<String, Object> params = new HashMap<>();

    private WhereClause() {
    }

    public static WhereClause create() {
        return new WhereClause();
    }

    /** Adds a condition that binds no parameter (e.g. {@code "x IS NOT NULL"}). */
    public WhereClause and(String condition) {
        conditions.add(condition);
        return this;
    }

    /** Adds a condition together with the single named parameter it references. */
    public WhereClause and(String condition, String param, Object value) {
        conditions.add(condition);
        params.put(param, value);
        return this;
    }

    /**
     * Binds a named parameter apart from any one condition — for a condition that
     * references several (such as a geography OR-group binding one IN list per scope).
     */
    public WhereClause param(String name, Object value) {
        params.put(name, value);
        return this;
    }

    /** The full clause: {@code WHERE} then the AND-joined conditions, folding empty to {@code 1 = 1}. */
    public String sql() {
        String predicate = conditions.isEmpty() ? "1 = 1" : String.join(" AND ", conditions);
        return "WHERE " + predicate;
    }

    /** The named parameters the conditions reference. */
    public Map<String, Object> params() {
        return Map.copyOf(params);
    }
}
