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

/**
 * Update order fees
 * @param {number} orderId - Order ID
 * @param {Object} feesData - Fees data
 * @param {number} feesData.domesticShippingFee - Domestic shipping fee in CNY
 * @param {number} feesData.internationalShippingFee - International shipping fee in VND
 * @param {number} feesData.estimatedWeight - Estimated weight in kg
 * @param {boolean} feesData.includeWoodenPackaging - Include wooden packaging service
 * @param {boolean} feesData.includeBubbleWrap - Include bubble wrap service
 * @param {boolean} feesData.includeItemCountCheck - Include item count check service
 * @param {string} feesData.note - Optional note
 * @returns {Promise<Object>} Updated order
 */
export const updateOrderFees = async (orderId, feesData) => {
  try {
    const response = await api.put(`/admin/orders/${orderId}/fees`, feesData);
    return response.data;
  } catch (error) {
    console.error('Update order fees error:', error);
    throw error;
  }
};

/**
 * Update order shipping address (Admin/Staff can update when PENDING or CONFIRMED)
 * @param {number} orderId - Order ID
 * @param {Object} addressData - Address data
 * @param {string} addressData.shippingAddress - New shipping address
 * @param {string} addressData.phone - New phone number
 * @param {string} addressData.note - Optional note
 * @returns {Promise<Object>} Updated order
 */
export const updateOrderAddress = async (orderId, addressData) => {
  try {
    const response = await api.put(`/admin/orders/${orderId}/address`, addressData);
    return response.data;
  } catch (error) {
    console.error('Update order address error:', error);
    throw error;
  }
};
