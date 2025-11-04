// src/features/product/components/search/SearchBar.jsx
import React, { useState, useEffect } from 'react';
import { Search, X, ChevronDown } from 'lucide-react';
import { MARKETPLACES, MARKETPLACE_CONFIG, DEFAULT_MARKETPLACE } from '../../../../shared/utils/constants';
import { getMarketplace, setMarketplace as saveMarketplace } from '../../../../shared/utils/storage';

const SearchBar = ({ onSearch, loading, initialKeyword = '', initialMarketplace, onMarketplaceChange }) => {
  const [keyword, setKeyword] = useState(initialKeyword);
  const [marketplace, setMarketplace] = useState(initialMarketplace || getMarketplace(DEFAULT_MARKETPLACE));

  useEffect(() => {
    setKeyword(initialKeyword);
  }, [initialKeyword]);

  useEffect(() => {
    if (initialMarketplace) {
      setMarketplace(initialMarketplace);
    }
  }, [initialMarketplace]);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (keyword.trim()) {
      onSearch(keyword.trim(), marketplace);
    }
  };

  const handleMarketplaceChange = (e) => {
    const newMarketplace = e.target.value;
    setMarketplace(newMarketplace);
    saveMarketplace(newMarketplace);

    // Notify parent component if handler is provided
    if (onMarketplaceChange) {
      onMarketplaceChange(newMarketplace);
    }
  };

  const handleClear = () => {
    setKeyword('');
    document.querySelector('input[name="search"]')?.focus();
  };

  return (
    <form onSubmit={handleSubmit} className="w-full max-w-3xl mx-auto">
      <div className="relative flex items-center gap-2">

        {/* Marketplace Selector */}
        <div className="relative flex-shrink-0">
          <select
            value={marketplace}
            onChange={handleMarketplaceChange}
            disabled={loading}
            className="appearance-none pl-4 pr-10 py-3 border-2 border-gray-300 rounded-lg bg-white hover:border-gray-400 focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent transition-all cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed font-medium"
          >
            {Object.values(MARKETPLACES).map((mp) => (
              <option key={mp} value={mp}>
                {MARKETPLACE_CONFIG[mp].label}
              </option>
            ))}
          </select>
          <ChevronDown className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4 pointer-events-none" />
        </div>

        {/* Search Input Container */}
        <div className="relative flex-1">
          <Search className="absolute left-4 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />

          <input
            type="text"
            name="search"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="Search for products (e.g., iPhone, laptop, headphones)..."
            className="w-full pl-12 pr-32 py-3 border-2 border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent transition-all"
            disabled={loading}
            autoFocus
          />

          {keyword && (
            <button
              type="button"
              onClick={handleClear}
              className="absolute right-28 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors"
              disabled={loading}
            >
              <X className="w-5 h-5" />
            </button>
          )}

          <button
            type="submit"
            disabled={loading || !keyword.trim()}
            className="absolute right-2 top-1/2 transform -translate-y-1/2 px-6 py-2 bg-red-600 text-white rounded-lg font-medium hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {loading ? (
              <span className="flex items-center gap-2">
                <span className="animate-spin">⏳</span>
                Searching...
              </span>
            ) : (
              'Search'
            )}
          </button>
        </div>
      </div>

      <p className="text-xs text-gray-500 text-center mt-2">
        Press <kbd className="px-2 py-1 bg-gray-100 rounded">Enter</kbd> to search
      </p>
    </form>
  );
};

export default SearchBar;