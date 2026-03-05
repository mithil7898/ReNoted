// NotesPage.jsx - Main Notes Page
//
// Purpose: Display all notes, handle create/edit/delete

import { useState, useEffect } from 'react';
import { getAllNotes, createNote, updateNote, deleteNote } from '../services/api';
import NoteCard from '../components/NoteCard';
import NoteForm from '../components/NoteForm';
import DeleteModal from '../components/DeleteModal';

function NotesPage() {
  // State management
  const [notes, setNotes] = useState([]);              // All notes from backend
  const [loading, setLoading] = useState(true);        // Loading indicator
  const [error, setError] = useState(null);            // Error messages
  const [showForm, setShowForm] = useState(false);     // Show/hide create form
  const [editingNote, setEditingNote] = useState(null);// Note being edited
  const [deletingNote, setDeletingNote] = useState(null); // Note to delete

  // Fetch all notes when component mounts
  useEffect(() => {
    fetchNotes();
  }, []);

  /**
   * Fetch all notes from backend
   */
  const fetchNotes = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await getAllNotes();
      setNotes(data);
    } catch (err) {
      setError('Failed to load notes. Please try again.');
      console.error('Error fetching notes:', err);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Handle creating new note
   */
  const handleCreate = async (noteData) => {
    try {
      const newNote = await createNote(noteData);
      setNotes([newNote, ...notes]); // Add to beginning of list
      setShowForm(false);
    } catch (err) {
      alert('Failed to create note: ' + err.message);
    }
  };

  /**
   * Handle updating existing note
   */
  const handleUpdate = async (id, noteData) => {
    try {
      const updatedNote = await updateNote(id, noteData);
      setNotes(notes.map(note => note.id === id ? updatedNote : note));
      setEditingNote(null);
    } catch (err) {
      alert('Failed to update note: ' + err.message);
    }
  };

  /**
   * Handle deleting note
   */
  const handleDelete = async () => {
    if (!deletingNote) return;
    
    try {
      await deleteNote(deletingNote.id);
      setNotes(notes.filter(note => note.id !== deletingNote.id));
      setDeletingNote(null);
    } catch (err) {
      alert('Failed to delete note: ' + err.message);
    }
  };

  // Loading state
  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-4 border-indigo-600 mx-auto"></div>
          <p className="mt-4 text-gray-600 text-lg">Loading notes...</p>
        </div>
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-red-50 to-pink-100 flex items-center justify-center p-4">
        <div className="bg-white rounded-lg shadow-xl p-8 max-w-md w-full">
          <div className="text-center">
            <div className="text-6xl mb-4">❌</div>
            <h2 className="text-2xl font-bold text-red-600 mb-4">Error</h2>
            <p className="text-gray-700 mb-6">{error}</p>
            <button 
              onClick={fetchNotes}
              className="bg-red-600 text-white px-6 py-2 rounded-lg hover:bg-red-700 transition"
            >
              Retry
            </button>
          </div>
        </div>
      </div>
    );
  }

  // Main UI
  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100">
      {/* Header */}
      <header className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4 py-6 flex justify-between items-center">
          <div>
            <h1 className="text-3xl font-bold text-indigo-600">ReNoted</h1>
            <p className="text-gray-600">Your notes, organized</p>
          </div>
          <button
            onClick={() => setShowForm(true)}
            className="bg-indigo-600 text-white px-6 py-3 rounded-lg hover:bg-indigo-700 transition font-semibold flex items-center gap-2"
          >
            <span className="text-xl">+</span>
            New Note
          </button>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 py-8">
        {/* Show form when creating or editing */}
        {(showForm || editingNote) && (
          <NoteForm
            note={editingNote}
            onSave={editingNote ? 
              (data) => handleUpdate(editingNote.id, data) : 
              handleCreate
            }
            onCancel={() => {
              setShowForm(false);
              setEditingNote(null);
            }}
          />
        )}

        {/* Notes Grid */}
        {notes.length === 0 ? (
          <div className="text-center py-16">
            <div className="text-6xl mb-4">📝</div>
            <h2 className="text-2xl font-bold text-gray-700 mb-2">No notes yet</h2>
            <p className="text-gray-600 mb-6">Create your first note to get started!</p>
            <button
              onClick={() => setShowForm(true)}
              className="bg-indigo-600 text-white px-6 py-3 rounded-lg hover:bg-indigo-700 transition"
            >
              Create Note
            </button>
          </div>
        ) : (
          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
            {notes.map(note => (
              <NoteCard
                key={note.id}
                note={note}
                onEdit={() => setEditingNote(note)}
                onDelete={() => setDeletingNote(note)}
              />
            ))}
          </div>
        )}
      </main>

      {/* Delete Confirmation Modal */}
      {deletingNote && (
        <DeleteModal
          note={deletingNote}
          onConfirm={handleDelete}
          onCancel={() => setDeletingNote(null)}
        />
      )}
    </div>
  );
}

export default NotesPage;