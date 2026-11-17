package com.example.miniinsurance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    /** Injectable clock so expiry, age and trip rules are deterministic in tests. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
