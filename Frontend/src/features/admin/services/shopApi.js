/**
 * Admin Shop API Service
 * Handles CRUD operations for shop management
 */

import api from '../../../shared/services/api';

/**
 * Get all shops
 * @returns {Promise<Array>} List of shops
 */
export const getAllShops = async () => {
  try {
    const response = await api.get('/admin/shops');
    return response.data;
  } catch (error) {
    console.error('Get all shops error:', error);
    throw error;
  }
};

/**
 * Get shop by ID
 * @param {number} id - Shop ID
 * @returns {Promise<Object>} Shop data
 */
export const getShopById = async (id) => {
  try {
    const response = await api.get(`/admin/shops/${id}`);
    return response.data;
  } catch (error) {
    console.error('Get shop by ID error:', error);
    throw error;
  }
};

/**
 * Create a new shop
 * @param {Object} shopData - Shop data
 * @returns {Promise<Object>} Created shop
 */
export const createShop = async (shopData) => {
  try {
    const response = await api.post('/admin/shops', shopData);
    return response.data;
  } catch (error) {
    console.error('Create shop error:', error);
    throw error;
  }
};

/**
 * Update a shop
 * @param {number} id - Shop ID
 * @param {Object} shopData - Updated shop data
 * @returns {Promise<Object>} Updated shop
 */
export const updateShop = async (id, shopData) => {
  try {
    const response = await api.put(`/admin/shops/${id}`, shopData);
    return response.data;
  } catch (error) {
    console.error('Update shop error:', error);
    throw error;
  }
};

/**
 * Delete a shop
 * @param {number} id - Shop ID
 * @returns {Promise<void>}
 */
export const deleteShop = async (id) => {
  try {
    await api.delete(`/admin/shops/${id}`);
  } catch (error) {
    console.error('Delete shop error:', error);
    throw error;
  }
};

