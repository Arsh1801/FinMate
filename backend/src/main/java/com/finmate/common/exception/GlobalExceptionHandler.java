package com.finmate.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        log.warn("API Exception [traceId={}]: status={}, code={}, message={}",
                traceId, ex.getStatus(), ex.getErrorCode(), ex.getMessage());

        ErrorResponse response = new ErrorResponse(
                ex.getStatus().value(),
                ex.getErrorCode().name(),
                ex.getMessage(),
                request.getRequestURI(),
                traceId,
                ex.getDetails()
        );
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        List<ValidationErrorDetail> details = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.add(new ValidationErrorDetail(fieldError.getField(), fieldError.getDefaultMessage()));
        }

        log.warn("Validation failed [traceId={}]: {} errors on path {}",
                traceId, details.size(), request.getRequestURI());

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ErrorCode.VALIDATION_FAILED.name(),
                "Validation failed for one or more fields",
                request.getRequestURI(),
                traceId,
                details
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        List<ValidationErrorDetail> details = new ArrayList<>();
        ex.getConstraintViolations().forEach(cv ->
                details.add(new ValidationErrorDetail(cv.getPropertyPath().toString(), cv.getMessage()))
        );

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ErrorCode.VALIDATION_FAILED.name(),
                "Constraint violation occurred",
                request.getRequestURI(),
                traceId,
                details
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        ErrorResponse response = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                ErrorCode.INVALID_CREDENTIALS.name(),
                "Invalid email or password",
                request.getRequestURI(),
                traceId,
                null
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLocked(LockedException ex, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        ErrorResponse response = new ErrorResponse(
                423,
                ErrorCode.ACCOUNT_LOCKED.name(),
                ex.getMessage(),
                request.getRequestURI(),
                traceId,
                null
        );
        return ResponseEntity.status(423).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        ErrorResponse response = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                ErrorCode.ACCESS_DENIED.name(),
                "Access denied: insufficient permissions",
                request.getRequestURI(),
                traceId,
                null
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockingFailureException ex, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        ErrorResponse response = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                ErrorCode.CONCURRENT_MODIFICATION.name(),
                "The resource was modified by another user or session. Please refresh and retry.",
                request.getRequestURI(),
                traceId,
                null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        log.error("Unhandled exception [traceId={}]: {}", traceId, ex.getMessage(), ex);

        ErrorResponse response = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ErrorCode.INTERNAL_ERROR.name(),
                "An unexpected server error occurred. Please contact support with trace ID.",
                request.getRequestURI(),
                traceId,
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
