package com.renoted.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * REFRESH TOKEN ENTITY - Long-lived tokens for getting new access tokens
 *
 * PURPOSE:
 * Store refresh tokens in database for:
 * - Token validation (check if exists and not revoked)
 * - Token revocation (logout, security breach)
 * - Automatic cleanup of expired tokens
 *
 * LIFECYCLE:
 * - Created: When user logs in
 * - Used: To get new access tokens when access token expires
 * - Revoked: On logout or security event
 * - Deleted: After expiration + grace period
 *
 * SECURITY:
 * - Stored in database (can be revoked)
 * - One-to-one with user (one active refresh token per user)
 * - Has expiration timestamp
 * - Can be marked as revoked
 */
@Data
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    /**
     * PRIMARY KEY
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * TOKEN VALUE
     *
     * The actual JWT refresh token string.
     *
     * Properties:
     * - Unique: Each token is different
     * - Not null: Required field
     * - Length 500: JWT tokens are long (~200-400 chars)
     *
     * Example:
     * "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
     */
    @Column(nullable = false, unique = true, length = 500)
    private String token;

    /**
     * USER RELATIONSHIP
     *
     * Each refresh token belongs to one user.
     *
     * @ManyToOne: Many refresh tokens can belong to one user
     * @JoinColumn: Specifies foreign key column name
     *
     * Why many-to-one?
     * - User can login from multiple devices
     * - Each device gets its own refresh token
     * - Can track and revoke per-device
     *
     * Alternative: One-to-one (one token per user)
     * - Simpler but forces logout on other devices
     * - Less flexible
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * EXPIRATION TIMESTAMP
     *
     * When this refresh token expires.
     *
     * Typical values:
     * - 7 days for normal use
     * - 30 days for "remember me"
     * - 1 day for high-security apps
     *
     * After expiration:
     * - Token cannot be used
     * - User must login again
     * - Token can be deleted from database
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /**
     * REVOKED FLAG
     *
     * Whether this token has been revoked.
     *
     * Revoked when:
     * - User logs out
     * - Security breach detected
     * - User changes password
     * - Admin action
     *
     * Revoked tokens:
     * - Cannot be used even if not expired
     * - Remain in database for audit trail
     * - Can be deleted after retention period
     */
    @Column(nullable = false)
    private Boolean revoked = false;

    /**
     * CREATION TIMESTAMP
     *
     * When this token was created.
     *
     * Uses:
     * - Audit trail
     * - Track token age
     * - Security monitoring
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * IP ADDRESS (Optional)
     *
     * IP address where token was created.
     *
     * Uses:
     * - Security monitoring
     * - Detect token theft
     * - Geographic restrictions
     */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /**
     * USER AGENT (Optional)
     *
     * Browser/device information.
     *
     * Uses:
     * - Show user "active sessions"
     * - Identify device
     * - Security alerts
     */
    @Column(name = "user_agent", length = 255)
    private String userAgent;
}