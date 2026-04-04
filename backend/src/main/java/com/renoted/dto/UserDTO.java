package com.renoted.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * USER DTO (Data Transfer Object) - Public User Information
 *
 * PURPOSE:
 * This DTO represents user data that is SAFE to send to the frontend.
 * It explicitly excludes sensitive information like passwords.
 *
 * WHAT IS A DTO?
 * DTO = Data Transfer Object
 *
 * A DTO is a simple object used to transfer data between layers.
 * It's NOT the same as an Entity!
 *
 * Entity vs DTO:
 *
 * Entity (User.java):
 * - Database representation
 * - Contains ALL fields (including password hash)
 * - Has JPA annotations (@Entity, @Column)
 * - Used internally in Service and Repository layers
 * - NEVER sent directly to frontend
 *
 * DTO (UserDTO.java):
 * - API representation
 * - Contains only SAFE fields (no password!)
 * - No JPA annotations
 * - Used in Controller layer (REST API)
 * - Safe to send to frontend
 *
 * WHY SEPARATE DTO AND ENTITY?
 *
 * 1. SECURITY:
 *    If we send User entity to frontend:
 *    {
 *      "id": 1,
 *      "username": "john",
 *      "password": "$2a$10$N9qo8...",  ← PASSWORD HASH EXPOSED!
 *      "email": "john@example.com"
 *    }
 *    Even though it's hashed, we should NEVER expose password field!
 *
 * 2. CONTROL:
 *    With DTO, we control exactly what data is exposed:
 *    {
 *      "id": 1,
 *      "username": "john",
 *      "email": "john@example.com",
 *      "fullName": "John Doe"
 *    }
 *    No password field at all!
 *
 * 3. FLEXIBILITY:
 *    - Entity changes don't break API
 *    - Can add computed fields
 *    - Can combine data from multiple entities
 *    - Can have multiple DTOs for same entity
 *
 * 4. VALIDATION:
 *    - Different validation rules for API vs Database
 *    - Entity has DB constraints (@Column)
 *    - DTO has API validation (@NotBlank, @Email)
 *
 * LOMBOK ANNOTATIONS:
 * @Data - Generates getters, setters, toString, equals, hashCode
 * @NoArgsConstructor - Generates no-argument constructor
 * @AllArgsConstructor - Generates constructor with all fields
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    /**
     * USER ID
     *
     * Included because:
     * - Frontend needs to identify users
     * - Used in relationships (notes belong to user ID)
     * - Not sensitive information
     *
     * Example: 1, 2, 3, etc.
     */
    private Long id;

    /**
     * USERNAME
     *
     * Public information that can be displayed:
     * - On notes ("Created by: john_doe")
     * - In user profiles
     * - In search results
     *
     * Example: "john_doe", "alice123"
     */
    private String username;

    /**
     * EMAIL
     *
     * Why include email?
     * - User needs to see their own email
     * - Needed for profile updates
     * - Used in "forgot password" flow
     *
     * Security note:
     * - Email is semi-sensitive (can receive spam)
     * - Only show user's own email
     * - Don't expose other users' emails publicly
     *
     * Example: "john@example.com"
     */
    private String email;

    /**
     * FULL NAME
     *
     * Optional display name
     *
     * Example: "John Doe", "Alice Smith"
     */
    private String fullName;

    /**
     * ROLE
     *
     * User's permission level
     *
     * Used by frontend to:
     * - Show/hide admin features
     * - Display user badge
     * - Control UI access
     *
     * Values: "ROLE_USER", "ROLE_ADMIN"
     */
    private String role;

    /**
     * ENABLED STATUS
     *
     * Account active/inactive status
     *
     * Frontend can use this to:
     * - Show "Account disabled" message
     * - Block certain actions
     *
     * Values: true (active), false (disabled)
     */
    private Boolean enabled;

    /**
     * CREATED AT
     *
     * When account was created
     *
     * Used for:
     * - "Member since" display
     * - Sorting users
     * - Account age verification
     */
    private LocalDateTime createdAt;

    /**
     * UPDATED AT
     *
     * Last profile update time
     *
     * Used for:
     * - Showing last activity
     * - Audit trail
     */
    private LocalDateTime updatedAt;

    /**
     * ⚠️ NOTICE: NO PASSWORD FIELD!
     *
     * We NEVER include password in DTO, even the hash!
     *
     * Why?
     * - Password hash should NEVER leave the backend
     * - Frontend doesn't need it
     * - Reduces attack surface
     * - Follows principle of least privilege
     *
     * For password changes:
     * - Use separate ChangePasswordRequest DTO
     * - Validate old password on backend
     * - Hash new password on backend
     * - Never send passwords to frontend
     */
}