package org.sabha.container;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Guardrail for issue #71: virtual threads ({@code spring.threads.virtual.enabled=true})
 * must actually take effect on the two paths the flag changes.
 *
 * <p>The rest of the integration suite drives the app through {@code MockMvc} (which
 * calls the dispatcher on the test's platform thread) and invokes the cron scanners
 * in-process — so neither path that the flag rewires is otherwise exercised. These two
 * tests close that gap through real interfaces:</p>
 *
 * <ul>
 *   <li><b>HTTP request path</b> — a real {@code RANDOM_PORT} request goes through the
 *       Tomcat connector. A request filter records whether the serving thread is
 *       virtual. The target ({@code /actuator/health}) runs the DataSource health
 *       indicator, so a HikariCP JDBC round-trip happens on that virtual thread — the
 *       textbook pinning surface from the issue.</li>
 *   <li><b>Scheduled path</b> — a probe {@code @Scheduled} method records whether the
 *       scheduler ran it on a virtual thread, confirming the cron scanners (ADR-0021)
 *       fire normally on virtual threads with the flag on.</li>
 * </ul>
 *
 * If the flag is ever removed, both assertions flip to platform threads and fail.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(VirtualThreadsIntegrationTest.VirtualThreadProbeConfig.class)
class VirtualThreadsIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    VirtualThreadProbeConfig.RequestThreadProbe requestProbe;

    @Autowired
    VirtualThreadProbeConfig.ScheduledThreadProbe scheduledProbe;

    @Test
    void httpRequestsAreServedOnVirtualThreads() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        // 200 means the DataSource health indicator ran — i.e. a HikariCP JDBC
        // round-trip happened on the serving thread we are about to inspect.
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(requestProbe.observedRequest()).isTrue();
        assertThat(requestProbe.servedOnVirtualThread()).isTrue();
    }

    @Test
    void scheduledTasksRunOnVirtualThreads() {
        await().atMost(10, TimeUnit.SECONDS).until(scheduledProbe::fired);

        assertThat(scheduledProbe.ranOnVirtualThread()).isTrue();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class VirtualThreadProbeConfig {

        @Bean
        RequestThreadProbe requestThreadProbe() {
            return new RequestThreadProbe();
        }

        @Bean
        ScheduledThreadProbe scheduledThreadProbe() {
            return new ScheduledThreadProbe();
        }

        /**
         * Stub JwtDecoder so the context starts without a Keycloak container — no
         * authenticated request is made here (the target endpoint is permitted).
         */
        @Bean
        @Primary
        JwtDecoder testJwtDecoder() {
            return token -> {
                throw new UnsupportedOperationException("JWT decoding not exercised in this test");
            };
        }

        /** Records whether the Tomcat thread serving a request is virtual. */
        static final class RequestThreadProbe extends OncePerRequestFilter {

            private final AtomicBoolean observed = new AtomicBoolean(false);
            private final AtomicBoolean virtual = new AtomicBoolean(false);

            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                    FilterChain filterChain) throws ServletException, IOException {
                virtual.set(Thread.currentThread().isVirtual());
                observed.set(true);
                filterChain.doFilter(request, response);
            }

            boolean observedRequest() {
                return observed.get();
            }

            boolean servedOnVirtualThread() {
                return virtual.get();
            }
        }

        /** Records whether the scheduler ran a task on a virtual thread. */
        static final class ScheduledThreadProbe {

            private final CountDownLatch latch = new CountDownLatch(1);
            private final AtomicReference<Boolean> virtual = new AtomicReference<>();

            @Scheduled(fixedDelay = 200, initialDelay = 0)
            void probe() {
                virtual.compareAndSet(null, Thread.currentThread().isVirtual());
                latch.countDown();
            }

            boolean fired() {
                return latch.getCount() == 0;
            }

            boolean ranOnVirtualThread() {
                return Boolean.TRUE.equals(virtual.get());
            }
        }
    }
}
