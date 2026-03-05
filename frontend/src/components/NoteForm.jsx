// NoteForm.jsx - Note Creation/Edit Form Component
//
// Purpose: Form for creating new notes or editing existing ones

import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';

function NoteForm({ note, onSave, onCancel }) {
  // Form state
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Populate form when editing existing note
  useEffect(() => {
    if (note) {
      setTitle(note.title || '');
      setContent(note.content || '');
    }
  }, [note]);

  /**
   * Validate form inputs
   */
  const validate = () => {
    const newErrors = {};

    // Title is required
    if (!title.trim()) {
      newErrors.title = 'Title is required';
    } else if (title.trim().length > 255) {
      newErrors.title = 'Title must be less than 255 characters';
    }

    // Content is optional, but check length if provided
    if (content && content.length > 10000) {
      newErrors.content = 'Content is too long';
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
      // Prepare data
      const noteData = {
        title: title.trim(),
        content: content.trim() || ''
      };

      // Call parent's save function
      await onSave(noteData);

      // Reset form (only if creating new note, not editing)
      if (!note) {
        setTitle('');
        setContent('');
      }
    } catch (error) {
      console.error('Error saving note:', error);
      setErrors({ submit: 'Failed to save note. Please try again.' });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-lg p-6 mb-8">
      {/* Form Header */}
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-gray-800">
          {note ? 'Edit Note' : 'Create New Note'}
        </h2>
        <button
          onClick={onCancel}
          className="text-gray-500 hover:text-gray-700 text-2xl"
          aria-label="Close"
        >
          ×
        </button>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit}>
        {/* Title Field */}
        <div className="mb-4">
          <label htmlFor="title" className="block text-sm font-semibold text-gray-700 mb-2">
            Title <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            id="title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className={`w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 ${
              errors.title ? 'border-red-500' : 'border-gray-300'
            }`}
            placeholder="Enter note title..."
            maxLength={255}
          />
          {errors.title && (
            <p className="text-red-500 text-sm mt-1">{errors.title}</p>
          )}
          <p className="text-gray-500 text-sm mt-1">
            {title.length}/255 characters
          </p>
        </div>

        {/* Content Field */}
        <div className="mb-6">
          <label htmlFor="content" className="block text-sm font-semibold text-gray-700 mb-2">
            Content
          </label>
          <textarea
            id="content"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            className={`w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 min-h-[200px] resize-y ${
              errors.content ? 'border-red-500' : 'border-gray-300'
            }`}
            placeholder="Enter note content..."
            rows={8}
          />
          {errors.content && (
            <p className="text-red-500 text-sm mt-1">{errors.content}</p>
          )}
        </div>

        {/* Submit Error */}
        {errors.submit && (
          <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg">
            <p className="text-red-600 text-sm">{errors.submit}</p>
          </div>
        )}

        {/* Action Buttons */}
        <div className="flex gap-3">
          <button
            type="submit"
            disabled={isSubmitting}
            className={`flex-1 bg-indigo-600 text-white px-6 py-3 rounded-lg font-semibold transition ${
              isSubmitting 
                ? 'opacity-50 cursor-not-allowed' 
                : 'hover:bg-indigo-700'
            }`}
          >
            {isSubmitting ? 'Saving...' : note ? 'Update Note' : 'Create Note'}
          </button>
          <button
            type="button"
            onClick={onCancel}
            className="flex-1 bg-gray-300 text-gray-700 px-6 py-3 rounded-lg hover:bg-gray-400 transition font-semibold"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}

NoteForm.propTypes = {
  note: PropTypes.shape({
    id: PropTypes.number,
    title: PropTypes.string,
    content: PropTypes.string,
  }),
  onSave: PropTypes.func.isRequired,
  onCancel: PropTypes.func.isRequired,
};

export default NoteForm;