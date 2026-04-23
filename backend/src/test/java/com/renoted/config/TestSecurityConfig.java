package com.renoted.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * TEST SECURITY CONFIGURATION
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * PURPOSE:
 * Override the Spring Boot’s default auto-config security for testing purposes.
 * This allows unit tests to access protected endpoints without authentication.
 *
 * WHY DO WE NEED THIS?
 * ────────────────────
 * Main SecurityConfig requires JWT authentication for /api/notes.
 * In unit tests, we're testing the controller logic, not authentication.
 * We need to bypass security to focus on controller behavior.
 *
 * APPROACH:
 * - Create a TestConfiguration class
 * - Override the SecurityFilterChain bean
 * - Disable security for testing
 * - Allow all endpoints to be accessed without authentication
 *
 * WHAT ABOUT SECURITY TESTING?
 * ────────────────────────────
 * This configuration is ONLY for unit tests that test controller logic.
 *
 * Later, you should write SEPARATE security tests that verify:
 * - Unauthorized requests are blocked
 * - Valid tokens are accepted
 * - Invalid tokens are rejected
 * - CORS rules are enforced
 *
 * Separation of concerns:
 * - Controller Unit Test: Test controller logic (no security)
 * - Security Unit Test: Test security configuration
 * - Integration Test: Test full flow with authentication
 *
 * HOW TEST CONFIGURATION WORKS:
 * ────────────────────────────
 * @TestConfiguration:
 * - Only loaded when testing
 * - Overrides main configuration
 * - Lower priority than @Configuration
 *
 * When @WebMvcTest loads:
 * 1. Loads main @Configuration classes
 * 2. Loads @TestConfiguration classes
 * 3. @TestConfiguration beans override main beans
 * 4. Test gets our security config instead of main
 * 5. Security is disabled for tests
 */

@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    /**
     * Override SecurityFilterChain for testing
     *
     * WHY OVERRIDE?
     * - Main security config requires authentication
     * - We want to test controller without auth complexity
     * - Focus on controller logic, not security
     *
     * WHAT WE DO:
     * - Disable CSRF (same as main config)
     * - Disable security filters
     * - Allow all requests
     * - No authentication required
     *
     * IMPORTANT:
     * This is ONLY for unit tests!
     * In production, actual security applies.
     */
    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        /*
         * Disable all security for testing
         *
         * authorizeHttpRequests:
         * - Handles authorization rules
         * - anyRequest().permitAll() = allow all requests
         * - No authentication needed
         *
         * Why permitAll()?
         * - Tests are focused on controller behavior
         * - Don't want to deal with authentication setup
         * - Makes tests simpler and faster
         * - Security tests are separate
         */
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        /*
         * Disable CSRF (same as main config)
         *
         * For our JWT API, CSRF is not needed.
         * Disabling here too for consistency.
         */
        http.csrf(csrf -> csrf.disable());

        return http.build();

        /*
         * RESULT:
         * - All endpoints accessible without authentication
         * - No JWT required
         * - Controller tests can run freely
         * - Focus on testing controller logic
         *
         * EXAMPLE TEST FLOW:
         * 1. Test sends POST /api/notes (no JWT)
         * 2. Security config allows it (TestSecurityConfig)
         * 3. Request reaches controller
         * 4. Controller processes normally
         * 5. Test verifies response
         */
    }
}

