package com.example.miniinsurance.quote.dto;

import com.example.miniinsurance.policy.PolicyType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = false)
public record HealthQuoteRequest(
        @NotNull @Past LocalDate dateOfBirth,
        @NotNull @DecimalMin("100") @DecimalMax("250") BigDecimal heightCm,
        @NotNull @DecimalMin("30") @DecimalMax("300") BigDecimal weightKg,
        @NotNull Boolean smoker,
        @NotNull Set<HealthCondition> conditions,
        @NotNull @DecimalMin("0.01") BigDecimal coverageAmount
) implements QuoteRequest {

    @Override
    public PolicyType policyType() {
        return PolicyType.HEALTH;
    }

    @Override
    public BigDecimal coverage() {
        return coverageAmount;
    }
}
