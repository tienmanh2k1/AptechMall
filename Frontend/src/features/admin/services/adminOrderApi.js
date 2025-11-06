/**
 * Admin Order API Service
 * Handles admin order management operations
 */

import api from '../../../shared/services/api';

/**
 * Get all orders (admin view)
 * @param {number} page - Page number
 * @param {number} size - Page size
 * @param {string} status - Optional status filter
 * @param {number} userId - Optional user ID filter
 * @returns {Promise<Object>} Page of orders
 */
export const getAllOrders = async (page = 0, size = 10, status = null, userId = null) => {
  try {
    const params = { page, size };
    if (status) params.status = status;
    if (userId) params.userId = userId;

    const response = await api.get('/admin/orders', { params });
    return response.data;
  } catch (error) {
    console.error('Get all orders error:', error);
    throw error;
  }
};

/**
 * Get order by ID (admin view)
 * @param {number} orderId - Order ID
 * @returns {Promise<Object>} Order details
 */
export const getOrderById = async (orderId) => {
  try {
    const response = await api.get(`/admin/orders/${orderId}`);
    return response.data;
  } catch (error) {
    console.error('Get order by ID error:', error);
    throw error;
  }
};

/**
 * Update order status
 * @param {number} orderId - Order ID
 * @param {string} status - New status
 * @param {string} note - Optional note
 * @returns {Promise<Object>} Updated order
 */
export const updateOrderStatus = async (orderId, status, note = null) => {
  try {
    const response = await api.put(`/admin/orders/${orderId}/status`, {
      status,
      note,
    });
    return response.data;
  } catch (error) {
    console.error('Update order status error:', error);
    throw error;
  }
};
