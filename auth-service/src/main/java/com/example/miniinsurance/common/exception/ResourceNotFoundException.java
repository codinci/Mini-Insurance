package com.example.miniinsurance.common.exception;

/** Thrown when a resource does not exist or is not visible to the caller. Maps to 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
