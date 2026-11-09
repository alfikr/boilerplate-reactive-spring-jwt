package com.jasavast.config;

import com.jasavast.core.security.SecurityUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import reactor.core.publisher.Mono;

/**
 * Schema is owned by Flyway (src/main/resources/db/migration), run over JDBC at startup;
 * all runtime access is R2DBC. Hibernate-style schema generation is intentionally absent.
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "com.jasavast.repository")
@EnableR2dbcAuditing
public class DatabaseConfiguration {

    public static final String SYSTEM_ACCOUNT = "system";

    /** Fills @CreatedBy / @LastModifiedBy from the reactive security context. */
    @Bean
    public ReactiveAuditorAware<String> auditorAware() {
        return () -> SecurityUtils.getCurrentUserLogin().switchIfEmpty(Mono.just(SYSTEM_ACCOUNT));
    }
}
