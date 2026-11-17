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

@JsonIgnoreProperties(ignoreUnknown = false)
public record MotorQuoteRequest(
        @NotNull @DecimalMin("0.01") BigDecimal vehicleValue,
        @NotNull @Min(1900) @Max(2100) Integer manufactureYear,
        @NotNull @Past LocalDate driverDateOfBirth,
        @NotNull @Min(0) @Max(100) Integer yearsLicensed,
        @NotNull VehicleUsage usage,
        @NotNull @Min(0) @Max(50) Integer atFaultClaimsLast3Years
) implements QuoteRequest {

    @Override
    public PolicyType policyType() {
        return PolicyType.MOTOR;
    }

    @Override
    public BigDecimal coverage() {
        return vehicleValue;
    }
}
