package com.example.miniinsurance.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI insuranceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mini Insurance Claims Management API")
                        .description("Backend API for managing insurance policies, claims, reviews, and settlements")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("codinci")
                                .email("dkibau@gmail.com"))
                        .license(new License()
                                .name("MIT License")));
    }
}