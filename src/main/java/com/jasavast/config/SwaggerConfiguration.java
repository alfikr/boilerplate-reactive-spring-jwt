package com.jasavast.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.time.LocalDate;

@Configuration
public class SwaggerConfiguration {
    @Bean
    public Docket getDocket(){
        return new Docket(DocumentationType.SWAGGER_2)
                .genericModelSubstitutes(ResponseEntity.class)
                .ignoredParameterTypes(ServerHttpRequest.class)
                .directModelSubstitute(LocalDate.class,String.class)
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())

                .build();
    }
}
