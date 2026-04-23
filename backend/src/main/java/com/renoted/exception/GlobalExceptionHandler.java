package com.renoted.exception;

import com.renoted.dto.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityNotFoundException;

import java.util.stream.Collectors;

/**
 * GLOBAL EXCEPTION HANDLER - FINAL VERSION
 *
 * Handles ALL possible backend exceptions
 * Ensures NO HTML responses ever
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * VALIDATION ERRORS (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<String>> handleValidationException(
            MethodArgumentNotValidException ex
    ) {

        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity.badRequest().body(
                ApiResponse.error(errors)
        );
    }

    /**
     * INVALID JSON / BAD REQUEST BODY
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidJson(Exception ex) {
        return ResponseEntity.badRequest().body(
                ApiResponse.error("Invalid request format")
        );
    }

    /**
     * DATABASE CONSTRAINT ERRORS
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDatabaseException(Exception ex) {
        return ResponseEntity.badRequest().body(
                ApiResponse.error("Database error occurred")
        );
    }

    /**
     * ENTITY NOT FOUND (JPA)
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFound(Exception ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error(ex.getMessage())
        );
    }

    /**
     * GENERIC RUNTIME EXCEPTION
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(
            RuntimeException ex
    ) {

        return ResponseEntity.badRequest().body(
                ApiResponse.error(ex.getMessage())
        );
    }

    /**
     * FINAL FALLBACK (VERY IMPORTANT)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAll(Exception ex) {

        ex.printStackTrace(); // 🔥 VERY IMPORTANT for debugging

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("Internal server error")
        );
    }
}