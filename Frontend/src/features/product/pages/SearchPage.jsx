// src/features/product/pages/SearchPage.jsx
import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import { productService } from '../services/productService';
import { DEFAULT_MARKETPLACE, MARKETPLACE_CONFIG } from '../../../shared/utils/constants';
import { getMarketplace } from '../../../shared/utils/storage';

// Import components
import SearchBar from '../components/search/SearchBar';
import SearchFilters from '../components/search/SearchFilters';
import SearchSuggestions from '../components/search/SearchSuggestions';
import SortDropdown from '../components/search/SortDropdown';
import SearchResults from '../components/search/SearchResults';

// Import shared components
import Loading from '../../../shared/components/Loading';
import ErrorMessage from '../../../shared/components/ErrorMessage';

const SearchPage = () => {
  // ============ ROUTING ============
  const [searchParams, setSearchParams] = useSearchParams();
  //const navigate = useNavigate();

  // ============ STATE ============
  const [results, setResults] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // ============ URL PARAMS ============
  const keyword = searchParams.get('q') || '';
  const marketplace = searchParams.get('marketplace') || getMarketplace(DEFAULT_MARKETPLACE);
  const category = searchParams.get('category') || '';
  const minPrice = searchParams.get('minPrice') || '';
  const maxPrice = searchParams.get('maxPrice') || '';
  const sort = searchParams.get('sort') || 'default';
  const pageParam = parseInt(searchParams.get('page') || '1');

  // Validate page number: must be positive integer, max 100 pages
  const MAX_PAGE = 100;
  const page = Math.max(1, Math.min(pageParam, MAX_PAGE));

  // ============ SEARCH EFFECT ============
  useEffect(() => {
    // If page param was invalid (> MAX_PAGE), correct it in URL
    if (pageParam !== page && keyword.trim()) {
      console.warn(`⚠️ Page ${pageParam} exceeds max (${MAX_PAGE}). Correcting to ${page}...`);
      toast.warning(`Trang ${pageParam} không hợp lệ. Chuyển đến trang ${page}.`);
      const params = { q: keyword, marketplace, page };
      if (category) params.category = category;
      if (minPrice) params.minPrice = minPrice;
      if (maxPrice) params.maxPrice = maxPrice;
      if (sort !== 'default') params.sort = sort;
      setSearchParams(params);
      return;
    }

    if (keyword.trim()) {
      performSearch();
    } else {
      setResults(null);
    }
  }, [keyword, marketplace, category, minPrice, maxPrice, sort, page]);

  // ============ API CALL ============
  const performSearch = async () => {
    setLoading(true);
    setError(null);

    try {
      console.log('🔍 Searching:', { marketplace, keyword, category, minPrice, maxPrice, sort, page });

      const data = await productService.searchProducts(
        marketplace,
        keyword,
        page,
        { category, minPrice, maxPrice, sort }
      );

      console.log('✅ SearchPage received data:', {
        hasProducts: !!data.products,
        productsLength: data.products?.length || 0,
        hasMeta: !!data.meta,
        totalResults: data.meta?.totalResults,
        data: data
      });

      // Validate page number - if no products and page > 1, reset to page 1
      if (data.products && data.products.length === 0 && page > 1) {
        console.warn(`⚠️ Page ${page} has no results. Redirecting to page 1...`);
        toast.info(`Trang ${page} không có sản phẩm. Quay lại trang 1.`);
        const params = { q: keyword, marketplace, page: 1 };
        if (category) params.category = category;
        if (minPrice) params.minPrice = minPrice;
        if (maxPrice) params.maxPrice = maxPrice;
        if (sort !== 'default') params.sort = sort;
        setSearchParams(params);
        return;
      }

      setResults(data);
    } catch (err) {
      console.error('❌ Search error:', err);
      setError(err.message || 'Failed to search products');
    } finally {
      setLoading(false);
    }
  };

  // ============ HANDLERS ============

  const handleSearch = (newKeyword, newMarketplace) => {
    if (!newKeyword.trim()) return;

    const params = {
      q: newKeyword.trim(),
      marketplace: newMarketplace || marketplace,
      page: 1
    };
    if (category) params.category = category;
    if (minPrice) params.minPrice = minPrice;
    if (maxPrice) params.maxPrice = maxPrice;
    if (sort !== 'default') params.sort = sort;

    setSearchParams(params);
  };

  const handleMarketplaceChange = (newMarketplace) => {
    // Update marketplace in URL without triggering a new search unless keyword exists
    if (keyword.trim()) {
      const params = {
        q: keyword,
        marketplace: newMarketplace,
        page: 1
      };
      if (category) params.category = category;
      if (minPrice) params.minPrice = minPrice;
      if (maxPrice) params.maxPrice = maxPrice;
      if (sort !== 'default') params.sort = sort;

      setSearchParams(params);
    }
  };

  const handleFiltersChange = (filters) => {
    const params = { q: keyword, marketplace, page: 1 };
    if (filters.category) params.category = filters.category;
    if (filters.minPrice) params.minPrice = filters.minPrice;
    if (filters.maxPrice) params.maxPrice = filters.maxPrice;
    if (sort !== 'default') params.sort = sort;

    setSearchParams(params);
  };

  const handleSortChange = (newSort) => {
    const params = { q: keyword, marketplace, page };
    if (category) params.category = category;
    if (minPrice) params.minPrice = minPrice;
    if (maxPrice) params.maxPrice = maxPrice;
    if (newSort !== 'default') params.sort = newSort;

    setSearchParams(params);
  };

  const handlePageChange = (newPage) => {
    const params = { q: keyword, marketplace, page: newPage };
    if (category) params.category = category;
    if (minPrice) params.minPrice = minPrice;
    if (maxPrice) params.maxPrice = maxPrice;
    if (sort !== 'default') params.sort = sort;

    setSearchParams(params);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleClearFilters = () => {
    setSearchParams({ q: keyword, marketplace, page: 1 });
  };

  // ============ DATA EXTRACTION ============
  const products = results?.products || [];
  const totalResults = results?.meta?.totalResults || 0;
  const hasFilters = category || minPrice || maxPrice || sort !== 'default';

  // ============ RENDER ============
  return (
    <div className="min-h-screen bg-gray-50">
      <div className="container mx-auto px-4 py-8">
        
        {/* ========== HEADER ========== */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-6 text-center">
            Search Products
          </h1>
          
          <SearchBar
            onSearch={handleSearch}
            loading={loading}
            initialKeyword={keyword}
            initialMarketplace={marketplace}
            onMarketplaceChange={handleMarketplaceChange}
          />
        </div>

        {/* ========== NO KEYWORD - SHOW SUGGESTIONS ========== */}
        {!keyword && !loading && (
          <SearchSuggestions onSelectSuggestion={handleSearch} />
        )}

        {/* ========== HAS KEYWORD - SHOW RESULTS ========== */}
        {keyword && (
          <div className="flex flex-col lg:flex-row gap-6">
            
            {/* ========== SIDEBAR - FILTERS ========== */}
            <aside className="w-full lg:w-64 flex-shrink-0">
              <SearchFilters
                onFiltersChange={handleFiltersChange}
                onClearFilters={handleClearFilters}
                initialFilters={{ category, minPrice, maxPrice }}
                hasActiveFilters={hasFilters}
              />
            </aside>

            {/* ========== MAIN CONTENT ========== */}
            <main className="flex-1">
              
              {/* Results Header */}
              {!loading && results && (
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between mb-6 gap-4">
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${MARKETPLACE_CONFIG[marketplace]?.color || 'bg-gray-600'} text-white`}>
                        {MARKETPLACE_CONFIG[marketplace]?.label || marketplace}
                      </span>
                    </div>
                    <p className="text-gray-600">
                      Found <span className="font-semibold text-gray-900">{totalResults.toLocaleString()}</span> results
                      for "<span className="font-semibold text-gray-900">{keyword}</span>"
                    </p>
                    {hasFilters && (
                      <button
                        onClick={handleClearFilters}
                        className="text-sm text-red-600 hover:text-red-700 mt-1"
                      >
                        Clear all filters
                      </button>
                    )}
                  </div>

                  <SortDropdown
                    value={sort}
                    onChange={handleSortChange}
                  />
                </div>
              )}

              {/* Loading State */}
              {loading && (
                <Loading message="Searching for products..." />
              )}

              {/* Error State */}
              {error && !loading && (
                <ErrorMessage
                  message={error}
                  onRetry={performSearch}
                />
              )}

              {/* Search Results */}
              {!loading && !error && results && (
                <SearchResults
                  products={products}
                  keyword={keyword}
                  currentPage={page}
                  totalResults={totalResults}
                  meta={results.meta}
                  onPageChange={handlePageChange}
                  maxPage={MAX_PAGE}
                />
              )}
            </main>
          </div>
        )}
      </div>
    </div>
  );
};

export default SearchPage;