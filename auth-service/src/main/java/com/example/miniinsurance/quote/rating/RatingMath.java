package com.example.miniinsurance.quote.rating;

import com.example.miniinsurance.common.exception.InvalidInputException;
import com.example.miniinsurance.quote.dto.BreakdownLine;
import com.example.miniinsurance.quote.dto.HealthCondition;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Shared rating arithmetic and factor bands. All money and factors are BigDecimal.
 * Rounding: money to 2 dp, multipliers to 4 dp, all HALF_UP.
 */
public final class RatingMath {

    /** Total multiplier cap applied to factors (not to the base). */
    public static final BigDecimal MAX_TOTAL_MULTIPLIER = new BigDecimal("6.00");

    /** Floor applied to the final premium. */
    public static final BigDecimal MINIMUM_PREMIUM = new BigDecimal("10.00");

    private static final BigDecimal THOUSAND = new BigDecimal("1000");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal CONDITION_FACTOR_PER_ITEM = new BigDecimal("1.15");
    private static final BigDecimal CONDITION_FACTOR_CAP = new BigDecimal("2.5");
    private static final BigDecimal PRIOR_CLAIM_FACTOR = new BigDecimal("1.15");
    private static final int MONEY_SCALE = 2;
    private static final int MULTIPLIER_SCALE = 4;

    private RatingMath() {
    }

    /** Base premium = coverage / 1,000 × ratePer1000, to 2 dp. */
    public static BigDecimal basePremium(BigDecimal coverage, BigDecimal ratePer1000) {
        return coverage.divide(THOUSAND, 10, RoundingMode.HALF_UP)
                .multiply(ratePer1000)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Applies factors to the base, then the total-multiplier cap, then the minimum premium.
     * Each adjustment is recorded in the breakdown so the customer can see why.
     */
    public static RatingOutcome price(String baseLabel, BigDecimal base, List<Factor> factors, PolicyTerms terms) {
        List<BreakdownLine> lines = new ArrayList<>();
        lines.add(new BreakdownLine(baseLabel, null, base.setScale(MONEY_SCALE, RoundingMode.HALF_UP)));

        BigDecimal multiplier = BigDecimal.ONE.setScale(MULTIPLIER_SCALE, RoundingMode.HALF_UP);
        for (Factor factor : factors) {
            multiplier = multiplier.multiply(factor.multiplier()).setScale(MULTIPLIER_SCALE, RoundingMode.HALF_UP);
            lines.add(new BreakdownLine(factor.label(), factor.multiplier(), null));
        }

        if (multiplier.compareTo(MAX_TOTAL_MULTIPLIER) > 0) {
            multiplier = MAX_TOTAL_MULTIPLIER;
            lines.add(new BreakdownLine("Total multiplier capped at 6.00", MAX_TOTAL_MULTIPLIER, null));
        }

        BigDecimal premium = base.multiply(multiplier).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (premium.compareTo(MINIMUM_PREMIUM) < 0) {
            premium = MINIMUM_PREMIUM;
            lines.add(new BreakdownLine("Minimum premium applied", null, premium));
        }

        lines.add(new BreakdownLine("Annual premium", null, premium));
        return new RatingOutcome(premium, List.copyOf(lines), null, terms);
    }

    public static Factor factor(String label, String multiplier) {
        return new Factor(label, new BigDecimal(multiplier));
    }

    public static int ageOn(LocalDate dateOfBirth, LocalDate today) {
        return Period.between(dateOfBirth, today).getYears();
    }

    /** Age bands: under 30 ×0.9, 30–39 ×1.0, 40–49 ×1.3, 50–59 ×1.8, 60 and over ×2.6. */
    public static Factor ageBand(int age) {
        if (age < 30) {
            return factor("Age band under 30", "0.9");
        }
        if (age < 40) {
            return factor("Age band 30–39", "1.0");
        }
        if (age < 50) {
            return factor("Age band 40–49", "1.3");
        }
        if (age < 60) {
            return factor("Age band 50–59", "1.8");
        }
        return factor("Age band 60 and over", "2.6");
    }

    /** Cumulative ×1.15 per condition, capped at ×2.5. */
    public static Factor conditionsFactor(Set<HealthCondition> conditions) {
        BigDecimal multiplier = power(CONDITION_FACTOR_PER_ITEM, conditions.size());
        if (multiplier.compareTo(CONDITION_FACTOR_CAP) > 0) {
            multiplier = CONDITION_FACTOR_CAP;
        }
        return new Factor("Conditions (" + conditions.size() + ")", multiplier);
    }

    /** Prior claims compound ×1.15 each. */
    public static Factor priorClaimsFactor(int claims) {
        return new Factor("Prior claims (" + claims + ")", power(PRIOR_CLAIM_FACTOR, claims));
    }

    /** BMI = weight / height², rounded to 1 dp (HALF_UP). */
    public static BigDecimal bmi(BigDecimal heightCm, BigDecimal weightKg) {
        BigDecimal heightM = heightCm.divide(HUNDRED, 4, RoundingMode.HALF_UP);
        return weightKg.divide(heightM.multiply(heightM), 1, RoundingMode.HALF_UP);
    }

    /** BMI bands: under 18.5 ×1.1, 18.5–24.9 ×1.0, 25–29.9 ×1.15, 30 and over ×1.4. */
    public static Factor bmiBand(BigDecimal bmi) {
        if (bmi.compareTo(new BigDecimal("18.5")) < 0) {
            return factor("BMI band under 18.5", "1.1");
        }
        if (bmi.compareTo(new BigDecimal("25")) < 0) {
            return factor("BMI band 18.5–24.9", "1.0");
        }
        if (bmi.compareTo(new BigDecimal("30")) < 0) {
            return factor("BMI band 25–29.9", "1.15");
        }
        return factor("BMI band 30 and over", "1.4");
    }

    /** Declarative input check used by calculators. Message names the field only, never the value. */
    public static void requireBetween(BigDecimal value, String min, String max, String field) {
        if (value.compareTo(new BigDecimal(min)) < 0 || value.compareTo(new BigDecimal(max)) > 0) {
            throw new InvalidInputException(field + " must be between " + min + " and " + max);
        }
    }

    static BigDecimal power(BigDecimal base, int exponent) {
        BigDecimal result = BigDecimal.ONE.setScale(MULTIPLIER_SCALE, RoundingMode.HALF_UP);
        for (int i = 0; i < exponent; i++) {
            result = result.multiply(base).setScale(MULTIPLIER_SCALE, RoundingMode.HALF_UP);
        }
        return result;
    }
}
