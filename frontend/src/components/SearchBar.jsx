/**
 * SearchBar Component
 * 
 * Purpose: Search input with live search functionality
 * 
 * Features:
 * - Search as you type (debounced)
 * - Clear button
 * - Loading indicator
 * 
 * Props:
 * - onSearch: Callback when search query changes
 * - placeholder: Placeholder text
 */

import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';

const SearchBar = ({ onSearch, placeholder = 'Search notes...' }) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [isSearching, setIsSearching] = useState(false);

  /**
   * Debounced search effect
   * Waits 300ms after user stops typing before searching
   */
  useEffect(() => {
    // Don't search on initial render
    if (searchQuery === '' && !isSearching) {
      return;
    }

    setIsSearching(true);

    // Debounce: wait 300ms after user stops typing
    const timeoutId = setTimeout(() => {
      onSearch(searchQuery);
      setIsSearching(false);
    }, 300);

    // Cleanup: cancel previous timeout if user keeps typing
    return () => {
      clearTimeout(timeoutId);
    };
  }, [searchQuery]);

  /**
   * Handle search input change
   */
  const handleChange = (e) => {
    setSearchQuery(e.target.value);
  };

  /**
   * Clear search
   */
  const handleClear = () => {
    setSearchQuery('');
    onSearch('');
  };

  return (
    <div className="search-bar relative">
      {/* Search Icon */}
      <div className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400">
        <svg
          className="w-5 h-5"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
          />
        </svg>
      </div>

      {/* Search Input */}
      <input
        type="text"
        value={searchQuery}
        onChange={handleChange}
        placeholder={placeholder}
        className="w-full pl-10 pr-20 py-3 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition-all"
      />

      {/* Right side: Loading indicator or Clear button */}
      <div className="absolute right-3 top-1/2 transform -translate-y-1/2 flex items-center gap-2">
        {/* Loading spinner */}
        {isSearching && (
          <div className="animate-spin rounded-full h-4 w-4 border-2 border-indigo-600 border-t-transparent"></div>
        )}

        {/* Clear button */}
        {searchQuery && (
          <button
            onClick={handleClear}
            className="text-gray-400 hover:text-gray-600 transition-colors"
            title="Clear search"
          >
            <svg
              className="w-5 h-5"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        )}
      </div>
    </div>
  );
};

SearchBar.propTypes = {
  onSearch: PropTypes.func.isRequired,
  placeholder: PropTypes.string,
};

export default SearchBar;