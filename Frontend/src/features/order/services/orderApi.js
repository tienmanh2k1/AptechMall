/**
 * Order API Service
 * Handles all order-related API calls using the shared Axios instance
 *
 * IMPORTANT: Endpoints updated to match Backend OrderController.java
 * Backend base path: /api/orders
 * User ID is automatically extracted from JWT token by backend
 */

import api from '../../../shared/services/api';

/**
 * Create an order (checkout)
 * Backend: POST /api/orders/checkout
 * User ID is automatically extracted from JWT token by backend
 *
 * @param {Object} checkoutData - Checkout data
 * @param {string} checkoutData.shippingAddress - Shipping address
 * @param {string} checkoutData.phone - Phone number
 * @param {string} checkoutData.note - Order note (optional)
 * @param {Array<number>} checkoutData.itemIds - Optional cart item IDs to checkout (defaults to all items if not provided)
 * @returns {Promise<Object>} Created order with orderNumber
 */
export const checkout = async (checkoutData) => {
  try {
    const requestBody = {
      shippingAddress: checkoutData.shippingAddress,
      phone: checkoutData.phone,
      note: checkoutData.note || ''
    };

    // Include itemIds if provided (for partial cart checkout)
    if (checkoutData.itemIds && checkoutData.itemIds.length > 0) {
      requestBody.itemIds = checkoutData.itemIds;
    }

    const response = await api.post('/orders/checkout', requestBody);
    // Backend returns { data: {...}, message: "...", success: true }
    return response.data.data;
  } catch (error) {
    console.error('Error creating order:', error);
    throw error;
  }
};

/**
 * Get user's orders with pagination
 * Backend: GET /api/orders?page={page}&size={size}
 * User ID is automatically extracted from JWT token by backend
 *
 * @param {number} page - Page number (0-indexed, default: 0)
 * @param {number} size - Page size (default: 10)
 * @param {string} status - Filter by status (optional)
 * @returns {Promise<Object>} Orders list with pagination metadata
 */
export const getOrders = async (page = 0, size = 10, status = null) => {
  try {
    const params = {
      page,
      size
    };
    if (status) {
      params.status = status;
    }

    const response = await api.get('/orders', { params });
    // Backend returns { data: {...}, message: "...", success: true }
    // Extract the data field which contains { orders, totalPages, etc }
    return response.data.data;
  } catch (error) {
    console.error('Error fetching orders:', error);
    throw error;
  }
};

/**
 * Get order by ID
 * Backend: GET /api/orders/{orderId}
 * User ID is automatically extracted from JWT token by backend
 *
 * @param {number} orderId - Order ID
 * @returns {Promise<Object>} Order details with items
 */
export const getOrderById = async (orderId) => {
  try {
    const response = await api.get(`/orders/${orderId}`);
    // Backend returns { data: {...}, message: "...", success: true }
    return response.data.data;
  } catch (error) {
    console.error('Error fetching order:', error);
    throw error;
  }
};

/**
 * Get order by order number
 * Backend: GET /api/orders/number/{orderNumber}
 * User ID is automatically extracted from JWT token by backend
 *
 * @param {string} orderNumber - Order number (e.g., ORD-20251030142530-A3F9)
 * @returns {Promise<Object>} Order details with items
 */
export const getOrderByNumber = async (orderNumber) => {
  try {
    const response = await api.get(`/orders/number/${orderNumber}`);
    // Backend returns { data: {...}, message: "...", success: true }
    return response.data.data;
  } catch (error) {
    console.error('Error fetching order by number:', error);
    throw error;
  }
};

/**
 * Update order status
 * Backend: PUT /api/orders/{orderId}/status
 * User ID is automatically extracted from JWT token by backend
 *
 * @param {number} orderId - Order ID
 * @param {string} status - New status (PENDING, CONFIRMED, SHIPPING, DELIVERED, CANCELLED)
 * @returns {Promise<Object>} Updated order
 */
export const updateOrderStatus = async (orderId, status) => {
  try {
    const response = await api.put(`/orders/${orderId}/status`, {
      status
    });
    // Backend returns { data: {...}, message: "...", success: true }
    return response.data.data;
  } catch (error) {
    console.error('Error updating order status:', error);
    throw error;
  }
};

/**
 * Cancel order (only if status is PENDING)
 * Backend: DELETE /api/orders/{orderId}
 * User ID is automatically extracted from JWT token by backend
 *
 * @param {number} orderId - Order ID
 * @returns {Promise<Object>} Cancelled order
 */
export const cancelOrder = async (orderId) => {
  try {
    const response = await api.delete(`/orders/${orderId}`);
    // Backend returns { data: {...}, message: "...", success: true }
    return response.data.data;
  } catch (error) {
    console.error('Error cancelling order:', error);
    throw error;
  }
};

/**
 * Pay remaining amount (30% + fees) from wallet
 * Backend: POST /api/orders/{orderId}/pay-remaining
 * User ID is automatically extracted from JWT token by backend
 *
 * @param {number} orderId - Order ID
 * @returns {Promise<Object>} Updated order with new payment status
 */
export const payRemainingAmount = async (orderId) => {
  try {
    const response = await api.post(`/orders/${orderId}/pay-remaining`);
    // Backend returns { data: {...}, message: "...", success: true }
    return response.data.data;
  } catch (error) {
    console.error('Error paying remaining amount:', error);
    throw error;
  }
};

/**
 * Update order shipping address (User can only update when order is PENDING)
 * Backend: PUT /api/orders/{orderId}/address
 * User ID is automatically extracted from JWT token by backend
 *
 * @param {number} orderId - Order ID
 * @param {Object} addressData - Address data
 * @param {string} addressData.shippingAddress - New shipping address
 * @param {string} addressData.phone - New phone number
 * @param {string} addressData.note - Optional note explaining the change
 * @returns {Promise<Object>} Updated order
 */
export const updateOrderAddress = async (orderId, addressData) => {
  try {
    const response = await api.put(`/orders/${orderId}/address`, addressData);
    // Backend returns { data: {...}, message: "...", success: true }
    return response.data.data;
  } catch (error) {
    console.error('Error updating order address:', error);
    throw error;
  }
};
