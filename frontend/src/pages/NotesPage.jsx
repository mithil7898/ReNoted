import React, { useState, useEffect } from 'react';
import NoteCard from '../components/NoteCard';
import NoteForm from '../components/NoteForm';
import DeleteModal from '../components/DeleteModal';
import NoteViewModal from '../components/NoteViewModal';
import Navbar from "../components/Navbar";
import SearchBar from '../components/SearchBar';
import TagManager from '../components/TagManager';
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

  // ================= STATE =================
  const [notes, setNotes] = useState([]);
  const [displayedNotes, setDisplayedNotes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [tagsLoading, setTagsLoading] = useState(true);
  const [error, setError] = useState(null);

  const [tags, setTags] = useState([]);

  const [showForm, setShowForm] = useState(false);
  const [editingNote, setEditingNote] = useState(null);
  const [deletingNote, setDeletingNote] = useState(null);

  const [searchQuery, setSearchQuery] = useState('');
  const [activeTagFilter, setActiveTagFilter] = useState(null);

  const [viewNote, setViewNote] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  // ================= FETCH =================
  const fetchNotes = async () => {
    try {
      setLoading(true);
      const data = await getAllNotes();
      setNotes(data);
      setDisplayedNotes(data);
    } catch (err) {
      setError("Failed to load notes");
    } finally {
      setLoading(false);
    }
  };

  const fetchTags = async () => {
    try {
      setTagsLoading(true);
      const data = await getAllTags();
      setTags(data);
    } finally {
      setTagsLoading(false);
    }
  };

  useEffect(() => {
    fetchNotes();
    fetchTags();
  }, []);

  // ================= CRUD =================
  const handleCreate = async (noteData) => {
    const newNote = await createNote(noteData);
    const updated = [newNote, ...notes];
    setNotes(updated);
    setDisplayedNotes(updated);
    setShowForm(false);
  };

  const handleUpdate = async (noteData) => {
    const updatedNote = await updateNote(editingNote.id, noteData);
    const updated = notes.map(n =>
      n.id === updatedNote.id ? updatedNote : n
    );
    setNotes(updated);
    setDisplayedNotes(updated);
    setShowForm(false);
    setEditingNote(null);
  };

  const handleDelete = async () => {
    await deleteNote(deletingNote.id);
    const updated = notes.filter(n => n.id !== deletingNote.id);
    setNotes(updated);
    setDisplayedNotes(updated);
    setDeletingNote(null);
  };

  // ================= SEARCH =================
  const handleSearch = async (query) => {
    setSearchQuery(query);

    if (!query.trim()) {
      setDisplayedNotes(notes);
      return;
    }

    const results = await searchNotes(query);
    setDisplayedNotes(results);
  };

  // ================= TAG FILTER =================
  const handleTagFilter = async (tagId) => {
    setActiveTagFilter(tagId);

    if (tagId === null) {
      setDisplayedNotes(notes);
      return;
    }

    const results = await filterNotesByTag(tagId);
    setDisplayedNotes(results);
  };

  // ================= RENDER =================
  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-indigo-50 to-purple-50">

      {/* 🔥 NAVBAR ALWAYS VISIBLE */}
      <Navbar />

      <div className="max-w-7xl mx-auto px-4 py-8">

        {/* 🔄 LOADING */}
        {loading || tagsLoading ? (
          <div className="flex justify-center items-center h-64">
            <div className="animate-spin h-12 w-12 border-4 border-indigo-600 border-t-transparent rounded-full"></div>
          </div>

        ) : error ? (

          /* ❌ ERROR */
          <div className="text-center text-red-600">
            {error}
          </div>

        ) : (

          /* ✅ MAIN CONTENT */
          <>
            {/* Header */}
            <div className="flex justify-between items-center mb-6">
              <h1 className="text-3xl font-bold text-indigo-800">ReNoted</h1>

              <button
                onClick={() => {
                  setEditingNote(null);
                  setShowForm(true);
                }}
                className="bg-indigo-600 text-white px-4 py-2 rounded-lg"
              >
                + New Note
              </button>
            </div>

            {/* Search */}
            <SearchBar onSearch={handleSearch} />

            {/* Tag Filter */}
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
              onCreateTag={async (name) => {
                const newTag = await createTag({ name });
                setTags([...tags, newTag]);
              }}
              onDeleteTag={async (id) => {
                await deleteTag(id);
                setTags(tags.filter(t => t.id !== id));
              }}
              onRefresh={fetchTags}
            />

            {/* Form */}
            {showForm && (
              <NoteForm
                note={editingNote}
                onSubmit={editingNote ? handleUpdate : handleCreate}
                onCancel={() => setShowForm(false)}
                availableTags={tags}
              />
            )}

            {/* Notes */}
            {displayedNotes.length === 0 ? (
              <p className="text-center mt-10 text-gray-500">
                No notes found
              </p>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mt-6">
                {displayedNotes.map(note => (
                  <NoteCard
                    key={note.id}
                    note={note}
                    availableTags={tags}
                    onEdit={(n) => {
                      setEditingNote(n);
                      setShowForm(true);
                    }}
                    onDelete={(n) => setDeletingNote(n)}
                    onViewNote={(n) => {
                      setViewNote(n);
                      setIsModalOpen(true);
                    }}
                    onRemoveTag={removeTagFromNote}
                  />
                ))}
              </div>
            )}

            {/* Modals */}
            {deletingNote && (
              <DeleteModal
                note={deletingNote}
                onConfirm={handleDelete}
                onCancel={() => setDeletingNote(null)}
              />
            )}

            <NoteViewModal
              note={viewNote}
              isOpen={isModalOpen}
              onClose={() => setIsModalOpen(false)}
            />
          </>
        )}
      </div>
    </div>
  );
};

export default NotesPage;