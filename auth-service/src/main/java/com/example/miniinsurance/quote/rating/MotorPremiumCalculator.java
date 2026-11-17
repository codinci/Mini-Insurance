package com.example.miniinsurance.quote.rating;

import com.example.miniinsurance.common.exception.InvalidInputException;
import com.example.miniinsurance.policy.PolicyType;
import com.example.miniinsurance.quote.dto.MotorQuoteRequest;
import com.example.miniinsurance.quote.dto.QuoteRequest;
import com.example.miniinsurance.quote.dto.VehicleUsage;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.example.miniinsurance.quote.rating.RatingMath.*;

@Component
public class MotorPremiumCalculator implements PremiumCalculator {

    private static final BigDecimal RATE_PER_1000 = new BigDecimal("25");
    private static final int MAX_VEHICLE_AGE_YEARS = 25;

    @Override
    public PolicyType policyType() {
        return PolicyType.MOTOR;
    }

    @Override
    public void validate(QuoteRequest request, LocalDate today) {
        MotorQuoteRequest r = (MotorQuoteRequest) request;
        requireBetween(r.vehicleValue(), "0.01", "10000000", "vehicleValue");
        if (r.manufactureYear() > today.getYear()) {
            throw new InvalidInputException("manufactureYear cannot be in the future");
        }
        int driverAge = ageOn(r.driverDateOfBirth(), today);
        if (r.yearsLicensed() > driverAge - 16) {
            throw new InvalidInputException("yearsLicensed cannot exceed driver age minus 16");
        }
    }

    @Override
    public RatingOutcome rate(QuoteRequest request, LocalDate today) {
        MotorQuoteRequest r = (MotorQuoteRequest) request;

        int driverAge = ageOn(r.driverDateOfBirth(), today);
        int vehicleAge = today.getYear() - r.manufactureYear();

        if (driverAge < 18) {
            return RatingOutcome.declined("DRIVER_UNDER_18");
        }
        if (r.atFaultClaimsLast3Years() >= 3) {
            return RatingOutcome.declined("THREE_OR_MORE_AT_FAULT_CLAIMS");
        }
        if (vehicleAge > MAX_VEHICLE_AGE_YEARS) {
            return RatingOutcome.declined("VEHICLE_TOO_OLD");
        }

        List<Factor> factors = new ArrayList<>();
        factors.add(vehicleAgeFactor(vehicleAge));
        if (driverAge < 25) {
            factors.add(factor("Driver under 25", "1.5"));
        }
        if (r.yearsLicensed() < 2) {
            factors.add(factor("Licensed under 2 years", "1.3"));
        }
        if (r.usage() == VehicleUsage.COMMERCIAL) {
            factors.add(factor("Commercial use", "1.25"));
        }
        if (r.atFaultClaimsLast3Years() > 0) {
            factors.add(new Factor(
                    "At-fault claims (" + r.atFaultClaimsLast3Years() + ")",
                    power(new BigDecimal("1.2"), r.atFaultClaimsLast3Years())
            ));
        }

        BigDecimal base = basePremium(r.vehicleValue(), RATE_PER_1000);
        return price("Base rate (25 per 1,000 vehicle value)", base, factors, null);
    }

    private static Factor vehicleAgeFactor(int vehicleAge) {
        if (vehicleAge <= 3) {
            return factor("Vehicle age 0–3 years", "1.0");
        }
        if (vehicleAge <= 10) {
            return factor("Vehicle age 4–10 years", "1.1");
        }
        return factor("Vehicle age 11–25 years", "1.3");
    }
}
