// NoteCard.jsx - Individual Note Card Component
//
// Purpose: Display a single note with edit/delete actions

import PropTypes from 'prop-types';

function NoteCard({ note, onEdit, onDelete }) {
  // Format date for display
  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  // Truncate content if too long
  const truncateContent = (content, maxLength = 150) => {
    if (!content) return 'No content';
    if (content.length <= maxLength) return content;
    return content.substring(0, maxLength) + '...';
  };

  return (
    <div className="bg-white rounded-lg shadow-md hover:shadow-xl transition-shadow p-6">
      {/* Note Title */}
      <h3 className="text-xl font-bold text-gray-800 mb-3 line-clamp-2">
        {note.title}
      </h3>

      {/* Note Content */}
      <p className="text-gray-600 mb-4 whitespace-pre-wrap">
        {truncateContent(note.content)}
      </p>

      {/* Timestamps */}
      <div className="text-sm text-gray-500 mb-4">
        <p>Created: {formatDate(note.createdAt)}</p>
        {note.updatedAt !== note.createdAt && (
          <p>Updated: {formatDate(note.updatedAt)}</p>
        )}
      </div>

      {/* Action Buttons */}
      <div className="flex gap-2">
        <button
          onClick={onEdit}
          className="flex-1 bg-indigo-600 text-white px-4 py-2 rounded-lg hover:bg-indigo-700 transition font-semibold"
        >
          Edit
        </button>
        <button
          onClick={onDelete}
          className="flex-1 bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 transition font-semibold"
        >
          Delete
        </button>
      </div>
    </div>
  );
}

NoteCard.propTypes = {
  note: PropTypes.shape({
    id: PropTypes.number.isRequired,
    title: PropTypes.string.isRequired,
    content: PropTypes.string,
    createdAt: PropTypes.string.isRequired,
    updatedAt: PropTypes.string.isRequired,
  }).isRequired,
  onEdit: PropTypes.func.isRequired,
  onDelete: PropTypes.func.isRequired,
};

export default NoteCard;