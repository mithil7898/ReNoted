package com.renoted.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * PASSWORD ENCODER CONFIGURATION
 *
 * PURPOSE:
 * This configuration class provides a BCrypt password encoder bean
 * that will be used throughout the application for password hashing.
 *
 * WHY A SEPARATE CONFIGURATION CLASS?
 * - Separates password encoding from security configuration
 * - Prevents circular dependency issues
 * - Makes the encoder available before SecurityConfig is initialized
 * - Follows Single Responsibility Principle
 *
 * WHAT IS A BEAN?
 * A bean is an object managed by Spring's IoC (Inversion of Control) container.
 * When we mark a method with @Bean:
 * 1. Spring calls this method
 * 2. Spring stores the returned object
 * 3. Spring injects it wherever needed (via @Autowired)
 *
 * SINGLETON PATTERN:
 * By default, Spring beans are singletons:
 * - Only ONE PasswordEncoder object is created
 * - Same instance is reused everywhere
 * - Thread-safe and efficient
 */
@Configuration  // Tells Spring: "This class contains bean definitions"
public class PasswordEncoderConfig {

    /**
     * CREATE PASSWORD ENCODER BEAN
     *
     * @Bean annotation:
     * - Tells Spring to call this method
     * - Register the return value as a bean
     * - Make it available for dependency injection
     *
     * BCRYPT EXPLAINED:
     *
     * BCryptPasswordEncoder is a password hashing function designed by
     * Niels Provos and David Mazières based on the Blowfish cipher.
     *
     * Key Features:
     * 1. ADAPTIVE: Can configure "strength" (work factor)
     *    - Default strength: 10
     *    - Higher = slower = more secure
     *    - Each increase doubles the time needed
     *
     * 2. SALTED AUTOMATICALLY:
     *    - Random salt generated per password
     *    - Salt is embedded in the hash
     *    - Same password = different hashes
     *
     * 3. ONE-WAY FUNCTION:
     *    - Cannot reverse hash to get password
     *    - Only way to verify: hash input and compare
     *
     * 4. SLOW BY DESIGN:
     *    - Takes ~100ms to hash (at strength 10)
     *    - Prevents brute force attacks
     *    - Barely noticeable for legitimate login
     *    - Makes attacker's life very difficult
     *
     * BCRYPT HASH FORMAT:
     * $2a$10$N9qo8uLOickgx2ZMRZoMye...
     * │ │  │  │
     * │ │  │  └─ Salt + Hash (combined)
     * │ │  └──── Work factor (10 = 2^10 = 1024 rounds)
     * │ └─────── BCrypt version (2a)
     * └────────── Algorithm identifier
     *
     * WHY STRENGTH 10?
     * - Good balance between security and performance
     * - Fast enough for login (~100ms)
     * - Slow enough to prevent brute force
     * - Can be increased as hardware improves
     *
     * ALTERNATIVES AND WHY BCRYPT IS BETTER:
     *
     * MD5 / SHA-256:
     * ❌ Too fast (billions of hashes per second)
     * ❌ No built-in salt
     * ❌ Vulnerable to rainbow tables
     *
     * BCrypt:
     * ✅ Slow by design
     * ✅ Automatic salting
     * ✅ Adaptive (configurable difficulty)
     * ✅ Industry standard
     *
     * @return PasswordEncoder - BCrypt implementation
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        /*
         * Create BCryptPasswordEncoder with default strength (10)
         *
         * What this encoder can do:
         * 1. encode(rawPassword) - Hash a plain text password
         * 2. matches(raw, encoded) - Verify password against hash
         *
         * Example usage:
         *
         * // Hashing a password (registration)
         * String rawPassword = "myPassword123";
         * String hash = passwordEncoder.encode(rawPassword);
         * // hash = "$2a$10$N9qo8uLOickgx2ZMRZoMye..."
         *
         * // Verifying a password (login)
         * String inputPassword = "myPassword123";
         * boolean matches = passwordEncoder.matches(inputPassword, hash);
         * // matches = true (correct password!)
         *
         * // Wrong password
         * boolean matches = passwordEncoder.matches("wrongPass", hash);
         * // matches = false (incorrect password!)
         */
        return new BCryptPasswordEncoder();

        /*
         * ADVANCED: Custom strength
         * If you want stronger hashing (slower but more secure):
         * return new BCryptPasswordEncoder(12);  // Strength 12 = 4x slower than 10
         *
         * Recommended strengths:
         * - 10: Good for most applications (default)
         * - 12: High security applications
         * - 14: Maximum security (slow!)
         *
         * Never go below 10 in production!
         */
    }

    /*
     * ═══════════════════════════════════════════════════════════
     * KEY TAKEAWAYS - PASSWORD SECURITY
     * ═══════════════════════════════════════════════════════════
     *
     * 1. NEVER STORE PLAIN TEXT PASSWORDS
     *    - Always hash before storing
     *    - Use BCrypt, not MD5 or SHA-256
     *
     * 2. BCRYPT PROPERTIES
     *    - One-way (cannot be reversed)
     *    - Salted (unique hash per password)
     *    - Slow (prevents brute force)
     *    - Adaptive (can increase difficulty)
     *
     * 3. VERIFICATION PROCESS
     *    - Never decrypt the hash
     *    - Hash the input password
     *    - Compare hashes
     *    - Match = correct password
     *
     * 4. SPRING SECURITY INTEGRATION
     *    - This bean is injected everywhere needed
     *    - Used by AuthenticationManager
     *    - Used in registration service
     *    - Single instance (singleton)
     *
     * 5. SECURITY BEST PRACTICES
     *    - Use strength 10 or higher
     *    - Never log passwords (even hashed)
     *    - Use HTTPS to protect passwords in transit
     *    - Enforce password complexity rules
     */
}