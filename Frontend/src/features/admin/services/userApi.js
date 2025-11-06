/**
 * Admin User API Service
 * Handles CRUD operations for user management
 */

import api from '../../../shared/services/api';

/**
 * Get all users
 * @returns {Promise<Array>} List of users
 */
export const getAllUsers = async () => {
  try {
    const response = await api.get('/users/');
    return response.data;
  } catch (error) {
    console.error('Get all users error:', error);
    throw error;
  }
};

/**
 * Get user by ID
 * @param {number} id - User ID
 * @returns {Promise<Object>} User data
 */
export const getUserById = async (id) => {
  try {
    const response = await api.get(`/users/${id}`);
    return response.data;
  } catch (error) {
    console.error('Get user by ID error:', error);
    throw error;
  }
};

/**
 * Update a user
 * @param {number} id - User ID
 * @param {Object} userData - Updated user data
 * @returns {Promise<Object>} Updated user
 */
export const updateUser = async (id, userData) => {
  try {
    const response = await api.put(`/users/${id}`, userData);
    return response.data;
  } catch (error) {
    console.error('Update user error:', error);
    throw error;
  }
};

/**
 * Partially update a user
 * @param {number} id - User ID
 * @param {Object} updates - Partial updates
 * @returns {Promise<Object>} Updated user
 */
export const patchUser = async (id, updates) => {
  try {
    const response = await api.patch(`/users/${id}`, updates);
    return response.data;
  } catch (error) {
    console.error('Patch user error:', error);
    throw error;
  }
};

/**
 * Delete a user
 * @param {number} id - User ID
 * @returns {Promise<void>}
 */
export const deleteUser = async (id) => {
  try {
    await api.delete(`/users/${id}`);
  } catch (error) {
    console.error('Delete user error:', error);
    throw error;
  }
};
