/**
 * TagFilter Component
 * 
 * Purpose: Display clickable tag filter buttons
 * 
 * Features:
 * - Show all available tags as filter buttons
 * - Highlight active filter
 * - "All" button to clear filter
 * - Show note count per tag (optional)
 * 
 * Props:
 * - tags: Array of all available tags
 * - activeTagId: Currently active tag filter (null for "All")
 * - onFilterChange: Callback when filter changes
 * - notes: Array of all notes (to calculate counts)
 */

import React from 'react';
import PropTypes from 'prop-types';

const TagFilter = ({ tags, activeTagId, onFilterChange, notes = [] }) => {
  /**
   * Count notes for each tag
   */
  const getTagCount = (tagId) => {
    return notes.filter(note => 
      note.tagIds && note.tagIds.includes(tagId)
    ).length;
  };

  /**
   * Get total notes count
   */
  const totalNotesCount = notes.length;

  /**
   * Generate color for tag button
   */
  const getTagColor = (name, isActive) => {
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
      hash = name.charCodeAt(i) + ((hash << 5) - hash);
    }
    const hue = Math.abs(hash % 360);
    
    if (isActive) {
      return {
        background: `hsl(${hue}, 70%, 50%)`,
        color: 'white',
        border: `hsl(${hue}, 70%, 40%)`,
      };
    } else {
      return {
        background: `hsl(${hue}, 70%, 95%)`,
        color: `hsl(${hue}, 70%, 30%)`,
        border: `hsl(${hue}, 70%, 80%)`,
      };
    }
  };

  return (
    <div className="tag-filter mb-6">
      {/* Label */}
      <h3 className="text-sm font-medium text-gray-700 mb-3">
        Filter by tag:
      </h3>

      {/* Filter buttons */}
      <div className="flex flex-wrap gap-2">
        {/* "All" button */}
        <button
          onClick={() => onFilterChange(null)}
          className={`px-4 py-2 rounded-full text-sm font-medium transition-all ${
            activeTagId === null
              ? 'bg-indigo-600 text-white shadow-md'
              : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
          }`}
        >
          All {totalNotesCount > 0 && `(${totalNotesCount})`}
        </button>

        {/* Tag filter buttons */}
        {tags.map(tag => {
          const count = getTagCount(tag.id);
          const isActive = activeTagId === tag.id;
          const colors = getTagColor(tag.name, isActive);

          return (
            <button
              key={tag.id}
              onClick={() => onFilterChange(tag.id)}
              style={{
                backgroundColor: colors.background,
                color: colors.color,
                borderWidth: '1px',
                borderStyle: 'solid',
                borderColor: colors.border,
              }}
              className={`px-4 py-2 rounded-full text-sm font-medium transition-all ${
                isActive ? 'shadow-md' : 'hover:shadow-sm'
              }`}
            >
              {tag.name} {count > 0 && `(${count})`}
            </button>
          );
        })}
      </div>

      {/* Active filter indicator */}
      {activeTagId !== null && (
        <p className="mt-3 text-sm text-gray-600">
          Showing notes with tag:{' '}
          <span className="font-medium">
            {tags.find(t => t.id === activeTagId)?.name}
          </span>
          <button
            onClick={() => onFilterChange(null)}
            className="ml-2 text-indigo-600 hover:text-indigo-700 underline"
          >
            Clear filter
          </button>
        </p>
      )}
    </div>
  );
};

TagFilter.propTypes = {
  tags: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.number.isRequired,
      name: PropTypes.string.isRequired,
    })
  ).isRequired,
  activeTagId: PropTypes.number,
  onFilterChange: PropTypes.func.isRequired,
  notes: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.number.isRequired,
      tagIds: PropTypes.arrayOf(PropTypes.number),
    })
  ),
};

export default TagFilter;