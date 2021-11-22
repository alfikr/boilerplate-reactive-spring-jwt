package com.jasavast.config;

import com.jasavast.core.security.filter.JWTFilter;
import com.jasavast.core.security.filter.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {
    @Autowired
    private ReactiveUserDetailsService userDetailsService;
    @Autowired
    private TokenProvider tokenProvider;
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager(){
        UserDetailsRepositoryReactiveAuthenticationManager manager = new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);
        manager.setPasswordEncoder(passwordEncoder());
        return manager;
    }
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http){
        return http
                .csrf()
                .disable()
                .addFilterAt(new JWTFilter(tokenProvider), SecurityWebFiltersOrder.HTTP_BASIC)
                .formLogin()
                .authenticationManager(reactiveAuthenticationManager())
                .and()
                .authorizeExchange()
                .pathMatchers(HttpMethod.OPTIONS).permitAll()
                .pathMatchers("/api/register").permitAll()
                .pathMatchers("/api/login").permitAll()
                .pathMatchers("/api/forgot").permitAll()
                .pathMatchers("/swagger-resources/**").permitAll()
                .pathMatchers("/swagger-ui/**").permitAll()
                .pathMatchers("/v2/api-docs").permitAll()
                .pathMatchers("/**").permitAll()
                .pathMatchers("/api/**").authenticated()
                .and()
                .build();
    }
}
