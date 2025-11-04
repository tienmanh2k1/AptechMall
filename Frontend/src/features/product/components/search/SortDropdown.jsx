// src/features/product/components/search/SortDropdown.jsx
import React from 'react';
import { ArrowUpDown } from 'lucide-react';

const SortDropdown = ({ value, onChange }) => {
  // Sort options matching backend API: ["Default", "PriceAsc", "PriceDesc", "Sales"]
  const sortOptions = [
    { value: 'default', label: 'Most Relevant' },
    { value: 'price-asc', label: 'Price: Low to High' },
    { value: 'price-desc', label: 'Price: High to Low' },
    { value: 'sales', label: 'Best Selling' },
  ];

  return (
    <div className="flex items-center gap-2">
      <ArrowUpDown className="w-4 h-4 text-gray-600" />
      <select
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent bg-white cursor-pointer"
      >
        {sortOptions.map(option => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </div>
  );
};

export default SortDropdown;