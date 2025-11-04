// src/features/product/components/search/SearchResults.jsx
import React from 'react';
import ProductCard from '../ProductCard';
import { ChevronLeft, ChevronRight } from 'lucide-react';

const SearchResults = ({ products, keyword, currentPage, totalResults, meta, onPageChange, maxPage = 100 }) => {
  // Use totalPages from backend meta if available, otherwise calculate from totalResults
  // Backend provides: meta.totalPages, meta.pageSize, meta.page, meta.totalResults
  // IMPORTANT: Limit totalPages to maxPage to prevent invalid pagination
  const backendTotalPages = meta?.totalPages || Math.ceil(totalResults / (meta?.pageSize || 20));
  const totalPages = Math.min(backendTotalPages, maxPage);
  const isLimited = backendTotalPages > maxPage; // true if pagination is limited

  if (products.length === 0) {
    return (
      <div className="text-center py-16 bg-white rounded-lg shadow-sm">
        <div className="text-6xl mb-4">🔍</div>
        <h3 className="text-xl font-semibold text-gray-900 mb-2">
          No products found
        </h3>
        <p className="text-gray-600 mb-4">
          We couldn't find any products matching "{keyword}"
        </p>
        <p className="text-sm text-gray-500">
          Try different keywords or remove filters
        </p>
      </div>
    );
  }

  return (
    <div>
      {/* Product Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6 mb-8">
        {products.map((product) => (
          <ProductCard
            key={product.itemId}
            product={product}
          />
        ))}
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 flex-wrap">
          <button
            onClick={() => onPageChange(currentPage - 1)}
            disabled={currentPage === 1}
            className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <ChevronLeft className="w-5 h-5" />
          </button>

          {(() => {
            const pages = [];
            const maxVisible = 5;
            
            let startPage = Math.max(1, currentPage - Math.floor(maxVisible / 2));
            let endPage = Math.min(totalPages, startPage + maxVisible - 1);
            
            if (endPage - startPage < maxVisible - 1) {
              startPage = Math.max(1, endPage - maxVisible + 1);
            }

            if (startPage > 1) {
              pages.push(
                <button key={1} onClick={() => onPageChange(1)} className="px-4 py-2 border rounded-lg hover:bg-gray-50">
                  1
                </button>
              );
              if (startPage > 2) pages.push(<span key="dots1" className="px-2">...</span>);
            }

            for (let i = startPage; i <= endPage; i++) {
              pages.push(
                <button
                  key={i}
                  onClick={() => onPageChange(i)}
                  className={`px-4 py-2 rounded-lg ${
                    currentPage === i ? 'bg-red-600 text-white' : 'border hover:bg-gray-50'
                  }`}
                >
                  {i}
                </button>
              );
            }

            if (endPage < totalPages) {
              if (endPage < totalPages - 1) pages.push(<span key="dots2" className="px-2">...</span>);
              pages.push(
                <button key={totalPages} onClick={() => onPageChange(totalPages)} className="px-4 py-2 border rounded-lg hover:bg-gray-50">
                  {totalPages}
                </button>
              );
            }

            return pages;
          })()}

          <button
            onClick={() => onPageChange(currentPage + 1)}
            disabled={currentPage === totalPages}
            className="px-4 py-2 border rounded-lg hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <ChevronRight className="w-5 h-5" />
          </button>

          <div className="ml-4 flex flex-col items-end">
            <span className="text-sm text-gray-600 whitespace-nowrap">
              Page {currentPage} of {totalPages}
            </span>
            {isLimited && (
              <span className="text-xs text-gray-500 italic">
                (Giới hạn {maxPage} trang)
              </span>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default SearchResults;