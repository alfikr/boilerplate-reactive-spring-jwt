package com.jasavast.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Injectable clock so time-dependent rules (reset key expiry) are testable. */
@Configuration
public class ClockConfiguration {
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
