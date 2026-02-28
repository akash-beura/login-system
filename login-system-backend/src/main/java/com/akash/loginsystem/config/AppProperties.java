package com.akash.loginsystem.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed binding for the `app:` block in application.yml.
 * All secrets are injected via env vars — never hardcoded.
 */
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private String frontendUrl;
    private String baseUrl;

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long expiryMs;
        private long refreshExpiryMs;
    }
}
