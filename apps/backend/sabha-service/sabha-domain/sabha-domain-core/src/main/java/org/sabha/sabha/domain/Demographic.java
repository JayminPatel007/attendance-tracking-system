package org.sabha.sabha.domain;

/**
 * The demographic dimension of a {@link SabhaKind} (CONTEXT § Sabha Type). The
 * five named demographics currently known to the organization. The <em>set of
 * kinds</em> is data (a {@code (demographic, track)} row registered by the
 * Madhyastha Karyalaya), so adding a new combination needs no code change
 * (ADR-0009); a genuinely new demographic category, were one to emerge, would.
 */
public enum Demographic {
    BAAL,
    BALIKA,
    YUVAK,
    YUVATI,
    SANYUKTA
}
