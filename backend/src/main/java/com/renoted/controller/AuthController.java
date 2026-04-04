package com.renoted.controller;

import com.renoted.dto.AuthResponse;
import com.renoted.dto.LoginRequest;
import com.renoted.dto.RegisterRequest;
import com.renoted.dto.UserDTO;
import com.renoted.service.UserService;
import com.renoted.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

/**
 * AUTHENTICATION CONTROLLER - Login and Registration Endpoints
 *
 * PURPOSE:
 * Handles user authentication operations:
 * 1. User registration (create account)
 * 2. User login (get JWT token)
 * 3. Get current user info
 *
 * ENDPOINTS:
 * POST /api/auth/register - Create new account
 * POST /api/auth/login    - Login and receive JWT
 * GET  /api/auth/me       - Get current user info
 *
 * SECURITY:
 * - /register and /login are public (permitAll in SecurityConfig)
 * - /me requires authentication (valid JWT token)
 *
 * @RestController:
 * - Marks this as a REST API controller
 * - Methods return data (not views)
 * - Automatic JSON serialization
 *
 * @RequestMapping("/api/auth"):
 * - Base path for all endpoints
 * - All methods start with /api/auth
 *
 * @RequiredArgsConstructor:
 * - Generates constructor for final fields
 * - Enables dependency injection
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    /**
     * DEPENDENCIES
     *
     * UserService:
     * - Handle user registration
     * - Convert User to UserDTO
     *
     * AuthenticationManager:
     * - Validate login credentials
     * - Authenticate user
     *
     * JwtUtil:
     * - Generate JWT tokens
     * - Create access tokens for users
     *
     * UserDetailsService:
     * - Load user for token generation
     * - Convert to Spring Security format
     *
     * Why final?
     * - Immutable after construction
     * - Thread-safe
     * - Required dependencies
     */
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    /**
     * REGISTER NEW USER
     *
     * Endpoint: POST /api/auth/register
     *
     * Purpose:
     * Create a new user account and return JWT token.
     *
     * REQUEST:
     * {
     *   "username": "john_doe",
     *   "email": "john@example.com",
     *   "password": "password123",
     *   "fullName": "John Doe"
     * }
     *
     * RESPONSE (Success - 200 OK):
     * {
     *   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *   "user": {
     *     "id": 1,
     *     "username": "john_doe",
     *     "email": "john@example.com",
     *     "fullName": "John Doe",
     *     "role": "ROLE_USER",
     *     "enabled": true,
     *     "createdAt": "2026-03-25T10:30:00",
     *     "updatedAt": "2026-03-25T10:30:00"
     *   }
     * }
     *
     * RESPONSE (Error - 400 Bad Request):
     * - Username already exists
     * - Email already exists
     * - Validation failed
     *
     * FLOW:
     * 1. Frontend sends registration data
     * 2. Spring validates (@Valid annotation)
     * 3. UserService creates user
     * 4. Password is hashed (BCrypt)
     * 5. User saved to database
     * 6. Generate JWT token for user
     * 7. Return token + user info
     * 8. User is automatically logged in!
     *
     * @PostMapping:
     * - Maps POST requests to this method
     * - Path: /api/auth/register
     *
     * @RequestBody:
     * - Reads JSON from request body
     * - Deserializes to RegisterRequest object
     *
     * @Valid:
     * - Triggers validation annotations
     * - @NotBlank, @Email, @Size, etc.
     * - If validation fails → 400 Bad Request
     * - Error details in response body
     *
     * ResponseEntity<AuthResponse>:
     * - Wrapper for HTTP response
     * - Contains status code + body
     * - ResponseEntity.ok() → 200 OK
     *
     * @param request - Registration data (validated)
     * @return ResponseEntity with JWT token and user info
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 1: CREATE USER ACCOUNT
         * ═══════════════════════════════════════════════════════════
         *
         * UserService handles:
         * 1. Check username availability
         * 2. Check email availability
         * 3. Hash password with BCrypt
         * 4. Create User entity
         * 5. Save to database
         * 6. Convert to UserDTO
         *
         * What happens inside:
         * - userRepository.existsByUsername() → Check uniqueness
         * - userRepository.existsByEmail() → Check uniqueness
         * - passwordEncoder.encode() → Hash password
         * - userRepository.save() → Persist to database
         * - convertToDTO() → Safe public representation
         *
         * Possible exceptions:
         * - RuntimeException("Username already exists")
         * - RuntimeException("Email already exists")
         *
         * These exceptions:
         * - Caught by Spring's exception handler
         * - Returned as 400 Bad Request (or 500 if uncaught)
         * - Message included in response
         *
         * Improvement:
         * Create custom exceptions:
         * - UsernameAlreadyExistsException
         * - EmailAlreadyExistsException
         * - Handle with @ExceptionHandler
         * - Return specific error responses
         */
        UserDTO user = userService.registerUser(request);

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 2: LOAD USER DETAILS FOR TOKEN GENERATION
         * ═══════════════════════════════════════════════════════════
         *
         * Why load user again?
         * - We need UserDetails (Spring Security format)
         * - UserDTO doesn't have password or authorities
         * - JwtUtil needs UserDetails to generate token
         *
         * userDetailsService.loadUserByUsername():
         * - Calls CustomUserDetailsService
         * - Queries database for user
         * - Converts to UserDetails
         * - Returns Spring Security representation
         *
         * UserDetails contains:
         * - username
         * - password (hashed)
         * - authorities (roles)
         * - account status flags
         *
         * Alternative approach:
         * Make registerUser() return UserDetails instead
         * But that mixes concerns (service returns security type)
         * Better to keep separation
         */
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 3: GENERATE JWT TOKEN
         * ═══════════════════════════════════════════════════════════
         *
         * Create JWT token for newly registered user.
         *
         * jwtUtil.generateToken(userDetails):
         * - Extracts username from UserDetails
         * - Creates JWT with:
         *   - Header: Algorithm (HS256)
         *   - Payload: Username, issued time, expiration
         *   - Signature: Signed with secret key
         * - Returns compact JWT string
         *
         * Token example:
         * "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huX2RvZSIsImlhdCI6MTUxNjIzOTAyMiwiZXhwIjoxNTE2MjQyNjIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
         *
         * Token validity:
         * - Configured in JwtUtil (default 10 hours)
         * - After expiration, user must login again
         * - Token contains expiration timestamp
         *
         * Security:
         * - Signed with secret key (tamper-proof)
         * - Contains only username (no sensitive data)
         * - Stateless (no server-side storage)
         */
        String token = jwtUtil.generateToken(userDetails);

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 4: CREATE AND RETURN AUTH RESPONSE
         * ═══════════════════════════════════════════════════════════
         *
         * Package token and user info into response.
         *
         * AuthResponse contains:
         * - token: JWT for authentication
         * - user: Public user information
         *
         * Frontend receives:
         * {
         *   "token": "eyJhbGc...",
         *   "user": {
         *     "id": 1,
         *     "username": "john_doe",
         *     "email": "john@example.com",
         *     ...
         *   }
         * }
         *
         * Frontend will:
         * 1. Store token in localStorage
         * 2. Store user info in state/localStorage
         * 3. Redirect to dashboard
         * 4. User is logged in!
         *
         * ResponseEntity.ok():
         * - Creates response with 200 OK status
         * - Sets body to AuthResponse
         * - Spring serializes to JSON automatically
         *
         * HTTP Response:
         * Status: 200 OK
         * Content-Type: application/json
         * Body: { "token": "...", "user": {...} }
         */
        return ResponseEntity.ok(new AuthResponse(token, user));

        /*
         * ✅ REGISTRATION COMPLETE!
         *
         * What happened:
         * 1. ✅ Validated registration data
         * 2. ✅ Created user account
         * 3. ✅ Hashed password securely
         * 4. ✅ Saved to database
         * 5. ✅ Generated JWT token
         * 6. ✅ Returned token + user info
         *
         * User can now:
         * - Store token
         * - Make authenticated requests
         * - Access protected endpoints
         * - No need to login again!
         */
    }

    /**
     * LOGIN USER
     *
     * Endpoint: POST /api/auth/login
     *
     * Purpose:
     * Authenticate user and return JWT token.
     *
     * REQUEST:
     * {
     *   "username": "john_doe",
     *   "password": "password123"
     * }
     *
     * RESPONSE (Success - 200 OK):
     * {
     *   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *   "user": {
     *     "id": 1,
     *     "username": "john_doe",
     *     "email": "john@example.com",
     *     "fullName": "John Doe",
     *     "role": "ROLE_USER",
     *     "enabled": true,
     *     "createdAt": "2026-03-25T10:30:00",
     *     "updatedAt": "2026-03-25T10:30:00"
     *   }
     * }
     *
     * RESPONSE (Error - 401 Unauthorized):
     * - Invalid username
     * - Invalid password
     * - Account disabled
     *
     * FLOW:
     * 1. Frontend sends credentials
     * 2. Spring validates format
     * 3. AuthenticationManager authenticates
     * 4. If valid → Generate JWT
     * 5. If invalid → 401 Unauthorized
     * 6. Return token + user info
     *
     * AUTHENTICATION PROCESS:
     * 1. Create authentication token (unauthenticated)
     * 2. AuthenticationManager.authenticate()
     *    - Calls UserDetailsService
     *    - Loads user from database
     *    - Compares password hashes (BCrypt)
     *    - Returns authenticated token if valid
     *    - Throws BadCredentialsException if invalid
     * 3. Generate JWT
     * 4. Return to user
     *
     * @param request - Login credentials (validated)
     * @return ResponseEntity with JWT token and user info
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 1: AUTHENTICATE USER
         * ═══════════════════════════════════════════════════════════
         *
         * Verify username and password are correct.
         *
         * UsernamePasswordAuthenticationToken:
         * - Represents authentication request
         * - Contains username and password
         * - Initially UNAUTHENTICATED
         *
         * Constructor parameters:
         * 1. principal (username): User identifier
         * 2. credentials (password): Plain text password
         *
         * authenticationManager.authenticate():
         * - Core Spring Security authentication
         * - Does the following:
         *   a. Calls UserDetailsService.loadUserByUsername()
         *   b. Gets UserDetails from database
         *   c. Uses PasswordEncoder to compare passwords
         *   d. BCrypt.matches(input, stored_hash)
         *   e. If match → Returns authenticated token
         *   f. If no match → Throws BadCredentialsException
         *
         * What happens inside:
         *
         * 1. Load user:
         *    UserDetails user = userDetailsService.loadUserByUsername("john_doe")
         *    → Returns user from database
         *
         * 2. Compare passwords:
         *    Input: "password123" (plain text from user)
         *    Stored: "$2a$10$N9qo8uLO..." (BCrypt hash from DB)
         *
         *    BCrypt process:
         *    - Extract salt from stored hash
         *    - Hash input with same salt
         *    - Compare hashes
         *    - Match? → Success! ✅
         *    - No match? → Failure! ❌
         *
         * 3. Return authentication:
         *    If passwords match:
         *    → Returns Authentication object (authenticated = true)
         *
         *    If passwords don't match:
         *    → Throws BadCredentialsException
         *    → Spring catches and returns 401 Unauthorized
         *
         * Possible Exceptions:
         *
         * BadCredentialsException:
         * - Wrong username or password
         * - Spring returns 401 Unauthorized
         * - Generic message (don't reveal which was wrong!)
         *
         * UsernameNotFoundException:
         * - User doesn't exist
         * - Also returns 401 Unauthorized
         * - Same generic message (security!)
         *
         * DisabledException:
         * - Account is disabled
         * - Returns 401 Unauthorized
         * - Message: "Account disabled"
         *
         * LockedException:
         * - Account is locked
         * - Returns 401 Unauthorized
         * - Message: "Account locked"
         *
         * We don't catch these exceptions here:
         * - Spring's exception handler manages them
         * - Returns proper HTTP status codes
         * - Includes error messages in response
         */
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        /*
         * ✅ AUTHENTICATION SUCCESSFUL!
         *
         * If we reach this line:
         * - Username exists in database
         * - Password hash matches
         * - Account is enabled
         * - User is authenticated!
         *
         * If authentication failed:
         * - Exception thrown above
         * - This code doesn't execute
         * - Spring returns error response
         */

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 2: LOAD USER DETAILS
         * ═══════════════════════════════════════════════════════════
         *
         * Get full user information for token generation.
         *
         * Why load again?
         * - Authentication only validates credentials
         * - We need UserDetails to generate JWT
         * - UserDetails has username for token payload
         *
         * loadUserByUsername():
         * - Queries database
         * - Returns UserDetails
         * - Same user we just authenticated
         *
         * Could we skip this?
         * - Authentication object contains UserDetails
         * - But cleaner to load explicitly
         * - More readable code
         * - Consistent pattern
         */
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 3: GENERATE JWT TOKEN
         * ═══════════════════════════════════════════════════════════
         *
         * Create access token for authenticated user.
         *
         * Same process as registration:
         * - Extract username from UserDetails
         * - Create JWT with claims
         * - Sign with secret key
         * - Return token string
         *
         * Token contains:
         * - Username
         * - Issued timestamp
         * - Expiration timestamp
         *
         * Token is valid for:
         * - Configured duration (default 10 hours)
         * - User can make authenticated requests
         * - After expiration, must login again
         */
        String token = jwtUtil.generateToken(userDetails);

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 4: GET USER DTO
         * ═══════════════════════════════════════════════════════════
         *
         * We need UserDTO for response, not UserDetails.
         *
         * Why?
         * - UserDetails has password hash (don't send!)
         * - UserDetails is Spring Security format
         * - UserDTO is our API format (safe, public)
         *
         * We fetch from UserService:
         * - Queries database by username
         * - Converts User entity to UserDTO
         * - Returns safe public representation
         *
         * UserDTO contains:
         * - id, username, email, fullName
         * - role, enabled status
         * - timestamps
         * - NO password field!
         */
        UserDTO user = userService.findByUsername(request.getUsername());

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 5: RETURN AUTH RESPONSE
         * ═══════════════════════════════════════════════════════════
         *
         * Package and send token + user info.
         *
         * Frontend receives:
         * {
         *   "token": "eyJhbGc...",
         *   "user": {...}
         * }
         *
         * Frontend will:
         * 1. Store token in localStorage
         * 2. Store user in state
         * 3. Redirect to dashboard
         * 4. Start making authenticated requests
         *
         * HTTP Response:
         * Status: 200 OK
         * Body: { "token": "...", "user": {...} }
         */
        return ResponseEntity.ok(new AuthResponse(token, user));

        /*
         * ✅ LOGIN COMPLETE!
         *
         * What happened:
         * 1. ✅ Validated credentials
         * 2. ✅ Authenticated user
         * 3. ✅ Generated JWT token
         * 4. ✅ Returned token + user info
         *
         * User can now:
         * - Store token
         * - Access protected endpoints
         * - Make authenticated requests
         */
    }

    /**
     * GET CURRENT USER
     *
     * Endpoint: GET /api/auth/me
     *
     * Purpose:
     * Get information about currently authenticated user.
     *
     * REQUEST:
     * GET /api/auth/me
     * Headers:
     *   Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     *
     * RESPONSE (Success - 200 OK):
     * {
     *   "id": 1,
     *   "username": "john_doe",
     *   "email": "john@example.com",
     *   "fullName": "John Doe",
     *   "role": "ROLE_USER",
     *   "enabled": true,
     *   "createdAt": "2026-03-25T10:30:00",
     *   "updatedAt": "2026-03-25T10:30:00"
     * }
     *
     * RESPONSE (Error - 401 Unauthorized):
     * - No token provided
     * - Invalid token
     * - Expired token
     *
     * USE CASES:
     * - Frontend refresh (page reload)
     * - Get latest user data
     * - Verify token still valid
     * - Update user profile display
     *
     * FLOW:
     * 1. Frontend sends request with JWT
     * 2. JwtAuthenticationFilter validates token
     * 3. Sets authentication in SecurityContext
     * 4. This method reads from SecurityContext
     * 5. Returns current user info
     *
     * SECURITY:
     * - Requires valid JWT token
     * - JwtAuthenticationFilter runs first
     * - Authentication must be set in context
     * - If no auth → 401 Unauthorized
     *
     * @return ResponseEntity with current user info
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser() {
        /*
         * ═══════════════════════════════════════════════════════════
         * GET AUTHENTICATION FROM SECURITY CONTEXT
         * ═══════════════════════════════════════════════════════════
         *
         * SecurityContextHolder:
         * - Thread-local storage for security info
         * - Contains authentication for current request
         * - Set by JwtAuthenticationFilter
         *
         * How did authentication get here?
         * 1. Request arrives with JWT token
         * 2. JwtAuthenticationFilter intercepts
         * 3. Extracts token from header
         * 4. Validates token
         * 5. Loads user from database
         * 6. Creates authentication object
         * 7. Sets in SecurityContext ← Here!
         * 8. Request continues to this controller
         *
         * getContext():
         * - Gets SecurityContext for current thread
         * - Each thread (request) has its own context
         *
         * getAuthentication():
         * - Gets Authentication object
         * - Contains authenticated user info
         * - null if not authenticated
         *
         * Authentication object contains:
         * - principal (UserDetails)
         * - credentials (null for JWT)
         * - authorities (roles)
         * - authenticated flag (true)
         */
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        /*
         * ═══════════════════════════════════════════════════════════
         * EXTRACT USERNAME FROM AUTHENTICATION
         * ═══════════════════════════════════════════════════════════
         *
         * Get the username from authenticated user.
         *
         * authentication.getName():
         * - Returns principal name (username)
         * - Shortcut for: getPrincipal().getUsername()
         * - More convenient than casting to UserDetails
         *
         * Alternative:
         * UserDetails userDetails = (UserDetails) authentication.getPrincipal();
         * String username = userDetails.getUsername();
         *
         * Our way is simpler:
         * String username = authentication.getName();
         *
         * This username comes from JWT token:
         * Token → JwtUtil.extractUsername() → "john_doe"
         * JwtAuthenticationFilter → loadUserByUsername("john_doe")
         * UserDetails → setAuthentication(authToken)
         * SecurityContext → authentication.getName() → "john_doe"
         */
        String username = authentication.getName();

        /*
         * ═══════════════════════════════════════════════════════════
         * FETCH USER FROM DATABASE
         * ═══════════════════════════════════════════════════════════
         *
         * Get fresh user data from database.
         *
         * Why query database?
         * - User data might have changed since token issued
         * - Token only contains username
         * - We want latest email, role, etc.
         * - Return up-to-date information
         *
         * userService.findByUsername():
         * - Queries database for user
         * - Converts User entity to UserDTO
         * - Returns safe public representation
         *
         * UserDTO contains latest:
         * - Email (user might have changed it)
         * - Role (admin might have upgraded them)
         * - Enabled status (account might be disabled)
         * - Other profile data
         *
         * Alternative approach:
         * Store all data in JWT token
         * Problem: Token becomes large and stale
         * Our approach: Token lightweight, always fresh data
         */
        UserDTO user = userService.findByUsername(username);

        /*
         * ═══════════════════════════════════════════════════════════
         * RETURN USER INFO
         * ═══════════════════════════════════════════════════════════
         *
         * Send current user data to frontend.
         *
         * ResponseEntity.ok(user):
         * - Creates 200 OK response
         * - Body contains UserDTO
         * - Spring serializes to JSON
         *
         * Frontend receives:
         * {
         *   "id": 1,
         *   "username": "john_doe",
         *   "email": "john@example.com",
         *   ...
         * }
         *
         * Frontend uses this to:
         * - Display user profile
         * - Show username in header
         * - Update local user state
         * - Verify account status
         * - Check role for UI features
         */
        return ResponseEntity.ok(user);

        /*
         * ✅ REQUEST COMPLETE!
         *
         * What happened:
         * 1. ✅ JWT token validated (by filter)
         * 2. ✅ Authentication set in context
         * 3. ✅ Extracted username
         * 4. ✅ Fetched latest user data
         * 5. ✅ Returned to frontend
         *
         * Frontend can:
         * - Update user display
         * - Verify still logged in
         * - Get latest profile info
         */
    }
}

/*
 * ═══════════════════════════════════════════════════════════
 * KEY TAKEAWAYS - AUTHENTICATION CONTROLLER
 * ═══════════════════════════════════════════════════════════
 *
 * 1. REGISTRATION
 *    - Create user account
 *    - Hash password with BCrypt
 *    - Generate JWT immediately
 *    - Return token + user info
 *    - User auto-logged in
 *
 * 2. LOGIN
 *    - Validate credentials
 *    - AuthenticationManager handles verification
 *    - BCrypt password comparison
 *    - Generate JWT token
 *    - Return token + user info
 *
 * 3. GET CURRENT USER
 *    - Requires valid JWT token
 *    - Read from SecurityContext
 *    - Fetch fresh data from database
 *    - Return latest user info
 *
 * 4. JWT FLOW
 *    - Login/Register → Get token
 *    - Store token in frontend
 *    - Send token in Authorization header
 *    - Backend validates token
 *    - Access granted
 *
 * 5. SECURITY
 *    - Passwords hashed with BCrypt
 *    - JWT tokens signed and verified
 *    - Stateless authentication
 *    - Token expiration enforced
 *    - No passwords in responses
 */