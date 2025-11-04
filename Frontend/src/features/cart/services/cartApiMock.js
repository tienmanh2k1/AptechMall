/**
 * Mock Cart API Service
 * Use this temporarily if backend cart endpoints are not ready
 * Replace with real cartApi.js when backend is ready
 */

// Mock in-memory cart storage
let mockCart = {
  userId: 1,
  items: []
};

let nextItemId = 1;

// Simulate API delay
const delay = (ms = 500) => new Promise(resolve => setTimeout(resolve, ms));

/**
 * Get user's cart
 */
export const getCart = async (userId) => {
  await delay(300);
  console.log('[MOCK] Getting cart for user:', userId);

  // Ensure cart structure has items array
  const cart = {
    userId: mockCart.userId,
    items: Array.isArray(mockCart.items) ? [...mockCart.items] : []
  };

  console.log('[MOCK] Returning cart:', cart);
  return cart;
};

/**
 * Add product to cart
 * Transforms frontend data to backend format (same as real API)
 */
export const addToCart = async (userId, product) => {
  await delay(300);

  // Transform platform to uppercase marketplace enum
  const getMarketplace = (platform) => {
    const platformLower = (platform || '').toLowerCase();
    switch (platformLower) {
      case 'aliexpress':
        return 'ALIEXPRESS';
      case '1688':
        return 'ALIBABA1688';
      default:
        return 'ALIEXPRESS';
    }
  };

  const marketplace = getMarketplace(product.platform);
  const productId = product.id;

  console.log('[MOCK] Adding to cart (frontend data):', product);

  // Check if product already exists in cart
  const existingItem = mockCart.items.find(
    item => item.productId === productId && item.marketplace === marketplace
  );

  if (existingItem) {
    // Update quantity if already exists
    existingItem.quantity += product.quantity || 1;
  } else {
    // Add new item with backend-expected field names
    const newItem = {
      id: nextItemId++,
      productId: productId,
      productName: product.title,                    // ✅ title → productName
      productImage: product.image,                   // ✅ image → productImage
      price: typeof product.price === 'string'
        ? parseFloat(product.price)
        : product.price,                             // ✅ Convert string to number
      marketplace: marketplace,                      // ✅ platform → marketplace (UPPERCASE)
      quantity: product.quantity || 1,
      // Variant fields (if present)
      ...(product.variantId && {
        variantId: product.variantId,
        variantName: product.variantName,
        variantOptions: product.variantOptions
      })
    };

    console.log('[MOCK] Stored item (backend format):', newItem);
    mockCart.items.push(newItem);
  }

  return { ...mockCart };
};

/**
 * Update cart item quantity
 */
export const updateCartItem = async (userId, itemId, quantity) => {
  await delay(300);
  console.log('[MOCK] Updating cart item:', itemId, 'quantity:', quantity);

  const item = mockCart.items.find(i => i.id === itemId);
  if (item) {
    item.quantity = quantity;
  }

  return { ...mockCart };
};

/**
 * Remove item from cart
 */
export const removeCartItem = async (userId, itemId) => {
  await delay(300);
  console.log('[MOCK] Removing cart item:', itemId);

  mockCart.items = mockCart.items.filter(i => i.id !== itemId);

  return { ...mockCart };
};

/**
 * Clear entire cart
 */
export const clearCart = async (userId) => {
  await delay(300);
  console.log('[MOCK] Clearing cart');

  mockCart.items = [];

  return { ...mockCart };
};
