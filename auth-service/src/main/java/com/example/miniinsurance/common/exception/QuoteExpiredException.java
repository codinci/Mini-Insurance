package com.example.miniinsurance.common.exception;

/** Thrown when a quote's validity window has passed. Maps to 410. */
public class QuoteExpiredException extends RuntimeException {

    public QuoteExpiredException(String message) {
        super(message);
    }
}
