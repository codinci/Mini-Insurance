package com.example.miniinsurance.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;

@Configuration
public class JacksonConfig {

    /**
     * Rejects unknown JSON properties in request bodies, so a client-supplied premium or any
     * other stray field fails with 400 instead of being silently dropped.
     * Applies to all JSON request bodies, including /api/auth.
     */
    @Bean
    public JsonMapperBuilderCustomizer strictUnknownPropertiesCustomizer() {
        return builder -> builder.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
