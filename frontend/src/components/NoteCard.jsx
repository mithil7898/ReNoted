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

const NoteCard = ({ note, availableTags, onEdit, onDelete, onRemoveTag, onViewNote }) => {
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
   * Truncate HTML content for preview
   */
  const truncateHTML = (html, maxLength = 200) => {
    if (!html) return '';
    
    // Strip HTML tags to get plain text for length check
    const div = document.createElement('div');
    div.innerHTML = html;
    const text = div.textContent || div.innerText || '';
    
    // If short enough, return original HTML
    if (text.length <= maxLength) {
      return html;
    }
    
    // Otherwise, truncate the text and wrap in paragraph
    const truncated = text.substring(0, maxLength).trim() + '...';
    return `<p>${truncated}</p>`;
  };

  /**
   * Get tag objects from tag IDs
   */
  const noteTags = (note.tagIds || [])
    .map(tagId => availableTags.find(tag => tag.id === tagId))
    .filter(tag => tag !== undefined);  // Filter out any not found tags

  return (
      <div 
      className="note-card bg-white rounded-lg shadow-md p-5 hover:shadow-lg transition-shadow cursor-pointer"
      onClick={() => onViewNote(note)}
      > 
      {/* Title */}
      <h3 className="text-xl font-bold text-gray-800 mb-2">
        {note.title}
      </h3>

      {/* Content - Rich Text Preview */}
      {note.content && (
        <div 
          className="note-content-preview text-gray-600 mb-4"
          dangerouslySetInnerHTML={{ 
            __html: truncateHTML(note.content, 200) 
          }}
        />
      )}

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
          onClick={(e) => {
            e.stopPropagation();
            onEdit(note);
          }}
          className="flex-1 bg-indigo-600 text-white px-4 py-2 rounded-md hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-colors"
        >
          Edit
        </button>

        <button
          onClick={(e) => {
            e.stopPropagation();
            onDelete(note);
          }}
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
  onViewNote: PropTypes.func.isRequired,  // ← ADD THIS
};

export default NoteCard;