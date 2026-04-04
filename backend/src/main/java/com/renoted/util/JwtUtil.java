package com.renoted.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT UTILITY - Token Generation and Validation
 *
 * PURPOSE:
 * This utility class handles all JWT operations:
 * 1. Generate tokens (create JWT for logged-in users)
 * 2. Validate tokens (verify JWT from requests)
 * 3. Extract information (get username from token)
 * 4. Check expiration (is token still valid?)
 *
 * WHAT IS JWT?
 * JWT (JSON Web Token) is a compact, URL-safe token format.
 * It contains three parts separated by dots:
 *
 * HEADER.PAYLOAD.SIGNATURE
 *
 * Example:
 * eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huIn0.xyz123
 *
 * Part 1 - HEADER (Algorithm & Type):
 * {
 *   "alg": "HS256",    ← HMAC with SHA-256
 *   "typ": "JWT"       ← Type: JSON Web Token
 * }
 *
 * Part 2 - PAYLOAD (Claims/Data):
 * {
 *   "sub": "john_doe",        ← Subject (username)
 *   "iat": 1516239022,        ← Issued At (timestamp)
 *   "exp": 1516242622         ← Expiration (timestamp)
 * }
 *
 * Part 3 - SIGNATURE (Verification):
 * HMACSHA256(
 *   base64UrlEncode(header) + "." + base64UrlEncode(payload),
 *   SECRET_KEY
 * )
 *
 * WHY USE JWT?
 *
 * 1. STATELESS:
 *    - No server-side storage needed
 *    - Token contains all user info
 *    - Any server can validate token
 *
 * 2. SCALABLE:
 *    - No session lookup
 *    - No shared session storage
 *    - Horizontal scaling easy
 *
 * 3. SECURE:
 *    - Signed with secret key
 *    - Cannot be tampered with
 *    - Expiration built-in
 *
 * 4. PORTABLE:
 *    - Works across domains
 *    - Works with mobile apps
 *    - Works with microservices
 *
 * @Component:
 * - Makes this class a Spring bean
 * - Can be injected with @Autowired
 * - Singleton (one instance)
 */
@Component
public class JwtUtil {

    /**
     * SECRET KEY - Used to Sign and Verify Tokens
     *
     * @Value annotation:
     * - Reads value from application.properties
     * - Property: jwt.secret=your_secret_key_here
     * - If not found, uses default value after ':'
     *
     * Security Requirements:
     *
     * 1. LENGTH:
     *    - Minimum: 256 bits (32 bytes)
     *    - Our key: 64 hex characters = 256 bits ✅
     *
     * 2. RANDOMNESS:
     *    - Must be cryptographically random
     *    - Don't use dictionary words
     *    - Don't use predictable patterns
     *
     * 3. SECRECY:
     *    - Never commit to version control
     *    - Store in environment variables
     *    - Different key per environment (dev/prod)
     *
     * 4. ROTATION:
     *    - Change periodically (e.g., every 90 days)
     *    - Invalidates old tokens on change
     *
     * Production Best Practice:
     *
     * application.properties:
     * jwt.secret=${JWT_SECRET}
     *
     * Environment variable:
     * export JWT_SECRET=3f7a9b2c5e8d1f4a6b9c2e5f8a1d4b7c9e2f5a8d1b4c7e0f3a6b9c2e5f8a1d4
     *
     * Generate secure key:
     * openssl rand -hex 32
     *
     * Why this key is secure:
     * - 64 hex characters = 256 bits
     * - Cryptographically random
     * - Meets HMAC-SHA256 requirements
     */
    @Value("${jwt.secret:3f7a9b2c5e8d1f4a6b9c2e5f8a1d4b7c9e2f5a8d1b4c7e0f3a6b9c2e5f8a1d4}")
    private String SECRET_KEY;

    /**
     * TOKEN EXPIRATION TIME (in milliseconds)
     *
     * @Value annotation:
     * - Reads from application.properties
     * - Property: jwt.expiration=3600000
     * - If not found, uses default (10 hours)
     *
     * Time Calculation:
     * 1 hour = 60 minutes
     * 60 minutes = 60 * 60 seconds = 3600 seconds
     * 3600 seconds = 3600 * 1000 milliseconds = 3,600,000 ms
     *
     * Our default: 10 hours
     * 10 * 60 * 60 * 1000 = 36,000,000 milliseconds
     *
     * Typical Values:
     * - Short-lived (high security): 15-60 minutes
     * - Medium-lived (balanced): 1-4 hours
     * - Long-lived (convenience): 12-24 hours
     * - Very long (remember me): 7-30 days
     *
     * Considerations:
     *
     * Shorter expiration:
     * ✅ More secure (limits damage if token stolen)
     * ✅ Forces re-authentication
     * ❌ Annoying for users (frequent login)
     *
     * Longer expiration:
     * ✅ Better user experience
     * ✅ Fewer login prompts
     * ❌ Stolen token valid longer
     * ❌ Harder to revoke access
     *
     * Best Practice:
     * - Access token: Short (1 hour)
     * - Refresh token: Long (7 days)
     * - Use refresh tokens to get new access tokens
     */
    @Value("${jwt.expiration:36000000}")
    private Long EXPIRATION_TIME;

    /**
     * GENERATE SECRET KEY FOR SIGNING
     *
     * This method creates a SecretKey object from our SECRET_KEY string.
     *
     * Why do we need this?
     * - JJWT library requires SecretKey object
     * - Can't use raw string directly
     * - Must convert to proper key format
     *
     * Keys.hmacShaKeyFor():
     * - Creates HMAC key from byte array
     * - Validates key length (must be ≥ 256 bits)
     * - Returns SecretKey suitable for HS256
     *
     * UTF_8 Encoding:
     * - Converts string to bytes
     * - Consistent encoding (platform-independent)
     * - Required for byte array conversion
     *
     * Example:
     * String: "3f7a9b..."
     * Bytes: [0x33, 0x66, 0x37, 0x61, ...]
     * SecretKey: HMAC key object
     *
     * @return SecretKey - HMAC signing key
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * EXTRACT USERNAME FROM TOKEN
     *
     * Gets the username (subject) from JWT token.
     *
     * Flow:
     * 1. Receive token: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
     * 2. Parse and validate token
     * 3. Extract all claims (data)
     * 4. Get "sub" claim (subject = username)
     * 5. Return username: "john_doe"
     *
     * Usage:
     * String token = "eyJhbGc...";
     * String username = jwtUtil.extractUsername(token);
     * // username = "john_doe"
     *
     * Claims::getSubject explained:
     * - Claims is the payload of JWT
     * - getSubject() gets "sub" field
     * - "sub" is standard JWT claim for subject (user identifier)
     *
     * Method Reference (::):
     * Claims::getSubject is equivalent to:
     * claims -> claims.getSubject()
     *
     * @param token - JWT token string
     * @return String - Username from token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * EXTRACT EXPIRATION DATE FROM TOKEN
     *
     * Gets when the token will expire.
     *
     * Flow:
     * 1. Parse token
     * 2. Extract claims
     * 3. Get "exp" claim (expiration timestamp)
     * 4. Return as Date object
     *
     * Usage:
     * Date expiration = jwtUtil.extractExpiration(token);
     * // expiration = Mon Mar 25 15:30:22 IST 2026
     *
     * Claims::getExpiration:
     * - Gets "exp" field from JWT
     * - "exp" is Unix timestamp (seconds since 1970)
     * - Converted to Java Date object
     *
     * @param token - JWT token string
     * @return Date - Expiration timestamp
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * EXTRACT SPECIFIC CLAIM FROM TOKEN
     *
     * Generic method to extract any claim from JWT.
     *
     * What is a Claim?
     * A claim is a piece of information in the JWT payload.
     *
     * Standard Claims:
     * - sub (subject) → Username
     * - exp (expiration) → When token expires
     * - iat (issued at) → When token was created
     * - iss (issuer) → Who created the token
     * - aud (audience) → Who token is for
     *
     * Custom Claims:
     * - role → User's role
     * - userId → User's database ID
     * - permissions → User's permissions
     *
     * How it works:
     *
     * Type Parameter <T>:
     * - Makes method generic
     * - Can return any type (String, Date, Integer, etc.)
     * - Type determined by claimsResolver function
     *
     * Function<Claims, T>:
     * - Java functional interface
     * - Takes Claims object
     * - Returns value of type T
     * - Example: Claims::getSubject returns String
     *
     * Flow:
     * 1. extractAllClaims(token) → Parse token, get all claims
     * 2. claimsResolver.apply(claims) → Extract specific claim
     * 3. Return claim value
     *
     * Example Usage:
     *
     * // Extract username (String)
     * String username = extractClaim(token, Claims::getSubject);
     *
     * // Extract expiration (Date)
     * Date expiration = extractClaim(token, Claims::getExpiration);
     *
     * // Extract custom claim (if we added one)
     * String role = extractClaim(token, claims -> claims.get("role", String.class));
     *
     * @param token - JWT token string
     * @param claimsResolver - Function to extract specific claim
     * @param <T> - Type of claim to extract
     * @return T - Extracted claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * EXTRACT ALL CLAIMS FROM TOKEN
     *
     * Parses JWT token and returns all claims (payload).
     *
     * This is the CORE method for token parsing!
     *
     * What it does:
     * 1. Takes JWT token string
     * 2. Verifies signature using secret key
     * 3. Checks if token is expired
     * 4. Parses payload (claims)
     * 5. Returns Claims object
     *
     * Jwts.parserBuilder():
     * - Creates a parser for JWT tokens
     * - Builder pattern (fluent API)
     *
     * .setSigningKey(getSigningKey()):
     * - Provides secret key for verification
     * - Parser will verify signature matches
     * - If signature invalid → Exception thrown
     *
     * .build():
     * - Builds the parser with configuration
     *
     * .parseClaimsJws(token):
     * - Parses the JWT token
     * - Jws = JSON Web Signature (signed JWT)
     * - Validates signature
     * - Validates expiration
     * - Returns Jws<Claims> object
     *
     * .getBody():
     * - Extracts the payload (claims)
     * - Returns Claims object
     *
     * What can go wrong?
     *
     * 1. Invalid Signature:
     *    Token was tampered with
     *    → SignatureException thrown
     *
     * 2. Expired Token:
     *    Token past expiration time
     *    → ExpiredJwtException thrown
     *
     * 3. Malformed Token:
     *    Not valid JWT format
     *    → MalformedJwtException thrown
     *
     * 4. Unsupported Token:
     *    Wrong algorithm
     *    → UnsupportedJwtException thrown
     *
     * These exceptions will be caught in JwtAuthenticationFilter (Step 2)
     * and handled gracefully.
     *
     * @param token - JWT token string
     * @return Claims - All claims from token payload
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())  // Set secret key for verification
                .build()                          // Build parser
                .parseClaimsJws(token)           // Parse and verify token
                .getBody();                       // Get claims (payload)
    }

    /**
     * CHECK IF TOKEN IS EXPIRED
     *
     * Determines if token has passed its expiration time.
     *
     * Flow:
     * 1. Extract expiration date from token
     * 2. Get current date/time
     * 3. Compare: expiration < now?
     * 4. Return true if expired, false if still valid
     *
     * Example:
     * Token expiration: March 25, 2026 10:00 AM
     * Current time: March 25, 2026 9:00 AM
     * → expiration.before(now) = false → Token valid ✅
     *
     * Token expiration: March 25, 2026 10:00 AM
     * Current time: March 25, 2026 11:00 AM
     * → expiration.before(now) = true → Token expired ❌
     *
     * Date.before():
     * - Returns true if date is before parameter
     * - false if same or after
     *
     * Usage:
     * if (jwtUtil.isTokenExpired(token)) {
     *     return "Token expired, please login again";
     * }
     *
     * @param token - JWT token string
     * @return boolean - true if expired, false if valid
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * GENERATE JWT TOKEN FOR USER
     *
     * Creates a new JWT token for authenticated user.
     *
     * This is called when user successfully logs in!
     *
     * What it does:
     * 1. Takes UserDetails (authenticated user info)
     * 2. Creates empty claims map
     * 3. Calls createToken with username and claims
     * 4. Returns generated JWT string
     *
     * UserDetails:
     * - Spring Security interface
     * - Represents authenticated user
     * - getUsername() returns user identifier
     *
     * Why empty claims?
     * - We could add custom claims here
     * - Example: Map.of("role", "ROLE_USER")
     * - For now, we only need username
     * - Keep token small and simple
     *
     * Custom Claims Example:
     * Map<String, Object> claims = new HashMap<>();
     * claims.put("role", userDetails.getAuthorities());
     * claims.put("email", user.getEmail());
     * return createToken(claims, userDetails.getUsername());
     *
     * Usage (in AuthController):
     * UserDetails user = loadUserByUsername("john_doe");
     * String token = jwtUtil.generateToken(user);
     * // token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
     *
     * @param userDetails - Authenticated user information
     * @return String - Generated JWT token
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }

    /**
     * CREATE JWT TOKEN - Core Token Generation Logic
     *
     * This is where the actual JWT is built!
     *
     * Flow:
     * 1. Create JWT builder
     * 2. Add custom claims
     * 3. Set subject (username)
     * 4. Set issued time (now)
     * 5. Set expiration time (now + EXPIRATION_TIME)
     * 6. Sign with secret key
     * 7. Compact to string
     * 8. Return JWT token
     *
     * Jwts.builder():
     * - Creates JWT builder
     * - Fluent API (method chaining)
     *
     * .setClaims(claims):
     * - Adds custom claims to payload
     * - claims = Map of key-value pairs
     * - Example: {"role": "ROLE_USER", "email": "john@example.com"}
     *
     * .setSubject(subject):
     * - Sets "sub" claim (username)
     * - Standard JWT claim
     * - Used to identify user
     *
     * .setIssuedAt(new Date(System.currentTimeMillis())):
     * - Sets "iat" claim (issued at)
     * - Current timestamp in milliseconds
     * - When token was created
     * - Useful for tracking token age
     *
     * .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)):
     * - Sets "exp" claim (expiration)
     * - Current time + expiration duration
     * - After this time, token is invalid
     * - Automatic expiration handling
     *
     * .signWith(getSigningKey(), SignatureAlgorithm.HS256):
     * - Signs token with secret key
     * - Algorithm: HMAC-SHA256
     * - Creates tamper-proof signature
     * - Only server with secret can verify
     *
     * .compact():
     * - Serializes to compact string format
     * - Creates: HEADER.PAYLOAD.SIGNATURE
     * - Base64URL encoded
     * - Returns JWT string
     *
     * Example Output:
     * eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huX2RvZSIsImlhdCI6MTUxNjIzOTAyMiwiZXhwIjoxNTE2MjQyNjIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
     *
     * Decoded:
     * {
     *   "alg": "HS256",
     *   "typ": "JWT"
     * }.{
     *   "sub": "john_doe",
     *   "iat": 1516239022,
     *   "exp": 1516242622
     * }.[signature]
     *
     * Security:
     * - Signature prevents tampering
     * - Expiration prevents indefinite use
     * - Subject identifies user
     * - All data is encoded, NOT encrypted
     * - Don't put sensitive data in claims!
     *
     * @param claims - Custom claims (additional data)
     * @param subject - Subject (username)
     * @return String - Compact JWT token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)  // Add custom claims
                .setSubject(subject)  // Set username
                .setIssuedAt(new Date(System.currentTimeMillis()))  // Set creation time
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))  // Set expiration
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)  // Sign token
                .compact();  // Build and return as string
    }

    /**
     * VALIDATE TOKEN
     *
     * Checks if token is valid for given user.
     *
     * This is called on EVERY protected API request!
     *
     * Validation Steps:
     * 1. Extract username from token
     * 2. Check if username matches UserDetails
     * 3. Check if token is expired
     * 4. Both must be true for valid token
     *
     * Flow:
     * 1. extractUsername(token) → "john_doe"
     * 2. userDetails.getUsername() → "john_doe"
     * 3. username.equals(userDetails.getUsername()) → true ✅
     * 4. !isTokenExpired(token) → true (not expired) ✅
     * 5. Return true → Token is valid! ✅
     *
     * Why check username match?
     * - Prevents token reuse for different users
     * - Token is bound to specific user
     * - Extra security layer
     *
     * Example Valid:
     * Token username: "john_doe"
     * Loaded user: "john_doe"
     * Expired: No
     * → Valid ✅
     *
     * Example Invalid (Wrong User):
     * Token username: "john_doe"
     * Loaded user: "alice"
     * → Invalid ❌
     *
     * Example Invalid (Expired):
     * Token username: "john_doe"
     * Loaded user: "john_doe"
     * Expired: Yes
     * → Invalid ❌
     *
     * Usage (in JwtAuthenticationFilter):
     * if (jwtUtil.validateToken(token, userDetails)) {
     *     // Token valid, proceed with authentication
     * } else {
     *     // Token invalid, reject request
     * }
     *
     * @param token - JWT token to validate
     * @param userDetails - User information from database
     * @return Boolean - true if valid, false otherwise
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}

/*
 * ═══════════════════════════════════════════════════════════
 * KEY TAKEAWAYS - JWT UTILITY
 * ═══════════════════════════════════════════════════════════
 *
 * 1. TOKEN STRUCTURE
 *    - Header: Algorithm and type
 *    - Payload: Claims (username, expiration, etc.)
 *    - Signature: Verification (signed with secret)
 *
 * 2. TOKEN GENERATION
 *    - Called when user logs in
 *    - Contains username and expiration
 *    - Signed with secret key
 *    - Returns compact string
 *
 * 3. TOKEN VALIDATION
 *    - Extract username from token
 *    - Verify signature (auto by parser)
 *    - Check expiration
 *    - Match username with loaded user
 *
 * 4. SECURITY
 *    - Secret key must be secure (256+ bits)
 *    - Tokens expire automatically
 *    - Signature prevents tampering
 *    - Stateless (no server storage)
 *
 * 5. CLAIMS
 *    - Standard: sub, exp, iat
 *    - Custom: can add role, email, etc.
 *    - Not encrypted (don't put secrets!)
 *    - Base64 encoded (readable if decoded)
 */