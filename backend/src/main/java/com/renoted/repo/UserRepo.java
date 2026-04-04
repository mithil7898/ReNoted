package com.renoted.repo;

import com.renoted.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * USER REPOSITORY - Database access layer for User entity
 *
 * PURPOSE:
 * This interface provides methods to interact with the users table
 * in the database. Spring Data JPA automatically implements these methods!
 *
 * WHAT IS A REPOSITORY?
 * The Repository pattern separates data access logic from business logic.
 *
 * Layer Structure:
 * Controller → Service → Repository → Database
 *                          ↑
 *                    We are here!
 *
 * SPRING DATA JPA MAGIC:
 * We only write the interface, Spring creates the implementation!
 *
 * How it works:
 * 1. We extend JpaRepository<User, Long>
 * 2. Spring sees this at startup
 * 3. Spring generates implementation class automatically
 * 4. We can inject and use it with @Autowired
 *
 * JpaRepository<User, Long> explained:
 * - User: The entity type this repository manages
 * - Long: The type of the entity's ID field
 *
 * FREE METHODS FROM JpaRepository:
 * We automatically get these methods without writing any code:
 * - save(user) - Insert or update
 * - findById(id) - Find by primary key
 * - findAll() - Get all users
 * - deleteById(id) - Delete by ID
 * - count() - Count total users
 * - existsById(id) - Check if user exists
 *
 * CUSTOM QUERY METHODS:
 * Spring Data JPA can generate queries from method names!
 * Method name pattern: findBy + FieldName + Condition
 */
@Repository  // Marks this as a repository bean (optional with JpaRepository, but good practice)
public interface UserRepo extends JpaRepository<User, Long> {

    /**
     * FIND USER BY USERNAME
     *
     * Method Name Convention:
     * findBy + Username (field name)
     *
     * Spring Data JPA automatically generates this SQL:
     * SELECT * FROM users WHERE username = ?
     *
     * Return Type - Optional<User>:
     * Why Optional instead of User?
     *
     * Problem with returning User directly:
     * - What if username doesn't exist?
     * - Return null? (causes NullPointerException)
     * - Throw exception? (not always appropriate)
     *
     * Solution - Optional:
     * - Container that may or may not have a value
     * - Forces us to handle "not found" case
     * - Prevents null pointer exceptions
     * - Makes code more explicit
     *
     * Usage examples:
     *
     * // Check if user exists
     * Optional<User> userOpt = userRepository.findByUsername("john");
     * if (userOpt.isPresent()) {
     *     User user = userOpt.get();
     *     // Use user
     * }
     *
     * // Get user or throw exception
     * User user = userRepository.findByUsername("john")
     *     .orElseThrow(() -> new UserNotFoundException("User not found"));
     *
     * // Get user or return default
     * User user = userRepository.findByUsername("john")
     *     .orElse(new User());
     *
     * // Execute action if present
     * userRepository.findByUsername("john")
     *     .ifPresent(user -> System.out.println("Found: " + user.getEmail()));
     *
     * @param username - The username to search for
     * @return Optional containing User if found, empty Optional if not found
     */
    Optional<User> findByUsername(String username);

    /**
     * FIND USER BY EMAIL
     *
     * Generated SQL:
     * SELECT * FROM users WHERE email = ?
     *
     * Use case:
     * - Login with email instead of username
     * - Check if email is already registered
     * - Password reset functionality
     * - Email verification
     *
     * Example usage:
     *
     * // Check if email already exists (during registration)
     * Optional<User> existing = userRepository.findByEmail("john@example.com");
     * if (existing.isPresent()) {
     *     throw new EmailAlreadyExistsException("Email already registered!");
     * }
     *
     * // Login with email
     * User user = userRepository.findByEmail(loginRequest.getEmail())
     *     .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
     *
     * @param email - The email address to search for
     * @return Optional containing User if found, empty Optional if not found
     */
    Optional<User> findByEmail(String email);

    /**
     * CHECK IF USERNAME EXISTS
     *
     * Method Name Convention:
     * existsBy + Username
     *
     * Generated SQL:
     * SELECT EXISTS(SELECT 1 FROM users WHERE username = ?)
     *
     * Return Type - boolean:
     * - true: Username exists in database
     * - false: Username is available
     *
     * Performance Note:
     * This is more efficient than findByUsername() for existence checks!
     *
     * Why?
     * - findByUsername() fetches entire User object from database
     * - existsByUsername() only checks existence (no data transfer)
     * - Database returns true/false immediately
     *
     * Use case:
     * - Validate username availability during registration
     * - Check duplicates before insert
     *
     * Example usage:
     *
     * // During registration validation
     * if (userRepository.existsByUsername("john_doe")) {
     *     throw new UsernameAlreadyExistsException("Username already taken!");
     * }
     *
     * // REST endpoint for username availability check
     * @GetMapping("/check-username")
     * public boolean checkUsername(@RequestParam String username) {
     *     return !userRepository.existsByUsername(username);
     * }
     *
     * @param username - The username to check
     * @return true if username exists, false otherwise
     */
    boolean existsByUsername(String username);

    /**
     * CHECK IF EMAIL EXISTS
     *
     * Generated SQL:
     * SELECT EXISTS(SELECT 1 FROM users WHERE email = ?)
     *
     * Use case:
     * - Validate email availability during registration
     * - Prevent duplicate accounts with same email
     *
     * Example usage:
     *
     * // During registration validation
     * if (userRepository.existsByEmail("john@example.com")) {
     *     throw new EmailAlreadyExistsException("Email already registered!");
     * }
     *
     * @param email - The email address to check
     * @return true if email exists, false otherwise
     */
    boolean existsByEmail(String email);

    /*
     * ═══════════════════════════════════════════════════════════
     * ADDITIONAL QUERY METHODS YOU COULD ADD
     * ═══════════════════════════════════════════════════════════
     *
     * Spring Data JPA supports many query patterns:
     *
     * // Find by username or email
     * Optional<User> findByUsernameOrEmail(String username, String email);
     *
     * // Find users by role
     * List<User> findByRole(String role);
     *
     * // Find enabled users only
     * List<User> findByEnabledTrue();
     *
     * // Find users created after date
     * List<User> findByCreatedAtAfter(LocalDateTime date);
     *
     * // Find users with username containing text (search)
     * List<User> findByUsernameContainingIgnoreCase(String search);
     *
     * // Count users by role
     * Long countByRole(String role);
     *
     * // Delete by username (returns number of deleted records)
     * Long deleteByUsername(String username);
     *
     * // Custom JPQL query
     * @Query("SELECT u FROM User u WHERE u.enabled = true AND u.role = :role")
     * List<User> findActiveUsersByRole(@Param("role") String role);
     *
     * // Native SQL query
     * @Query(value = "SELECT * FROM users WHERE created_at > NOW() - INTERVAL '7 days'",
     *        nativeQuery = true)
     * List<User> findRecentUsers();
     */
}

/*
 * ═══════════════════════════════════════════════════════════
 * KEY TAKEAWAYS - SPRING DATA JPA REPOSITORIES
 * ═══════════════════════════════════════════════════════════
 *
 * 1. NO IMPLEMENTATION NEEDED
 *    - Write only the interface
 *    - Spring generates implementation automatically
 *    - Ready to use with @Autowired
 *
 * 2. METHOD NAME CONVENTIONS
 *    - findBy + FieldName → SELECT WHERE field = ?
 *    - existsBy + FieldName → SELECT EXISTS(...)
 *    - countBy + FieldName → SELECT COUNT(...)
 *    - deleteBy + FieldName → DELETE WHERE field = ?
 *
 * 3. RETURN TYPES
 *    - Optional<T> → May or may not exist (safe)
 *    - List<T> → Multiple results (can be empty)
 *    - boolean → Existence check (true/false)
 *    - Long → Count or delete result
 *
 * 4. OPTIONAL BENEFITS
 *    - Prevents NullPointerException
 *    - Forces handling of "not found" case
 *    - Makes code more explicit
 *    - Better than returning null
 *
 * 5. PERFORMANCE TIPS
 *    - Use existsBy for checking existence
 *    - Use findBy only when you need the data
 *    - Add indexes on frequently queried fields
 *    - Use pagination for large result sets
 */