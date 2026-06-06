package org.sabha.identity.domain;

/**
 * The selective track a Regular demographic feeds into (ADR-0006): the children's
 * demographics (Baal/Balika) feed Bal Sevak Sabha ({@code BSS}); the youth
 * demographics (Yuvak/Yuvati) feed Yuvak/Yuvati Sevak Sabha ({@code YSS}). The
 * all-encompassing Sanyukta kind has no selective counterpart, so nominating from
 * it is rejected.
 */
public final class SelectiveTrack {

    public static final String BSS = "BSS";
    public static final String YSS = "YSS";

    private SelectiveTrack() {
    }

    /** The selective track for a Regular {@code demographic}, or throws if none exists. */
    public static String forDemographic(String demographic) {
        return switch (demographic) {
            case "BAAL", "BALIKA" -> BSS;
            case "YUVAK", "YUVATI" -> YSS;
            default -> throw new NoSelectiveTrackException(demographic);
        };
    }
}
