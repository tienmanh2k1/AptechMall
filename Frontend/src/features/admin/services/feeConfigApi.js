/**
 * System Fee Config API Service
 * Handles CRUD operations for system fee configuration
 */

import api from '../../../shared/services/api';

/**
 * Get all fee configurations
 * @returns {Promise<Array>} List of fee configs
 */
export const getAllFeeConfigs = async () => {
  try {
    const response = await api.get('/admin/fee-config');
    return response.data;
  } catch (error) {
    console.error('Get all fee configs error:', error);
    throw error;
  }
};

/**
 * Get active fee configuration
 * @returns {Promise<Object>} Active fee config
 */
export const getActiveFeeConfig = async () => {
  try {
    const response = await api.get('/admin/fee-config/active');
    return response.data;
  } catch (error) {
    console.error('Get active fee config error:', error);
    throw error;
  }
};

/**
 * Get fee config by ID
 * @param {number} id - Config ID
 * @returns {Promise<Object>} Fee config data
 */
export const getFeeConfigById = async (id) => {
  try {
    const response = await api.get(`/admin/fee-config/${id}`);
    return response.data;
  } catch (error) {
    console.error('Get fee config by ID error:', error);
    throw error;
  }
};

/**
 * Create a new fee configuration
 * @param {Object} configData - Fee config data
 * @returns {Promise<Object>} Created fee config
 */
export const createFeeConfig = async (configData) => {
  try {
    const response = await api.post('/admin/fee-config', configData);
    return response.data;
  } catch (error) {
    console.error('Create fee config error:', error);
    throw error;
  }
};

/**
 * Update a fee configuration
 * @param {number} id - Config ID
 * @param {Object} configData - Updated config data
 * @returns {Promise<Object>} Updated fee config
 */
export const updateFeeConfig = async (id, configData) => {
  try {
    const response = await api.put(`/admin/fee-config/${id}`, configData);
    return response.data;
  } catch (error) {
    console.error('Update fee config error:', error);
    throw error;
  }
};

/**
 * Activate a fee configuration
 * @param {number} id - Config ID
 * @returns {Promise<Object>} Activated fee config
 */
export const activateFeeConfig = async (id) => {
  try {
    const response = await api.post(`/admin/fee-config/${id}/activate`);
    return response.data;
  } catch (error) {
    console.error('Activate fee config error:', error);
    throw error;
  }
};

/**
 * Delete a fee configuration
 * @param {number} id - Config ID
 * @returns {Promise<void>}
 */
export const deleteFeeConfig = async (id) => {
  try {
    await api.delete(`/admin/fee-config/${id}`);
  } catch (error) {
    console.error('Delete fee config error:', error);
    throw error;
  }
};
