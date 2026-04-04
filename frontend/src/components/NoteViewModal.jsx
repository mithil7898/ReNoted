/**
 * NoteViewModal Component
 * 
 * Purpose: Display note in full detail in a floating modal
 */

import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
import './NoteViewModal.css';

const NoteViewModal = ({ note, isOpen, onClose }) => {
  /**
   * Handle Escape key press
   */
  useEffect(() => {
    const handleEscape = (e) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };

    document.addEventListener('keydown', handleEscape);
    return () => document.removeEventListener('keydown', handleEscape);
  }, [isOpen, onClose]);

  /**
   * Prevent body scroll when modal is open
   */
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = 'unset';
    }

    return () => {
      document.body.style.overflow = 'unset';
    };
  }, [isOpen]);

  /**
   * Format timestamp
   */
  const formatDate = (dateString) => {
    if (!dateString) return 'Unknown';
    
    try {
      const date = new Date(dateString);
      if (isNaN(date.getTime())) return 'Invalid Date';
      
      return date.toLocaleDateString('en-US', {
        month: 'long',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch (error) {
      console.error('Date formatting error:', error);
      return 'Invalid Date';
    }
  };

  // Don't render if not open or no note
  if (!isOpen || !note) return null;

  // Debug log
  console.log('NoteViewModal rendering with note:', note);

  return (
    <>
      {/* Backdrop */}
      <div 
        className="modal-backdrop"
        onClick={onClose}
        aria-label="Close modal"
      />

      {/* Modal */}
      <div className="modal-container">
        <div className="modal-content">
          {/* Close Button */}
          <button
            className="modal-close"
            onClick={onClose}
            aria-label="Close"
            title="Close (Esc)"
          >
            ✕
          </button>

          {/* Title */}
          <h1 className="modal-title">
            {note.title || 'Untitled Note'}
          </h1>

          {/* Metadata */}
          <div className="modal-metadata">
            <span>Created: {formatDate(note.createdAt)}</span>
            <span>•</span>
            <span>Updated: {formatDate(note.updatedAt)}</span>
          </div>

          {/* Divider */}
          <div className="modal-divider"></div>

          {/* Content */}
          {note.content && note.content.trim() !== '' ? (
            <div 
              className="modal-body"
              dangerouslySetInnerHTML={{ __html: note.content }}
            />
          ) : (
            <div className="modal-empty">
              <p>This note has no content yet.</p>
            </div>
          )}
        </div>
      </div>
    </>
  );
};

NoteViewModal.propTypes = {
  note: PropTypes.shape({
    id: PropTypes.number,
    title: PropTypes.string,
    content: PropTypes.string,
    createdAt: PropTypes.string,
    updatedAt: PropTypes.string,
  }),
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
};

export default NoteViewModal;