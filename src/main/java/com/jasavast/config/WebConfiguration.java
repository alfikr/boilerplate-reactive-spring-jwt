package com.jasavast.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

@Configuration
@Slf4j
public class WebConfiguration {

    /**
     * CORS is applied by Spring Security ({@code http.cors(...)}) so pre-flight requests
     * are answered before authentication. Origins must be listed explicitly: a wildcard
     * origin combined with credentials is rejected by browsers and by Spring.
     * Authentication uses a bearer header, not cookies, so credentials are not allowed.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(ApplicationProperties properties) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        List<String> origins = properties.cors().allowedOrigins();
        if (origins == null || origins.isEmpty()) {
            log.debug("No CORS origins configured; cross-origin requests are not allowed");
            return source;
        }
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Link", "X-Total-Count"));
        config.setAllowCredentials(false);
        config.setMaxAge(Duration.ofHours(1));
        source.registerCorsConfiguration("/api/**", config);
        source.registerCorsConfiguration("/v3/api-docs/**", config);
        log.debug("CORS enabled for origins {}", origins);
        return source;
    }
}
