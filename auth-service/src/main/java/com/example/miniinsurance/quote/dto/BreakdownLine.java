package com.example.miniinsurance.quote.dto;

import java.math.BigDecimal;

/**
 * One line of the premium explanation.
 * factor: multiplier applied (null for base and total lines).
 * amount: premium value at a base or adjustment line (null for plain factor lines).
 */
public record BreakdownLine(String label, BigDecimal factor, BigDecimal amount) {
}
