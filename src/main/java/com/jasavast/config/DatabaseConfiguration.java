package com.jasavast.config;

import com.jasavast.core.security.AuthorityConstants;
import com.jasavast.core.security.SecurityUtils;
import com.jasavast.domain.Authority;
import com.jasavast.repository.AuthorityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Configuration
@EnableReactiveMongoRepositories(basePackages = "com.jasavast.repository")
@EnableReactiveMongoAuditing
@Slf4j
public class DatabaseConfiguration {

    public static final String SYSTEM_ACCOUNT = "system";

    /** Fills @CreatedBy / @LastModifiedBy from the reactive security context. */
    @Bean
    public ReactiveAuditorAware<String> auditorAware() {
        return () -> SecurityUtils.getCurrentUserLogin().switchIfEmpty(Mono.just(SYSTEM_ACCOUNT));
    }

    /**
     * Idempotent seed of the base authorities (save by id = upsert). Blocking is
     * intentional: it runs once on the startup thread and fails startup if MongoDB is
     * unreachable instead of failing later on the first registration.
     */
    @Bean
    public ApplicationRunner seedAuthorities(AuthorityRepository authorityRepository) {
        return args -> authorityRepository
                .saveAll(Flux.just(AuthorityConstants.ADMIN, AuthorityConstants.USER).map(Authority::new))
                .then()
                .doOnSuccess(v -> log.debug("Authorities seeded"))
                .block(Duration.ofSeconds(30));
    }
}
