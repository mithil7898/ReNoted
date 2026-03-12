/**
 * NoteForm Component (v0.3 - WITH TAGS)
 * 
 * Purpose: Form for creating and editing notes with tag support
 * 
 * Props:
 * - note: Existing note object (for edit mode) or null (for create mode)
 * - onSubmit: Callback when form is submitted
 * - onCancel: Callback when cancel is clicked
 * - availableTags: Array of all available tags
 * - onCreateTag: Callback to create new tag
 */

import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import TagSelector from './TagSelector';

const NoteForm = ({ note, onSubmit, onCancel, availableTags, onCreateTag }) => {
  // Form state
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [selectedTagIds, setSelectedTagIds] = useState([]);
  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  /**
   * Populate form when editing existing note
   */
  useEffect(() => {
    if (note) {
      setTitle(note.title || '');
      setContent(note.content || '');
      setSelectedTagIds(note.tagIds || []);
    } else {
      // Reset form for create mode
      setTitle('');
      setContent('');
      setSelectedTagIds([]);
    }
    setErrors({});
  }, [note]);

  /**
   * Validate form
   */
  const validate = () => {
    const newErrors = {};

    // Title validation
    if (!title.trim()) {
      newErrors.title = 'Title is required';
    } else if (title.length > 255) {
      newErrors.title = 'Title must be less than 255 characters';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  /**
   * Handle form submission
   */
  const handleSubmit = async (e) => {
    e.preventDefault();

    // Validate
    if (!validate()) {
      return;
    }

    setIsSubmitting(true);

    try {
      // Prepare note data
      const noteData = {
        title: title.trim(),
        content: content.trim(),
        tagIds: selectedTagIds,  // Include selected tag IDs
      };

      // Call parent's submit handler
      await onSubmit(noteData);

      // Reset form after successful submit (only for create mode)
      if (!note) {
        setTitle('');
        setContent('');
        setSelectedTagIds([]);
      }
    } catch (error) {
      console.error('Error submitting note:', error);
      setErrors({ submit: 'Failed to save note. Please try again.' });
    } finally {
      setIsSubmitting(false);
    }
  };

  /**
   * Handle tag selection change
   */
  const handleTagsChange = (newTagIds) => {
    setSelectedTagIds(newTagIds);
  };

  /**
   * Character count for title
   */
  const titleCharCount = title.length;
  const titleCharLimit = 255;
  const titleCharsRemaining = titleCharLimit - titleCharCount;

  return (
    <div className="note-form bg-white p-6 rounded-lg shadow-md mb-6">
      <h2 className="text-xl font-bold mb-4 text-gray-800">
        {note ? 'Edit Note' : 'Create New Note'}
      </h2>

      <form onSubmit={handleSubmit}>
        {/* Title Input */}
        <div className="mb-4">
          <label
            htmlFor="title"
            className="block text-sm font-medium text-gray-700 mb-2"
          >
            Title *
          </label>
          <input
            type="text"
            id="title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 ${
              errors.title ? 'border-red-500' : 'border-gray-300'
            }`}
            placeholder="Enter note title"
            maxLength={titleCharLimit}
          />

          {/* Character counter */}
          <div className="flex justify-between mt-1">
            <div>
              {errors.title && (
                <p className="text-red-500 text-sm">{errors.title}</p>
              )}
            </div>
            <p
              className={`text-sm ${
                titleCharsRemaining < 20 ? 'text-red-500' : 'text-gray-500'
              }`}
            >
              {titleCharsRemaining} characters remaining
            </p>
          </div>
        </div>

        {/* Content Textarea */}
        <div className="mb-4">
          <label
            htmlFor="content"
            className="block text-sm font-medium text-gray-700 mb-2"
          >
            Content
          </label>
          <textarea
            id="content"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            placeholder="Enter note content (optional)"
            rows={6}
          />
        </div>

        {/* Tag Selector */}
        <div className="mb-4">
          <TagSelector
            selectedTagIds={selectedTagIds}
            availableTags={availableTags}
            onChange={handleTagsChange}
            onCreateTag={onCreateTag}
          />
        </div>

        {/* Submit Error */}
        {errors.submit && (
          <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-md">
            <p className="text-red-600 text-sm">{errors.submit}</p>
          </div>
        )}

        {/* Action Buttons */}
        <div className="flex gap-3">
          <button
            type="submit"
            disabled={isSubmitting}
            className="flex-1 bg-indigo-600 text-white px-4 py-2 rounded-md hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors"
          >
            {isSubmitting
              ? 'Saving...'
              : note
              ? 'Update Note'
              : 'Create Note'}
          </button>

          <button
            type="button"
            onClick={onCancel}
            disabled={isSubmitting}
            className="flex-1 bg-gray-200 text-gray-700 px-4 py-2 rounded-md hover:bg-gray-300 focus:outline-none focus:ring-2 focus:ring-gray-400 disabled:cursor-not-allowed transition-colors"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
};

NoteForm.propTypes = {
  note: PropTypes.shape({
    id: PropTypes.number,
    title: PropTypes.string,
    content: PropTypes.string,
    tagIds: PropTypes.arrayOf(PropTypes.number),
    createdAt: PropTypes.string,
    updatedAt: PropTypes.string,
  }),
  onSubmit: PropTypes.func.isRequired,
  onCancel: PropTypes.func.isRequired,
  availableTags: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.number.isRequired,
      name: PropTypes.string.isRequired,
    })
  ).isRequired,
  onCreateTag: PropTypes.func,
};

export default NoteForm;