// api.js - API Service Layer
// 
// Purpose: Centralized place for all backend API calls
// Updated for v0.3 with Tags and Search functionality

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
// HEALTH CHECK ENDPOINTS (v0.1)
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
// NOTE CRUD OPERATIONS (v0.2 + v0.3 with tags)
// ============================================================================

/**
 * Get All Notes
 * GET /api/notes
 * 
 * Returns: Array of note objects with tagIds
 * Example: [{ id: 1, title: "Note", tagIds: [1, 2], ... }]
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
 * Returns: Note object with tagIds
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
 * @param {Array<number>} noteData.tagIds - Array of tag IDs (optional)
 * 
 * Returns: Created note with ID, timestamps, and tagIds
 * 
 * Example:
 * createNote({ 
 *   title: "My Note", 
 *   content: "Content here",
 *   tagIds: [1, 2]
 * })
 */
export const createNote = async (noteData) => {
  try {
    const response = await api.post('/notes', noteData);
    return response.data;
  } catch (error) {
    console.error('Failed to create note:', error);
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
 * @param {Array<number>} noteData.tagIds - Array of tag IDs
 * 
 * Returns: Updated note
 * 
 * Example - Remove tag 2 from note:
 * updateNote(1, { 
 *   title: "My Note", 
 *   content: "Content",
 *   tagIds: [1, 3]  // Removed tag 2
 * })
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
  } catch (error) {
    console.error(`Failed to delete note ${id}:`, error);
    throw error;
  }
};

// ============================================================================
// SEARCH & FILTER OPERATIONS (v0.3)
// ============================================================================

/**
 * Search Notes
 * GET /api/notes/search?query={keyword}
 * 
 * Searches in both title and content (case-insensitive)
 * 
 * @param {string} query - Search keyword
 * Returns: Array of matching notes
 * 
 * Example:
 * searchNotes("spring") → All notes with "spring" in title or content
 */
export const searchNotes = async (query) => {
  try {
    const response = await api.get('/notes/search', {
      params: { query }
    });
    return response.data;
  } catch (error) {
    console.error('Failed to search notes:', error);
    throw error;
  }
};

/**
 * Filter Notes by Tag
 * GET /api/notes/filter/tag/{tagId}
 * 
 * Returns all notes that have the specified tag
 * 
 * @param {number} tagId - Tag ID to filter by
 * Returns: Array of notes with this tag
 * 
 * Example:
 * filterNotesByTag(1) → All notes tagged with tag ID 1
 */
export const filterNotesByTag = async (tagId) => {
  try {
    const response = await api.get(`/notes/filter/tag/${tagId}`);
    return response.data;
  } catch (error) {
    console.error(`Failed to filter notes by tag ${tagId}:`, error);
    throw error;
  }
};

// ============================================================================
// TAG OPERATIONS (v0.3)
// ============================================================================

/**
 * Get All Tags
 * GET /api/tags
 * 
 * Returns: Array of all tags (alphabetically sorted by backend)
 * Example: [{ id: 1, name: "Java" }, { id: 2, name: "Spring" }]
 */
export const getAllTags = async () => {
  try {
    const response = await api.get('/tags');
    return response.data;
  } catch (error) {
    console.error('Failed to fetch tags:', error);
    throw error;
  }
};

/**
 * Get Single Tag by ID
 * GET /api/tags/{id}
 * 
 * @param {number} id - Tag ID
 * Returns: Tag object
 */
export const getTagById = async (id) => {
  try {
    const response = await api.get(`/tags/${id}`);
    return response.data;
  } catch (error) {
    console.error(`Failed to fetch tag ${id}:`, error);
    throw error;
  }
};

/**
 * Create New Tag
 * POST /api/tags
 * 
 * @param {Object} tagData - Tag data
 * @param {string} tagData.name - Tag name (required)
 * 
 * Returns: Created tag with normalized name
 * 
 * Example:
 * createTag({ name: "machine learning" })
 * Returns: { id: 5, name: "Machine Learning" }  // Normalized to Title Case
 * 
 * Note: Backend prevents duplicate tags (case-insensitive)
 */
export const createTag = async (tagData) => {
  try {
    const response = await api.post('/tags', tagData);
    return response.data;
  } catch (error) {
    console.error('Failed to create tag:', error);
    if (error.response?.data?.message) {
      throw new Error(error.response.data.message);
    }
    if (error.response?.status === 400) {
      throw new Error('Tag already exists or invalid name');
    }
    throw error;
  }
};

/**
 * Delete Tag
 * DELETE /api/tags/{id}
 * 
 * Note: Cannot delete tag if it's being used by notes
 * Must remove tag from all notes first
 * 
 * @param {number} id - Tag ID to delete
 * Returns: void (no content)
 * 
 * Error handling:
 * - 404: Tag not found
 * - 400: Tag is in use (remove from notes first)
 */
export const deleteTag = async (id) => {
  try {
    await api.delete(`/tags/${id}`);
  } catch (error) {
    console.error(`Failed to delete tag ${id}:`, error);
    if (error.response?.status === 400) {
      throw new Error('Cannot delete tag - it is being used by notes. Remove it from all notes first.');
    }
    if (error.response?.status === 404) {
      throw new Error('Tag not found');
    }
    throw error;
  }
};

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

/**
 * Remove tag from note
 * 
 * This is a convenience function that:
 * 1. Fetches current note
 * 2. Filters out the unwanted tag
 * 3. Updates the note
 * 
 * @param {number} noteId - Note ID
 * @param {number} tagIdToRemove - Tag ID to remove
 * Returns: Updated note
 * 
 * Example:
 * removeTagFromNote(7, 2)  // Remove tag 2 from note 7
 */
export const removeTagFromNote = async (noteId, tagIdToRemove) => {
  try {
    // Get current note
    const note = await getNoteById(noteId);
    
    // Filter out unwanted tag
    const updatedTagIds = note.tagIds.filter(id => id !== tagIdToRemove);
    
    // Update note with new tag list
    const updatedNote = await updateNote(noteId, {
      title: note.title,
      content: note.content,
      tagIds: updatedTagIds
    });
    
    return updatedNote;
  } catch (error) {
    console.error(`Failed to remove tag ${tagIdToRemove} from note ${noteId}:`, error);
    throw error;
  }
};

/**
 * Add tag to note
 * 
 * This is a convenience function that:
 * 1. Fetches current note
 * 2. Adds new tag ID (if not already present)
 * 3. Updates the note
 * 
 * @param {number} noteId - Note ID
 * @param {number} tagIdToAdd - Tag ID to add
 * Returns: Updated note
 * 
 * Example:
 * addTagToNote(7, 3)  // Add tag 3 to note 7
 */
export const addTagToNote = async (noteId, tagIdToAdd) => {
  try {
    // Get current note
    const note = await getNoteById(noteId);
    
    // Add tag if not already present
    const updatedTagIds = note.tagIds.includes(tagIdToAdd)
      ? note.tagIds  // Already has tag, no change
      : [...note.tagIds, tagIdToAdd];  // Add new tag
    
    // Update note
    const updatedNote = await updateNote(noteId, {
      title: note.title,
      content: note.content,
      tagIds: updatedTagIds
    });
    
    return updatedNote;
  } catch (error) {
    console.error(`Failed to add tag ${tagIdToAdd} to note ${noteId}:`, error);
    throw error;
  }
};

// Export configured axios instance
export default api;