/**
 * Cart API Service
 * Handles all cart-related API calls using the shared Axios instance
 */

import api from '../../../shared/services/api';

/**
 * Get user's cart
 * User ID is automatically extracted from JWT token by backend
 * @returns {Promise<Object>} Cart data with items
 */
export const getCart = async () => {
  try {
    // Backend endpoint: GET /api/cart (userId from JWT token)
    const response = await api.get('/cart');

    // Debug: Log backend response structure
    console.log('📦 [cartApi.getCart] Backend response:', {
      status: response.status,
      data: response.data,
      dataType: typeof response.data,
      hasItems: response.data?.items !== undefined,
      hasNestedData: response.data?.data !== undefined,
      itemsIsArray: Array.isArray(response.data?.items),
      nestedItemsIsArray: Array.isArray(response.data?.data?.items),
      itemsCount: response.data?.items?.length || 0
    });

    // Handle nested response structure: {success, message, data: {items: []}}
    // Backend returns: {success: true, message: "...", data: {items: [...]}}
    if (response.data?.data) {
      console.log('✅ [cartApi.getCart] Using nested data structure (response.data.data)');
      return response.data.data; // Return the nested "data" object which contains "items"
    }

    // Handle flat response structure: {items: []}
    return response.data;
  } catch (error) {
    console.error('❌ [cartApi.getCart] Error fetching cart:', error);
    throw error;
  }
};

/**
 * Add product to cart
 *
 * IMPORTANT: This function transforms frontend product data to backend-expected format:
 * - title → productName
 * - image → productImage
 * - platform → marketplace (UPPERCASE: ALIEXPRESS, ALIBABA1688)
 * - price → number (auto-converts string to number with parseFloat)
 * - currency field is removed (not needed by backend)
 *
 * User ID is automatically extracted from JWT token by backend
 *
 * @param {Object} product - Product object (frontend format)
 * @param {string} product.id - Product ID
 * @param {string} product.platform - Marketplace (aliexpress, 1688) - will be transformed to uppercase
 * @param {string} product.title - Product title - will be sent as productName
 * @param {number|string} product.price - Product price (string or number) - will be converted to number
 * @param {string} product.image - Product image URL - will be sent as productImage
 * @param {number} product.quantity - Quantity to add (default: 1)
 * @param {string} [product.variantId] - Variant ID (optional)
 * @param {string} [product.variantName] - Variant display name (optional, e.g., "Size M - Red")
 * @param {string} [product.variantOptions] - Variant options string (optional, e.g., "Size: M, Color: Red")
 * @returns {Promise<Object>} Updated cart
 */
export const addToCart = async (product) => {
  try {
    // Transform platform to uppercase marketplace enum
    const getMarketplace = (platform) => {
      const platformLower = (platform || '').toLowerCase();
      switch (platformLower) {
        case 'aliexpress':
          return 'ALIEXPRESS';
        case '1688':
          return 'ALIBABA1688';
        default:
          return 'ALIEXPRESS'; // Default fallback
      }
    };

    // Transform frontend fields to backend-expected format
    const requestBody = {
      productId: product.id,
      productName: product.title,                    // ✅ title → productName
      productImage: product.image,                   // ✅ image → productImage
      price: typeof product.price === 'string'
        ? parseFloat(product.price)
        : product.price,                             // ✅ Convert string to number
      marketplace: getMarketplace(product.platform), // ✅ platform → marketplace (UPPERCASE)
      quantity: product.quantity || 1
    };

    // Add variant fields if they exist
    if (product.variantId) {
      requestBody.variantId = product.variantId;
      requestBody.variantName = product.variantName;
      requestBody.variantOptions = product.variantOptions;
    }

    // Debug log to verify correct format
    console.log('📦 Cart request body (mapped):', JSON.stringify(requestBody, null, 2));

    // Backend endpoint: POST /api/cart/items (userId from JWT token)
    const response = await api.post('/cart/items', requestBody);

    // Debug: Log backend response
    console.log('✅ [cartApi.addToCart] Backend response:', {
      status: response.status,
      data: response.data,
      hasItems: response.data?.items !== undefined,
      hasNestedData: response.data?.data !== undefined,
      itemsCount: response.data?.items?.length || 0,
      nestedItemsCount: response.data?.data?.items?.length || 0
    });

    // Handle nested response structure: {success, message, data: {items: []}}
    if (response.data?.data) {
      console.log('✅ [cartApi.addToCart] Using nested data structure (response.data.data)');
      return response.data.data;
    }

    return response.data;
  } catch (error) {
    console.error('❌ Error adding to cart:', error.response?.data || error);
    throw error;
  }
};

/**
 * Update cart item quantity
 * User ID is automatically extracted from JWT token by backend
 * @param {number} itemId - Cart item ID
 * @param {number} quantity - New quantity
 * @returns {Promise<Object>} Updated cart
 */
export const updateCartItem = async (itemId, quantity) => {
  try {
    // Backend endpoint: PUT /api/cart/items/{itemId} (userId from JWT token)
    const response = await api.put(`/cart/items/${itemId}`, {
      quantity
    });
    return response.data;
  } catch (error) {
    console.error('Error updating cart item:', error);
    throw error;
  }
};

/**
 * Remove item from cart
 * User ID is automatically extracted from JWT token by backend
 * @param {number} itemId - Cart item ID
 * @returns {Promise<Object>} Updated cart
 */
export const removeCartItem = async (itemId) => {
  try {
    // Backend endpoint: DELETE /api/cart/items/{itemId} (userId from JWT token)
    const response = await api.delete(`/cart/items/${itemId}`);
    return response.data;
  } catch (error) {
    console.error('Error removing cart item:', error);
    throw error;
  }
};

/**
 * Clear entire cart
 * User ID is automatically extracted from JWT token by backend
 * @returns {Promise<Object>} Empty cart response
 */
export const clearCart = async () => {
  try {
    // Backend endpoint: DELETE /api/cart/clear (userId from JWT token)
    const response = await api.delete('/cart/clear');
    return response.data;
  } catch (error) {
    console.error('Error clearing cart:', error);
    throw error;
  }
};
