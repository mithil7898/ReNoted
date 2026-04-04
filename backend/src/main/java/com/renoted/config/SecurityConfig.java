package com.renoted.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.renoted.filter.JwtAuthenticationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import java.util.Arrays;
import java.util.List;

/**
 * SECURITY CONFIGURATION - Spring Security Setup
 *
 * PURPOSE:
 * This class configures the entire security system for our application.
 * It defines which endpoints are public, which require authentication,
 * and how authentication should work.
 *
 * WHAT IS SPRING SECURITY?
 * Spring Security is a framework that provides:
 * 1. Authentication (who are you?)
 * 2. Authorization (what can you do?)
 * 3. Protection against common attacks (CSRF, XSS, etc.)
 *
 * HOW SPRING SECURITY WORKS:
 *
 * Request Flow:
 * Client → Security Filters → Controller → Service → Repository
 *            ↑
 *      We configure this!
 *
 * Security Filters (in order):
 * 1. CorsFilter - Handle cross-origin requests
 * 2. CsrfFilter - CSRF protection (we'll disable)
 * 3. JwtAuthenticationFilter - Validate JWT tokens (Phase 3)
 * 4. UsernamePasswordAuthenticationFilter - Handle login
 * 5. FilterSecurityInterceptor - Check authorization
 *
 * ANNOTATIONS EXPLAINED:
 *
 * @Configuration:
 * - Marks this as a configuration class
 * - Spring processes this at startup
 * - Contains @Bean methods
 *
 * @EnableWebSecurity:
 * - Enables Spring Security for web applications
 * - Activates security filter chain
 * - Enables @PreAuthorize, @Secured annotations
 *
 * @RequiredArgsConstructor:
 * - Lombok annotation
 * - Generates constructor for final fields
 * - Enables dependency injection
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * SECURITY FILTER CHAIN - Main Security Configuration
     *
     * This is the HEART of Spring Security configuration!
     *
     * @Bean:
     * - Spring creates this object at startup
     * - Registers it in application context
     * - Used by security filters
     *
     * HttpSecurity:
     * - Builder object for configuring security
     * - Fluent API (method chaining)
     * - Each method returns HttpSecurity for chaining
     *
     * What we're configuring:
     * 1. CORS (allow frontend to call backend)
     * 2. CSRF (disable for JWT API)
     * 3. Session management (stateless)
     * 4. Endpoint authorization rules
     *
     * @param http - HttpSecurity builder object (injected by Spring)
     * @return SecurityFilterChain - Configured security filter chain
     * @throws Exception - If configuration fails
     */

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 1: CONFIGURE CORS (Cross-Origin Resource Sharing)
         * ═══════════════════════════════════════════════════════════
         *
         * What is CORS?
         * CORS allows our frontend (localhost:5173) to make requests
         * to our backend (localhost:8080).
         *
         * Without CORS:
         * Browser: "Frontend and backend have different origins!"
         * Browser: "BLOCKED for security!"
         *
         * With CORS:
         * Backend: "I trust http://localhost:5173"
         * Browser: "OK, requests allowed!"
         *
         * Why do we need this?
         * - Frontend and backend run on different ports
         * - Browser's Same-Origin Policy blocks cross-origin requests
         * - CORS tells browser which origins are trusted
         *
         * What happens here:
         * http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
         *
         * Breakdown:
         * 1. cors -> ... is a Lambda expression (Java 8+)
         * 2. cors is a CorsCustomizer object
         * 3. cors.configurationSource(...) sets CORS rules
         * 4. corsConfigurationSource() is our method below
         *
         * Lambda Explanation:
         * Old way (verbose):
         * http.cors(new Customizer<CorsConfigurer<HttpSecurity>>() {
         *     @Override
         *     public void customize(CorsConfigurer<HttpSecurity> cors) {
         *         cors.configurationSource(corsConfigurationSource());
         *     }
         * });
         *
         * New way (lambda):
         * http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
         *
         * Much cleaner!
         */
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 2: DISABLE CSRF (Cross-Site Request Forgery) Protection
         * ═══════════════════════════════════════════════════════════
         *
         * What is CSRF?
         * CSRF is an attack where an evil site tricks your browser
         * into making requests to our site using your credentials.
         *
         * Traditional CSRF Attack (Session-based):
         * 1. You login to bank.com → Get session cookie
         * 2. You visit evil.com
         * 3. evil.com has: <form action="bank.com/transfer" method="POST">
         * 4. Form auto-submits
         * 5. Browser auto-sends your session cookie
         * 6. Bank thinks it's you and transfers money!
         *
         * CSRF Protection (Traditional):
         * Server sends CSRF token with each page
         * Client must include token in requests
         * Server validates token
         * Evil site can't get your CSRF token → Attack fails!
         *
         * Why We DISABLE CSRF for Our API:
         *
         * Our API uses JWT tokens, NOT cookies/sessions!
         *
         * JWT Flow:
         * 1. Login → Receive JWT token
         * 2. Store token in localStorage (JavaScript)
         * 3. Include token in Authorization header
         * 4. Evil site CANNOT access localStorage
         * 5. Evil site CANNOT get your token
         * 6. CSRF attack impossible!
         *
         * Key Difference:
         * Session Cookie:
         * - Browser automatically sends cookie
         * - Evil site benefits from auto-send
         * - Vulnerable to CSRF
         *
         * JWT Token:
         * - Must be manually added to request
         * - Browser doesn't auto-send
         * - Evil site can't access localStorage
         * - NOT vulnerable to CSRF
         *
         * When to keep CSRF enabled:
         * - Server-rendered pages (Thymeleaf, JSP)
         * - Session-based authentication
         * - Cookie-based auth
         *
         * When to disable CSRF:
         * - REST APIs with JWT ✅ (our case!)
         * - Stateless authentication
         * - Mobile app backends
         *
         * Security Note:
         * Disabling CSRF is SAFE for JWT APIs
         * BUT you must use HTTPS in production!
         * HTTP + JWT = tokens can be intercepted
         * HTTPS + JWT = tokens encrypted in transit
         */
        http.csrf(csrf -> csrf.disable());

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 3: CONFIGURE SESSION MANAGEMENT (Stateless)
         * ═══════════════════════════════════════════════════════════
         *
         * What are Sessions?
         * Traditional web apps store user state on the server.
         *
         * Session-based Flow:
         * 1. User logs in
         * 2. Server creates session object in memory
         * 3. Server sends session ID to client as cookie
         * 4. Client sends cookie with each request
         * 5. Server looks up session by ID
         * 6. Server knows who you are
         *
         * Problems with Sessions:
         * - Server must store session data (memory/database)
         * - Difficult to scale (session must exist on same server)
         * - Sticky sessions needed with load balancers
         * - Session storage overhead
         * - Server restart = all users logged out
         *
         * Our Approach: STATELESS (JWT)
         *
         * JWT Flow:
         * 1. User logs in
         * 2. Server creates JWT token
         * 3. Server sends token to client
         * 4. Client stores token (localStorage)
         * 5. Client sends token with each request
         * 6. Server validates token (no lookup needed!)
         * 7. Server knows who you are from token data
         *
         * Benefits of Stateless:
         * - No server-side storage needed
         * - Easy to scale horizontally
         * - Any server can handle any request
         * - No sticky sessions needed
         * - Server restart doesn't affect users
         * - Perfect for microservices
         *
         * SessionCreationPolicy Options:
         *
         * ALWAYS:
         * - Spring Security always creates session
         * - Traditional web apps
         *
         * IF_REQUIRED:
         * - Create session if needed
         * - Default behavior
         *
         * NEVER:
         * - Never create session
         * - But will use existing session if present
         *
         * STATELESS: ✅ (Our choice!)
         * - Never create session
         * - Never use existing session
         * - Pure stateless authentication
         * - Perfect for JWT APIs
         *
         * What this does:
         * - Disables session creation
         * - Disables JSESSIONID cookie
         * - Each request is independent
         * - No server-side state
         */
        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        /*
         * ═══════════════════════════════════════════════════════════
         * STEP 4: CONFIGURE AUTHORIZATION RULES (Who Can Access What)
         * ═══════════════════════════════════════════════════════════
         *
         * This is where we define which endpoints are public
         * and which require authentication.
         *
         * Format:
         * requestMatchers("pattern").permitAll() → Public
         * requestMatchers("pattern").authenticated() → Requires login
         * anyRequest().authenticated() → Everything else needs login
         *
         * Path Matching Patterns:
         * "/api/auth/register" → Exact match
         * "/api/auth/**" → /api/auth/anything/here
         * "/api/**" → /api/anything
         * "/**" → Everything
         *
         * Order Matters!
         * Rules are checked in order, first match wins.
         *
         * Example:
         * requestMatchers("/api/**").authenticated() → Requires login
         * requestMatchers("/api/auth/**").permitAll() → Public
         *
         * Problem: First rule matches /api/auth/login!
         * Result: Login endpoint requires authentication! (impossible!)
         *
         * Correct Order:
         * requestMatchers("/api/auth/**").permitAll() → Public (checked first)
         * requestMatchers("/api/**").authenticated() → Requires login
         * Result: Login is public, other APIs require auth ✅
         *
         * Our Rules:
         * 1. /api/auth/** → Public (anyone can register/login)
         * 2. /api/** → Protected (must be authenticated)
         */
        http.authorizeHttpRequests(auth -> auth
                /*
                 * PUBLIC ENDPOINTS
                 *
                 * These endpoints are accessible without authentication.
                 *
                 * /api/auth/**:
                 * - /api/auth/register → User registration
                 * - /api/auth/login → User login
                 * - /api/auth/refresh → Token refresh (Phase 3)
                 *
                 * Why public?
                 * - Users need to register before they have an account
                 * - Users need to login before they have a token
                 * - Chicken and egg problem: can't get token without login!
                 *
                 * permitAll():
                 * - No authentication required
                 * - Anyone can access
                 * - No token needed
                 */
                .requestMatchers("/api/auth/**").permitAll()

                /*
                 * PROTECTED ENDPOINTS
                 *
                 * Everything under /api/** requires authentication
                 * (except /api/auth/** which was already permitted above)
                 *
                 * Protected endpoints:
                 * - /api/notes/** → All note operations
                 * - /api/tags/** → All tag operations
                 * - /api/users/** → User profile operations
                 * - Any other /api/** paths
                 *
                 * authenticated():
                 * - Must be logged in
                 * - Must have valid JWT token (Phase 3)
                 * - Token validated by JwtAuthenticationFilter (Phase 3)
                 *
                 * What happens if not authenticated?
                 * 1. Request comes in without token
                 * 2. Security filter chain checks rules
                 * 3. Endpoint requires authentication
                 * 4. No valid token found
                 * 5. Returns 401 Unauthorized
                 * 6. Frontend redirects to login page
                 */
                .requestMatchers("/api/**").authenticated()

                /*
                 * FALLBACK RULE
                 *
                 * Any request not matching above patterns
                 * also requires authentication.
                 *
                 * This is a safety net:
                 * - Catches any endpoints we forgot to specify
                 * - Secure by default (requires auth)
                 * - Better than accidentally leaving endpoints public
                 *
                 * Principle: Default Deny
                 * - Explicit public endpoints (permitAll)
                 * - Everything else requires authentication
                 * - Secure by default!
                 */
                .anyRequest().authenticated()
        );

        /*
         * ═══════════════════════════════════════════════════════════
         * ADD JWT AUTHENTICATION FILTER
         * ═══════════════════════════════════════════════════════════
         *
         * Register our custom JWT filter in the security chain.
         *
         * addFilterBefore:
         * - Adds our filter BEFORE specified filter
         * - Our filter runs first
         * - Validates JWT
         * - Sets authentication
         *
         * UsernamePasswordAuthenticationFilter.class:
         * - Spring Security's login filter
         * - Handles username/password authentication
         * - We want JWT checked before this
         *
         * Why before UsernamePasswordAuthenticationFilter?
         * - JWT validation should happen early
         * - Before traditional login filter
         * - Proper filter order
         *
         * Filter Order:
         * 1. CORS filter
         * 2. JwtAuthenticationFilter ← Our filter
         * 3. UsernamePasswordAuthenticationFilter
         * 4. Other security filters
         * 5. Controller
         */
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        /*
         * ═══════════════════════════════════════════════════════════
         * BUILD AND RETURN SECURITY FILTER CHAIN
         * ═══════════════════════════════════════════════════════════
         *
         * http.build():
         * - Takes all our configurations
         * - Builds the SecurityFilterChain
         * - Registers filters in correct order
         * - Returns configured chain
         *
         * Spring uses this chain to:
         * - Process every HTTP request
         * - Apply security rules
         * - Authenticate users
         * - Authorize access
         * - Protect endpoints
         */
        return http.build();

        /*
         * What happens now?
         *
         * When application starts:
         * 1. Spring sees @Bean annotation
         * 2. Calls this method
         * 3. Gets SecurityFilterChain
         * 4. Registers security filters
         * 5. Security is active!
         *
         * When request arrives:
         * 1. Request enters filter chain
         * 2. CORS filter allows localhost:5173
         * 3. CSRF check skipped (disabled)
         * 4. Session creation skipped (stateless)
         * 5. JWT filter validates token (Phase 3)
         * 6. Authorization check:
         *    - /api/auth/login → Public ✅
         *    - /api/notes → Requires auth, has token? ✅ or ❌
         * 7. If all pass → Request reaches controller
         * 8. If any fail → 401 or 403 error
         */
    }

    /**
     * CORS CONFIGURATION SOURCE
     *
     * Defines which origins (domains) are allowed to access our API.
     *
     * What is an Origin?
     * Origin = Protocol + Domain + Port
     *
     * Examples:
     * http://localhost:5173 → Origin
     * http://localhost:8080 → Different origin (different port)
     * https://localhost:5173 → Different origin (different protocol)
     * http://example.com → Different origin (different domain)
     *
     * Same-Origin Policy:
     * Browsers block requests between different origins by default.
     *
     * Example:
     * Frontend: http://localhost:5173
     * Backend: http://localhost:8080
     * Browser: "Different ports = different origins = BLOCKED!"
     *
     * CORS Headers Fix This:
     * Backend sends headers telling browser:
     * "I allow requests from http://localhost:5173"
     * Browser: "OK, I'll allow it!"
     *
     * @return CorsConfigurationSource - CORS configuration
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        /*
         * Create CORS configuration object
         *
         * This object will hold all CORS settings:
         * - Allowed origins
         * - Allowed methods
         * - Allowed headers
         * - Allow credentials
         */
        CorsConfiguration configuration = new CorsConfiguration();

        /*
         * ═══════════════════════════════════════════════════════════
         * ALLOWED ORIGINS
         * ═══════════════════════════════════════════════════════════
         *
         * Which frontend origins can access our API?
         *
         * Development:
         * - http://localhost:5173 (Vite dev server)
         * - http://localhost:3000 (alternative port)
         *
         * Production:
         * - https://yourdomain.com (your actual domain)
         * - https://www.yourdomain.com (www subdomain)
         *
         * Security Warning:
         * NEVER use "*" (allow all origins) in production!
         *
         * Bad (Security Risk):
         * configuration.setAllowedOrigins(Arrays.asList("*"));
         *
         * Why it's bad:
         * - Allows ANY website to call your API
         * - evil.com can access user data
         * - Defeats purpose of CORS
         * - Major security vulnerability
         *
         * Good (Specific Origins):
         * configuration.setAllowedOrigins(Arrays.asList(
         *     "http://localhost:5173",
         *     "https://yourdomain.com"
         * ));
         *
         * Result:
         * - Only these specific origins allowed
         * - evil.com blocked
         * - Secure!
         *
         * Environment-Based Configuration (Production Best Practice):
         * Read from environment variable:
         * String allowedOrigin = System.getenv("ALLOWED_ORIGIN");
         * configuration.setAllowedOrigins(Arrays.asList(allowedOrigin));
         *
         * Development: ALLOWED_ORIGIN=http://localhost:5173
         * Production: ALLOWED_ORIGIN=https://yourdomain.com
         */
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));

        /*
         * ═══════════════════════════════════════════════════════════
         * ALLOWED METHODS
         * ═══════════════════════════════════════════════════════════
         *
         * Which HTTP methods are allowed?
         *
         * HTTP Methods:
         * GET → Read data
         * POST → Create data
         * PUT → Update data (full replacement)
         * DELETE → Delete data
         * PATCH → Update data (partial)
         * OPTIONS → Preflight request (CORS check)
         *
         * Why OPTIONS?
         * Browsers send OPTIONS request before actual request
         * to check if CORS allows the operation.
         *
         * Preflight Request Flow:
         * 1. Frontend: "I want to POST to /api/notes"
         * 2. Browser: "Let me check with OPTIONS first"
         * 3. Browser → OPTIONS /api/notes
         * 4. Backend → "POST is allowed"
         * 5. Browser: "OK, sending actual POST request"
         * 6. Browser → POST /api/notes
         *
         * Why "*" is OK here:
         * - Methods are not a security concern
         * - Your endpoints define what's actually allowed
         * - This just prevents browser from blocking
         *
         * Example:
         * CORS allows POST, but endpoint doesn't exist
         * → 404 Not Found (endpoint handles it)
         *
         * CORS blocks POST
         * → Request never reaches backend (browser blocks)
         */
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        /*
         * ═══════════════════════════════════════════════════════════
         * ALLOWED HEADERS
         * ═══════════════════════════════════════════════════════════
         *
         * Which request headers are allowed?
         *
         * Common Headers:
         *
         * Authorization:
         * - Contains JWT token
         * - Format: "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
         * - Required for protected endpoints
         *
         * Content-Type:
         * - Tells backend what data format
         * - application/json → JSON data
         * - multipart/form-data → File uploads
         * - text/plain → Plain text
         *
         * Accept:
         * - Tells backend what format client wants
         * - application/json → "Send me JSON"
         * - text/html → "Send me HTML"
         *
         * X-Custom-Header:
         * - Any custom headers your app needs
         * - Must be explicitly allowed
         *
         * Why "*" is convenient:
         * - Allows all headers
         * - Frontend can send any header needed
         * - No need to update backend for new headers
         *
         * More restrictive (if needed):
         * configuration.setAllowedHeaders(Arrays.asList(
         *     "Authorization",
         *     "Content-Type",
         *     "Accept"
         * ));
         */
        configuration.setAllowedHeaders(Arrays.asList("*"));

        /*
         * ═══════════════════════════════════════════════════════════
         * ALLOW CREDENTIALS
         * ═══════════════════════════════════════════════════════════
         *
         * Should browser send cookies/credentials with requests?
         *
         * What are credentials?
         * - Cookies
         * - HTTP authentication
         * - TLS client certificates
         *
         * true: Include credentials
         * false: Don't include credentials
         *
         * Our Case:
         * - We use JWT tokens in Authorization header
         * - NOT using cookies for authentication
         * - Could be false
         *
         * Why true?
         * - Future-proofing (if we add cookies later)
         * - Doesn't hurt (we're not using cookies now)
         * - Some libraries expect this to be true
         *
         * Frontend must also set:
         * fetch('/api/notes', {
         *   credentials: 'include'  // Send cookies
         * })
         *
         * Without this, browser won't send cookies even if allowed.
         */
        configuration.setAllowCredentials(true);

        /*
         * ═══════════════════════════════════════════════════════════
         * REGISTER CONFIGURATION FOR ALL PATHS
         * ═══════════════════════════════════════════════════════════
         *
         * Apply CORS configuration to which paths?
         *
         * UrlBasedCorsConfigurationSource:
         * - Maps URL patterns to CORS configurations
         * - Can have different CORS rules for different paths
         *
         * Examples:
         *
         * Same config for all paths:
         * source.registerCorsConfiguration("/**", configuration);
         *
         * Different configs:
         * source.registerCorsConfiguration("/api/**", apiCorsConfig);
         * source.registerCorsConfiguration("/public/**", publicCorsConfig);
         *
         * Our Setup:
         * "/**" → All paths use same CORS configuration
         *
         * URL Pattern Syntax:
         * "/**" → All paths
         * "/api/**" → /api/anything
         * "/api/auth/**" → /api/auth/anything
         * "/api/notes/*" → /api/notes/123 (single level)
         * "/api/notes/**" → /api/notes/123/comments/456 (multiple levels)
         */
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;

        /*
         * Summary - CORS Headers Sent:
         *
         * Access-Control-Allow-Origin: http://localhost:5173
         * Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
         * Access-Control-Allow-Headers: *
         * Access-Control-Allow-Credentials: true
         *
         * Browser sees these headers and allows the request!
         */
    }

    /**
     * AUTHENTICATION MANAGER BEAN
     *
     * The AuthenticationManager is responsible for authenticating users.
     *
     * What does it do?
     * - Takes username and password
     * - Validates credentials
     * - Returns Authentication object if valid
     * - Throws exception if invalid
     *
     * Flow:
     * 1. User sends: { username: "john", password: "secret123" }
     * 2. AuthenticationManager receives credentials
     * 3. Calls UserDetailsService to load user
     * 4. Compares hashed passwords
     * 5. If match → Returns authenticated user
     * 6. If no match → Throws BadCredentialsException
     *
     * Why expose as Bean?
     * - We'll need it in AuthController (Phase 3)
     * - Used for login endpoint
     * - Injected via @Autowired
     *
     * AuthenticationConfiguration:
     * - Spring Security provides this automatically
     * - Contains default authentication setup
     * - We extract AuthenticationManager from it
     *
     * Usage (Phase 3):
     * @Autowired
     * private AuthenticationManager authenticationManager;
     *
     * public String login(LoginRequest request) {
     *     authenticationManager.authenticate(
     *         new UsernamePasswordAuthenticationToken(
     *             request.getUsername(),
     *             request.getPassword()
     *         )
     *     );
     *     // If we get here, authentication succeeded!
     * }
     *
     * @param config - Authentication configuration (injected by Spring)
     * @return AuthenticationManager - Used for authentication
     * @throws Exception - If configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();

        /*
         * What Spring Does:
         * 1. Creates default AuthenticationManager
         * 2. Configures it with:
         *    - UserDetailsService (we'll create in Phase 3)
         *    - PasswordEncoder (we created in Phase 1)
         * 3. Returns configured manager
         * 4. Ready to authenticate users!
         */
    }
}

/*
 * ═══════════════════════════════════════════════════════════
 * KEY TAKEAWAYS - SPRING SECURITY CONFIGURATION
 * ═══════════════════════════════════════════════════════════
 *
 * 1. SECURITY FILTER CHAIN
 *    - Intercepts all requests
 *    - Applies security rules in order
 *    - First match wins
 *    - Public endpoints before protected
 *
 * 2. CORS CONFIGURATION
 *    - Allows frontend to call backend
 *    - Specify exact origins (never *)
 *    - Required for different ports
 *    - Browser enforces, not server
 *
 * 3. CSRF DISABLED FOR JWT
 *    - JWT doesn't need CSRF protection
 *    - Tokens not auto-sent by browser
 *    - Safe to disable for stateless APIs
 *    - Keep enabled for session-based auth
 *
 * 4. STATELESS SESSION MANAGEMENT
 *    - No server-side sessions
 *    - JWT tokens contain all info
 *    - Easy to scale horizontally
 *    - Perfect for microservices
 *
 * 5. AUTHORIZATION RULES
 *    - Public: /api/auth/** (register, login)
 *    - Protected: /api/** (everything else)
 *    - Default: Require authentication
 *    - Secure by default principle
 *
 * 6. AUTHENTICATION MANAGER
 *    - Validates credentials
 *    - Used in login endpoint
 *    - Works with UserDetailsService
 *    - Returns authenticated user
 */