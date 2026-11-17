package com.example.miniinsurance.quote.dto;

import com.example.miniinsurance.policy.PolicyType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = false)
public record TravelQuoteRequest(
        @NotNull TravelRegion region,
        @NotNull LocalDate tripStart,
        @NotNull LocalDate tripEnd,
        @NotNull @Size(min = 1, max = 10) List<@NotNull @Past LocalDate> travellerDatesOfBirth,
        @NotNull Boolean preExistingConditions,
        @NotNull Boolean adventureActivities,
        @NotNull @DecimalMin("0.01") BigDecimal coverageAmount
) implements QuoteRequest {

    @Override
    public PolicyType policyType() {
        return PolicyType.TRAVEL;
    }

    @Override
    public BigDecimal coverage() {
        return coverageAmount;
    }
}
