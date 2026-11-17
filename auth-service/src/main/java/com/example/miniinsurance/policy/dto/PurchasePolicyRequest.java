package com.example.miniinsurance.policy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Purchase a policy from a stored quote. Type, coverage and premium come from the quote,
 * so they are not accepted here. Unknown fields (for example a client-supplied premium) are rejected.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record PurchasePolicyRequest(

        @NotNull(message = "Quote id is required")
        Long quoteId,

        @NotNull(message = "Start date is required")
        LocalDate startDate
) {
}
