package com.example.miniinsurance.quote.rating;

import com.example.miniinsurance.policy.PolicyType;
import com.example.miniinsurance.quote.dto.ConstructionType;
import com.example.miniinsurance.quote.dto.HomeQuoteRequest;
import com.example.miniinsurance.quote.dto.LocationRisk;
import com.example.miniinsurance.quote.dto.QuoteRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.example.miniinsurance.quote.rating.RatingMath.*;

@Component
public class HomePremiumCalculator implements PremiumCalculator {

    private static final BigDecimal RATE_PER_1000 = new BigDecimal("3");

    @Override
    public PolicyType policyType() {
        return PolicyType.HOME;
    }

    @Override
    public void validate(QuoteRequest request, LocalDate today) {
        HomeQuoteRequest r = (HomeQuoteRequest) request;
        requireBetween(r.propertyValue(), "0.01", "10000000", "propertyValue");
    }

    @Override
    public RatingOutcome rate(QuoteRequest request, LocalDate today) {
        HomeQuoteRequest r = (HomeQuoteRequest) request;

        if (r.priorClaims() >= 3 && r.locationRisk() == LocationRisk.HIGH) {
            return RatingOutcome.declined("PRIOR_CLAIMS_IN_HIGH_RISK_LOCATION");
        }

        List<Factor> factors = new ArrayList<>();
        factors.add(constructionFactor(r.constructionType()));
        factors.add(propertyAgeFactor(r.propertyAgeYears()));
        factors.add(locationFactor(r.locationRisk()));
        if (r.hasSecuritySystem()) {
            factors.add(factor("Security system", "0.9"));
        }
        if (r.priorClaims() > 0) {
            factors.add(priorClaimsFactor(r.priorClaims()));
        }

        BigDecimal base = basePremium(r.propertyValue(), RATE_PER_1000);
        return price("Base rate (3 per 1,000 property value)", base, factors, null);
    }

    private static Factor constructionFactor(ConstructionType type) {
        return switch (type) {
            case BRICK -> factor("Construction: brick", "1.0");
            case CONCRETE -> factor("Construction: concrete", "0.9");
            case WOOD -> factor("Construction: wood", "1.3");
            case OTHER -> factor("Construction: other", "1.1");
        };
    }

    private static Factor propertyAgeFactor(int ageYears) {
        if (ageYears <= 20) {
            return factor("Property age 0–20 years", "1.0");
        }
        if (ageYears <= 50) {
            return factor("Property age 21–50 years", "1.1");
        }
        return factor("Property age over 50 years", "1.25");
    }

    private static Factor locationFactor(LocationRisk risk) {
        return switch (risk) {
            case LOW -> factor("Location risk: low", "0.9");
            case MEDIUM -> factor("Location risk: medium", "1.0");
            case HIGH -> factor("Location risk: high", "1.4");
        };
    }
}
