import api from '../../../shared/services/api';
import { SORT_OPTIONS } from '../../../shared/utils/constants';

// Error code mapping
const ERROR_CODES = {
  VALIDATION_ERROR: 400,
  NOT_FOUND: 404,
  UPSTREAM_ERROR: 502,
  UPSTREAM_TIMEOUT: 504,
  UPSTREAM_NO_DATA: 'UPSTREAM_NO_DATA' // Special case for provider 205 responses
};

/**
 * Generate a simple hash from query parameters for analytics deduplication
 * @param {object} params - Query parameters
 * @returns {string} Hash string
 */
const generateQueryHash = (params) => {
  const sortedParams = Object.keys(params)
    .filter(key => params[key] !== undefined && params[key] !== null && params[key] !== '')
    .sort()
    .map(key => `${key}=${params[key]}`)
    .join('&');

  // Simple hash function (djb2 algorithm)
  let hash = 5381;
  for (let i = 0; i < sortedParams.length; i++) {
    hash = ((hash << 5) + hash) + sortedParams.charCodeAt(i);
  }
  return Math.abs(hash).toString(36);
};

export const productService = {
  /**
   * Fetch product details using platform + id endpoint
   * @param {string} platform - The marketplace platform (aliexpress, 1688)
   * @param {string} id - The provider's product ID
   * @returns {Promise} Normalized product data with platform field
   *
   * Response Schema:
   * {
   *   platform: 'aliexpress' | '1688',
   *   id: string,
   *   title: string,
   *   price: { value: string, currency: string },
   *   images: string[],
   *   shop: { name: string, id: string },
   *   url: string,
   *   attributes: object,
   *   badge: string,
   *   lastUpdated: string
   * }
   */
  getProductById: async (platform, id) => {
    const startTime = Date.now();

    try {
      // Call endpoint with platform and id
      const response = await api.get(`/${platform}/products/${id}`);
      const latencyMs = Date.now() - startTime;

      // Log observability data
      console.log('Product detail loaded:', {
        route: `/${platform}/products/${id}`,
        platform,
        id,
        status: response.status,
        latencyMs,
        fromCache: response.headers['x-from-cache'] === 'true',
        priceCurrency: response.data?.price?.currency
      });

      return response.data;
    } catch (error) {
      const latencyMs = Date.now() - startTime;
      const statusCode = error.response?.status;
      const errorData = error.response?.data;

      // Log error with observability data
      console.error('Product service error:', {
        route: `/${platform}/products/${id}`,
        platform,
        id,
        status: statusCode,
        latencyMs,
        errorCode: errorData?.code || 'UNKNOWN',
        provider: errorData?.provider
      });

      // Normalize error response
      let enhancedError;

      if (statusCode === 400) {
        enhancedError = new Error(errorData?.message || 'VALIDATION_ERROR: Invalid platform or product ID');
        enhancedError.code = 'VALIDATION_ERROR';
      } else if (statusCode === 404) {
        enhancedError = new Error(errorData?.message || 'NOT_FOUND: Product not found');
        enhancedError.code = 'NOT_FOUND';
      } else if (statusCode === 502) {
        enhancedError = new Error(errorData?.message || 'UPSTREAM_ERROR: Provider service error');
        enhancedError.code = 'UPSTREAM_ERROR';
        enhancedError.provider = errorData?.provider;
      } else if (statusCode === 504) {
        enhancedError = new Error(errorData?.message || 'UPSTREAM_TIMEOUT: Provider timeout');
        enhancedError.code = 'UPSTREAM_TIMEOUT';
        enhancedError.provider = errorData?.provider;
      } else if (errorData?.code === 'UPSTREAM_NO_DATA') {
        // Special handling for provider 205 responses
        enhancedError = new Error(errorData?.message || 'UPSTREAM_NO_DATA: Provider returned no data');
        enhancedError.code = 'UPSTREAM_NO_DATA';
        enhancedError.provider = errorData?.provider;
      } else {
        enhancedError = new Error(error.message || 'Failed to fetch product');
        enhancedError.code = 'UNKNOWN';
      }

      enhancedError.platform = platform;
      enhancedError.id = id;
      enhancedError.statusCode = statusCode;
      enhancedError.latencyMs = latencyMs;

      throw enhancedError;
    }
  },

  /**
   * Search products in a specific marketplace
   * @param {string} marketplace - The marketplace enum value (aliexpress, 1688)
   * @param {string} keyword - Search keyword
   * @param {number} page - Page number
   * @param {object} filters - Additional filters (category, minPrice, maxPrice, sort)
   * @returns {Promise} Search results with normalized schema
   *
   * Response Schema:
   * {
   *   platform: 'aliexpress' | '1688',
   *   items: [{
   *     id: string,
   *     platform: string,
   *     title: string,
   *     price: { value: string, currency: string },
   *     thumb: string,
   *     url: string,
   *     shop: { name: string }
   *   }],
   *   badge: string,
   *   nextPage: number
   * }
   */
  searchProducts: async (marketplace = 'aliexpress', keyword, page = 1, filters = {}) => {
    const startTime = Date.now();

    try {
      // Build query params
      const params = {
        keyword,
        page,
        ...filters // Spread filters (category, minPrice, maxPrice, sort)
      };

      // Convert sort from string to integer if present
      if (params.sort && typeof params.sort === 'string') {
        params.sort = SORT_OPTIONS[params.sort] ?? 0; // Default to 0 (relevance) if not found
      }

      // Remove empty/undefined values
      Object.keys(params).forEach(key => {
        if (params[key] === undefined || params[key] === null || params[key] === '') {
          delete params[key];
        }
      });

      // 🔍 DEBUG: Log request params BEFORE API call
      console.log('🚀 Making API request:', {
        endpoint: `/${marketplace}/search/simple`,
        params,
        fullUrl: `http://localhost:8080/api/${marketplace}/search/simple?${new URLSearchParams(params).toString()}`
      });

      // Make API call with marketplace-specific endpoint
      const response = await api.get(`/${marketplace}/search/simple`, {
        params
      });

      const latencyMs = Date.now() - startTime;

      // Generate query hash for analytics deduplication
      const queryHash = generateQueryHash({ marketplace, keyword, page, ...filters });

      // Log observability data
      console.log('Search completed:', {
        route: `/${marketplace}/search/simple`,
        platform: marketplace,
        keyword,
        page,
        queryHash,  // For analytics deduplication
        status: response.status,
        latencyMs,
        resultCount: response.data?.products?.length || 0,
        fromCache: response.headers['x-from-cache'] === 'true'
      });

      const data = response.data;

      // 🔍 DEBUG: Log raw response structure
      console.log('📦 Raw backend response:', {
        hasMeta: !!data?.meta,
        meta: data?.meta, // Full meta object
        hasProducts: !!data?.products,
        productsLength: data?.products?.length,
        firstProduct: data?.products?.[0],
        firstProductId: data?.products?.[0]?.itemId || data?.products?.[0]?.id,
        lastProductId: data?.products?.[data?.products?.length - 1]?.itemId || data?.products?.[data?.products?.length - 1]?.id
      });

      // Backend returns normalized format: { meta: {...}, products: [...] }
      // Enrich each product with platform field and map fields for frontend compatibility

      if (data?.products && Array.isArray(data.products)) {
        console.log('🔄 Processing', data.products.length, 'products from backend...');

        // Enrich and normalize each product
        data.products = data.products.map(product => {
          // Extract platform from itemId (format: "ae-123456" → "ae" → "aliexpress")
          const itemIdPrefix = product.itemId?.split('-')[0]; // Extract "ae" or "1688"
          let extractedPlatform = marketplace; // Default to search context

          if (itemIdPrefix === 'ae') {
            extractedPlatform = 'aliexpress';
          } else if (itemIdPrefix === '1688') {
            extractedPlatform = '1688';
          }

          return {
            ...product,
            // Use full itemId as canonical ID (WITH platform prefix like "ae-1005005939226934")
            // Backend API expects format: /api/{platform}/products/{id} where id includes prefix
            id: product.itemId || product.id,
            // Add platform field (extracted from itemId or search context)
            platform: extractedPlatform,
            // Map currencySign to currency for consistency
            currency: product.currencySign || product.currency || (marketplace === 'aliexpress' ? 'USD' : 'CNY'),
            // Map salesCount to totalSales for ProductCard compatibility
            totalSales: product.salesCount || 0,
            // Map productUrl to itemUrl for compatibility
            itemUrl: product.productUrl || product.itemUrl,
            // Use promotionPercent from backend as discount
            discount: product.promotionPercent || (
              product.hasDiscount && product.originalPrice && product.currentPrice
                ? Math.round(((parseFloat(product.originalPrice) - parseFloat(product.currentPrice)) / parseFloat(product.originalPrice)) * 100)
                : null
            ),
            // Convert rating to number if it's a string
            rating: typeof product.rating === 'string' ? parseFloat(product.rating) : product.rating,
            // Keep reviewCount from backend
            reviewCount: product.reviewCount || 0
          };
        });

        console.log('✅ Processed products:', data.products.length, 'items');
        console.log('📦 Sample product:', {
          id: data.products[0]?.id,
          platform: data.products[0]?.platform,
          itemId: data.products[0]?.itemId,
          title: data.products[0]?.title?.substring(0, 50) + '...',
          currency: data.products[0]?.currency
        });
      } else {
        console.warn('⚠️ No products array found in response');
      }

      return data;
    } catch (error) {
      const latencyMs = Date.now() - startTime;
      const statusCode = error.response?.status;
      const errorData = error.response?.data;

      // Generate query hash for error tracking
      const queryHash = generateQueryHash({ marketplace, keyword, page, ...filters });

      console.error('Search error:', {
        route: `/${marketplace}/search/simple`,
        platform: marketplace,
        keyword,
        page,
        queryHash,  // For analytics deduplication
        status: statusCode,
        latencyMs,
        errorCode: errorData?.code || 'UNKNOWN'
      });

      throw error.response?.data?.message || 'Failed to search products';
    }
  },

  
};