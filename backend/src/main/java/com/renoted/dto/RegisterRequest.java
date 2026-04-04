package com.renoted.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REGISTER REQUEST DTO - User Registration Data
 *
 * PURPOSE:
 * This DTO captures user registration information from the frontend.
 * It includes validation rules to ensure data quality before processing.
 *
 * WHAT IS BEAN VALIDATION?
 * Jakarta Bean Validation (formerly Java Bean Validation) is a framework
 * for validating Java objects using annotations.
 *
 * How it works:
 * 1. Frontend sends registration data
 * 2. Spring MVC receives it in Controller
 * 3. @Valid annotation triggers validation
 * 4. If validation fails → Return 400 Bad Request with error details
 * 5. If validation passes → Continue to service layer
 *
 * Common Validation Annotations:
 * @NotNull - Field cannot be null
 * @NotBlank - String cannot be null, empty, or whitespace
 * @NotEmpty - Collection/Array/String cannot be empty
 * @Size - String/Collection must have specific length
 * @Email - Must be valid email format
 * @Min, @Max - Number range validation
 * @Pattern - Regex pattern matching
 * @Past, @Future - Date validation
 *
 * Why Validate?
 * - Prevent bad data from entering database
 * - Provide clear error messages to users
 * - Fail fast (catch errors early)
 * - Security (prevent injection attacks)
 * - Data integrity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    /**
     * USERNAME
     *
     * @NotBlank:
     * - Cannot be null
     * - Cannot be empty string ("")
     * - Cannot be only whitespace ("   ")
     *
     * message: Error message returned if validation fails
     *
     * @Size:
     * - min = 3: Username must be at least 3 characters
     * - max = 50: Username cannot exceed 50 characters
     *
     * Why these constraints?
     * - Too short (1-2 chars) = Hard to remember, typos
     * - Too long (>50 chars) = Database limit, UI issues
     * - 3-50 is standard range for usernames
     *
     * Additional validation we could add:
     * @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
     */
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    /**
     * EMAIL
     *
     * @NotBlank:
     * - Email is required
     *
     * @Email:
     * - Must be valid email format
     * - Checks for @ symbol
     * - Checks for domain
     * - Examples of valid: "john@example.com", "alice@test.co.uk"
     * - Examples of invalid: "notanemail", "missing@domain", "@nodomain.com"
     *
     * How @Email works:
     * - Uses regex pattern internally
     * - Pattern: ^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$
     * - Not perfect but catches most invalid formats
     *
     * @Size:
     * - max = 100: Prevent extremely long emails
     *
     * Best Practice:
     * - Send verification email after registration
     * - Confirm email is real and accessible
     * - Block disposable email domains (optional)
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    /**
     * PASSWORD
     *
     * @NotBlank:
     * - Password is required
     *
     * @Size:
     * - min = 6: Minimum security requirement
     * - max = 100: Practical limit for input
     *
     * Why minimum 6 characters?
     * - Industry standard minimum
     * - Better: enforce 8+ characters
     * - Best: enforce complexity rules
     *
     * Password Security Levels:
     *
     * Weak (current):
     * - Only length validation
     * - Allows "123456" (very weak!)
     *
     * Medium (recommended):
     * @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$",
     *         message = "Password must contain at least one letter and one number")
     *
     * Strong (best):
     * @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
     *         message = "Password must contain uppercase, lowercase, number, and special character")
     *
     * Frontend should also:
     * - Show password strength meter
     * - Require password confirmation
     * - Check against common passwords (e.g., "password123")
     *
     * Security Note:
     * - This is the PLAIN TEXT password from user
     * - We will hash it immediately in the service layer
     * - NEVER log or store this plain text password
     * - Hash it, then discard the plain text
     */
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    /**
     * FULL NAME
     *
     * Optional field (no @NotBlank)
     *
     * @Size:
     * - max = 100: Reasonable limit for names
     *
     * Why optional?
     * - Some users prefer pseudonymity
     * - Can be added later in profile
     * - Not critical for account creation
     *
     * Validation we could add:
     * @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Name can only contain letters and spaces")
     */
    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    private String fullName;

    /*
     * ═══════════════════════════════════════════════════════════
     * VALIDATION IN ACTION - EXAMPLE
     * ═══════════════════════════════════════════════════════════
     *
     * Request from frontend:
     * POST /api/auth/register
     * {
     *   "username": "ab",           // TOO SHORT!
     *   "email": "notanemail",      // INVALID EMAIL!
     *   "password": "12345",        // TOO SHORT!
     *   "fullName": "John Doe"
     * }
     *
     * Validation fails, response:
     * HTTP 400 Bad Request
     * {
     *   "errors": [
     *     {
     *       "field": "username",
     *       "message": "Username must be between 3 and 50 characters"
     *     },
     *     {
     *       "field": "email",
     *       "message": "Email must be valid"
     *     },
     *     {
     *       "field": "password",
     *       "message": "Password must be between 6 and 100 characters"
     *     }
     *   ]
     * }
     *
     * Valid request:
     * {
     *   "username": "john_doe",
     *   "email": "john@example.com",
     *   "password": "securePass123",
     *   "fullName": "John Doe"
     * }
     *
     * Validation passes → Continues to service layer
     */
}