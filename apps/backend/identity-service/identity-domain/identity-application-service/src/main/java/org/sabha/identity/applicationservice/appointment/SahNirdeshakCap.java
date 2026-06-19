package org.sabha.identity.applicationservice.appointment;

import java.util.UUID;

import org.springframework.stereotype.Service;

/**
 * The Sah-Nirdeshak cap of two per (Kshetra, demographic) (ADR-0025 §3), in one
 * place. {@link RoleAppointmentService} asks it to enforce the limit at
 * appointment time; the web role console asks it (over the BFF) to render the
 * 2/2 indicator and disable the appoint action at the limit. The threshold and
 * the "reached" rule live here so the write and read paths can never disagree.
 */
@Service
public class SahNirdeshakCap {

    /** ADR-0025 §3: at most this many active Sah-Nirdeshaks per (Kshetra, demographic). */
    public static final int CAP = 2;

    private final SahNirdeshakCountLookup counts;

    public SahNirdeshakCap(SahNirdeshakCountLookup counts) {
        this.counts = counts;
    }

    public CapStatus statusFor(UUID kshetraId, String demographic) {
        return new CapStatus(counts.activeCount(kshetraId, demographic), CAP);
    }

    /** How many active Sah-Nirdeshaks a (Kshetra, demographic) holds, against the cap. */
    public record CapStatus(int active, int cap) {

        public boolean reached() {
            return active >= cap;
        }
    }
}
