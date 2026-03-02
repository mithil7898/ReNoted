package com.renoted.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/**
 * CORS Configuration
 *
 * Purpose: Allow frontend (React on port 5173) to call backend (Spring Boot on port 8080)
 *
 * What is CORS?
 * - CORS = Cross-Origin Resource Sharing
 * - Security feature in browsers
 * - By default, browsers block requests from different origins (domains/ports)
 * - Example: localhost:5173 calling localhost:8080 is "cross-origin"
 *
 * Why we need this:
 * - Our frontend runs on port 5173
 * - Our backend runs on port 8080
 * - Browser sees these as different origins
 * - Without CORS config, browser blocks the API calls
 *
 * This class tells Spring Boot:
 * "Allow requests from localhost:5173 to access our APIs"
 */
@Configuration  // Marks this as a Spring configuration class
public class CorsConfig {

    /**
     * Creates and configures the CORS filter
     *
     * @Bean annotation tells Spring to create this object
     * and make it available for dependency injection
     */
    @Bean
    public CorsFilter corsFilter() {
        // Create CORS configuration
        CorsConfiguration config = new CorsConfiguration();

        // Allow credentials (cookies, authorization headers)
        // Important for when we add JWT authentication later
        config.setAllowCredentials(true);

        // Allow requests from our React frontend
        // In production, this would be your actual domain
        // For development, we allow localhost:5173
        config.setAllowedOrigins(Arrays.asList("http://localhost:5173"));

        // Allow all HTTP headers
        // Headers include: Content-Type, Authorization, etc.
        config.addAllowedHeader("*");

        // Allow all HTTP methods
        // GET, POST, PUT, DELETE, PATCH, OPTIONS
        config.addAllowedMethod("*");

        // Create URL-based CORS configuration source
        // This maps CORS config to URL patterns
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // Apply CORS config to all endpoints (/**)
        // /** means "match all paths and sub-paths"
        source.registerCorsConfiguration("/**", config);

        // Create and return the CORS filter
        // Spring will automatically apply this filter to all requests
        return new CorsFilter(source);
    }
}