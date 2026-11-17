package com.example.miniinsurance.quote.rating;

import com.example.miniinsurance.common.exception.InvalidInputException;
import com.example.miniinsurance.policy.PolicyType;
import com.example.miniinsurance.quote.dto.QuoteRequest;
import com.example.miniinsurance.quote.dto.TravelQuoteRequest;
import com.example.miniinsurance.quote.dto.TravelRegion;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.example.miniinsurance.quote.rating.RatingMath.*;

/**
 * Trip premium. Duration is priced in the base: 8 per 1,000 cover per started 7-day block,
 * multiplied by traveller count. Factors (region, oldest traveller, adventure, pre-existing)
 * then apply under the usual cap.
 */
@Component
public class TravelPremiumCalculator implements PremiumCalculator {

    private static final BigDecimal RATE_PER_1000_PER_WEEK = new BigDecimal("8");
    private static final long MAX_TRIP_DAYS = 180;
    private static final int MAX_DECLINE_AGE = 85;

    @Override
    public PolicyType policyType() {
        return PolicyType.TRAVEL;
    }

    @Override
    public void validate(QuoteRequest request, LocalDate today) {
        TravelQuoteRequest r = (TravelQuoteRequest) request;
        requireBetween(r.coverageAmount(), "5000", "500000", "coverageAmount");
        if (r.tripStart().isBefore(today)) {
            throw new InvalidInputException("tripStart cannot be in the past");
        }
        if (r.tripEnd().isBefore(r.tripStart())) {
            throw new InvalidInputException("tripEnd cannot be before tripStart");
        }
        if (tripDays(r) > MAX_TRIP_DAYS) {
            throw new InvalidInputException("Trip cannot exceed 180 days");
        }
    }

    @Override
    public RatingOutcome rate(QuoteRequest request, LocalDate today) {
        TravelQuoteRequest r = (TravelQuoteRequest) request;

        LocalDate oldestDob = r.travellerDatesOfBirth().stream()
                .min(Comparator.naturalOrder())
                .orElseThrow(() -> new InvalidInputException("At least one traveller is required"));
        int oldestAge = ageOn(oldestDob, today);
        int travellers = r.travellerDatesOfBirth().size();

        if (oldestAge > MAX_DECLINE_AGE) {
            return RatingOutcome.declined("TRAVELLER_OVER_85");
        }
        if (r.region() == TravelRegion.HIGH_RISK && r.adventureActivities()) {
            return RatingOutcome.declined("HIGH_RISK_REGION_WITH_ADVENTURE");
        }

        long weeks = (tripDays(r) + 6) / 7;
        BigDecimal base = basePremium(r.coverageAmount(), RATE_PER_1000_PER_WEEK)
                .multiply(BigDecimal.valueOf(weeks * travellers))
                .setScale(2, RoundingMode.HALF_UP);

        List<Factor> factors = new ArrayList<>();
        factors.add(regionFactor(r.region()));
        factors.add(ageBand(oldestAge));
        if (r.adventureActivities()) {
            factors.add(factor("Adventure activities", "1.5"));
        }
        if (r.preExistingConditions()) {
            factors.add(factor("Pre-existing conditions", "1.4"));
        }

        String baseLabel = "Base rate (8 per 1,000 cover per week, "
                + weeks + " week(s), " + travellers + " traveller(s))";
        return price(baseLabel, base, factors, new PolicyTerms(null, r.tripStart(), r.tripEnd()));
    }

    private static long tripDays(TravelQuoteRequest r) {
        return ChronoUnit.DAYS.between(r.tripStart(), r.tripEnd()) + 1;
    }

    private static Factor regionFactor(TravelRegion region) {
        return switch (region) {
            case DOMESTIC -> factor("Region: domestic", "1.0");
            case REGIONAL -> factor("Region: regional", "1.2");
            case WORLDWIDE -> factor("Region: worldwide", "1.8");
            case HIGH_RISK -> factor("Region: high risk", "2.5");
        };
    }
}
