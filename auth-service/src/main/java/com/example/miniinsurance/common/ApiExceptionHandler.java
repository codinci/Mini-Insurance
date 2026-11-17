package com.example.miniinsurance.common;

import com.example.miniinsurance.common.ApiErrorResponse.FieldViolation;
import com.example.miniinsurance.common.exception.BusinessRuleException;
import com.example.miniinsurance.common.exception.ConflictException;
import com.example.miniinsurance.common.exception.InvalidInputException;
import com.example.miniinsurance.common.exception.QuoteExpiredException;
import com.example.miniinsurance.common.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

/**
 * Global error mapping. Messages here are fixed or come from our own exceptions;
 * submitted input values and stack traces are never returned to the client.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(404, "Not Found", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        return build(409, "Conflict", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ApiErrorResponse> handleConcurrentModification(RuntimeException ex, HttpServletRequest request) {
        return build(409, "Conflict", "The resource was changed by another request. Please retry.", request, List.of());
    }

    @ExceptionHandler(QuoteExpiredException.class)
    public ResponseEntity<ApiErrorResponse> handleExpired(QuoteExpiredException ex, HttpServletRequest request) {
        return build(410, "Gone", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        return build(422, "Unprocessable Content", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidInput(InvalidInputException ex, HttpServletRequest request) {
        return build(400, "Bad Request", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldViolation(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return build(400, "Bad Request", "Validation failed", request, violations);
    }

    /** Covers malformed JSON, unknown fields, unknown policyType and missing discriminator. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(400, "Bad Request", "Malformed or unsupported request body", request, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return build(400, "Bad Request", "Invalid value for parameter '" + ex.getName() + "'", request, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(403, "Forbidden", "Access denied", request, List.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return build(401, "Unauthorized", "Authentication required", request, List.of());
    }

    /** Unknown route: keep 404 rather than falling through to the generic 500 handler. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(404, "Not Found", "Resource not found", request, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return build(405, "Method Not Allowed", "HTTP method not supported for this resource", request, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return build(415, "Unsupported Media Type", "Content type not supported", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        // Log the exception type only: messages may echo submitted data.
        log.error("Unhandled {} on {}", ex.getClass().getName(), request.getRequestURI());
        return build(500, "Internal Server Error", "An unexpected error occurred", request, List.of());
    }

    private ResponseEntity<ApiErrorResponse> build(
            int status,
            String error,
            String message,
            HttpServletRequest request,
            List<FieldViolation> fieldErrors
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status,
                error,
                message,
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.valueOf(status)).body(body);
    }
}
