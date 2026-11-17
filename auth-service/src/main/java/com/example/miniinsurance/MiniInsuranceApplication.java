package com.example.miniinsurance;

import com.example.miniinsurance.security.JwtConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtConfig.class)
public class MiniInsuranceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniInsuranceApplication.class, args);
    }

}
