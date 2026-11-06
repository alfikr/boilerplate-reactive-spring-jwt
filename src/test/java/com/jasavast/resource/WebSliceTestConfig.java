package com.jasavast.resource;

import com.jasavast.api.coba.CobaAja;
import com.jasavast.config.ApplicationProperties;
import com.jasavast.config.ClockConfiguration;
import com.jasavast.config.SecurityConfiguration;
import com.jasavast.config.WebConfiguration;
import com.jasavast.core.annotation.AspectConfiguration;
import com.jasavast.core.api.ApiDispatcher;
import com.jasavast.core.security.SecurityUserService;
import com.jasavast.core.security.filter.TokenProvider;
import com.jasavast.service.AccountService;
import com.jasavast.service.MailService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

/** The real security, dispatcher, aspect and services; only repositories are mocked. */
@TestConfiguration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableConfigurationProperties(ApplicationProperties.class)
@Import({SecurityConfiguration.class, WebConfiguration.class, ClockConfiguration.class, TokenProvider.class,
        SecurityUserService.class, ApiDispatcher.class, CobaAja.class, AspectConfiguration.class,
        AccountService.class, MailService.class})
class WebSliceTestConfig {
}
