package com.jasavast.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Typed binding for the {@code application.*} namespace. Validated at startup so a
 * missing secret fails fast instead of producing unsigned/weak tokens at runtime.
 */
@ConfigurationProperties(prefix = "application")
@Validated
public record ApplicationProperties(@Valid Security security, @Valid Cors cors, @Valid Mail mail) {

    public record Security(
            /* Base64 encoded HMAC key, at least 64 bytes (HS512). Generate: openssl rand -base64 64 */
            @NotBlank String secret,
            @Positive @DefaultValue("86400") long tokenValiditySeconds,
            @Positive @DefaultValue("2592000") long tokenValiditySecondsRememberMe) {
    }

    public record Cors(@DefaultValue List<String> allowedOrigins) {
    }

    public record Mail(
            @NotBlank @DefaultValue("no-reply@localhost") String from,
            /* Public base URL used to build activation / reset links in e-mails. */
            @NotBlank @DefaultValue("http://localhost:8080") String baseUrl) {
    }
}
