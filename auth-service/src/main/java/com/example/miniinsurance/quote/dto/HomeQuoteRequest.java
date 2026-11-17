package com.example.miniinsurance.quote.dto;

import com.example.miniinsurance.policy.PolicyType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = false)
public record HomeQuoteRequest(
        @NotNull @DecimalMin("0.01") BigDecimal propertyValue,
        @NotNull ConstructionType constructionType,
        @NotNull @Min(0) @Max(300) Integer propertyAgeYears,
        @NotNull LocationRisk locationRisk,
        @NotNull Boolean hasSecuritySystem,
        @NotNull @Min(0) @Max(50) Integer priorClaims
) implements QuoteRequest {

    @Override
    public PolicyType policyType() {
        return PolicyType.HOME;
    }

    @Override
    public BigDecimal coverage() {
        return propertyValue;
    }
}
