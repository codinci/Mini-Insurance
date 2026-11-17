package com.example.miniinsurance.quote.rating;

import java.math.BigDecimal;

/** A named multiplier applied to the base premium. */
public record Factor(String label, BigDecimal multiplier) {
}
