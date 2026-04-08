package com.renoted.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TOKEN REFRESH RESPONSE - Response with new access token
 *
 * Sent to frontend after successful token refresh.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshResponse {

    /**
     * NEW ACCESS TOKEN
     *
     * Fresh access token with renewed expiration.
     *
     * Frontend should:
     * - Replace old access token
     * - Retry failed request
     * - Continue normal operation
     */
    private String accessToken;

    /**
     * TOKEN TYPE
     *
     * Always "Bearer" for JWT tokens.
     */
    private String tokenType = "Bearer";

    public TokenRefreshResponse(String accessToken) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
    }
}