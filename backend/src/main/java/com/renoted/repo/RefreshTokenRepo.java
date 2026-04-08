package com.renoted.repo;

import com.renoted.entity.RefreshToken;
import com.renoted.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * REFRESH TOKEN REPOSITORY - Database access for refresh tokens
 *
 * Provides methods to:
 * - Find tokens by token string
 * - Find tokens by user
 * - Delete expired tokens
 * - Delete all tokens for a user (logout all devices)
 * - Delete revoked tokens
 */
@Repository
public interface RefreshTokenRepo extends JpaRepository<RefreshToken, Long> {

    /**
     * FIND BY TOKEN STRING
     *
     * Look up refresh token by its token value.
     *
     * Used during token refresh:
     * - User sends refresh token
     * - We find it in database
     * - Check if valid and not revoked
     * - Generate new access token
     *
     * Returns Optional:
     * - Empty if token doesn't exist
     * - Contains RefreshToken if found
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * FIND ALL BY USER
     *
     * Get all refresh tokens for a user.
     *
     * Use cases:
     * - Show user their active sessions
     * - Logout from all devices
     * - Security: See where user is logged in
     *
     * Returns List:
     * - Empty list if user has no tokens
     * - List of RefreshToken if found
     */
    List<RefreshToken> findByUser(User user);

    /**
     * DELETE BY USER
     *
     * Delete all refresh tokens for a user.
     *
     * Use cases:
     * - User logs out from all devices
     * - User changes password (invalidate all sessions)
     * - Security breach (force re-login everywhere)
     *
     * Returns:
     * - Number of tokens deleted
     *
     * @Modifying: Indicates this query modifies data
     * @Transactional: Required for delete/update queries
     */
    void deleteByUser(User user);

    /**
     * DELETE EXPIRED TOKENS
     *
     * Clean up tokens that have expired.
     *
     * Query explanation:
     * DELETE FROM refresh_tokens WHERE expires_at < NOW()
     *
     * Scheduled task should call this:
     * - Daily: Clean up expired tokens
     * - Keep database lean
     * - Remove old audit data
     *
     * Returns:
     * - Number of tokens deleted
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    void deleteExpiredTokens(LocalDateTime now);

    /**
     * DELETE REVOKED TOKENS OLDER THAN
     *
     * Clean up old revoked tokens.
     *
     * Query explanation:
     * DELETE FROM refresh_tokens
     * WHERE revoked = true AND created_at < cutoff_date
     *
     * Use case:
     * - Keep revoked tokens for audit (30 days)
     * - Delete after retention period
     *
     * Returns:
     * - Number of tokens deleted
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.revoked = true AND rt.createdAt < :cutoffDate")
    void deleteOldRevokedTokens(LocalDateTime cutoffDate);

    /**
     * CHECK IF TOKEN EXISTS AND NOT REVOKED
     *
     * Quick check for token validity.
     *
     * More efficient than:
     * - findByToken().isPresent()
     * - Doesn't fetch entire object
     *
     * Returns:
     * - true if token exists and not revoked
     * - false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(rt) > 0 THEN true ELSE false END FROM RefreshToken rt WHERE rt.token = :token AND rt.revoked = false")
    boolean existsByTokenAndNotRevoked(String token);

    /**
     * COUNT ACTIVE TOKENS FOR USER
     *
     * How many active sessions does user have?
     *
     * Active = not expired AND not revoked
     *
     * Use case:
     * - Limit number of concurrent sessions
     * - Show user "You're logged in on 3 devices"
     *
     * Returns:
     * - Number of active tokens
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user = :user AND rt.expiresAt > :now AND rt.revoked = false")
    long countActiveTokensForUser(User user, LocalDateTime now);
}
