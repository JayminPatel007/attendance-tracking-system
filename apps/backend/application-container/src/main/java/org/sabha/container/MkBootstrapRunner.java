package org.sabha.container;

import org.sabha.identity.applicationservice.bootstrap.BootstrapMkCommand;
import org.sabha.identity.applicationservice.bootstrap.MkBootstrapService;
import org.sabha.identity.domain.Gender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Install-time seed of the first Madhyastha Karyalaya member (ADR-0011's one-off
 * bootstrap). Reads operator-supplied credentials from the environment and hands
 * them to {@link MkBootstrapService}, which is idempotent — so this runs safely
 * on every boot. When no bootstrap credentials are configured it does nothing,
 * keeping ordinary boots (and the other integration tests) untouched.
 */
@Component
public class MkBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MkBootstrapRunner.class);

    private final MkBootstrapService bootstrap;
    private final String username;
    private final String password;
    private final String fullName;
    private final String gender;
    private final String mobile;

    public MkBootstrapRunner(
            MkBootstrapService bootstrap,
            @Value("${sabha.bootstrap.mk.username:}") String username,
            @Value("${sabha.bootstrap.mk.password:}") String password,
            @Value("${sabha.bootstrap.mk.full-name:Madhyastha Karyalaya Member}") String fullName,
            @Value("${sabha.bootstrap.mk.gender:MALE}") String gender,
            @Value("${sabha.bootstrap.mk.mobile:}") String mobile) {
        this.bootstrap = bootstrap;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.gender = gender;
        this.mobile = mobile;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            log.info("MK bootstrap credentials not configured; skipping install seed.");
            return;
        }
        bootstrap.ensureBootstrapMember(new BootstrapMkCommand(
                fullName, Gender.valueOf(gender), mobile, username, password));
    }
}
