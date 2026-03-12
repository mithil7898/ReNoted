/**
 * TagPill Component
 * 
 * Purpose: Display a single tag as a colored pill/badge
 * 
 * Props:
 * - tag: Tag object { id, name }
 * - onRemove: Optional callback when X is clicked
 * - removable: Boolean, show remove button or not
 * 
 * Usage:
 * <TagPill tag={{ id: 1, name: "Java" }} />
 * <TagPill tag={tag} onRemove={() => handleRemove(tag.id)} removable />
 */

import React from 'react';
import PropTypes from 'prop-types';

const TagPill = ({ tag, onRemove, removable = false }) => {
  /**
   * Generate consistent color for tag name
   * Same tag name always gets same color
   */
  const getTagColor = (name) => {
    // Generate hash from name
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
      hash = name.charCodeAt(i) + ((hash << 5) - hash);
    }
    
    // Convert to hue (0-360)
    const hue = Math.abs(hash % 360);
    
    // Return HSL color (consistent, readable colors)
    return `hsl(${hue}, 70%, 90%)`;  // Light, pastel colors
  };

  /**
   * Get darker border color for same tag
   */
  const getTagBorderColor = (name) => {
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
      hash = name.charCodeAt(i) + ((hash << 5) - hash);
    }
    const hue = Math.abs(hash % 360);
    return `hsl(${hue}, 70%, 70%)`;  // Darker for border
  };

  const backgroundColor = getTagColor(tag.name);
  const borderColor = getTagBorderColor(tag.name);

  return (
    <span
      className="tag-pill"
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '4px',
        padding: '4px 10px',
        borderRadius: '12px',
        fontSize: '12px',
        fontWeight: '500',
        backgroundColor: backgroundColor,
        border: `1px solid ${borderColor}`,
        color: '#1f2937',
        marginRight: '6px',
        marginBottom: '6px',
        transition: 'all 0.2s',
      }}
    >
      {/* Tag name */}
      <span>{tag.name}</span>

      {/* Remove button (optional) */}
      {removable && onRemove && (
        <button
          onClick={(e) => {
            e.stopPropagation();  // Prevent parent click events
            onRemove(tag.id);
          }}
          style={{
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            padding: '0',
            marginLeft: '2px',
            fontSize: '14px',
            color: '#6b7280',
            display: 'flex',
            alignItems: 'center',
            transition: 'color 0.2s',
          }}
          onMouseEnter={(e) => e.target.style.color = '#ef4444'}
          onMouseLeave={(e) => e.target.style.color = '#6b7280'}
          title="Remove tag"
        >
          ✕
        </button>
      )}
    </span>
  );
};

TagPill.propTypes = {
  tag: PropTypes.shape({
    id: PropTypes.number.isRequired,
    name: PropTypes.string.isRequired,
  }).isRequired,
  onRemove: PropTypes.func,
  removable: PropTypes.bool,
};

export default TagPill;