package com.jasavast.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.web.ReactivePageableHandlerMethodArgumentResolver;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.WebExceptionHandler;
import org.zalando.problem.spring.webflux.advice.ProblemExceptionHandler;
import org.zalando.problem.spring.webflux.advice.ProblemHandling;
@Configuration
@Slf4j
public class WebConfiguration implements WebFluxConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String baseUrl="http://localhost:8080/";

        registry
                .addResourceHandler(baseUrl + "/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/")
                .resourceChain(false);

    }

    @Bean
    public CorsWebFilter corsWebFilter(){
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        if (config.getAllowedOrigins()!=null && !config.getAllowedOrigins().isEmpty()){
            log.debug("Register cors filter");
            source.registerCorsConfiguration("/api",config);
            source.registerCorsConfiguration("/management",config);
            source.registerCorsConfiguration("/v2/api-docs",config);
        }
        source.registerCorsConfiguration("/**",config);
        return new CorsWebFilter(source);
    }
    @Bean
    public HandlerMethodArgumentResolver reactivePageableHandlerMethodArgumentResolver(){
        return new ReactivePageableHandlerMethodArgumentResolver();
    }
    @Bean
    public HandlerMethodArgumentResolver reactiveSortHandlerMethodArgumentResolver(){
        return new ReactivePageableHandlerMethodArgumentResolver();
    }
    @Bean
    @Order(-2)
    public WebExceptionHandler problemExceptionHandler(ObjectMapper mapper, ProblemHandling problemHandling){
        return new ProblemExceptionHandler(mapper,problemHandling);
    }
}
