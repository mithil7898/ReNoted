/**
 * TagSelector Component
 * 
 * Purpose: Multi-select dropdown for choosing tags
 * 
 * Features:
 * - Shows all available tags
 * - Allows selecting multiple tags
 * - Shows selected tags as pills
 * - Option to create new tags
 * 
 * Props:
 * - selectedTagIds: Array of selected tag IDs
 * - availableTags: Array of all available tags
 * - onChange: Callback when selection changes
 * - onCreateTag: Optional callback to create new tag
 */

import React, { useState } from 'react';
import PropTypes from 'prop-types';
import TagPill from './TagPill';

const TagSelector = ({ selectedTagIds, availableTags, onChange, onCreateTag }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [newTagName, setNewTagName] = useState('');
  const [showCreateInput, setShowCreateInput] = useState(false);

  /**
   * Toggle tag selection
   */
  const handleToggleTag = (tagId) => {
    if (selectedTagIds.includes(tagId)) {
      // Remove tag
      onChange(selectedTagIds.filter(id => id !== tagId));
    } else {
      // Add tag
      onChange([...selectedTagIds, tagId]);
    }
  };

  /**
   * Remove tag from selection
   */
  const handleRemoveTag = (tagId) => {
    onChange(selectedTagIds.filter(id => id !== tagId));
  };

  /**
   * Create new tag
   */
  const handleCreateTag = async () => {
    if (newTagName.trim() && onCreateTag) {
      await onCreateTag(newTagName.trim());
      setNewTagName('');
      setShowCreateInput(false);
    }
  };

  // Get selected tag objects
  const selectedTags = availableTags.filter(tag => 
    selectedTagIds.includes(tag.id)
  );

  return (
    <div className="tag-selector">
      {/* Label */}
      <label className="block text-sm font-medium text-gray-700 mb-2">
        Tags
      </label>

      {/* Selected tags display */}
      <div className="mb-2 min-h-[32px]">
        {selectedTags.length > 0 ? (
          selectedTags.map(tag => (
            <TagPill
              key={tag.id}
              tag={tag}
              removable
              onRemove={handleRemoveTag}
            />
          ))
        ) : (
          <p className="text-sm text-gray-400">No tags selected</p>
        )}
      </div>

      {/* Dropdown button */}
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm text-sm text-left bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-indigo-500"
      >
        {isOpen ? 'Close tag selector' : 'Select tags'}
      </button>

      {/* Dropdown menu */}
      {isOpen && (
        <div className="mt-2 border border-gray-200 rounded-md bg-white shadow-lg max-h-60 overflow-y-auto">
          {/* Available tags */}
          {availableTags.length > 0 ? (
            <div className="p-2">
              {availableTags.map(tag => {
                const isSelected = selectedTagIds.includes(tag.id);
                return (
                  <div
                    key={tag.id}
                    onClick={() => handleToggleTag(tag.id)}
                    className={`px-3 py-2 rounded cursor-pointer flex items-center justify-between ${
                      isSelected
                        ? 'bg-indigo-50 hover:bg-indigo-100'
                        : 'hover:bg-gray-50'
                    }`}
                  >
                    <span className="text-sm">{tag.name}</span>
                    {isSelected && (
                      <span className="text-indigo-600">✓</span>
                    )}
                  </div>
                );
              })}
            </div>
          ) : (
            <p className="p-4 text-sm text-gray-400 text-center">
              No tags available
            </p>
          )}

          {/* Create new tag section */}
          {onCreateTag && (
            <div className="border-t border-gray-200 p-2">
              {!showCreateInput ? (
                <button
                  type="button"
                  onClick={() => setShowCreateInput(true)}
                  className="w-full px-3 py-2 text-sm text-indigo-600 hover:bg-indigo-50 rounded"
                >
                  + Create new tag
                </button>
              ) : (
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={newTagName}
                    onChange={(e) => setNewTagName(e.target.value)}
                    onKeyPress={(e) => e.key === 'Enter' && handleCreateTag()}
                    placeholder="Tag name"
                    className="flex-1 px-2 py-1 text-sm border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-indigo-500"
                    autoFocus
                  />
                  <button
                    type="button"
                    onClick={handleCreateTag}
                    className="px-3 py-1 text-sm bg-indigo-600 text-white rounded hover:bg-indigo-700"
                  >
                    Add
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setShowCreateInput(false);
                      setNewTagName('');
                    }}
                    className="px-2 py-1 text-sm text-gray-600 hover:bg-gray-100 rounded"
                  >
                    ✕
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

TagSelector.propTypes = {
  selectedTagIds: PropTypes.arrayOf(PropTypes.number).isRequired,
  availableTags: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.number.isRequired,
      name: PropTypes.string.isRequired,
    })
  ).isRequired,
  onChange: PropTypes.func.isRequired,
  onCreateTag: PropTypes.func,
};

export default TagSelector;