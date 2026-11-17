package com.example.miniinsurance.common.exception;

/**
 * Thrown for cross-field input validation that Bean Validation cannot express.
 * Messages MUST NOT include submitted values. Maps to 400.
 */
public class InvalidInputException extends RuntimeException {

    public InvalidInputException(String message) {
        super(message);
    }
}
