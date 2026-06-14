package org.sabha.container;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

/**
 * The {@link OpenAPIDefinition} names the generated contract (issue #73): springdoc
 * otherwise titles it "OpenAPI definition" / "v0", which would leak into the
 * generated web and mobile client class names. A static title and version keep the
 * committed spec stable for the drift gate (it isn't tied to the build version).
 */
@OpenAPIDefinition(info = @Info(
        title = "Sabha Attendance API",
        version = "0.1.0",
        description = "Public /api Bearer chain and session-cookie /bff chain (ADR-0022)."))
@SpringBootApplication(scanBasePackages = "org.sabha")
public class BackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
