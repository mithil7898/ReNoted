package com.renoted.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * USER ENTITY - Represents a user account in the system
 *
 * PURPOSE:
 * This entity stores user account information including credentials,
 * profile data, and security settings.
 *
 * SECURITY NOTES:
 * - Passwords are NEVER stored in plain text
 * - We use BCrypt hashing (one-way, irreversible)
 * - Each user has a role (ROLE_USER or ROLE_ADMIN)
 * - Email and username must be unique
 *
 * DATABASE TABLE: users
 *
 * RELATIONSHIP:
 * - One User can have many Notes (One-to-Many)
 * - User is the "parent" in this relationship
 * - Deleting a user will cascade delete their notes
 *
 * WHY LOMBOK ANNOTATIONS:
 * @Data - Generates getters, setters, toString, equals, hashCode
 * @Entity - Marks this class as a JPA entity (database table)
 * @Table - Specifies the database table name
 */
@Data
@Entity
@Table(name = "users")
@EqualsAndHashCode(exclude = {"notes"})  // Exclude notes from equals/hashCode to prevent circular references
@ToString(exclude = {"notes"})  // Exclude notes from toString to prevent circular references
public class User {

    /**
     * PRIMARY KEY
     *
     * @Id - Marks this field as the primary key
     * @GeneratedValue - Database automatically generates values
     * IDENTITY strategy - Uses database's auto-increment feature
     *
     * PostgreSQL creates a sequence: users_id_seq
     * Each new user gets next ID: 1, 2, 3, 4...
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * USERNAME
     *
     * Rules:
     * - Must be unique (no two users can have same username)
     * - Cannot be null (required field)
     * - Max 50 characters
     *
     * Used for:
     * - Login credential
     * - Display name
     * - Identifying user
     *
     * Example: "john_doe", "alice123"
     */
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    /**
     * EMAIL
     *
     * Rules:
     * - Must be unique (one email = one account)
     * - Cannot be null
     * - Max 100 characters
     *
     * Used for:
     * - Alternative login credential
     * - Password reset
     * - Notifications
     *
     * Example: "john@example.com"
     */
    @Column(unique = true, nullable = false, length = 100)
    private String email;

    /**
     * PASSWORD (HASHED!)
     *
     * ⚠️ CRITICAL SECURITY CONCEPT:
     *
     * This field stores the BCrypt HASH, not the actual password!
     *
     * Process:
     * 1. User enters password: "mySecret123"
     * 2. BCrypt hashes it: "$2a$10$N9qo8uLO..."
     * 3. We store the hash in this field
     * 4. Original password is discarded (never stored!)
     *
     * Why 255 characters?
     * - BCrypt hashes are ~60 characters
     * - We allow extra space for future algorithms
     *
     * Verification:
     * - User enters password during login
     * - We hash the input
     * - Compare hash with stored hash
     * - If match → login success!
     *
     * Security Benefits:
     * - Database breach doesn't expose passwords
     * - Each password has unique salt
     * - Cannot reverse hash to get original password
     */
    @Column(nullable = false, length = 255)
    private String password;  // This stores BCrypt hash, NOT plain text!

    /**
     * FULL NAME
     *
     * Optional field for user's real name
     *
     * Examples: "John Doe", "Alice Smith"
     */
    @Column(name = "full_name", length = 100)
    private String fullName;

    /**
     * ROLE
     *
     * Determines user permissions:
     * - ROLE_USER: Regular user (default)
     * - ROLE_ADMIN: Administrator with extra permissions
     *
     * Spring Security uses these for authorization:
     * @PreAuthorize("hasRole('ADMIN')")
     *
     * Why "ROLE_" prefix?
     * - Spring Security convention
     * - Helps distinguish roles from other authorities
     *
     * Default: "ROLE_USER" for new registrations
     */
    @Column(nullable = false, length = 20)
    private String role = "ROLE_USER";

    /**
     * ENABLED FLAG
     *
     * Controls account status:
     * - true: Account is active, user can login
     * - false: Account is disabled, login blocked
     *
     * Use cases:
     * - Email verification (disabled until verified)
     * - Account suspension
     * - Soft delete (disable instead of delete)
     *
     * Default: true (accounts active on creation)
     */
    @Column(nullable = false)
    private Boolean enabled = true;

    /**
     * TIMESTAMPS
     *
     * Automatic timestamp management:
     *
     * @CreationTimestamp - Sets value when entity is first saved
     * - Never changes after creation
     * - Useful for "Member since" displays
     *
     * @UpdateTimestamp - Updates every time entity is modified
     * - Tracks last profile update
     * - Useful for auditing
     *
     * Column definition:
     * - updatable = false: createdAt never changes after insert
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * RELATIONSHIP: User → Notes (One-to-Many)
     *
     * @OneToMany - One user has many notes
     * mappedBy = "user" - The Note entity has a "user" field that owns this relationship
     * cascade = CascadeType.ALL - Operations on user cascade to notes
     * orphanRemoval = true - Delete note if removed from user's list
     *
     * WHAT IS CASCADE?
     * When we save/delete a user, what happens to their notes?
     *
     * CascadeType.ALL means:
     * - Save user → Save all their notes
     * - Delete user → Delete all their notes
     * - Update user → Update relationships
     *
     * WHAT IS ORPHAN REMOVAL?
     * If we remove a note from user.getNotes() list,
     * that note becomes an "orphan" (no parent user).
     * orphanRemoval = true automatically deletes orphaned notes.
     *
     * Example:
     * user.getNotes().remove(note);  // Note has no user anymore
     * userRepository.save(user);      // Note is deleted from database!
     *
     * LAZY vs EAGER LOADING:
     * FetchType.LAZY (default for OneToMany):
     * - Notes are NOT loaded when we fetch user
     * - Only loaded when we call user.getNotes()
     * - Saves memory and improves performance
     *
     * Why exclude from equals/hashCode?
     * - Loading notes collection triggers database query
     * - Can cause infinite loops in bidirectional relationships
     * - Only use ID for entity comparison
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Note> notes = new java.util.HashSet<>();

    /**
     * WHY USE Set INSTEAD OF List?
     *
     * Set benefits:
     * - No duplicate notes (Set automatically prevents duplicates)
     * - Better performance for contains() checks
     * - Matches database reality (each note is unique)
     *
     * List drawbacks:
     * - Can have duplicates
     * - Slower for large collections
     * - Order doesn't matter for notes
     */
}