package com.renoted.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AUTH RESPONSE DTO - Authentication Response with JWT Token
 *
 * PURPOSE:
 * This DTO is sent to frontend after successful login or registration.
 *
 * WHAT DOES IT CONTAIN?
 * 1. JWT token - For authentication on future requests
 * 2. User information - Display in UI
 *
 * RESPONSE STRUCTURE:
 * {
 *   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
 *   "user": {
 *     "id": 1,
 *     "username": "john_doe",
 *     "email": "john@example.com",
 *     "fullName": "John Doe",
 *     "role": "ROLE_USER",
 *     "enabled": true,
 *     "createdAt": "2026-03-25T10:30:00",
 *     "updatedAt": "2026-03-25T10:30:00"
 *   }
 * }
 *
 * FRONTEND USAGE:
 * 1. Receive this response
 * 2. Store token in localStorage
 * 3. Display user info in UI
 * 4. Include token in future requests
 *
 * Example:
 * localStorage.setItem('token', response.token);
 * localStorage.setItem('user', JSON.stringify(response.user));
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /**
     * JWT TOKEN
     *
     * The access token for authentication.
     *
     * Format:
     * "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huX2RvZSIsImlhdCI6MTUxNjIzOTAyMn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
     *
     * Contains:
     * - Header (algorithm)
     * - Payload (username, expiration)
     * - Signature (verification)
     *
     * Frontend will:
     * 1. Store in localStorage
     * 2. Include in Authorization header on requests:
     *    Authorization: Bearer <token>
     *
     * Token Expiration:
     * - Configured in JwtUtil (default 10 hours)
     * - After expiration, user must login again
     * - Frontend should handle 401 errors
     */
    private String token;

    /**
     * USER INFORMATION
     *
     * Public user data (no password!)
     *
     * Contains:
     * - id: User database ID
     * - username: Display name
     * - email: Contact info
     * - fullName: Real name
     * - role: Permission level
     * - enabled: Account status
     * - createdAt: Registration time
     * - updatedAt: Last update
     *
     * Frontend uses this to:
     * - Display user profile
     * - Show username in header
     * - Show/hide admin features based on role
     * - Check account status
     *
     * Security:
     * - NO password field (even hashed)
     * - Only safe public information
     * - Follows DTO pattern
     */
    private UserDTO user;

    /*
     * ═══════════════════════════════════════════════════════════
     * AUTHENTICATION FLOW - COMPLETE PICTURE
     * ═══════════════════════════════════════════════════════════
     *
     * 1. USER LOGIN
     *    POST /api/auth/login
     *    { username: "john", password: "secret" }
     *
     * 2. BACKEND VALIDATES
     *    - Check username exists
     *    - Verify password hash
     *    - Generate JWT token
     *
     * 3. SEND AUTH RESPONSE
     *    {
     *      "token": "eyJhbGc...",
     *      "user": { ... }
     *    }
     *
     * 4. FRONTEND STORES
     *    localStorage.setItem('token', response.token)
     *    localStorage.setItem('user', JSON.stringify(response.user))
     *
     * 5. AUTHENTICATED REQUESTS
     *    GET /api/notes
     *    Headers: {
     *      Authorization: "Bearer eyJhbGc..."
     *    }
     *
     * 6. BACKEND VALIDATES TOKEN
     *    - JwtAuthenticationFilter extracts token
     *    - Validates signature and expiration
     *    - Loads user from database
     *    - Sets authentication
     *    - Request proceeds
     *
     * 7. RESPONSE SENT
     *    User's notes returned
     */
}