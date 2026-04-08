package com.renoted.service;

import com.renoted.entity.RefreshToken;
import com.renoted.entity.User;
import com.renoted.repo.RefreshTokenRepo;
import com.renoted.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * REFRESH TOKEN SERVICE - Business logic for refresh tokens
 *
 * Responsibilities:
 * - Create refresh tokens
 * - Validate refresh tokens
 * - Revoke refresh tokens
 * - Clean up expired tokens
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepo RefreshTokenRepo;
    private final JwtUtil jwtUtil;

    /**
     * REFRESH TOKEN EXPIRATION (milliseconds)
     *
     * Default: 7 days = 7 * 24 * 60 * 60 * 1000 = 604,800,000 ms
     *
     * Configuration:
     * - application.properties: jwt.refresh.expiration=604800000
     * - Override per environment
     */
    @Value("${jwt.refresh.expiration:604800000}")
    private Long refreshTokenDurationMs;

    /**
     * CREATE REFRESH TOKEN
     *
     * Generate new refresh token for user.
     *
     * Process:
     * 1. Generate unique token string (UUID + JWT)
     * 2. Calculate expiration time
     * 3. Create RefreshToken entity
     * 4. Save to database
     * 5. Return token
     *
     * Token format:
     * - Can be simple UUID
     * - Or JWT with refresh-specific claims
     * - We use UUID for simplicity
     *
     * @param user - User to create token for
     * @return RefreshToken entity
     */
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();

        // Generate unique token
        // Using UUID ensures uniqueness
        // Alternative: Generate JWT with refresh claims
        refreshToken.setToken(UUID.randomUUID().toString());

        // Set user
        refreshToken.setUser(user);

        // Calculate expiration
        // Current time + configured duration
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenDurationMs / 1000));

        // Not revoked initially
        refreshToken.setRevoked(false);

        // Save to database
        return RefreshTokenRepo.save(refreshToken);
    }

    /**
     * VERIFY REFRESH TOKEN
     *
     * Check if refresh token is valid.
     *
     * Validation steps:
     * 1. Find token in database
     * 2. Check if exists
     * 3. Check if not revoked
     * 4. Check if not expired
     * 5. Return token if all checks pass
     *
     * @param token - Token string to validate
     * @return RefreshToken if valid
     * @throws RuntimeException if invalid
     */
    public RefreshToken verifyRefreshToken(String token) {
        // Find token in database
        RefreshToken refreshToken = RefreshTokenRepo.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        // Check if revoked
        if (refreshToken.getRevoked()) {
            throw new RuntimeException("Refresh token has been revoked");
        }

        // Check if expired
        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            // Token expired, delete it
            RefreshTokenRepo.delete(refreshToken);
            throw new RuntimeException("Refresh token has expired");
        }

        // Token is valid
        return refreshToken;
    }

    /**
     * REVOKE TOKEN
     *
     * Mark token as revoked.
     *
     * Use cases:
     * - User logs out
     * - Security breach
     * - Suspicious activity
     *
     * Revoked vs Deleted:
     * - Revoked: Marked as invalid, kept for audit
     * - Deleted: Removed from database
     *
     * We revoke first, delete later (cleanup job)
     *
     * @param token - Token string to revoke
     */
    public void revokeToken(String token) {
        RefreshTokenRepo.findByToken(token)
                .ifPresent(refreshToken -> {
                    refreshToken.setRevoked(true);
                    RefreshTokenRepo.save(refreshToken);
                });
    }

    /**
     * REVOKE ALL USER TOKENS
     *
     * Revoke all refresh tokens for a user.
     *
     * Use cases:
     * - User changes password
     * - Force logout from all devices
     * - Security breach
     *
     * @param user - User whose tokens to revoke
     */
    public void revokeAllUserTokens(User user) {
        RefreshTokenRepo.findByUser(user)
                .forEach(token -> {
                    token.setRevoked(true);
                    RefreshTokenRepo.save(token);
                });
    }

    /**
     * DELETE USER TOKENS
     *
     * Permanently delete all tokens for user.
     *
     * Use case:
     * - User account deleted
     * - Immediate cleanup needed
     *
     * @param user - User whose tokens to delete
     */
    public void deleteUserTokens(User user) {
        RefreshTokenRepo.deleteByUser(user);
    }

    /**
     * CLEAN UP EXPIRED TOKENS
     *
     * Delete all expired tokens from database.
     *
     * Should be called by scheduled task:
     * @Scheduled(cron = "0 0 2 * * ?")  // 2 AM daily
     * public void cleanupExpiredTokens() {
     *     refreshTokenService.cleanupExpiredTokens();
     * }
     *
     * Keeps database lean
     * Removes old data
     */
    public void cleanupExpiredTokens() {
        RefreshTokenRepo.deleteExpiredTokens(LocalDateTime.now());
    }

    /**
     * CLEAN UP OLD REVOKED TOKENS
     *
     * Delete revoked tokens older than retention period.
     *
     * Retention period: 30 days
     * - Keep revoked tokens for audit
     * - Delete after 30 days
     *
     * Should be called by scheduled task
     */
    public void cleanupOldRevokedTokens() {
        // Keep revoked tokens for 30 days
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        RefreshTokenRepo.deleteOldRevokedTokens(cutoffDate);
    }

    /**
     * COUNT ACTIVE SESSIONS
     *
     * How many active sessions does user have?
     *
     * Use case:
     * - Show user their active devices
     * - Limit concurrent sessions
     *
     * @param user - User to count sessions for
     * @return Number of active sessions
     */
    public long countActiveSessions(User user) {
        return RefreshTokenRepo.countActiveTokensForUser(user, LocalDateTime.now());
    }
}