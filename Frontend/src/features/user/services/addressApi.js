/**
 * Address API Service
 * Handles user address management API calls
 */

import api from '../../../shared/services/api';

/**
 * Get all addresses for current user
 * @returns {Promise<Array>} List of addresses
 */
export const getAllAddresses = async () => {
  try {
    const response = await api.get('/addresses');
    return response.data;
  } catch (error) {
    console.error('Get all addresses error:', error);
    throw error;
  }
};

/**
 * Get address by ID
 * @param {number} addressId - Address ID
 * @returns {Promise<Object>} Address data
 */
export const getAddressById = async (addressId) => {
  try {
    const response = await api.get(`/addresses/${addressId}`);
    return response.data;
  } catch (error) {
    console.error('Get address by ID error:', error);
    throw error;
  }
};

/**
 * Get default address for current user
 * @returns {Promise<Object|null>} Default address or null
 */
export const getDefaultAddress = async () => {
  try {
    const response = await api.get('/addresses/default');
    return response.data;
  } catch (error) {
    if (error.response?.status === 404) {
      return null;
    }
    console.error('Get default address error:', error);
    throw error;
  }
};

/**
 * Create new address
 * @param {Object} addressData - Address data
 * @param {string} addressData.receiverName - Receiver name
 * @param {string} addressData.phone - Phone number
 * @param {string} addressData.province - Province/City
 * @param {string} addressData.district - District
 * @param {string} addressData.ward - Ward
 * @param {string} addressData.addressDetail - Detailed address
 * @param {string} [addressData.addressType] - Address type (HOME, OFFICE, OTHER)
 * @param {boolean} [addressData.isDefault] - Is default address
 * @returns {Promise<Object>} Created address
 */
export const createAddress = async (addressData) => {
  try {
    const response = await api.post('/addresses', addressData);
    return response.data;
  } catch (error) {
    console.error('Create address error:', error);
    throw error;
  }
};

/**
 * Update address
 * @param {number} addressId - Address ID
 * @param {Object} addressData - Updated address data
 * @returns {Promise<Object>} Updated address
 */
export const updateAddress = async (addressId, addressData) => {
  try {
    const response = await api.put(`/addresses/${addressId}`, addressData);
    return response.data;
  } catch (error) {
    console.error('Update address error:', error);
    throw error;
  }
};

/**
 * Delete address
 * @param {number} addressId - Address ID
 * @returns {Promise<void>}
 */
export const deleteAddress = async (addressId) => {
  try {
    await api.delete(`/addresses/${addressId}`);
  } catch (error) {
    console.error('Delete address error:', error);
    throw error;
  }
};

/**
 * Set address as default
 * @param {number} addressId - Address ID
 * @returns {Promise<Object>} Updated address
 */
export const setDefaultAddress = async (addressId) => {
  try {
    const response = await api.put(`/addresses/${addressId}/default`);
    return response.data;
  } catch (error) {
    console.error('Set default address error:', error);
    throw error;
  }
};
