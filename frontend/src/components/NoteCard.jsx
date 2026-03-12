/**
 * NoteCard Component (v0.3 - WITH TAGS)
 * 
 * Purpose: Display individual note with tags
 * 
 * Props:
 * - note: Note object with tagIds
 * - availableTags: Array of all available tags (to display tag names)
 * - onEdit: Callback when edit button is clicked
 * - onDelete: Callback when delete button is clicked
 * - onRemoveTag: Callback when tag remove button is clicked
 */

import React from 'react';
import PropTypes from 'prop-types';
import TagPill from './TagPill';

const NoteCard = ({ note, availableTags, onEdit, onDelete, onRemoveTag }) => {
  /**
   * Format timestamp to readable string
   */
  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  /**
   * Truncate content to max length
   */
  const truncateContent = (text, maxLength = 150) => {
    if (!text) return '';
    if (text.length <= maxLength) return text;
    return text.substring(0, maxLength) + '...';
  };

  /**
   * Get tag objects from tag IDs
   */
  const noteTags = (note.tagIds || [])
    .map(tagId => availableTags.find(tag => tag.id === tagId))
    .filter(tag => tag !== undefined);  // Filter out any not found tags

  return (
    <div className="note-card bg-white rounded-lg shadow-md p-5 hover:shadow-lg transition-shadow">
      {/* Title */}
      <h3 className="text-xl font-bold text-gray-800 mb-2">
        {note.title}
      </h3>

      {/* Content */}
      <p className="text-gray-600 mb-4 whitespace-pre-wrap">
        {truncateContent(note.content)}
      </p>

      {/* Tags */}
      {noteTags.length > 0 && (
        <div className="mb-4">
          {noteTags.map(tag => (
            <TagPill
              key={tag.id}
              tag={tag}
              removable={!!onRemoveTag}  // Show remove button if callback provided
              onRemove={() => onRemoveTag && onRemoveTag(note.id, tag.id)}
            />
          ))}
        </div>
      )}

      {/* Timestamps */}
      <div className="text-sm text-gray-500 mb-4">
        <p>Created: {formatDate(note.createdAt)}</p>
        <p>Updated: {formatDate(note.updatedAt)}</p>
      </div>

      {/* Action Buttons */}
      <div className="flex gap-3">
        <button
          onClick={() => onEdit(note)}
          className="flex-1 bg-indigo-600 text-white px-4 py-2 rounded-md hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-colors"
        >
          Edit
        </button>

        <button
          onClick={() => onDelete(note)}
          className="flex-1 bg-red-600 text-white px-4 py-2 rounded-md hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 transition-colors"
        >
          Delete
        </button>
      </div>
    </div>
  );
};

NoteCard.propTypes = {
  note: PropTypes.shape({
    id: PropTypes.number.isRequired,
    title: PropTypes.string.isRequired,
    content: PropTypes.string,
    tagIds: PropTypes.arrayOf(PropTypes.number),
    createdAt: PropTypes.string.isRequired,
    updatedAt: PropTypes.string.isRequired,
  }).isRequired,
  availableTags: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.number.isRequired,
      name: PropTypes.string.isRequired,
    })
  ).isRequired,
  onEdit: PropTypes.func.isRequired,
  onDelete: PropTypes.func.isRequired,
  onRemoveTag: PropTypes.func,
};

export default NoteCard;