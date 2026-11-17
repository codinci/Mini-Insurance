package com.example.miniinsurance.quote.rating;

import com.example.miniinsurance.policy.PolicyType;
import com.example.miniinsurance.quote.dto.QuoteRequest;

import java.time.LocalDate;

/**
 * One implementation per policy type. Pure logic: no persistence, no clock.
 * The caller supplies {@code today} so age and date rules are deterministic.
 * Implementations cast the request to their own type; the registry guarantees the match.
 */
public interface PremiumCalculator {

    PolicyType policyType();

    /** Business-range checks that Bean Validation cannot express. Throws InvalidInputException (400). */
    void validate(QuoteRequest request, LocalDate today);

    /** Applies decline rules, then prices. Never throws for an out-of-appetite risk; returns a declined outcome. */
    RatingOutcome rate(QuoteRequest request, LocalDate today);
}
