/**
 * NotesPage Component (v0.3 - COMPLETE WITH TAGS & SEARCH)
 * 
 * Purpose: Main page for managing notes with tags, search, and filters
 * 
 * Features:
 * - CRUD operations for notes
 * - Tag management (create, delete, assign to notes)
 * - Search notes by title/content
 * - Filter notes by tag
 * - Remove tags from notes
 */

import React, { useState, useEffect } from 'react';
import NoteCard from '../components/NoteCard';
import NoteForm from '../components/NoteForm';
import DeleteModal from '../components/DeleteModal';
import NoteViewModal from '../components/NoteViewModal';
import SearchBar from '../components/SearchBar';
import TagManager from '../components/TagManager';  // ← ADD THIS
import TagFilter from '../components/TagFilter';
import {
  getAllNotes,
  createNote,
  updateNote,
  deleteNote,
  searchNotes,
  filterNotesByTag,
  removeTagFromNote,
  getAllTags,
  createTag,
  deleteTag,
} from '../services/api';

const NotesPage = () => {
  // ============================================================================
  // STATE MANAGEMENT
  // ============================================================================

  // Notes state
  const [notes, setNotes] = useState([]);
  const [displayedNotes, setDisplayedNotes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [viewNote, setViewNote] = useState(null);  // ← ADD THIS
  const [isModalOpen, setIsModalOpen] = useState(false);  // ← ADD THIS

  // Tags state
  const [tags, setTags] = useState([]);
  const [tagsLoading, setTagsLoading] = useState(true);

  // UI state
  const [showForm, setShowForm] = useState(false);
  const [editingNote, setEditingNote] = useState(null);
  const [deletingNote, setDeletingNote] = useState(null);

  // Search & Filter state
  const [searchQuery, setSearchQuery] = useState('');
  const [activeTagFilter, setActiveTagFilter] = useState(null);

  // ============================================================================
  // DATA FETCHING
  // ============================================================================

  /**
   * Fetch all notes from backend
   */
  const fetchNotes = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await getAllNotes();
      setNotes(data);
      setDisplayedNotes(data);
    } catch (err) {
      console.error('Error fetching notes:', err);
      setError('Failed to load notes. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  /**
   * Fetch all tags from backend
   */
  const fetchTags = async () => {
    try {
      setTagsLoading(true);
      const data = await getAllTags();
      setTags(data);
    } catch (err) {
      console.error('Error fetching tags:', err);
    } finally {
      setTagsLoading(false);
    }
  };

  /**
   * Initial data load
   */
  useEffect(() => {
    fetchNotes();
    fetchTags();
  }, []);

  // ============================================================================
  // NOTE CRUD OPERATIONS
  // ============================================================================

  /**
   * CREATE - Handle note creation
   */
  const handleCreate = async (noteData) => {
    try {
      const newNote = await createNote(noteData);
      setNotes([newNote, ...notes]);
      setShowForm(false);
      
      // Refresh displayed notes based on current filters
      applyFilters([newNote, ...notes]);
    } catch (err) {
      console.error('Error creating note:', err);
      throw err;
    }
  };

  /**
   * UPDATE - Handle note update
   */
  const handleUpdate = async (noteData) => {
    try {
      const updatedNote = await updateNote(editingNote.id, noteData);
      const updatedNotes = notes.map((note) =>
        note.id === updatedNote.id ? updatedNote : note
      );
      setNotes(updatedNotes);
      setShowForm(false);
      setEditingNote(null);

      // Refresh displayed notes
      applyFilters(updatedNotes);
    } catch (err) {
      console.error('Error updating note:', err);
      throw err;
    }
  };

  /**
   * DELETE - Handle note deletion
   */
  const handleDelete = async () => {
    try {
      await deleteNote(deletingNote.id);
      const remainingNotes = notes.filter((note) => note.id !== deletingNote.id);
      setNotes(remainingNotes);
      setDeletingNote(null);

      // Refresh displayed notes
      applyFilters(remainingNotes);
    } catch (err) {
      console.error('Error deleting note:', err);
      alert('Failed to delete note. Please try again.');
    }
  };

  /**
   * FORM - Handle form submission (create or update)
   */
  const handleFormSubmit = async (noteData) => {
    if (editingNote) {
      await handleUpdate(noteData);
    } else {
      await handleCreate(noteData);
    }
  };

  /**
   * EDIT - Open form with existing note
   */
  const handleEdit = (note) => {
    setEditingNote(note);
    setShowForm(true);
  };

  /**
   * CANCEL - Close form
   */
  const handleCancel = () => {
    setShowForm(false);
    setEditingNote(null);
  };

  // ============================================================================
  // TAG OPERATIONS
  // ============================================================================

  /**
   * CREATE TAG - Create new tag
   */
  const handleCreateTag = async (tagName) => {
    try {
      const newTag = await createTag({ name: tagName });
      setTags([...tags, newTag]);
      return newTag;
    } catch (err) {
      console.error('Error creating tag:', err);
      alert('Failed to create tag. It might already exist.');
      throw err;
    }
  };

  /**
   * DELETE TAG - Delete tag and refresh data
   * 
   * After deletion:
   * - Tag removed from tags list
   * - Notes refreshed (tag removed from their tagIds)
   * - Filters reapplied
   */
  const handleDeleteTag = async (tagId) => {
    try {
      // Delete tag
      await deleteTag(tagId);
      
      // Update tags list
      setTags(tags.filter(tag => tag.id !== tagId));
      
      // Refresh notes (to get updated tagIds)
      await fetchNotes();
      
      // Clear tag filter if we just deleted the active filter
      if (activeTagFilter === tagId) {
        setActiveTagFilter(null);
      }
    } catch (err) {
      console.error('Error deleting tag:', err);
      alert('Failed to delete tag. Please try again.');
    }
  };

  /**
   * REMOVE TAG FROM NOTE - Remove specific tag from note
   */
  const handleRemoveTagFromNote = async (noteId, tagId) => {
    try {
      // Use the helper function from api.js
      const updatedNote = await removeTagFromNote(noteId, tagId);
      
      // Update local state
      const updatedNotes = notes.map(note =>
        note.id === noteId ? updatedNote : note
      );
      setNotes(updatedNotes);

      // Refresh displayed notes
      applyFilters(updatedNotes);
    } catch (err) {
      console.error('Error removing tag from note:', err);
      alert('Failed to remove tag from note.');
    }
  };

  // ============================================================================
  // SEARCH & FILTER
  // ============================================================================

  /**
   * SEARCH - Handle search query
   */
  const handleSearch = async (query) => {
    setSearchQuery(query);
    setActiveTagFilter(null);  // Clear tag filter when searching

    if (!query.trim()) {
      // Empty search - show all notes
      setDisplayedNotes(notes);
      return;
    }

    try {
      const results = await searchNotes(query);
      setDisplayedNotes(results);
    } catch (err) {
      console.error('Error searching notes:', err);
      setDisplayedNotes([]);
    }
  };

  /**
   * FILTER BY TAG - Handle tag filter change
   */
  const handleTagFilter = async (tagId) => {
    setActiveTagFilter(tagId);
    setSearchQuery('');  // Clear search when filtering by tag

    if (tagId === null) {
      // "All" button clicked - show all notes
      setDisplayedNotes(notes);
      return;
    }

    try {
      const results = await filterNotesByTag(tagId);
      setDisplayedNotes(results);
    } catch (err) {
      console.error('Error filtering notes by tag:', err);
      setDisplayedNotes([]);
    }
  };

  /**
   * APPLY FILTERS - Reapply current search/filter after note changes
   */
  const applyFilters = (updatedNotes) => {
    if (searchQuery) {
      // Reapply search
      searchNotes(searchQuery).then(results => setDisplayedNotes(results));
    } else if (activeTagFilter !== null) {
      // Reapply tag filter
      filterNotesByTag(activeTagFilter).then(results => setDisplayedNotes(results));
    } else {
      // No filters - show all
      setDisplayedNotes(updatedNotes);
    }
  };

  /**
   * EDIT NOTE - Open form with existing note data
   */
  const handleEditNote = (note) => {
    setEditingNote(note);
    setShowForm(true);
  };

  /**
   * VIEW NOTE - Open modal
   */
  const handleViewNote = (note) => {
    setViewNote(note);
    setIsModalOpen(true);
  };

  /**
   * CLOSE MODAL
   */
  const handleCloseModal = () => {
    setIsModalOpen(false);
    setTimeout(() => setViewNote(null), 300);
  };

  /**
   * DELETE NOTE - Show delete confirmation
   */
  const handleDeleteClick = (note) => {
    setDeletingNote(note);
  };

  // ============================================================================
  // RENDER
  // ============================================================================

  // ============================================================================
  // RENDER
  // ============================================================================

  if (loading || tagsLoading) {
    return (
      <div className="flex justify-center items-center h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-4 border-indigo-600 border-t-transparent"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-8">
        <div className="bg-red-50 border border-red-200 rounded-lg p-6 text-center">
          <p className="text-red-600 mb-4">❌ {error}</p>
          <button
            onClick={fetchNotes}
            className="bg-red-600 text-white px-6 py-2 rounded-md hover:bg-red-700"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-indigo-50 to-purple-50">
      <div className="max-w-7xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="flex justify-between items-center mb-8">
          <div>
            <h1 className="text-4xl font-bold text-indigo-900">ReNoted</h1>
            <p className="text-gray-600 mt-1">Your notes, organized</p>
          </div>
          <button
            onClick={() => {
              setEditingNote(null);
              setShowForm(true);
            }}
            className="bg-indigo-600 text-white px-6 py-3 rounded-lg hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-colors flex items-center gap-2"
          >
            <span className="text-xl">+</span>
            New Note
          </button>
        </div>

        {/* Search Bar */}
        <div className="mb-6">
          <SearchBar onSearch={handleSearch} />
        </div>

        {/* Tag Filters */}
        {tags.length > 0 && (
          <TagFilter
            tags={tags}
            activeTagId={activeTagFilter}
            onFilterChange={handleTagFilter}
            notes={notes}
          />
        )}

        {/* Tag Manager */}
        <TagManager
          tags={tags}
          notes={notes}
          onCreateTag={handleCreateTag}
          onDeleteTag={handleDeleteTag}
          onRefresh={fetchTags}
        />

        {/* Note Form */}
        {showForm && (
          <NoteForm
            note={editingNote}
            onSubmit={handleFormSubmit}
            onCancel={handleCancel}
            availableTags={tags}
            onCreateTag={handleCreateTag}
          />
        )}

        {/* Notes Grid */}
        {displayedNotes.length === 0 ? (
          <div className="text-center py-16">
            <div className="text-6xl mb-4">📝</div>
            <h2 className="text-2xl font-semibold text-gray-700 mb-2">
              {searchQuery
                ? 'No notes found'
                : activeTagFilter
                ? 'No notes with this tag'
                : 'No notes yet'}
            </h2>
            <p className="text-gray-500 mb-6">
              {searchQuery
                ? `No notes match "${searchQuery}"`
                : activeTagFilter
                ? 'Try selecting a different tag or create a new note'
                : 'Create your first note to get started!'}
            </p>
            {(searchQuery || activeTagFilter) && (
              <button
                onClick={() => {
                  setSearchQuery('');
                  setActiveTagFilter(null);
                  setDisplayedNotes(notes);
                }}
                className="text-indigo-600 hover:text-indigo-700 underline"
              >
                Clear filters
              </button>
            )}
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {displayedNotes.map((note) => (
              <NoteCard
              key={note.id}
              note={note}
              availableTags={tags}
              onEdit={handleEditNote}
              onDelete={handleDeleteClick}
              onRemoveTag={handleRemoveTagFromNote}
              onViewNote={handleViewNote}  // ← ADD THIS
            />
            ))}
          </div>
        )}

        {/* Delete Confirmation Modal */}
        {deletingNote && (
          <DeleteModal
            note={deletingNote}
            onConfirm={handleDelete}
            onCancel={() => setDeletingNote(null)}
          />
        )}
        {/* Note View Modal */}
        <NoteViewModal
          note={viewNote}
          isOpen={isModalOpen}
          onClose={handleCloseModal}
        />
      </div>
    </div>
  );
};

export default NotesPage;