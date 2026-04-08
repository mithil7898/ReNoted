package com.renoted.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AUTH RESPONSE DTO - Authentication Response with JWT Tokens
 *
 * Updated to include both access and refresh tokens.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /**
     * ACCESS TOKEN (Short-lived)
     *
     * Used for API requests.
     * Expires in 1 hour (configurable).
     */
    private String accessToken;

    /**
     * REFRESH TOKEN (Long-lived)
     *
     * Used to get new access tokens.
     * Expires in 7 days (configurable).
     */
    private String refreshToken;

    /**
     * USER INFORMATION
     *
     * Public user data.
     */
    private UserDTO user;

    /**
     * TOKEN TYPE
     *
     * Always "Bearer" for JWT.
     */
    private String tokenType = "Bearer";

    // Constructor without tokenType (sets default)
    public AuthResponse(String accessToken, String refreshToken, UserDTO user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
        this.tokenType = "Bearer";
    }
}