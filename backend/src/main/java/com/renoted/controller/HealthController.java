package com.renoted.controller;

// Import statements
// These tell Java which classes we're using from Spring Framework
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * HealthController
 *
 * Purpose: Provides health check and status endpoints for the application
 * This is our FIRST REST controller!
 *
 * Why we need this:
 * - To verify the application is running
 * - To test our REST API setup
 * - To check database connectivity (later)
 * - Common in production apps (monitoring tools use this)
 */
@RestController  // <-- This annotation tells Spring: "This class handles web requests"
//     Combines @Controller + @ResponseBody
//     Automatically converts return values to JSON

@RequestMapping("/api")  // <-- Base URL path for all endpoints in this controller
//     All methods here will start with /api/...
//     Example: /api/health, /api/status
public class HealthController {

    /**
     * Health Check Endpoint
     *
     * URL: GET http://localhost:8080/api/health
     *
     * Purpose: Simple endpoint to verify application is running
     * Returns: JSON with status and timestamp
     *
     * How it works:
     * 1. Client sends GET request to /api/health
     * 2. Spring routes request to this method
     * 3. Method creates a Map (like a dictionary)
     * 4. Spring automatically converts Map to JSON
     * 5. JSON response sent back to client
     */
    @GetMapping("/health")  // <-- Maps HTTP GET requests to /api/health to this method
    //     @GetMapping = shorthand for @RequestMapping(method = GET)
    public Map<String, Object> healthCheck() {
        // Why Map<String, Object>?
        // - Map = key-value pairs (like JSON)
        // - String = keys are text ("status", "timestamp")
        // - Object = values can be any type (String, number, boolean, etc.)

        // Create a new HashMap to store response data
        // HashMap = implementation of Map interface
        Map<String, Object> response = new HashMap<>();

        // Add status field
        // This tells client: "Yes, I'm alive and working!"
        response.put("status", "UP");

        // Add message field
        response.put("message", "ReNoted API is running successfully!");

        // Add timestamp - useful for debugging
        // Shows exactly when this endpoint was called
        response.put("timestamp", LocalDateTime.now().toString());

        // Add version info
        response.put("version", "v0.1");

        // Return the map
        // Spring Boot automatically converts this to JSON:
        // {
        //   "status": "UP",
        //   "message": "ReNoted API is running successfully!",
        //   "timestamp": "2024-03-02T10:30:45.123",
        //   "version": "v0.1"
        // }
        return response;
    }

    /**
     * Welcome Endpoint
     *
     * URL: GET http://localhost:8080/api/welcome
     *
     * Purpose: Simple welcome message
     * Returns: Plain string (not JSON)
     */
    @GetMapping("/welcome")
    public String welcome() {
        // Return type is String, not Map
        // Spring still handles this - returns plain text response
        return "Welcome to ReNoted - Your Notion-like Note Taking App!";
    }
}
