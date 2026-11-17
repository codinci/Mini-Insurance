package com.example.miniinsurance.policy.dto;

import com.example.miniinsurance.policy.PolicyStatus;
import com.example.miniinsurance.policy.PolicyType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PolicyResponse(
        Long id,
        String policyNumber,
        PolicyType policyType,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal premium,
        BigDecimal coverageAmount,
        PolicyStatus status
) {
}