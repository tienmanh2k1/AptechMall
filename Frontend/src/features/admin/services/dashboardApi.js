/**
 * Admin Dashboard API Service
 * Handles dashboard statistics and analytics
 */

import api from '../../../shared/services/api';

/**
 * Get dashboard statistics
 * @returns {Promise<Object>} Dashboard statistics
 */
export const getDashboardStats = async () => {
  try {
    const response = await api.get('/admin/dashboard/stats');
    return response.data;
  } catch (error) {
    console.error('Get dashboard stats error:', error);
    throw error;
  }
};
