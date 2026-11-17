package com.example.miniinsurance.common.exception;

/** Thrown when the request conflicts with current state, e.g. a quote already purchased. Maps to 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
