package org.sabha.identity.domain;

import java.time.Instant;
import java.util.List;

import org.sabha.common.DomainEvent;

/**
 * What an aggregate must offer for its flow to be OTP-guarded (issue #130): the
 * dispatch acknowledgement and its registered events. {@link PasswordReset} and
 * {@link HomeSabhaTransfer} implement it, which is the whole of what the shared
 * orchestration needs to know about them — everything else (who may start the
 * flow, what success does) stays with the feature.
 *
 * <p>The OTP state machine itself is not part of this contract: the aggregate
 * composes an {@link OtpChallenge} and exposes code entry under its own
 * vocabulary ({@code verify} / {@code confirm}), which the flow drives through a
 * caller-supplied lambda rather than through this interface.</p>
 */
public interface OtpGuarded {

    /** Records that the OTP was dispatched, registering the flow's "sent" event. */
    void markOtpSent(Instant now);

    /** Drains the events registered since the last pull (see {@code AggregateRoot}). */
    List<DomainEvent> pullDomainEvents();
}
