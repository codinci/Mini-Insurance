package com.example.miniinsurance.quote.rating;

import com.example.miniinsurance.common.exception.InvalidInputException;
import com.example.miniinsurance.quote.dto.HealthCondition;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static com.example.miniinsurance.quote.rating.RatingMath.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RatingMathTest {

    @Test
    void capsTotalMultiplierAtSix() {
        RatingOutcome outcome = price(
                "base",
                new BigDecimal("100.00"),
                List.of(factor("a", "2"), factor("b", "2"), factor("c", "2")),
                null
        );

        assertThat(outcome.annualPremium()).isEqualByComparingTo("600.00");
        assertThat(outcome.breakdown())
                .anySatisfy(line -> assertThat(line.label()).contains("capped"));
    }

    @Test
    void appliesMinimumPremiumFloor() {
        RatingOutcome outcome = price("base", new BigDecimal("5.00"), List.of(factor("none", "1.0")), null);

        assertThat(outcome.annualPremium()).isEqualByComparingTo("10.00");
        assertThat(outcome.breakdown())
                .anySatisfy(line -> assertThat(line.label()).isEqualTo("Minimum premium applied"));
    }

    @Test
    void computesBmiToOneDecimalPlace() {
        assertThat(bmi(new BigDecimal("180"), new BigDecimal("90"))).isEqualByComparingTo("27.8");
    }

    @Test
    void bmiBandBoundaries() {
        assertThat(bmiBand(new BigDecimal("18.4")).multiplier()).isEqualByComparingTo("1.1");
        assertThat(bmiBand(new BigDecimal("24.9")).multiplier()).isEqualByComparingTo("1.0");
        assertThat(bmiBand(new BigDecimal("25.0")).multiplier()).isEqualByComparingTo("1.15");
        assertThat(bmiBand(new BigDecimal("30.0")).multiplier()).isEqualByComparingTo("1.4");
    }

    @Test
    void ageBandBoundariesAreLowerInclusive() {
        assertThat(ageBand(29).multiplier()).isEqualByComparingTo("0.9");
        assertThat(ageBand(30).multiplier()).isEqualByComparingTo("1.0");
        assertThat(ageBand(40).multiplier()).isEqualByComparingTo("1.3");
        assertThat(ageBand(50).multiplier()).isEqualByComparingTo("1.8");
        assertThat(ageBand(60).multiplier()).isEqualByComparingTo("2.6");
    }

    @Test
    void ageIsCalculatedFromDateOfBirth() {
        assertThat(ageOn(LocalDate.of(1995, 11, 18), LocalDate.of(2026, 11, 17))).isEqualTo(30);
        assertThat(ageOn(LocalDate.of(1995, 11, 17), LocalDate.of(2026, 11, 17))).isEqualTo(31);
    }

    @Test
    void conditionsCompoundAtFifteenPercent() {
        Factor conditions = conditionsFactor(Set.of(HealthCondition.ASTHMA, HealthCondition.DIABETES));

        assertThat(conditions.multiplier()).isEqualByComparingTo("1.3225");
    }

    @Test
    void requireBetweenRejectsOutOfRangeWithoutEchoingValue() {
        assertThatThrownBy(() -> requireBetween(new BigDecimal("999999999"), "1", "100", "coverageAmount"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("coverageAmount must be between 1 and 100");
    }
}
