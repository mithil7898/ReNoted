package com.renoted.dto;

import java.time.LocalDateTime;

/**
 * ApiResponse<T> - Standard Response Wrapper
 *
 * PURPOSE:
 * This class standardizes ALL API responses in your application.
 *
 * WHY DO WE NEED THIS?
 *
 * Without this:
 * - Every API returns different formats ❌
 * - Hard for frontend/Postman to handle ❌
 * - No consistent error handling ❌
 *
 * With this:
 * - Every response looks the SAME ✅
 * - Easy to debug ✅
 * - Professional API design ✅
 *
 * GENERIC TYPE <T>:
 * - Allows flexibility
 * - Can wrap ANY type of data:
 *   - List<Note>
 *   - User
 *   - String
 *   - null (for delete)
 *
 * Example Usage:
 *
 * SUCCESS:
 * {
 *   "success": true,
 *   "message": "Notes fetched successfully",
 *   "data": [...],
 *   "timestamp": "2026-04-08T12:00:00"
 * }
 *
 * ERROR:
 * {
 *   "success": false,
 *   "message": "Note not found",
 *   "data": null,
 *   "timestamp": "2026-04-08T12:00:00"
 * }
 */
public class ApiResponse<T> {

    /**
     * Indicates whether request was successful
     * true  → success
     * false → failure
     */
    private boolean success;

    /**
     * Human-readable message
     * Example:
     * - "Note created successfully"
     * - "Invalid input"
     */
    private String message;

    /**
     * Actual data payload
     * Can be:
     * - Object
     * - List
     * - null (for delete operations)
     */
    private T data;

    /**
     * Timestamp of response
     * Helps in debugging & logging
     */
    private LocalDateTime timestamp;

    /**
     * Constructor for all fields
     */
    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Static factory method for SUCCESS response
     *
     * WHY static?
     * - Cleaner code
     * - Avoids new keyword everywhere
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    /**
     * Static factory method for ERROR response
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }

    // Getters (Required for JSON serialization)
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public LocalDateTime getTimestamp() { return timestamp; }
}