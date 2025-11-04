// src/features/product/components/search/SearchFilters.jsx
import React, { useState, useEffect } from 'react';
import { Filter, X } from 'lucide-react';

const SearchFilters = ({ onFiltersChange, onClearFilters, initialFilters, hasActiveFilters }) => {
  const [filters, setFilters] = useState({
    category: initialFilters.category || '',
    minPrice: initialFilters.minPrice || '',
    maxPrice: initialFilters.maxPrice || '',
  });

  // Sync với initialFilters từ URL
  useEffect(() => {
    setFilters({
      category: initialFilters.category || '',
      minPrice: initialFilters.minPrice || '',
      maxPrice: initialFilters.maxPrice || '',
    });
  }, [initialFilters.category, initialFilters.minPrice, initialFilters.maxPrice]);

  const handleApplyFilters = () => {
    onFiltersChange(filters);
  };

  const handleReset = () => {
    setFilters({ category: '', minPrice: '', maxPrice: '' });
    onClearFilters();
  };

  const categories = [
    { id: '', label: 'All Categories' },
    { id: '1', label: 'Electronics' },
    { id: '2', label: 'Fashion' },
    { id: '3', label: 'Home & Garden' },
    { id: '4', label: 'Sports' },
    { id: '5', label: 'Toys' },
  ];

  return (
    <div className="bg-white rounded-lg shadow-sm p-6 sticky top-20">
      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-gray-900 flex items-center gap-2">
          <Filter className="w-5 h-5" />
          Filters
        </h3>
        {hasActiveFilters && (
          <button
            onClick={handleReset}
            className="text-sm text-red-600 hover:text-red-700 flex items-center gap-1"
          >
            <X className="w-4 h-4" />
            Clear
          </button>
        )}
      </div>

      {/* Category Filter */}
      <div className="mb-6">
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Category
        </label>
        <select
          value={filters.category}
          onChange={(e) => setFilters({ ...filters, category: e.target.value })}
          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent"
        >
          {categories.map(cat => (
            <option key={cat.id} value={cat.id}>
              {cat.label}
            </option>
          ))}
        </select>
      </div>

      {/* Price Range Filter */}
      <div className="mb-6">
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Price Range (VND)
        </label>
        <div className="grid grid-cols-2 gap-3">
          <input
            type="number"
            placeholder="Min"
            value={filters.minPrice}
            onChange={(e) => setFilters({ ...filters, minPrice: e.target.value })}
            className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent"
          />
          <input
            type="number"
            placeholder="Max"
            value={filters.maxPrice}
            onChange={(e) => setFilters({ ...filters, maxPrice: e.target.value })}
            className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent"
          />
        </div>
      </div>

      {/* Quick Price Filters */}
      <div className="mb-6">
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Quick Filters
        </label>
        <div className="space-y-2">
          {[
            { label: 'Under 100,000đ', min: '', max: '100000' },
            { label: '100,000đ - 500,000đ', min: '100000', max: '500000' },
            { label: '500,000đ - 1,000,000đ', min: '500000', max: '1000000' },
            { label: 'Over 1,000,000đ', min: '1000000', max: '' },
          ].map((range, idx) => (
            <button
              key={idx}
              onClick={() => setFilters({ ...filters, minPrice: range.min, maxPrice: range.max })}
              className="w-full text-left px-3 py-2 text-sm text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
            >
              {range.label}
            </button>
          ))}
        </div>
      </div>

      {/* Apply Button */}
      <button
        onClick={handleApplyFilters}
        className="w-full px-4 py-2 bg-red-600 text-white rounded-lg font-medium hover:bg-red-700 transition-colors"
      >
        Apply Filters
      </button>
    </div>
  );
};

export default SearchFilters;