package com.example.miniinsurance.common.exception;

/** Thrown when a well-formed request violates a business rule, e.g. declined quote. Maps to 422. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
