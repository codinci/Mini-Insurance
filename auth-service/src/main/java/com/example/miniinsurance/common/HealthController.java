package com.example.miniinsurance.common;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Operation(summary = "Check API health")
    @GetMapping
    public String health() {
        return "Insurance API is running";
    }
}