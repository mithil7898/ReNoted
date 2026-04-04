package com.renoted.service;

import com.renoted.entity.User;
import com.renoted.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;

/**
 * CUSTOM USER DETAILS SERVICE - Load Users for Authentication
 *
 * PURPOSE:
 * This service tells Spring Security how to load user information
 * from our database for authentication purposes.
 *
 * WHAT IS UserDetailsService?
 * UserDetailsService is a core Spring Security interface with ONE method:
 * - loadUserByUsername(String username)
 *
 * Spring Security uses this during authentication:
 * 1. User submits login (username + password)
 * 2. Spring Security calls loadUserByUsername(username)
 * 3. We fetch User from database
 * 4. We convert User entity to UserDetails
 * 5. Spring Security compares passwords
 * 6. If match → Authentication successful!
 *
 * WHY DO WE NEED THIS?
 * Spring Security doesn't know about our User entity or UserRepository.
 * We must teach it how to load users from OUR database.
 *
 * Flow:
 * Login Request → AuthenticationManager → UserDetailsService → Database
 *                                            ↑
 *                                    This is what we're implementing!
 *
 * @Service:
 * - Marks this as a Spring service
 * - Spring will create instance
 * - Makes it available for injection
 *
 * @RequiredArgsConstructor:
 * - Lombok annotation
 * - Generates constructor for final fields
 * - Enables dependency injection
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    /**
     * DEPENDENCY INJECTION
     *
     * We need UserRepository to fetch users from database.
     *
     * Why final?
     * - Immutable after construction
     * - Thread-safe
     * - Required dependency (can't be null)
     *
     * Lombok's @RequiredArgsConstructor generates:
     * public CustomUserDetailsService(UserRepository userRepository) {
     *     this.userRepository = userRepository;
     * }
     */
    private final UserRepo userRepo;

    /**
     * LOAD USER BY USERNAME - Core Method for Authentication
     *
     * This is THE method Spring Security calls during authentication!
     *
     * WHEN IS THIS CALLED?
     *
     * Scenario 1: Login
     * User submits login form
     * → AuthenticationManager tries to authenticate
     * → Calls loadUserByUsername(username)
     * → We return UserDetails
     * → Spring compares passwords
     * → Success or failure
     *
     * Scenario 2: JWT Validation (Every Request)
     * Request with JWT token arrives
     * → JwtAuthenticationFilter extracts username from token
     * → Calls loadUserByUsername(username)
     * → We return UserDetails
     * → Spring validates token against UserDetails
     * → Request proceeds or rejected
     *
     * WHAT THIS METHOD DOES:
     * 1. Receive username (from login or JWT)
     * 2. Query database for user
     * 3. If not found → Throw UsernameNotFoundException
     * 4. If found → Convert to UserDetails
     * 5. Return UserDetails to Spring Security
     *
     * Flow Diagram:
     *
     * Login: "john_doe" + "password123"
     *    ↓
     * Spring Security: "Let me verify this"
     *    ↓
     * loadUserByUsername("john_doe")
     *    ↓
     * userRepository.findByUsername("john_doe")
     *    ↓
     * Database: Returns User entity
     *    ↓
     * Convert to UserDetails (Spring Security format)
     *    ↓
     * Return to Spring Security
     *    ↓
     * Spring Security: Compare passwords
     *    - User password hash from DB: "$2a$10$N9qo8uLO..."
     *    - Input password: "password123"
     *    - BCrypt hash input: "$2a$10$N9qo8uLO..."
     *    - Match? YES → Authenticated! ✅
     *
     * @param username - Username to load (from login or JWT)
     * @return UserDetails - Spring Security user representation
     * @throws UsernameNotFoundException - If user not found in database
     *
     * IMPORTANT INTERFACE CONTRACT:
     * - Must throw UsernameNotFoundException if user not found
     * - Must return UserDetails if user found
     * - Never return null!
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 1: FETCH USER FROM DATABASE
         * ═══════════════════════════════════════════════════════════
         *
         * Query database for user by username.
         *
         * userRepository.findByUsername(username):
         * - Executes: SELECT * FROM users WHERE username = ?
         * - Returns: Optional<User>
         *
         * Optional<User>:
         * - Container that may or may not have a value
         * - isPresent() → true if user exists
         * - get() → returns User if exists
         * - orElseThrow() → returns User or throws exception
         *
         * Why Optional?
         * - Prevents null pointer exceptions
         * - Forces us to handle "not found" case
         * - Makes code more explicit
         *
         * .orElseThrow():
         * - If user exists → Return User
         * - If user doesn't exist → Throw exception
         *
         * Lambda expression:
         * () -> new UsernameNotFoundException(...)
         *
         * Equivalent to:
         * new Supplier<UsernameNotFoundException>() {
         *     @Override
         *     public UsernameNotFoundException get() {
         *         return new UsernameNotFoundException("User not found: " + username);
         *     }
         * }
         *
         * UsernameNotFoundException:
         * - Spring Security exception
         * - Signals authentication failure
         * - Spring catches this and returns 401 Unauthorized
         * - User sees: "Invalid credentials"
         *
         * Security Note:
         * Don't reveal whether username or password was wrong!
         *
         * Bad (Security Risk):
         * throw new UsernameNotFoundException("Username not found")
         * → Attacker knows username doesn't exist
         * → Can enumerate valid usernames
         *
         * Good (Secure):
         * Spring Security shows generic message:
         * "Invalid username or password"
         * → Attacker can't tell which was wrong
         * → Prevents user enumeration
         */
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 2: CONVERT TO USERDETAILS
         * ═══════════════════════════════════════════════════════════
         *
         * Spring Security doesn't know about our User entity.
         * It only understands UserDetails interface.
         *
         * We must convert: User entity → UserDetails
         *
         * What is UserDetails?
         * Spring Security interface with these methods:
         * - getUsername() → User identifier
         * - getPassword() → Hashed password
         * - getAuthorities() → Roles/permissions
         * - isEnabled() → Account active?
         * - isAccountNonExpired() → Account expired?
         * - isAccountNonLocked() → Account locked?
         * - isCredentialsNonExpired() → Password expired?
         *
         * org.springframework.security.core.userdetails.User:
         * Built-in implementation of UserDetails interface
         * (Note: Different from our com.renoted.entity.User!)
         *
         * Constructor Parameters:
         * 1. username - String
         * 2. password - String (hashed)
         * 3. authorities - Collection<GrantedAuthority>
         *
         * Why this works:
         * - Spring Security gets UserDetails
         * - Can call getPassword() to get hash
         * - Can call getAuthorities() to check permissions
         * - Can verify account status
         *
         * Password Comparison:
         * Spring Security does:
         * passwordEncoder.matches(inputPassword, userDetails.getPassword())
         * → BCrypt hashes input
         * → Compares with stored hash
         * → Returns true/false
         *
         * We don't compare passwords ourselves!
         * Spring Security handles it automatically!
         */
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),           // Username from our entity
                user.getPassword(),           // BCrypt hash from our entity
                getAuthorities(user)          // Convert role to authorities
        );

        /*
         * What happens next?
         *
         * Login scenario:
         * 1. Spring Security gets UserDetails
         * 2. Extracts password hash: "$2a$10$N9qo8uLO..."
         * 3. User input password: "password123"
         * 4. BCrypt hashes input with same salt
         * 5. Compares hashes
         * 6. If match → Authentication success!
         * 7. If no match → Authentication failure!
         *
         * JWT validation scenario:
         * 1. Spring Security gets UserDetails
         * 2. Validates username matches token
         * 3. Checks authorities if needed
         * 4. Sets authentication in SecurityContext
         * 5. Request proceeds to controller
         */
    }

    /**
     * CONVERT USER ROLE TO AUTHORITIES
     *
     * Spring Security uses "authorities" for permissions.
     * We must convert our role string to GrantedAuthority collection.
     *
     * What is GrantedAuthority?
     * Interface representing a permission granted to a user.
     *
     * Examples:
     * - ROLE_USER
     * - ROLE_ADMIN
     * - READ_PRIVILEGE
     * - WRITE_PRIVILEGE
     *
     * SimpleGrantedAuthority:
     * Basic implementation of GrantedAuthority.
     * Wraps a string (the role name).
     *
     * Our User entity has:
     * private String role = "ROLE_USER";
     *
     * We convert to:
     * Collection<GrantedAuthority> = [SimpleGrantedAuthority("ROLE_USER")]
     *
     * Why Collection?
     * - Users can have multiple roles/permissions
     * - Example: [ROLE_USER, ROLE_ADMIN, READ_PRIVILEGE]
     * - We only have one role per user (for now)
     *
     * Collections.singletonList():
     * - Creates immutable list with one element
     * - More efficient than ArrayList for single item
     * - Cannot be modified after creation
     *
     * Flow:
     * User entity: role = "ROLE_USER"
     *    ↓
     * new SimpleGrantedAuthority("ROLE_USER")
     *    ↓
     * Collections.singletonList(authority)
     *    ↓
     * [GrantedAuthority("ROLE_USER")]
     *    ↓
     * Spring Security can check permissions
     *
     * Usage in Controllers (later):
     * @PreAuthorize("hasRole('USER')")  // Checks for ROLE_USER
     * @PreAuthorize("hasRole('ADMIN')") // Checks for ROLE_ADMIN
     *
     * Spring Security automatically:
     * - Adds "ROLE_" prefix if missing
     * - hasRole('USER') checks for 'ROLE_USER'
     * - hasAuthority('ROLE_USER') checks exact string
     *
     * Multiple Roles Example (if we supported it):
     * List<GrantedAuthority> authorities = new ArrayList<>();
     * authorities.add(new SimpleGrantedAuthority(user.getRole()));
     * authorities.add(new SimpleGrantedAuthority("READ_PRIVILEGE"));
     * return authorities;
     *
     * @param user - User entity with role
     * @return Collection<GrantedAuthority> - Spring Security authorities
     */
    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        /*
         * Convert single role string to GrantedAuthority collection
         *
         * Example:
         * Input: user.getRole() = "ROLE_USER"
         * Output: [SimpleGrantedAuthority("ROLE_USER")]
         *
         * This allows Spring Security to:
         * 1. Check if user has specific role
         * 2. Authorize access to endpoints
         * 3. Display/hide UI elements based on role
         */
        return Collections.singletonList(new SimpleGrantedAuthority(user.getRole()));
    }
}

/*
 * ═══════════════════════════════════════════════════════════
 * KEY TAKEAWAYS - CUSTOM USER DETAILS SERVICE
 * ═══════════════════════════════════════════════════════════
 *
 * 1. PURPOSE
 *    - Teaches Spring Security how to load users
 *    - Bridges our User entity and Spring Security
 *    - Called during authentication and JWT validation
 *
 * 2. LOAD USER BY USERNAME
 *    - Fetches user from database
 *    - Throws exception if not found
 *    - Converts to UserDetails
 *    - Returns to Spring Security
 *
 * 3. USER DETAILS
 *    - Spring Security user representation
 *    - Contains username, password, authorities
 *    - Used for authentication and authorization
 *    - Different from our User entity!
 *
 * 4. AUTHORITIES
 *    - Represent permissions/roles
 *    - Used for access control
 *    - Our user has one role
 *    - Can have multiple in advanced scenarios
 *
 * 5. SECURITY
 *    - Never return null (throw exception instead)
 *    - Generic error messages (prevent enumeration)
 *    - Password comparison handled by Spring
 *    - We only provide the hash
 */