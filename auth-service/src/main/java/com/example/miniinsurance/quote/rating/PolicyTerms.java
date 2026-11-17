package com.example.miniinsurance.quote.rating;

import java.time.LocalDate;

/**
 * Term data a type needs at purchase time. Only the fields relevant to the type are set.
 * LIFE: termYears. TRAVEL: tripStart and tripEnd.
 */
public record PolicyTerms(Integer termYears, LocalDate tripStart, LocalDate tripEnd) {
}
