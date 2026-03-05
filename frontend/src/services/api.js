// api.js - API Service Layer
// 
// Purpose: Centralized place for all backend API calls
// Updated with Note CRUD operations

import axios from 'axios';

// Base URL for all API calls
const API_BASE_URL = 'http://localhost:8080/api';

// Create axios instance with default configuration
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

// ============================================================================
// HEALTH CHECK ENDPOINTS (from v0.1)
// ============================================================================

/**
 * Health Check
 * GET /api/health
 */
export const healthCheck = async () => {
  try {
    const response = await api.get('/health');
    return response.data;
  } catch (error) {
    console.error('Health check failed:', error);
    throw error;
  }
};

/**
 * Get Welcome Message
 * GET /api/welcome
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

// ============================================================================
// NOTE CRUD OPERATIONS (v0.2)
// ============================================================================

/**
 * Get All Notes
 * GET /api/notes
 * 
 * Returns: Array of note objects
 * Example: [{ id: 1, title: "My Note", content: "...", createdAt: "...", updatedAt: "..." }]
 */
export const getAllNotes = async () => {
  try {
    const response = await api.get('/notes');
    return response.data;
  } catch (error) {
    console.error('Failed to fetch notes:', error);
    throw error;
  }
};

/**
 * Get Single Note by ID
 * GET /api/notes/{id}
 * 
 * @param {number} id - Note ID
 * Returns: Note object
 */
export const getNoteById = async (id) => {
  try {
    const response = await api.get(`/notes/${id}`);
    return response.data;
  } catch (error) {
    console.error(`Failed to fetch note ${id}:`, error);
    throw error;
  }
};

/**
 * Create New Note
 * POST /api/notes
 * 
 * @param {Object} noteData - Note data
 * @param {string} noteData.title - Note title (required)
 * @param {string} noteData.content - Note content (optional)
 * 
 * Returns: Created note with ID and timestamps
 * 
 * Example:
 * createNote({ title: "My Note", content: "Content here" })
 */
export const createNote = async (noteData) => {
  try {
    const response = await api.post('/notes', noteData);
    return response.data;
  } catch (error) {
    console.error('Failed to create note:', error);
    // Extract validation error message if available
    if (error.response?.data?.message) {
      throw new Error(error.response.data.message);
    }
    throw error;
  }
};

/**
 * Update Existing Note
 * PUT /api/notes/{id}
 * 
 * @param {number} id - Note ID to update
 * @param {Object} noteData - Updated note data
 * @param {string} noteData.title - Note title
 * @param {string} noteData.content - Note content
 * 
 * Returns: Updated note
 */
export const updateNote = async (id, noteData) => {
  try {
    const response = await api.put(`/notes/${id}`, noteData);
    return response.data;
  } catch (error) {
    console.error(`Failed to update note ${id}:`, error);
    if (error.response?.data?.message) {
      throw new Error(error.response.data.message);
    }
    throw error;
  }
};

/**
 * Delete Note
 * DELETE /api/notes/{id}
 * 
 * @param {number} id - Note ID to delete
 * Returns: void (no content)
 */
export const deleteNote = async (id) => {
  try {
    await api.delete(`/notes/${id}`);
    // DELETE returns 204 No Content, so no response.data
  } catch (error) {
    console.error(`Failed to delete note ${id}:`, error);
    throw error;
  }
};

// Export configured axios instance for custom calls
export default api;