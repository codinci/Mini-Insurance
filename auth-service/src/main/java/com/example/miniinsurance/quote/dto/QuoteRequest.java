package com.example.miniinsurance.quote.dto;

import com.example.miniinsurance.policy.PolicyType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Type-specific quote request, discriminated by {@code policyType}.
 * Clients never supply a premium, multiplier or status.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "policyType",
        visible = false
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = HealthQuoteRequest.class, name = "HEALTH"),
        @JsonSubTypes.Type(value = MotorQuoteRequest.class, name = "MOTOR"),
        @JsonSubTypes.Type(value = HomeQuoteRequest.class, name = "HOME"),
        @JsonSubTypes.Type(value = TravelQuoteRequest.class, name = "TRAVEL"),
        @JsonSubTypes.Type(value = LifeQuoteRequest.class, name = "LIFE")
})
@Schema(
        description = "Rating inputs for one policy type. The policyType field selects the shape.",
        discriminatorProperty = "policyType",
        oneOf = {
                HealthQuoteRequest.class,
                MotorQuoteRequest.class,
                HomeQuoteRequest.class,
                TravelQuoteRequest.class,
                LifeQuoteRequest.class
        }
)
public interface QuoteRequest {

    PolicyType policyType();

    /** Insured amount used as the base for pricing. */
    BigDecimal coverage();
}
