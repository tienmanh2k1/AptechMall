// src/features/product/components/search/SearchSuggestions.jsx
import React from 'react';
import { TrendingUp } from 'lucide-react';

const SearchSuggestions = ({ onSelectSuggestion }) => {
  const trendingSearches = [
    'iPhone 15',
    'Laptop Gaming',
    'Wireless Headphones',
    'Smart Watch',
    'Camera',
    'Mechanical Keyboard',
    'Gaming Mouse',
    'iPad Pro',
  ];

  return (
    <div className="max-w-4xl mx-auto">
      {/* Trending Searches */}
      <div className="mb-8">
        <h2 className="text-xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
          <TrendingUp className="w-5 h-5 text-red-600" />
          Trending Searches
        </h2>
        <div className="flex flex-wrap gap-3">
          {trendingSearches.map((term, idx) => (
            <button
              key={idx}
              onClick={() => onSelectSuggestion(term)}
              className="px-4 py-2 bg-white border-2 border-gray-200 text-gray-700 rounded-lg hover:border-red-500 hover:text-red-600 transition-all"
            >
              {term}
            </button>
          ))}
        </div>
      </div>

      {/* Categories Shortcut */}
      <div className="mt-8">
        <h2 className="text-xl font-semibold text-gray-900 mb-4">
          Browse by Category
        </h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {[
            { name: 'Electronics', emoji: '📱' },
            { name: 'Fashion', emoji: '👕' },
            { name: 'Home & Garden', emoji: '🏠' },
            { name: 'Sports', emoji: '⚽' },
          ].map((cat, idx) => (
            <button
              key={idx}
              onClick={() => onSelectSuggestion(cat.name)}
              className="p-6 bg-white rounded-lg shadow-sm hover:shadow-md transition-shadow text-center"
            >
              <div className="text-4xl mb-2">{cat.emoji}</div>
              <div className="font-medium text-gray-900">{cat.name}</div>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};

export default SearchSuggestions;