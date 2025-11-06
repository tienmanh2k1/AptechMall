/**
 * Admin Wallet API Service
 * Handles wallet management operations for administrators
 */

import api from '../../../shared/services/api';

/**
 * Get all user wallets
 * @returns {Promise<Array>} List of all wallets with user information
 */
export const getAllWallets = async () => {
  try {
    const response = await api.get('/wallet/admin/all');
    return response.data;
  } catch (error) {
    console.error('Get all wallets error:', error);
    throw error;
  }
};

/**
 * Lock a user's wallet
 * @param {number} userId - User ID
 * @returns {Promise<Object>} Success response
 */
export const lockWallet = async (userId) => {
  try {
    const response = await api.post(`/wallet/${userId}/lock`);
    return response.data;
  } catch (error) {
    console.error('Lock wallet error:', error);
    throw error;
  }
};

/**
 * Unlock a user's wallet
 * @param {number} userId - User ID
 * @returns {Promise<Object>} Success response
 */
export const unlockWallet = async (userId) => {
  try {
    const response = await api.post(`/wallet/${userId}/unlock`);
    return response.data;
  } catch (error) {
    console.error('Unlock wallet error:', error);
    throw error;
  }
};
