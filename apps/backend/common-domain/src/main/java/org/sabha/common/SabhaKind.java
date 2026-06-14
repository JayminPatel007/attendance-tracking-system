package org.sabha.common;

/**
 * The {@code TRACK_DEMOGRAPHIC} encoding carried denormalized in {@code
 * sabhas.sabha_kind} (e.g. {@code REGULAR_YUVAK}, {@code YSS_YUVAK}). This is the
 * one place the encoding is defined; the cross-context structural lookup parses it
 * in Java through {@link #parse}, and the role-scoped read-model predicates derive
 * the demographic in SQL through {@link #demographicSql} (see {@link
 * CallerVisibility}).
 *
 * <p>The track/demographic boundary is the <em>first</em> underscore, so the
 * demographic is everything after it. {@link #demographicSql} uses {@code
 * split_part(..., '_', 2)}, which equals {@link #demographic()} only while every
 * demographic is a single token. If a multi-token demographic is ever introduced,
 * both derivations change here together — this is the single point of change the
 * two former call sites used to warn each other to "keep in step".</p>
 */
public record SabhaKind(String track, String demographic) {

    public static SabhaKind parse(String sabhaKind) {
        int boundary = sabhaKind.indexOf('_');
        return new SabhaKind(sabhaKind.substring(0, boundary), sabhaKind.substring(boundary + 1));
    }

    /** The {@code sabha_kind} column value for a {@code (track, demographic)} pair. */
    public static String encode(String track, String demographic) {
        return track + "_" + demographic;
    }

    /**
     * SQL deriving the demographic from a {@code sabha_kind} column on the given
     * table alias. Kept equivalent to {@link #demographic()} — see the class note.
     */
    public static String demographicSql(String sabhaAlias) {
        return "split_part(" + sabhaAlias + ".sabha_kind, '_', 2)";
    }
}
