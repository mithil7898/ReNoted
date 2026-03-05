// DeleteModal.jsx - Delete Confirmation Modal
//
// Purpose: Confirm before deleting a note

import PropTypes from 'prop-types';

function DeleteModal({ note, onConfirm, onCancel }) {
  return (
    // Modal Overlay (dark background)
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      {/* Modal Content */}
      <div className="bg-white rounded-lg shadow-2xl max-w-md w-full p-6 animate-fade-in">
        {/* Header */}
        <div className="flex items-center gap-3 mb-4">
          <div className="text-4xl">⚠️</div>
          <h2 className="text-2xl font-bold text-gray-800">Delete Note?</h2>
        </div>

        {/* Message */}
        <p className="text-gray-600 mb-2">
          Are you sure you want to delete this note? This action cannot be undone.
        </p>

        {/* Note Preview */}
        <div className="bg-gray-50 rounded-lg p-4 mb-6 border border-gray-200">
          <h3 className="font-semibold text-gray-800 mb-2">{note.title}</h3>
          {note.content && (
            <p className="text-gray-600 text-sm line-clamp-3">
              {note.content}
            </p>
          )}
        </div>

        {/* Action Buttons */}
        <div className="flex gap-3">
          <button
            onClick={onCancel}
            className="flex-1 bg-gray-300 text-gray-700 px-6 py-3 rounded-lg hover:bg-gray-400 transition font-semibold"
          >
            Cancel
          </button>
          <button
            onClick={onConfirm}
            className="flex-1 bg-red-600 text-white px-6 py-3 rounded-lg hover:bg-red-700 transition font-semibold"
          >
            Delete
          </button>
        </div>
      </div>
    </div>
  );
}

DeleteModal.propTypes = {
  note: PropTypes.shape({
    id: PropTypes.number.isRequired,
    title: PropTypes.string.isRequired,
    content: PropTypes.string,
  }).isRequired,
  onConfirm: PropTypes.func.isRequired,
  onCancel: PropTypes.func.isRequired,
};

export default DeleteModal;