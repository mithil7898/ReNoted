package com.renoted.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LOGIN REQUEST DTO - User Login Credentials
 *
 * PURPOSE:
 * This DTO captures login credentials from the frontend.
 *
 * WHAT IS THIS FOR?
 * When user tries to login, frontend sends:
 * POST /api/auth/login
 * {
 *   "username": "john_doe",
 *   "password": "password123"
 * }
 *
 * Spring MVC deserializes JSON to this object.
 * We validate it and authenticate user.
 *
 * VALIDATION:
 * @NotBlank ensures fields are not empty.
 * Spring validates before method execution.
 *
 * SECURITY:
 * - Password is plain text HERE (from user input)
 * - We will hash it for comparison
 * - NEVER log this object (contains plain password!)
 * - Only exists in memory briefly
 * - Discarded after authentication
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /**
     * USERNAME
     *
     * User's login identifier
     *
     * @NotBlank:
     * - Cannot be null
     * - Cannot be empty string
     * - Cannot be only whitespace
     *
     * If validation fails:
     * - Returns 400 Bad Request
     * - Message: "Username is required"
     */
    @NotBlank(message = "Username is required")
    private String username;

    /**
     * PASSWORD (PLAIN TEXT)
     *
     * ⚠️ SECURITY WARNING:
     * This contains the user's PLAIN TEXT password!
     *
     * Flow:
     * 1. User types password in browser
     * 2. Frontend sends to backend (over HTTPS!)
     * 3. Backend receives in this DTO
     * 4. We immediately use for authentication
     * 5. Spring Security hashes and compares
     * 6. This object is discarded
     *
     * NEVER:
     * - Store this password
     * - Log this password
     * - Send this password anywhere else
     * - Keep this object in memory longer than needed
     *
     * ALWAYS:
     * - Use HTTPS in production (encrypts in transit)
     * - Let Spring Security handle comparison
     * - Discard immediately after authentication
     *
     * @NotBlank:
     * - Password is required
     */
    @NotBlank(message = "Password is required")
    private String password;

    /*
     * ═══════════════════════════════════════════════════════════
     * SECURITY BEST PRACTICES
     * ═══════════════════════════════════════════════════════════
     *
     * 1. HTTPS ONLY IN PRODUCTION
     *    - Plain passwords sent over network
     *    - HTTP = visible to anyone
     *    - HTTPS = encrypted in transit
     *    - NEVER use HTTP for login in production!
     *
     * 2. NEVER LOG PASSWORDS
     *    - Don't log this object
     *    - Don't log password field
     *    - Even for debugging!
     *    - Logs might be compromised
     *
     * 3. MINIMIZE EXPOSURE
     *    - Keep in memory briefly
     *    - Use for authentication immediately
     *    - Let garbage collector clean up
     *    - Don't store in variables longer than needed
     *
     * 4. VALIDATION ONLY
     *    - We only validate format here
     *    - Don't check password strength here
     *    - Spring Security handles actual verification
     *    - BCrypt comparison happens elsewhere
     */
}