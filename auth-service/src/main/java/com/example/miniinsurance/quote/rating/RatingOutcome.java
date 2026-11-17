package com.example.miniinsurance.quote.rating;

import com.example.miniinsurance.quote.dto.BreakdownLine;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result of rating one request. Either priced (premium, breakdown, optional terms)
 * or declined (reason code only).
 */
public record RatingOutcome(
        BigDecimal annualPremium,
        List<BreakdownLine> breakdown,
        String declineReason,
        PolicyTerms terms
) {

    public static RatingOutcome declined(String reason) {
        return new RatingOutcome(null, List.of(), reason, null);
    }

    public boolean isDeclined() {
        return declineReason != null;
    }
}
