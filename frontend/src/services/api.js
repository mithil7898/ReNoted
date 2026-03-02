// api.js - API Service Layer
// 
// Purpose: Centralized place for all backend API calls
// Why we need this:
// - Don't repeat backend URL everywhere
// - Easy to change backend URL later (one place)
// - Consistent error handling
// - Can add authentication headers later (JWT)

import axios from 'axios';

// Base URL for all API calls
// This points to our Spring Boot backend
// Later, we can use environment variables for production
const API_BASE_URL = 'http://localhost:8080/api';

// Create an axios instance with default configuration
// Think of this as a pre-configured HTTP client
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',  // We're sending/receiving JSON
  },
  timeout: 10000,  // 10 second timeout (prevents hanging forever)
});

// API Functions
// Each function corresponds to a backend endpoint

/**
 * Health Check
 * Calls: GET /api/health
 * Purpose: Verify backend is running
 */
export const healthCheck = async () => {
  try {
    const response = await api.get('/health');
    return response.data;  // Returns the JSON data
  } catch (error) {
    console.error('Health check failed:', error);
    throw error;  // Re-throw so caller can handle it
  }
};

/**
 * Get Welcome Message
 * Calls: GET /api/welcome
 * Purpose: Test plain text endpoint
 */
export const getWelcome = async () => {
  try {
    const response = await api.get('/welcome');
    return response.data;
  } catch (error) {
    console.error('Welcome API failed:', error);
    throw error;
  }
};

// Export the configured axios instance
// This allows other files to make custom API calls if needed
export default api;