package com.renoted.filter;

import com.renoted.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT AUTHENTICATION FILTER - Validate JWT on Every Request
 *
 * PURPOSE:
 * This filter intercepts EVERY HTTP request and:
 * 1. Extracts JWT token from Authorization header
 * 2. Validates the token
 * 3. Loads user from database
 * 4. Sets authentication in SecurityContext
 * 5. Allows request to proceed to controller
 *
 * WHAT IS A FILTER?
 * A filter is code that runs BEFORE your controller.
 *
 * Request Flow:
 * Client Request
 *    ↓
 * CORS Filter (allow cross-origin)
 *    ↓
 * JwtAuthenticationFilter ← WE ARE HERE!
 *    ↓
 * Security Filter Chain (check permissions)
 *    ↓
 * DispatcherServlet (Spring MVC)
 *    ↓
 * Your Controller (NoteController, etc.)
 *    ↓
 * Response
 *
 * WHY EXTEND OncePerRequestFilter?
 *
 * OncePerRequestFilter:
 * - Guarantees filter runs ONCE per request
 * - Even with forwards/includes
 * - Base class from Spring
 *
 * Alternative: Implementing Filter interface directly
 * Problem: Might execute multiple times
 * Solution: OncePerRequestFilter ensures single execution
 *
 * WHEN DOES THIS RUN?
 *
 * This filter runs on EVERY request:
 * - GET /api/notes ✅
 * - POST /api/notes ✅
 * - POST /api/auth/login ✅ (even public endpoints!)
 * - GET /favicon.ico ✅ (yes, even this!)
 *
 * For public endpoints (/api/auth/**):
 * - Filter runs
 * - No token in request (that's OK!)
 * - Filter skips authentication
 * - Request proceeds (SecurityConfig allows it)
 *
 * For protected endpoints (/api/**):
 * - Filter runs
 * - Token must be present
 * - Token must be valid
 * - User must exist
 * - Authentication set in context
 * - Request proceeds
 *
 * @Component:
 * - Registers this filter as Spring bean
 * - Spring Security auto-configures it
 * - Adds to filter chain
 *
 * @RequiredArgsConstructor:
 * - Generates constructor for final fields
 * - Enables dependency injection
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * DEPENDENCIES
     *
     * We need these services to validate tokens and load users.
     *
     * JwtUtil:
     * - Extract username from token
     * - Validate token
     * - Check expiration
     *
     * UserDetailsService:
     * - Load user from database
     * - Convert to UserDetails
     * - Used for authentication
     *
     * Why final?
     * - Immutable after construction
     * - Thread-safe (filters are singletons!)
     * - Required dependencies
     */
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    /**
     * DO FILTER INTERNAL - Main Filter Logic
     *
     * This method is called for EVERY HTTP request!
     *
     * @NonNull annotations:
     * - Indicates parameters cannot be null
     * - Documentation + runtime checking
     * - IDE warnings if null passed
     *
     * Parameters:
     *
     * request (HttpServletRequest):
     * - Incoming HTTP request
     * - Contains headers, body, parameters
     * - We extract Authorization header from this
     *
     * response (HttpServletResponse):
     * - Outgoing HTTP response
     * - Can set status codes, headers
     * - Usually not modified in this filter
     *
     * filterChain (FilterChain):
     * - Chain of filters
     * - Calling filterChain.doFilter() passes request to next filter
     * - If we don't call it, request stops here!
     *
     * Flow:
     * 1. Extract token from header
     * 2. If no token → Skip authentication, proceed
     * 3. If token exists → Validate it
     * 4. If valid → Set authentication
     * 5. Proceed to next filter
     *
     * @param request - HTTP request
     * @param response - HTTP response
     * @param filterChain - Filter chain
     * @throws ServletException - If servlet error
     * @throws IOException - If I/O error
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 1: EXTRACT JWT TOKEN FROM AUTHORIZATION HEADER
         * ═══════════════════════════════════════════════════════════
         *
         * JWT tokens are sent in the Authorization header:
         * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
         *
         * Format:
         * "Bearer " + token
         *
         * Why "Bearer"?
         * - Standard prefix for token-based auth
         * - From OAuth 2.0 specification
         * - Indicates "bearer token" authentication
         * - Anyone with token can use it (bearer)
         *
         * What we do:
         * 1. Get Authorization header from request
         * 2. Check if it exists
         * 3. Check if it starts with "Bearer "
         * 4. Extract token part (everything after "Bearer ")
         *
         * request.getHeader("Authorization"):
         * - Gets header value
         * - Returns null if header doesn't exist
         * - Case-insensitive ("authorization" works too)
         *
         * Example:
         * Header: "Authorization: Bearer eyJhbGc..."
         * authHeader = "Bearer eyJhbGc..."
         *
         * Header: "Authorization: eyJhbGc..." (missing Bearer)
         * authHeader = "eyJhbGc..." (won't match startsWith check)
         *
         * No header:
         * authHeader = null
         */
        final String authHeader = request.getHeader("Authorization");

        /*
         * Declare variables for token and username
         *
         * Why declare here?
         * - Need to use in multiple if blocks
         * - Scope accessible throughout method
         *
         * Initialize to null:
         * - Will be set if valid header found
         * - Null check later determines if we authenticate
         */
        String jwt = null;
        String username = null;

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 2: VALIDATE AUTHORIZATION HEADER FORMAT
         * ═══════════════════════════════════════════════════════════
         *
         * Check if header exists and has correct format.
         *
         * Conditions:
         * authHeader != null → Header exists
         * authHeader.startsWith("Bearer ") → Correct format
         *
         * Why "Bearer " with space?
         * - Standard format: "Bearer <token>"
         * - Space separates prefix from token
         * - Without space: "Bearereyj..." (wrong!)
         * - With space: "Bearer eyj..." (correct!)
         *
         * Valid examples:
         * "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." ✅
         * "Bearer ey..." ✅
         *
         * Invalid examples:
         * "Bearereyj..." ❌ (no space)
         * "eyJhbG..." ❌ (no Bearer prefix)
         * null ❌ (no header)
         * "Basic dXNlcjpwYXNz" ❌ (different auth type)
         */
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            /*
             * Extract JWT token from header
             *
             * authHeader.substring(7):
             * - Removes first 7 characters
             * - "Bearer " is 7 characters (including space)
             * - Returns everything after "Bearer "
             *
             * Example:
             * Input: "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
             *         0123456
             *         ↑ position 7 starts here
             *
             * Output: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
             *
             * Why substring(7)?
             * "Bearer " = B(0) e(1) a(2) r(3) e(4) r(5) space(6)
             * Position 7 = start of token
             */
            jwt = authHeader.substring(7);

            /*
             * ═══════════════════════════════════════════════════════════
             * STEP 3: EXTRACT USERNAME FROM TOKEN
             * ═══════════════════════════════════════════════════════════
             *
             * Parse JWT token and extract username (subject).
             *
             * jwtUtil.extractUsername(jwt):
             * - Parses token
             * - Verifies signature
             * - Extracts "sub" claim
             * - Returns username string
             *
             * What can go wrong?
             *
             * 1. Invalid signature:
             *    Token was tampered with
             *    → SignatureException thrown
             *
             * 2. Token expired:
             *    Past expiration time
             *    → ExpiredJwtException thrown
             *
             * 3. Malformed token:
             *    Not valid JWT format
             *    → MalformedJwtException thrown
             *
             * 4. Unsupported algorithm:
             *    Wrong signing algorithm
             *    → UnsupportedJwtException thrown
             *
             * try-catch block:
             * - Catches all JWT parsing exceptions
             * - Logs error (optional)
             * - Sets username to null
             * - Request continues without authentication
             * - Spring Security will reject if endpoint requires auth
             *
             * Why not throw exception?
             * - Filter shouldn't stop entire request
             * - Let Spring Security handle authorization
             * - Maybe endpoint is public anyway
             * - Better user experience (specific error later)
             */
            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (Exception e) {
                /*
                 * Token parsing failed
                 *
                 * Possible reasons:
                 * - Token expired
                 * - Token tampered with
                 * - Token malformed
                 * - Invalid signature
                 *
                 * What we do:
                 * - Log error (optional, commented out)
                 * - Set username to null
                 * - Continue filter chain
                 * - Spring Security will deny access if needed
                 *
                 * Logging (uncomment if needed):
                 * System.err.println("JWT Token validation error: " + e.getMessage());
                 *
                 * Production:
                 * Use proper logger:
                 * private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
                 * logger.error("JWT validation failed", e);
                 */
                // Token invalid, username stays null
                // Request will fail authentication if endpoint requires it
            }
        }

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 4: AUTHENTICATE USER IF TOKEN IS VALID
         * ═══════════════════════════════════════════════════════════
         *
         * If we successfully extracted username, proceed with authentication.
         *
         * Conditions to authenticate:
         *
         * 1. username != null
         *    → Token exists and was parsed successfully
         *    → Username was extracted
         *
         * 2. SecurityContextHolder.getContext().getAuthentication() == null
         *    → User is not already authenticated
         *    → Prevents re-authentication on same request
         *
         * Why check if already authenticated?
         * - Filter might run multiple times (forwards/includes)
         * - Don't waste resources re-authenticating
         * - Authentication is expensive (database call)
         *
         * SecurityContextHolder:
         * - Thread-local storage for security information
         * - Stores authentication for current request
         * - Accessible throughout request lifecycle
         * - Cleared after request completes
         *
         * getContext():
         * - Returns SecurityContext for current thread
         * - Contains authentication information
         *
         * getAuthentication():
         * - Returns Authentication object
         * - null if not authenticated
         * - Contains user details if authenticated
         */
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            /*
             * ═══════════════════════════════════════════════════════════
             * STEP 4A: LOAD USER DETAILS FROM DATABASE
             * ═══════════════════════════════════════════════════════════
             *
             * Fetch full user information from database.
             *
             * Why load from database?
             * - Token only contains username
             * - Need full user details for authorization
             * - Need password hash to validate token
             * - Need roles/permissions
             *
             * userDetailsService.loadUserByUsername(username):
             * - Calls our CustomUserDetailsService
             * - Queries database for user
             * - Returns UserDetails object
             * - Throws UsernameNotFoundException if not found
             *
             * Flow:
             * 1. Token username: "john_doe"
             * 2. Load user from database
             * 3. User found → UserDetails returned
             * 4. User not found → Exception thrown
             *
             * What if user deleted after token issued?
             * - Token still valid (not expired)
             * - But user no longer exists
             * - loadUserByUsername throws exception
             * - Authentication fails
             * - Request denied
             * - User must login again
             *
             * This is a feature!
             * - Deleted users can't access system
             * - Even with valid token
             * - Database is source of truth
             */
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            /*
             * ═══════════════════════════════════════════════════════════
             * STEP 4B: VALIDATE TOKEN AGAINST USER DETAILS
             * ═══════════════════════════════════════════════════════════
             *
             * Verify token is valid for this specific user.
             *
             * jwtUtil.validateToken(jwt, userDetails):
             * - Extracts username from token
             * - Compares with userDetails.getUsername()
             * - Checks if token expired
             * - Returns true if all valid
             *
             * Validation checks:
             * 1. Username match:
             *    Token username == UserDetails username
             *
             * 2. Not expired:
             *    Token expiration > current time
             *
             * Why validate even though we already parsed token?
             * - Extra security layer
             * - Ensures token belongs to this user
             * - Checks expiration again (time might have passed)
             * - Prevents token reuse attacks
             *
             * Example attack (prevented):
             * 1. Attacker gets token for user A
             * 2. Attacker modifies payload: user A → user B
             * 3. Attacker sends modified token
             * 4. Our filter extracts "user B" from token
             * 5. Loads user B from database
             * 6. Validates token against user B
             * 7. Validation fails! (signature doesn't match)
             * 8. Attack prevented! ✅
             */
            if (jwtUtil.validateToken(jwt, userDetails)) {
                /*
                 * ═══════════════════════════════════════════════════════════
                 * STEP 4C: CREATE AUTHENTICATION TOKEN
                 * ═══════════════════════════════════════════════════════════
                 *
                 * Create Spring Security authentication object.
                 *
                 * UsernamePasswordAuthenticationToken:
                 * - Spring Security class
                 * - Represents authenticated user
                 * - Contains user details and authorities
                 *
                 * Constructor parameters:
                 * 1. principal (userDetails):
                 *    - The authenticated user
                 *    - Usually UserDetails object
                 *    - Can be retrieved later via SecurityContext
                 *
                 * 2. credentials (null):
                 *    - Password (we don't need it anymore)
                 *    - Set to null for security
                 *    - Already validated via token
                 *
                 * 3. authorities (userDetails.getAuthorities()):
                 *    - User's roles/permissions
                 *    - Used for authorization checks
                 *    - Example: [ROLE_USER]
                 *
                 * Why set credentials to null?
                 * - Password already validated
                 * - No need to keep in memory
                 * - Security best practice
                 * - Reduces risk if authentication object leaked
                 *
                 * What are authorities?
                 * - Collection<GrantedAuthority>
                 * - User's permissions
                 * - Example: [ROLE_USER, ROLE_ADMIN]
                 * - Used by @PreAuthorize annotations
                 */
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,                    // Principal (user)
                                null,                          // Credentials (not needed)
                                userDetails.getAuthorities()   // Authorities (roles)
                        );

                /*
                 * ═══════════════════════════════════════════════════════════
                 * STEP 4D: SET ADDITIONAL AUTHENTICATION DETAILS
                 * ═══════════════════════════════════════════════════════════
                 *
                 * Add request-specific details to authentication.
                 *
                 * WebAuthenticationDetailsSource:
                 * - Builds authentication details from request
                 * - Includes IP address, session ID, etc.
                 *
                 * buildDetails(request):
                 * - Creates WebAuthenticationDetails object
                 * - Contains:
                 *   - Remote IP address
                 *   - Session ID (if any)
                 *
                 * Why set details?
                 * - Useful for logging/auditing
                 * - Track where requests come from
                 * - Detect suspicious activity
                 * - Not required but good practice
                 *
                 * Example details:
                 * {
                 *   "remoteAddress": "127.0.0.1",
                 *   "sessionId": null
                 * }
                 *
                 * Accessing details later (in controller):
                 * Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                 * WebAuthenticationDetails details = (WebAuthenticationDetails) auth.getDetails();
                 * String ip = details.getRemoteAddress();
                 */
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                /*
                 * ═══════════════════════════════════════════════════════════
                 * STEP 4E: SET AUTHENTICATION IN SECURITY CONTEXT
                 * ═══════════════════════════════════════════════════════════
                 *
                 * Tell Spring Security that user is authenticated!
                 *
                 * SecurityContextHolder:
                 * - Thread-local storage
                 * - Stores authentication for current thread
                 * - Accessible throughout request
                 *
                 * .getContext():
                 * - Gets SecurityContext for current thread
                 * - Creates one if doesn't exist
                 *
                 * .setAuthentication(authToken):
                 * - Stores authentication in context
                 * - User is now authenticated for this request!
                 * - Spring Security will allow access to protected endpoints
                 *
                 * What happens after this:
                 * 1. Request continues to next filter
                 * 2. Eventually reaches controller
                 * 3. Controller can access authenticated user:
                 *    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                 *    UserDetails user = (UserDetails) auth.getPrincipal();
                 *    String username = user.getUsername();
                 *
                 * 4. Spring Security checks permissions:
                 *    @PreAuthorize("hasRole('USER')") → Checks authorities
                 *    If user has role → Allow
                 *    If user doesn't have role → Deny (403 Forbidden)
                 *
                 * 5. After request completes:
                 *    Spring clears SecurityContext
                 *    Next request starts fresh
                 *
                 * Thread-Local Explained:
                 * - Each thread has its own SecurityContext
                 * - Thread handling this request has this authentication
                 * - Other threads have their own authentication
                 * - No interference between requests
                 * - Thread-safe!
                 */
                SecurityContextHolder.getContext().setAuthentication(authToken);

                /*
                 * ✅ AUTHENTICATION COMPLETE!
                 *
                 * Summary of what we did:
                 * 1. ✅ Extracted JWT from Authorization header
                 * 2. ✅ Parsed token and got username
                 * 3. ✅ Loaded user from database
                 * 4. ✅ Validated token against user
                 * 5. ✅ Created authentication object
                 * 6. ✅ Set authentication in SecurityContext
                 *
                 * User is now authenticated!
                 * Request can proceed to controller!
                 */
            }
        }

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 5: CONTINUE FILTER CHAIN
         * ═══════════════════════════════════════════════════════════
         *
         * Pass request to next filter in chain.
         *
         * filterChain.doFilter(request, response):
         * - Calls next filter
         * - Eventually reaches controller
         * - MUST be called or request stops here!
         *
         * What happens if we don't call this?
         * - Request never reaches controller
         * - Response never sent
         * - Client hangs waiting for response
         * - Bad user experience!
         *
         * Filter chain continues to:
         * 1. Other security filters
         * 2. Authorization filter (check permissions)
         * 3. DispatcherServlet (Spring MVC)
         * 4. Your controller
         * 5. Response sent back to client
         *
         * Finally block:
         * - This runs whether authentication succeeded or failed
         * - Ensures request always continues
         * - Filter doesn't block valid public endpoints
         *
         * Scenarios:
         *
         * 1. Valid JWT token:
         *    → Authentication set
         *    → Request continues
         *    → Controller can access user info
         *
         * 2. Invalid/expired token on protected endpoint:
         *    → Authentication NOT set
         *    → Request continues
         *    → Authorization filter denies access (401)
         *
         * 3. No token on public endpoint:
         *    → Authentication NOT set
         *    → Request continues
         *    → SecurityConfig allows it (permitAll)
         *
         * 4. No token on protected endpoint:
         *    → Authentication NOT set
         *    → Request continues
         *    → Authorization filter denies access (401)
         */
        filterChain.doFilter(request, response);
    }
}

/*
 * ═══════════════════════════════════════════════════════════
 * KEY TAKEAWAYS - JWT AUTHENTICATION FILTER
 * ═══════════════════════════════════════════════════════════
 *
 * 1. FILTER EXECUTION
 *    - Runs on EVERY request
 *    - Before controller
 *    - Intercepts and validates JWT
 *    - Sets authentication if valid
 *
 * 2. TOKEN EXTRACTION
 *    - From Authorization header
 *    - Format: "Bearer <token>"
 *    - Validates format
 *    - Extracts token part
 *
 * 3. TOKEN VALIDATION
 *    - Parse token (verify signature)
 *    - Extract username
 *    - Load user from database
 *    - Validate token against user
 *    - Check expiration
 *
 * 4. AUTHENTICATION
 *    - Create authentication object
 *    - Set in SecurityContext
 *    - Available to controllers
 *    - Used for authorization
 *
 * 5. ERROR HANDLING
 *    - Invalid token → Skip authentication
 *    - Let Spring Security deny if needed
 *    - Don't stop request flow
 *    - Always continue filter chain
 *
 * 6. SECURITY CONTEXT
 *    - Thread-local storage
 *    - Stores current user
 *    - Cleared after request
 *    - Thread-safe
 */
