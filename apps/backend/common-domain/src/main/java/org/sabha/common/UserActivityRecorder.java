package org.sabha.common;

import java.time.Instant;
import java.util.UUID;

/**
 * Records a User's activity signals so the proxy picker's informational "last
 * seen" hint can be computed (Slice 14). The signals are write-once-per-event
 * upserts keyed by user: a login (web OIDC) and an offline-sync push. The third
 * source — the last attendance marking — is read directly from the marking log
 * rather than recorded here, so it is not part of this port.
 *
 * <p>The port lives in common-domain because the writers span bounded contexts
 * (the attendance context records sync; the identity/web shell records login),
 * while the {@code user_activity} table the implementation upserts into is read
 * back by the attendance context's proxy Sabha picker.</p>
 */
public interface UserActivityRecorder {

    void recordSync(UUID userId, Instant at);

    void recordLogin(UUID userId, Instant at);
}
