/**
 * Cart API Service Selector
 * Automatically imports mock or real API based on config
 */

import { USE_MOCK_API } from '../../../config/api.config';

// Import based on config
let api;
if (USE_MOCK_API) {
  api = import('./cartApiMock.js');
  console.log('[CART API] Using MOCK implementation');
} else {
  api = import('./cartApi.js');
  console.log('[CART API] Using REAL backend implementation');
}

// Re-export with lazy loading
export const getCart = async (...args) => {
  const module = await api;
  return module.getCart(...args);
};

export const addToCart = async (...args) => {
  const module = await api;
  return module.addToCart(...args);
};

export const updateCartItem = async (...args) => {
  const module = await api;
  return module.updateCartItem(...args);
};

export const removeCartItem = async (...args) => {
  const module = await api;
  return module.removeCartItem(...args);
};

export const clearCart = async (...args) => {
  const module = await api;
  return module.clearCart(...args);
};
