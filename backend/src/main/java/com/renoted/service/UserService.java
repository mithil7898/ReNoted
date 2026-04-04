package com.renoted.service;

import com.renoted.dto.RegisterRequest;
import com.renoted.dto.UserDTO;
import com.renoted.entity.User;
import com.renoted.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * USER SERVICE - Business Logic Layer for User Operations
 *
 * PURPOSE:
 * This service handles all business logic related to user management,
 * including registration, validation, and data conversion.
 *
 * LAYER ARCHITECTURE:
 * Controller → Service → Repository → Database
 *               ↑
 *         We are here!
 *
 * SERVICE LAYER RESPONSIBILITIES:
 * 1. Business Logic:
 *    - Validate business rules
 *    - Check if username/email already exists
 *    - Hash passwords before storage
 *
 * 2. Transaction Management:
 *    - Ensure operations succeed or fail together
 *    - Rollback on errors
 *
 * 3. Data Conversion:
 *    - Convert Entity ↔ DTO
 *    - Hide sensitive data
 *
 * 4. Orchestration:
 *    - Coordinate multiple repository calls
 *    - Call other services if needed
 *
 * WHY SERVICE LAYER?
 *
 * Without Service Layer (BAD):
 * Controller does everything:
 * - Handle HTTP requests
 * - Validate business rules
 * - Hash passwords
 * - Call repository
 * - Convert DTO
 * Result: Fat controllers, hard to test, code duplication
 *
 * With Service Layer (GOOD):
 * Controller: Handle HTTP only
 * Service: Business logic and orchestration
 * Repository: Database access only
 * Result: Clean separation, easy to test, reusable logic
 *
 * LOMBOK @RequiredArgsConstructor:
 * Generates constructor for all final fields
 * Enables constructor-based dependency injection
 *
 * Example:
 * @RequiredArgsConstructor generates:
 * public UserService(userRepo userRepo, PasswordEncoder passwordEncoder) {
 *     this.userRepo = userRepo;
 *     this.passwordEncoder = passwordEncoder;
 * }
 */
@Service  // Marks this as a Spring service component
@RequiredArgsConstructor  // Generates constructor for final fields (dependency injection)
@Transactional  // All methods run in database transactions
public class UserService {

    /**
     * DEPENDENCIES
     *
     * These are injected automatically by Spring via constructor injection.
     *
     * Why final?
     * - Cannot be changed after construction
     * - Thread-safe
     * - Makes dependencies explicit
     * - Prevents accidental reassignment
     *
     * Why constructor injection?
     * - Best practice (recommended by Spring)
     * - Enables immutability (final fields)
     * - Easy to test (pass mocks in constructor)
     * - Prevents circular dependencies
     * - Makes required dependencies obvious
     *
     * Alternative (Field Injection - NOT RECOMMENDED):
     * @Autowired
     * private userRepo userRepo;
     *
     * Why constructor is better?
     * - Field injection hides dependencies
     * - Cannot use final fields
     * - Harder to test
     * - Allows partial construction (null fields)
     */
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    /**
     * REGISTER NEW USER
     *
     * This is the main registration flow. Let's break down every step!
     *
     * Flow:
     * 1. Check if username already exists → Fail if taken
     * 2. Check if email already exists → Fail if taken
     * 3. Create new User entity
     * 4. Set username and email from request
     * 5. Hash the password (CRITICAL SECURITY STEP)
     * 6. Set optional full name
     * 7. Set default role (ROLE_USER)
     * 8. Set account as enabled
     * 9. Save to database
     * 10. Convert to DTO (hide password)
     * 11. Return DTO to controller
     *
     * @param request - Registration data from frontend (validated)
     * @return UserDTO - Public user information (no password)
     * @throws RuntimeException if username or email already exists
     *
     * @Transactional:
     * - This method runs in a database transaction
     * - If ANY step fails, ALL changes are rolled back
     * - Database remains consistent
     *
     * Example:
     * Step 1-8 succeed, Step 9 (save) fails
     * → Transaction rolls back
     * → No partial data in database
     * → Database unchanged
     */
    public UserDTO registerUser(RegisterRequest request) {
        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 1: VALIDATE USERNAME AVAILABILITY
         * ═══════════════════════════════════════════════════════════
         *
         * Check if username is already taken
         *
         * Why this is important:
         * - Usernames must be unique (database constraint)
         * - Better to check BEFORE attempting insert
         * - Provides clear error message to user
         * - Prevents database constraint violation
         *
         * What happens here:
         * userRepo.existsByUsername("john_doe")
         * → Executes: SELECT EXISTS(SELECT 1 FROM users WHERE username = 'john_doe')
         * → Returns: true (exists) or false (available)
         *
         * If username exists:
         * → Throw RuntimeException with message
         * → Controller catches exception
         * → Returns HTTP 400 Bad Request to frontend
         * → User sees: "Username already exists"
         *
         * If username available:
         * → Continue to next validation
         */
        if (userRepo.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
            /*
             * IMPROVEMENT IDEAS:
             * 1. Create custom exception:
             *    throw new UsernameAlreadyExistsException("Username already taken");
             *
             * 2. Return suggested usernames:
             *    "john_doe is taken. Try: john_doe123, john_doe2024"
             *
             * 3. Add username validation endpoint:
             *    GET /api/auth/check-username?username=john_doe
             *    → Check availability while user types
             */
        }

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 2: VALIDATE EMAIL AVAILABILITY
         * ═══════════════════════════════════════════════════════════
         *
         * Check if email is already registered
         *
         * Why this is important:
         * - Prevents multiple accounts with same email
         * - Email is often used for password reset
         * - Email must be unique (database constraint)
         *
         * Security consideration:
         * - Don't reveal if email exists (prevents user enumeration)
         * - In production, return generic message
         * - Or silently send "account already exists" email
         *
         * User Enumeration Attack:
         * Attacker tries: test@gmail.com, admin@company.com, etc.
         * If we say "Email already registered":
         * → Attacker knows these emails have accounts
         * → Can target them for phishing, brute force, etc.
         *
         * Better approach (production):
         * Always return "Success! Check your email"
         * If email exists: Send "account already exists" email
         * If email new: Send verification email
         * Attacker cannot tell the difference!
         */
        if (userRepo.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
            /*
             * PRODUCTION IMPROVEMENT:
             * Don't throw exception, instead:
             * 1. Return success message
             * 2. Send email to existing address
             * 3. Email says: "Someone tried to register with your email"
             * 4. Prevents user enumeration
             */
        }

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 3: CREATE USER ENTITY
         * ═══════════════════════════════════════════════════════════
         *
         * Create new User object in memory (not saved to database yet)
         *
         * At this point:
         * - User exists only in application memory
         * - No database changes yet
         * - Transaction hasn't committed
         * - If anything fails, no data is lost
         */
        User user = new User();

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 4: SET USERNAME
         * ═══════════════════════════════════════════════════════════
         *
         * Copy username from request to entity
         *
         * We already validated:
         * - Username is not blank (Bean Validation in RegisterRequest)
         * - Username length is 3-50 characters
         * - Username is available (Step 1 above)
         *
         * Safe to set!
         */
        user.setUsername(request.getUsername());

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 5: SET EMAIL
         * ═══════════════════════════════════════════════════════════
         *
         * Copy email from request to entity
         *
         * We already validated:
         * - Email is not blank
         * - Email format is valid (@Email annotation)
         * - Email is available (Step 2 above)
         *
         * Lowercase conversion (best practice):
         * user.setEmail(request.getEmail().toLowerCase());
         * Why? Prevents duplicate emails like "John@example.com" vs "john@example.com"
         */
        user.setEmail(request.getEmail());

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 6: HASH PASSWORD - CRITICAL SECURITY STEP
         * ═══════════════════════════════════════════════════════════
         *
         * This is THE MOST IMPORTANT security operation!
         *
         * What's happening:
         * Input: request.getPassword() = "myPassword123" (plain text from user)
         * Process: passwordEncoder.encode() → BCrypt hashing
         * Output: "$2a$10$N9qo8uLOickgx2ZMRZoMye..." (irreversible hash)
         * Storage: user.setPassword(hash) → Store ONLY the hash
         *
         * Step-by-step BCrypt process:
         * 1. Generate random salt: "x8Ks2pQz"
         * 2. Combine: "myPassword123" + "x8Ks2pQz"
         * 3. Hash 1024 times (2^10, strength=10)
         * 4. Embed salt in hash
         * 5. Return: "$2a$10$x8Ks2pQz...[hash]..."
         *
         * Time taken: ~100ms
         * Can it be reversed? NO! Mathematically impossible
         *
         * SECURITY CRITICAL:
         * ❌ NEVER do this: user.setPassword(request.getPassword())
         *    This stores plain text password → CATASTROPHIC security breach!
         *
         * ✅ ALWAYS do this: user.setPassword(passwordEncoder.encode(...))
         *    This stores BCrypt hash → Secure even if database is stolen
         *
         * What if database is hacked?
         * Plain text storage:
         * - Attacker sees: password = "myPassword123"
         * - Attacker can login immediately
         * - All accounts compromised instantly
         * - Password reuse exposes other accounts
         *
         * BCrypt storage:
         * - Attacker sees: password = "$2a$10$N9qo8uLO..."
         * - Attacker cannot reverse hash
         * - Must brute force each password individually
         * - At ~100ms per attempt, takes YEARS
         * - Strong passwords are practically uncrackable
         *
         * Additional security:
         * - Use HTTPS to protect password in transit
         * - Never log passwords (even hashed ones)
         * - Rate limit login attempts
         * - Implement account lockout after failed attempts
         */
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        user.setPassword(hashedPassword);

        /*
         * Memory management note:
         * The plain text password (request.getPassword()) is now:
         * 1. Stored in hashedPassword variable (hashed form)
         * 2. Still in request object (plain text)
         *
         * Once this method returns:
         * - request object may be garbage collected
         * - Plain text password is removed from memory
         * - Only hash remains in database
         *
         * For extra security (optional):
         * You could clear the password from request:
         * request.setPassword(null);
         * But Java's garbage collector will handle it anyway
         */

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 7: SET FULL NAME (OPTIONAL)
         * ═══════════════════════════════════════════════════════════
         *
         * Full name is optional during registration
         *
         * Can be null or empty → That's OK!
         * User can add it later in profile settings
         */
        user.setFullName(request.getFullName());

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 8: SET DEFAULT ROLE
         * ═══════════════════════════════════════════════════════════
         *
         * All new users get ROLE_USER by default
         *
         * Why not let users choose their role?
         * - Security! Users could make themselves admins
         * - Only system/admins should assign roles
         *
         * How to make someone admin?
         * - Manual database update
         * - Admin panel (restricted endpoint)
         * - Never through public registration
         *
         * Role values:
         * - ROLE_USER: Regular user (default)
         * - ROLE_ADMIN: Administrator (manually assigned)
         *
         * Spring Security convention:
         * - Roles must start with "ROLE_" prefix
         * - Spring removes prefix when checking
         * - hasRole('USER') checks for 'ROLE_USER'
         * - hasRole('ADMIN') checks for 'ROLE_ADMIN'
         */
        user.setRole("ROLE_USER");

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 9: ENABLE ACCOUNT
         * ═══════════════════════════════════════════════════════════
         *
         * Set account as enabled (active)
         *
         * enabled = true: User can login immediately
         * enabled = false: Account exists but cannot login
         *
         * Use cases for disabled accounts:
         * 1. Email verification required:
         *    - Set enabled = false on registration
         *    - Send verification email
         *    - Set enabled = true when email clicked
         *
         * 2. Account suspension:
         *    - Admin disables rule-breaking accounts
         *    - User cannot login until re-enabled
         *
         * 3. Soft delete:
         *    - Instead of deleting user
         *    - Disable account
         *    - Preserve data for audit/compliance
         *
         * Current implementation:
         * - Immediate activation (enabled = true)
         * - User can login right after registration
         *
         * Production improvement:
         * - Require email verification
         * - Set enabled = false here
         * - Create verification token
         * - Send verification email
         * - Enable account when token verified
         */
        user.setEnabled(true);

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 10: SAVE TO DATABASE
         * ═══════════════════════════════════════════════════════════
         *
         * Persist user entity to database
         *
         * What happens:
         * userRepo.save(user)
         * → Hibernate generates SQL
         * → Executes INSERT statement
         * → Database assigns ID (auto-increment)
         * → Returns user with ID populated
         *
         * Generated SQL (approximately):
         * INSERT INTO users (username, email, password, full_name, role, enabled, created_at, updated_at)
         * VALUES ('john_doe', 'john@example.com', '$2a$10$...', 'John Doe', 'ROLE_USER', true, NOW(), NOW())
         * RETURNING id;
         *
         * After save:
         * - user.getId() is populated (e.g., 123)
         * - user.getCreatedAt() is set (timestamp)
         * - user.getUpdatedAt() is set (timestamp)
         * - User exists in database permanently
         *
         * Transaction behavior:
         * - This save is part of @Transactional method
         * - Changes are not committed yet
         * - If exception occurs after this:
         *   → Transaction rolls back
         *   → INSERT is undone
         *   → Database unchanged
         * - If method completes successfully:
         *   → Transaction commits
         *   → Changes are permanent
         */
        User savedUser = userRepo.save(user);

        /*
         * Why save to new variable?
         * - save() returns the persisted entity
         * - ID and timestamps are populated
         * - We need these for the DTO
         * - Original user object is updated too (JPA managed entity)
         * - But explicit variable makes code clearer
         */

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 11: CONVERT TO DTO
         * ═══════════════════════════════════════════════════════════
         *
         * Convert User entity to UserDTO
         *
         * Why convert?
         * - Entity has password field (even hashed, shouldn't be exposed)
         * - DTO only has public information
         * - Safe to send to frontend
         * - Follows separation of concerns
         *
         * What convertToDTO() does:
         * - Copy safe fields: id, username, email, etc.
         * - EXCLUDE password field
         * - Return UserDTO
         *
         * Flow:
         * Entity (User) → Service → DTO (UserDTO) → Controller → Frontend
         *
         * Frontend receives:
         * {
         *   "id": 123,
         *   "username": "john_doe",
         *   "email": "john@example.com",
         *   "fullName": "John Doe",
         *   "role": "ROLE_USER",
         *   "enabled": true,
         *   "createdAt": "2026-03-23T10:30:00",
         *   "updatedAt": "2026-03-23T10:30:00"
         * }
         *
         * Notice: NO password field!
         *
         * Security note:
         * Even though password is hashed, we still don't expose it
         * Why?
         * - Defense in depth
         * - No reason for frontend to see it
         * - Reduces attack surface
         * - Password hash could reveal algorithm/salt info
         */
        return convertToDTO(savedUser);

        /*
         * ═══════════════════════════════════════════════════════════
         * REGISTRATION COMPLETE!
         * ═══════════════════════════════════════════════════════════
         *
         * Summary of what happened:
         * 1. ✅ Validated username availability
         * 2. ✅ Validated email availability
         * 3. ✅ Created User entity
         * 4. ✅ Hashed password securely
         * 5. ✅ Set default role and enabled status
         * 6. ✅ Saved to database
         * 7. ✅ Converted to safe DTO
         * 8. ✅ Returned to controller
         *
         * What happens next:
         * 1. Controller receives UserDTO
         * 2. Controller wraps in HTTP response
         * 3. Sends to frontend as JSON
         * 4. Frontend stores user info (no password)
         * 5. User can now login!
         *
         * Database state:
         * users table:
         * | id  | username | email           | password (hash)      | role      | enabled |
         * |-----|----------|-----------------|----------------------|-----------|---------|
         * | 123 | john_doe | john@example.com| $2a$10$N9qo8uLO...  | ROLE_USER | true    |
         */
    }

    /**
     * FIND USER BY USERNAME
     *
     * Fetch user by username and convert to DTO.
     *
     * Used by:
     * - Login endpoint (get user info)
     * - Current user endpoint (get latest data)
     *
     * @param username - Username to search for
     * @return UserDTO - User information
     * @throws RuntimeException if user not found
     */
    public UserDTO findByUsername(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        return convertToDTO(user);
    }

    /**
     * CONVERT ENTITY TO DTO
     *
     * Helper method to convert User entity to UserDTO
     *
     * Purpose:
     * - Hide sensitive information (password)
     * - Create API-safe representation
     * - Maintain clean separation of concerns
     *
     * What's copied:
     * ✅ id - User identifier
     * ✅ username - Display name
     * ✅ email - Contact info
     * ✅ fullName - Optional display name
     * ✅ role - Permission level
     * ✅ enabled - Account status
     * ✅ createdAt - Registration timestamp
     * ✅ updatedAt - Last update timestamp
     *
     * What's NOT copied:
     * ❌ password - NEVER expose, even hashed!
     * ❌ notes - Would cause circular serialization
     *
     * @param user - Entity from database
     * @return UserDTO - Safe public representation
     */
    private UserDTO convertToDTO(User user) {
        /*
         * Manual field mapping
         *
         * Alternative approaches:
         *
         * 1. ModelMapper (library):
         *    ModelMapper mapper = new ModelMapper();
         *    return mapper.map(user, UserDTO.class);
         *
         * 2. MapStruct (annotation processor):
         *    @Mapper
         *    interface UserMapper {
         *        UserDTO toDTO(User user);
         *    }
         *
         * 3. Lombok Builder:
         *    return UserDTO.builder()
         *        .id(user.getId())
         *        .username(user.getUsername())
         *        .build();
         *
         * Why manual mapping here?
         * - Full control over what's copied
         * - No external dependencies
         * - Easy to understand for learning
         * - No magic/hidden behavior
         */
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );

        /*
         * This creates a new UserDTO using AllArgsConstructor
         * Generated by Lombok in UserDTO class
         *
         * Equivalent to:
         * UserDTO dto = new UserDTO();
         * dto.setId(user.getId());
         * dto.setUsername(user.getUsername());
         * dto.setEmail(user.getEmail());
         * // ... etc
         * return dto;
         */
    }

    /*
     * ═══════════════════════════════════════════════════════════
     * ADDITIONAL METHODS WE'LL ADD IN NEXT PHASES
     * ═══════════════════════════════════════════════════════════
     *
     * // Find user by username (for login)
     * public UserDTO findByUsername(String username) { ... }
     *
     * // Find user by email (for password reset)
     * public UserDTO findByEmail(String email) { ... }
     *
     * // Update user profile
     * public UserDTO updateProfile(Long id, UpdateProfileRequest request) { ... }
     *
     * // Change password
     * public void changePassword(Long id, ChangePasswordRequest request) { ... }
     *
     * // Get current authenticated user
     * public UserDTO getCurrentUser() { ... }
     *
     * // Delete user account
     * public void deleteUser(Long id) { ... }
     */
}

/*
 * ═══════════════════════════════════════════════════════════
 * KEY TAKEAWAYS - USER SERVICE
 * ═══════════════════════════════════════════════════════════
 *
 * 1. SERVICE LAYER ROLE
 *    - Business logic (validation, rules)
 *    - Data conversion (Entity ↔ DTO)
 *    - Transaction management
 *    - Orchestration (coordinate multiple repos)
 *
 * 2. PASSWORD SECURITY
 *    - ALWAYS hash passwords
 *    - Use BCrypt (or Argon2, PBKDF2)
 *    - NEVER store plain text
 *    - NEVER expose hashes to frontend
 *
 * 3. VALIDATION STRATEGY
 *    - Bean Validation in DTO (format, length)
 *    - Business validation in Service (uniqueness)
 *    - Database constraints (last defense)
 *    - Fail fast, clear error messages
 *
 * 4. TRANSACTION MANAGEMENT
 *    - @Transactional ensures atomicity
 *    - All-or-nothing (no partial saves)
 *    - Rollback on exception
 *    - Database consistency guaranteed
 *
 * 5. ENTITY VS DTO
 *    - Entity = Database representation
 *    - DTO = API representation
 *    - Never mix them
 *    - Service converts between them
 *
 * 6. DEPENDENCY INJECTION
 *    - Constructor injection (best practice)
 *    - Final fields (immutability)
 *    - Easy testing (pass mocks)
 *    - Explicit dependencies
 */

