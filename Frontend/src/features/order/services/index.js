/**
 * Order API Service Selector
 * Automatically imports mock or real API based on config
 */

import { USE_MOCK_API } from '../../../config/api.config';

// Import based on config and initialize sample data
let api;
if (USE_MOCK_API) {
  api = import('./orderApiMock.js').then(module => {
    console.log('[ORDER API] Using MOCK implementation');
    // Add sample orders for testing
    module.addSampleOrders?.(1);
    return module;
  });
} else {
  api = import('./orderApi.js');
  console.log('[ORDER API] Using REAL backend implementation');
}

// Re-export with lazy loading
export const checkout = async (...args) => {
  const module = await api;
  return module.checkout(...args);
};

export const getOrders = async (...args) => {
  const module = await api;
  return module.getOrders(...args);
};

export const getOrderById = async (...args) => {
  const module = await api;
  return module.getOrderById(...args);
};

export const updateOrderStatus = async (...args) => {
  const module = await api;
  return module.updateOrderStatus(...args);
};

export const cancelOrder = async (...args) => {
  const module = await api;
  return module.cancelOrder(...args);
};

export const getOrderByNumber = async (...args) => {
  const module = await api;
  return module.getOrderByNumber(...args);
};
