package com.example.miniinsurance.quote.rating;

import com.example.miniinsurance.policy.PolicyType;
import com.example.miniinsurance.quote.dto.HealthCondition;
import com.example.miniinsurance.quote.dto.HealthQuoteRequest;
import com.example.miniinsurance.quote.dto.QuoteRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.example.miniinsurance.quote.rating.RatingMath.*;

@Component
public class HealthPremiumCalculator implements PremiumCalculator {

    private static final BigDecimal RATE_PER_1000 = new BigDecimal("12");

    @Override
    public PolicyType policyType() {
        return PolicyType.HEALTH;
    }

    @Override
    public void validate(QuoteRequest request, LocalDate today) {
        HealthQuoteRequest r = (HealthQuoteRequest) request;
        requireBetween(r.coverageAmount(), "10000", "1000000", "coverageAmount");
    }

    @Override
    public RatingOutcome rate(QuoteRequest request, LocalDate today) {
        HealthQuoteRequest r = (HealthQuoteRequest) request;

        int age = ageOn(r.dateOfBirth(), today);
        if (age < 18 || age > 75) {
            return RatingOutcome.declined("HEALTH_AGE_OUT_OF_RANGE");
        }
        if (r.conditions().contains(HealthCondition.CANCER_HISTORY)
                && r.conditions().contains(HealthCondition.HEART_DISEASE)) {
            return RatingOutcome.declined("CANCER_HISTORY_WITH_HEART_DISEASE");
        }

        BigDecimal bmi = bmi(r.heightCm(), r.weightKg());
        List<Factor> factors = new ArrayList<>();
        factors.add(ageBand(age));
        factors.add(bmiBand(bmi));
        if (!r.conditions().isEmpty()) {
            factors.add(conditionsFactor(r.conditions()));
        }
        if (r.smoker()) {
            factors.add(factor("Smoker", "1.4"));
        }

        BigDecimal base = basePremium(r.coverageAmount(), RATE_PER_1000);
        return price("Base rate (12 per 1,000 cover)", base, factors, null);
    }
}
