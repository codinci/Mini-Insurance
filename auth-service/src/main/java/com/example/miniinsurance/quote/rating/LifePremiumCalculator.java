package com.example.miniinsurance.quote.rating;

import com.example.miniinsurance.policy.PolicyType;
import com.example.miniinsurance.quote.dto.LifeQuoteRequest;
import com.example.miniinsurance.quote.dto.OccupationRisk;
import com.example.miniinsurance.quote.dto.QuoteRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.example.miniinsurance.quote.rating.RatingMath.*;

@Component
public class LifePremiumCalculator implements PremiumCalculator {

    private static final BigDecimal RATE_PER_1000 = new BigDecimal("4");
    private static final int MAX_DECLINE_TERM_YEARS = 40;

    @Override
    public PolicyType policyType() {
        return PolicyType.LIFE;
    }

    @Override
    public void validate(QuoteRequest request, LocalDate today) {
        LifeQuoteRequest r = (LifeQuoteRequest) request;
        requireBetween(r.sumAssured(), "25000", "5000000", "sumAssured");
    }

    @Override
    public RatingOutcome rate(QuoteRequest request, LocalDate today) {
        LifeQuoteRequest r = (LifeQuoteRequest) request;

        int age = ageOn(r.dateOfBirth(), today);
        if (age < 18 || age > 70) {
            return RatingOutcome.declined("LIFE_AGE_OUT_OF_RANGE");
        }
        if (r.termYears() > MAX_DECLINE_TERM_YEARS) {
            return RatingOutcome.declined("TERM_TOO_LONG");
        }

        List<Factor> factors = new ArrayList<>();
        factors.add(ageBand(age));
        factors.add(termFactor(r.termYears()));
        if (!r.conditions().isEmpty()) {
            factors.add(conditionsFactor(r.conditions()));
        }
        if (r.smoker()) {
            factors.add(factor("Smoker", "1.4"));
        }
        factors.add(occupationFactor(r.occupationRisk()));

        BigDecimal base = basePremium(r.sumAssured(), RATE_PER_1000);
        return price(
                "Base rate (4 per 1,000 sum assured)",
                base,
                factors,
                new PolicyTerms(r.termYears(), null, null)
        );
    }

    private static Factor termFactor(int termYears) {
        if (termYears <= 10) {
            return factor("Term up to 10 years", "1.0");
        }
        if (termYears <= 20) {
            return factor("Term 11–20 years", "1.2");
        }
        return factor("Term 21–40 years", "1.5");
    }

    private static Factor occupationFactor(OccupationRisk risk) {
        return switch (risk) {
            case LOW -> factor("Occupation: low hazard", "1.0");
            case MEDIUM -> factor("Occupation: medium hazard", "1.2");
            case HIGH -> factor("Occupation: high hazard", "1.6");
        };
    }
}
