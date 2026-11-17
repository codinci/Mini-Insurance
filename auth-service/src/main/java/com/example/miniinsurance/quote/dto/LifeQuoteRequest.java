package com.example.miniinsurance.quote.dto;

import com.example.miniinsurance.policy.PolicyType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = false)
public record LifeQuoteRequest(
        @NotNull @Past LocalDate dateOfBirth,
        @NotNull @Min(1) @Max(100) Integer termYears,
        @NotNull Boolean smoker,
        @NotNull Set<HealthCondition> conditions,
        @NotNull OccupationRisk occupationRisk,
        @NotNull @DecimalMin("0.01") BigDecimal sumAssured
) implements QuoteRequest {

    @Override
    public PolicyType policyType() {
        return PolicyType.LIFE;
    }

    @Override
    public BigDecimal coverage() {
        return sumAssured;
    }
}
