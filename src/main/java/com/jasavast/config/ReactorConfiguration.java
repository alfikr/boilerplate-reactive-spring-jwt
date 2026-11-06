package com.jasavast.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Hooks;

/**
 * Operator-level assembly tracing. It captures a stack trace on every operator
 * assembly, which is expensive, so it is only enabled in the dev profile.
 */
@Configuration
@Profile("dev")
public class ReactorConfiguration {
    public ReactorConfiguration() {
        Hooks.onOperatorDebug();
    }
}
