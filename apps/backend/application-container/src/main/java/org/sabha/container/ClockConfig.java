package org.sabha.container;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Application-wide {@link Clock} bean. Injecting {@code Clock} (rather than
 * calling {@code Instant.now()} directly) lets integration tests substitute a
 * mutable test clock to exercise cron-driven Occurrence state transitions
 * without waiting wall-clock time (Slice 3).
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock systemClock(@Value("${sabha.time-zone:Asia/Kolkata}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
