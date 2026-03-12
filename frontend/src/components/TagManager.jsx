/**
 * TagManager Component
 * 
 * Purpose: Manage tags (view, create, delete)
 * 
 * Features:
 * - List all tags
 * - Create new tag
 * - Delete tag (with confirmation)
 * - Show note count per tag
 * 
 * Props:
 * - tags: Array of all tags
 * - notes: Array of all notes (to show counts)
 * - onCreateTag: Callback to create tag
 * - onDeleteTag: Callback to delete tag
 * - onRefresh: Callback to refresh tags list
 */

import React, { useState } from 'react';
import PropTypes from 'prop-types';
import TagPill from './TagPill';

const TagManager = ({ tags, notes, onCreateTag, onDeleteTag, onRefresh }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [newTagName, setNewTagName] = useState('');
  const [isCreating, setIsCreating] = useState(false);
  const [deletingTagId, setDeletingTagId] = useState(null);

  /**
   * Count notes using this tag
   */
  const getTagCount = (tagId) => {
    return notes.filter(note => 
      note.tagIds && note.tagIds.includes(tagId)
    ).length;
  };

  /**
   * Handle create tag
   */
  const handleCreateTag = async (e) => {
    e.preventDefault();
    
    if (!newTagName.trim()) {
      alert('Please enter a tag name');
      return;
    }

    setIsCreating(true);
    try {
      await onCreateTag(newTagName.trim());
      setNewTagName('');
      onRefresh();  // Refresh tags list
    } catch (err) {
      console.error('Error creating tag:', err);
    } finally {
      setIsCreating(false);
    }
  };

  /**
   * Handle delete tag
   * 
   * New behavior:
   * - Shows warning with note count
   * - Lists affected notes
   * - User confirms
   * - Tag deleted from all notes automatically
   * - Notes remain intact
   */
  const handleDeleteTag = async (tagId) => {
    const tag = tags.find(t => t.id === tagId);
    const noteCount = getTagCount(tagId);

    // Prepare confirmation message
    let confirmMessage = `Delete tag "${tag.name}"?\n\n`;
    
    if (noteCount > 0) {
      // Tag is in use - show warning
      const notesWithTag = notes.filter(note => 
        note.tagIds && note.tagIds.includes(tagId)
      );
      const notesList = notesWithTag
        .map(n => `  • ${n.title}`)
        .slice(0, 5)  // Show first 5 notes
        .join('\n');
      
      confirmMessage += `⚠️ This tag is used by ${noteCount} note(s):\n\n`;
      confirmMessage += notesList;
      
      if (noteCount > 5) {
        confirmMessage += `\n  ... and ${noteCount - 5} more`;
      }
      
      confirmMessage += '\n\n';
      confirmMessage += '✅ The tag will be removed from all notes.\n';
      confirmMessage += '✅ Your notes will NOT be deleted.\n';
      confirmMessage += '\nThis action cannot be undone.\n\n';
      confirmMessage += 'Continue?';
    } else {
      // Tag not in use - simple confirmation
      confirmMessage += 'This action cannot be undone.';
    }

    // Confirm deletion
    const confirmDelete = window.confirm(confirmMessage);

    if (!confirmDelete) return;

    // Delete tag
    setDeletingTagId(tagId);
    try {
      await onDeleteTag(tagId);
      onRefresh();  // Refresh tags list
      
      // Show success message
      if (noteCount > 0) {
        alert(`✅ Tag "${tag.name}" deleted successfully!\n\nIt has been removed from ${noteCount} note(s).`);
      }
    } catch (err) {
      console.error('Error deleting tag:', err);
      alert('❌ Failed to delete tag. Please try again.');
    } finally {
      setDeletingTagId(null);
    }
  };

  if (!isOpen) {
    return (
      <div className="mb-6">
        <button
          onClick={() => setIsOpen(true)}
          className="text-indigo-600 hover:text-indigo-700 text-sm font-medium flex items-center gap-2"
        >
          <span>🏷️</span>
          Manage Tags ({tags.length})
        </button>
      </div>
    );
  }

  return (
    <div className="mb-6 bg-white rounded-lg shadow-md p-6">
      {/* Header */}
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-xl font-bold text-gray-800">
          Tag Management
        </h2>
        <button
          onClick={() => setIsOpen(false)}
          className="text-gray-400 hover:text-gray-600 text-xl"
        >
          ✕
        </button>
      </div>

      {/* Create Tag Form */}
      <form onSubmit={handleCreateTag} className="mb-6">
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Create New Tag
        </label>
        <div className="flex gap-2">
          <input
            type="text"
            value={newTagName}
            onChange={(e) => setNewTagName(e.target.value)}
            placeholder="Tag name (e.g., 'Machine Learning')"
            className="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-indigo-500"
            disabled={isCreating}
          />
          <button
            type="submit"
            disabled={isCreating}
            className="bg-indigo-600 text-white px-6 py-2 rounded-md hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:bg-gray-400"
          >
            {isCreating ? 'Creating...' : 'Create'}
          </button>
        </div>
        <p className="text-sm text-gray-500 mt-1">
          Tag names are automatically formatted to Title Case
        </p>
      </form>

      {/* Tags List */}
      <div>
        <h3 className="text-sm font-medium text-gray-700 mb-3">
          All Tags ({tags.length})
        </h3>

        {tags.length === 0 ? (
          <p className="text-gray-400 text-center py-8">
            No tags yet. Create your first tag above!
          </p>
        ) : (
          <div className="space-y-2">
            {tags.map(tag => {
              const noteCount = getTagCount(tag.id);
              const isDeleting = deletingTagId === tag.id;

              return (
                <div
                  key={tag.id}
                  className="flex items-center justify-between p-3 border border-gray-200 rounded-md hover:bg-gray-50"
                >
                  {/* Tag info */}
                  <div className="flex items-center gap-3">
                    <TagPill tag={tag} />
                    <span className="text-sm text-gray-600">
                      {noteCount === 0 ? (
                        <span className="text-gray-400">Not used</span>
                      ) : (
                        <span>
                          Used by {noteCount} note{noteCount !== 1 ? 's' : ''}
                        </span>
                      )}
                    </span>
                  </div>

                  {/* Delete button */}
                  <button
                    onClick={() => handleDeleteTag(tag.id)}
                    disabled={isDeleting}
                    className="px-4 py-1 rounded text-sm bg-red-50 text-red-600 hover:bg-red-100 disabled:bg-gray-100 disabled:text-gray-400"
                    title={
                        noteCount > 0
                        ? `Delete tag (will be removed from ${noteCount} note${noteCount !== 1 ? 's' : ''})`
                        : 'Delete tag'
                    }
                    >
                    {isDeleting ? 'Deleting...' : 'Delete'}
                    </button>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Info Box */}
      <div className="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-md">
        <p className="text-sm text-blue-800">
            <strong>💡 How tag deletion works:</strong>
        </p>
        <ul className="text-sm text-blue-800 mt-2 ml-4 space-y-1">
            <li>✅ Deleting a tag removes it from all notes automatically</li>
            <li>✅ Your notes remain intact and will not be deleted</li>
            <li>✅ You'll see a warning before deletion with affected notes</li>
        </ul>
        </div>
    </div>
  );
};

TagManager.propTypes = {
  tags: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.number.isRequired,
      name: PropTypes.string.isRequired,
    })
  ).isRequired,
  notes: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.number.isRequired,
      tagIds: PropTypes.arrayOf(PropTypes.number),
    })
  ).isRequired,
  onCreateTag: PropTypes.func.isRequired,
  onDeleteTag: PropTypes.func.isRequired,
  onRefresh: PropTypes.func.isRequired,
};

export default TagManager;