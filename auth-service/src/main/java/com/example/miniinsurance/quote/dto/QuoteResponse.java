package com.example.miniinsurance.quote.dto;

import com.example.miniinsurance.policy.PolicyType;
import com.example.miniinsurance.quote.QuoteStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record QuoteResponse(
        Long id,
        PolicyType policyType,
        QuoteStatus status,
        BigDecimal annualPremium,
        String currency,
        BigDecimal coverageAmount,
        List<BreakdownLine> breakdown,
        LocalDateTime expiresAt,
        String declineReason
) {
}
