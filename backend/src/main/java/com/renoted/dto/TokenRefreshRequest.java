package com.renoted.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TOKEN REFRESH REQUEST - Request to refresh access token
 *
 * Sent by frontend when access token expires.
 * Contains refresh token to get new access token.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshRequest {

    /**
     * REFRESH TOKEN
     *
     * The long-lived refresh token.
     *
     * Frontend sends this when:
     * - Access token expires
     * - Receives 401 Unauthorized
     *
     * Backend uses this to:
     * - Verify user identity
     * - Generate new access token
     */
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}