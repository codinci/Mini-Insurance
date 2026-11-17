package com.example.miniinsurance.quote.rating;

import com.example.miniinsurance.common.exception.InvalidInputException;
import com.example.miniinsurance.quote.dto.ConstructionType;
import com.example.miniinsurance.quote.dto.HealthCondition;
import com.example.miniinsurance.quote.dto.HealthQuoteRequest;
import com.example.miniinsurance.quote.dto.HomeQuoteRequest;
import com.example.miniinsurance.quote.dto.LifeQuoteRequest;
import com.example.miniinsurance.quote.dto.LocationRisk;
import com.example.miniinsurance.quote.dto.MotorQuoteRequest;
import com.example.miniinsurance.quote.dto.OccupationRisk;
import com.example.miniinsurance.quote.dto.TravelQuoteRequest;
import com.example.miniinsurance.quote.dto.TravelRegion;
import com.example.miniinsurance.quote.dto.VehicleUsage;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Per-type rating rules: decline outcomes, validation, and baseline prices. */
class CalculatorRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 17);

    private final HealthPremiumCalculator health = new HealthPremiumCalculator();
    private final MotorPremiumCalculator motor = new MotorPremiumCalculator();
    private final HomePremiumCalculator home = new HomePremiumCalculator();
    private final TravelPremiumCalculator travel = new TravelPremiumCalculator();
    private final LifePremiumCalculator life = new LifePremiumCalculator();

    // ---- HEALTH ----

    @Test
    void healthDeclinesOutsideAgeRange() {
        HealthQuoteRequest r = health(LocalDate.of(1946, 1, 1), "180", "71.3", false, Set.of(), "100000");

        assertThat(health.rate(r, TODAY).declineReason()).isEqualTo("HEALTH_AGE_OUT_OF_RANGE");
    }

    @Test
    void healthDeclinesCancerHistoryWithHeartDisease() {
        HealthQuoteRequest r = health(
                LocalDate.of(1990, 1, 1), "180", "71.3", false,
                Set.of(HealthCondition.CANCER_HISTORY, HealthCondition.HEART_DISEASE), "100000");

        assertThat(health.rate(r, TODAY).declineReason()).isEqualTo("CANCER_HISTORY_WITH_HEART_DISEASE");
    }

    @Test
    void healthBaselinePriceForAgeThirtyOneNonSmokerHealthyBmi() {
        // 100,000 / 1,000 × 12 = 1,200; age 31 ×1.0; BMI 22.0 ×1.0
        RatingOutcome outcome = health.rate(health(LocalDate.of(1995, 6, 1), "180", "71.3", false, Set.of(), "100000"), TODAY);

        assertThat(outcome.isDeclined()).isFalse();
        assertThat(outcome.annualPremium()).isEqualByComparingTo("1200.00");
    }

    @Test
    void healthSmokerSurcharge() {
        RatingOutcome outcome = health.rate(health(LocalDate.of(1995, 6, 1), "180", "71.3", true, Set.of(), "100000"), TODAY);

        assertThat(outcome.annualPremium()).isEqualByComparingTo("1680.00");
    }

    @Test
    void healthOverweightBmiBand() {
        // BMI 27.8 falls in 25–29.9 ×1.15
        RatingOutcome outcome = health.rate(health(LocalDate.of(1995, 6, 1), "180", "90", false, Set.of(), "100000"), TODAY);

        assertThat(outcome.annualPremium()).isEqualByComparingTo("1380.00");
    }

    @Test
    void healthRejectsCoverageOutsideLimits() {
        HealthQuoteRequest r = health(LocalDate.of(1995, 6, 1), "180", "71.3", false, Set.of(), "5000");

        assertThatThrownBy(() -> health.validate(r, TODAY)).isInstanceOf(InvalidInputException.class);
    }

    // ---- MOTOR ----

    @Test
    void motorDeclinesVehicleOlderThanTwentyFiveYears() {
        MotorQuoteRequest r = motor(2000, 0, 1985, 20, VehicleUsage.PRIVATE);

        assertThat(motor.rate(r, TODAY).declineReason()).isEqualTo("VEHICLE_TOO_OLD");
    }

    @Test
    void motorDeclinesThreeAtFaultClaims() {
        MotorQuoteRequest r = motor(2024, 3, 1985, 20, VehicleUsage.PRIVATE);

        assertThat(motor.rate(r, TODAY).declineReason()).isEqualTo("THREE_OR_MORE_AT_FAULT_CLAIMS");
    }

    @Test
    void motorBaselinePrice() {
        // 20,000 / 1,000 × 25 = 500; vehicle age 2 ×1.0; driver 41 ×1.0; licensed 20y ×1.0
        RatingOutcome outcome = motor.rate(motor(2024, 0, 1985, 20, VehicleUsage.PRIVATE), TODAY);

        assertThat(outcome.annualPremium()).isEqualByComparingTo("500.00");
    }

    @Test
    void motorRejectsFutureManufactureYear() {
        MotorQuoteRequest r = motor(2027, 0, 1985, 20, VehicleUsage.PRIVATE);

        assertThatThrownBy(() -> motor.validate(r, TODAY)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void motorRejectsYearsLicensedBeyondDriverAgeMinusSixteen() {
        MotorQuoteRequest r = motor(2024, 0, 2010, 5, VehicleUsage.PRIVATE);

        assertThatThrownBy(() -> motor.validate(r, TODAY)).isInstanceOf(InvalidInputException.class);
    }

    // ---- HOME ----

    @Test
    void homeDeclinesThreePriorClaimsInHighRiskLocation() {
        HomeQuoteRequest r = home(3, LocationRisk.HIGH);

        assertThat(home.rate(r, TODAY).declineReason()).isEqualTo("PRIOR_CLAIMS_IN_HIGH_RISK_LOCATION");
    }

    @Test
    void homeBaselinePrice() {
        // 200,000 / 1,000 × 3 = 600; brick ×1.0; age ≤20 ×1.0; medium ×1.0
        RatingOutcome outcome = home.rate(home(0, LocationRisk.MEDIUM), TODAY);

        assertThat(outcome.annualPremium()).isEqualByComparingTo("600.00");
    }

    // ---- TRAVEL ----

    @Test
    void travelRejectsEndBeforeStart() {
        TravelQuoteRequest r = travel(LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 5), 1900);

        assertThatThrownBy(() -> travel.validate(r, TODAY)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void travelRejectsTripLongerThanOneHundredEightyDays() {
        TravelQuoteRequest r = travel(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 8, 1), 1990);

        assertThatThrownBy(() -> travel.validate(r, TODAY)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void travelRejectsStartInThePast() {
        TravelQuoteRequest r = travel(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5), 1990);

        assertThatThrownBy(() -> travel.validate(r, TODAY)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void travelDeclinesTravellerOverEightyFive() {
        TravelQuoteRequest r = travel(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 7), 1930);

        assertThat(travel.rate(r, TODAY).declineReason()).isEqualTo("TRAVELLER_OVER_85");
    }

    @Test
    void travelBaselinePriceForOneWeekOneTraveller() {
        // 10,000 / 1,000 × 8 = 80 × 1 week × 1 traveller; oldest age 30 ×1.0; domestic ×1.0
        RatingOutcome outcome = travel.rate(travel(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 7), 1996), TODAY);

        assertThat(outcome.annualPremium()).isEqualByComparingTo("80.00");
        assertThat(outcome.terms().tripStart()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(outcome.terms().tripEnd()).isEqualTo(LocalDate.of(2027, 1, 7));
    }

    @Test
    void travelDurationIsChargedPerStartedWeek() {
        // 8 days = 2 started weeks → 160
        RatingOutcome outcome = travel.rate(travel(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 8), 1996), TODAY);

        assertThat(outcome.annualPremium()).isEqualByComparingTo("160.00");
    }

    // ---- LIFE ----

    @Test
    void lifeDeclinesTermOverForty() {
        LifeQuoteRequest r = life(41, LocalDate.of(1990, 1, 1));

        assertThat(life.rate(r, TODAY).declineReason()).isEqualTo("TERM_TOO_LONG");
    }

    @Test
    void lifeBaselinePriceAndTerms() {
        // 100,000 / 1,000 × 4 = 400; age 36 ×1.0; term ≤10 ×1.0; occupation LOW ×1.0
        RatingOutcome outcome = life.rate(life(10, LocalDate.of(1990, 1, 1)), TODAY);

        assertThat(outcome.annualPremium()).isEqualByComparingTo("400.00");
        assertThat(outcome.terms().termYears()).isEqualTo(10);
    }

    // ---- builders ----

    private static HealthQuoteRequest health(
            LocalDate dob, String height, String weight, boolean smoker, Set<HealthCondition> conditions, String coverage) {
        return new HealthQuoteRequest(
                dob, new BigDecimal(height), new BigDecimal(weight), smoker, conditions, new BigDecimal(coverage));
    }

    private static MotorQuoteRequest motor(int manufactureYear, int atFault, int driverBirthYear, int yearsLicensed, VehicleUsage usage) {
        return new MotorQuoteRequest(
                new BigDecimal("20000"), manufactureYear, LocalDate.of(driverBirthYear, 1, 1),
                yearsLicensed, usage, atFault);
    }

    private static HomeQuoteRequest home(int priorClaims, LocationRisk location) {
        return new HomeQuoteRequest(
                new BigDecimal("200000"), ConstructionType.BRICK, 10, location, false, priorClaims);
    }

    private static TravelQuoteRequest travel(LocalDate start, LocalDate end, int travellerBirthYear) {
        return new TravelQuoteRequest(
                TravelRegion.DOMESTIC,
                start,
                end,
                List.of(LocalDate.of(travellerBirthYear, 1, 1)),
                false,
                false,
                new BigDecimal("10000")
        );
    }

    private static LifeQuoteRequest life(int termYears, LocalDate dob) {
        return new LifeQuoteRequest(
                dob, termYears, false, Set.of(), OccupationRisk.LOW, new BigDecimal("100000"));
    }
}
