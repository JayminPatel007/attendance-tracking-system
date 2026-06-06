package org.sabha.identity.domain;

import java.util.UUID;

/**
 * The Kshetra has no selective ({@code BSS}/{@code YSS}) Sabha of the nominee's
 * demographic for them to join (ADR-0006) — the selective Sabha must be defined
 * (Slice 12) before children can be nominated into it. A {@link NotFoundException}
 * so the workflow surfaces a clear "no such Sabha" rather than a generic 422.
 */
public class NoSelectiveSabhaException extends org.sabha.common.NotFoundException {

    public NoSelectiveSabhaException(UUID kshetraId, String demographic, String track) {
        super("No " + track + " " + demographic + " Sabha exists in Kshetra " + kshetraId);
    }
}
