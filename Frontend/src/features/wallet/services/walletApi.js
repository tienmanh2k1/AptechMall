import api from '../../../shared/services/api';

/**
 * Wallet API Service
 * Handles all wallet-related API calls
 */

/**
 * Get current user's wallet information
 * @returns {Promise} Wallet data with balance
 */
export const getWallet = async () => {
  const response = await api.get('/wallet');
  return response.data;
};

/**
 * Initiate deposit transaction
 * @param {Object} depositData - Deposit request data
 * @param {number} depositData.amount - Amount to deposit
 * @param {string} depositData.paymentGateway - Payment gateway (VNPAY, MOMO, ZALOPAY, BANK_TRANSFER)
 * @param {string} depositData.returnUrl - URL to redirect after payment
 * @returns {Promise} Deposit initiation response with payment URL
 */
export const initiateDeposit = async (depositData) => {
  const response = await api.post('/wallet/deposit/initiate', depositData);
  return response.data;
};

/**
 * Process deposit callback from payment gateway
 * @param {number} amount - Deposit amount
 * @param {string} paymentGateway - Payment gateway used
 * @param {string} referenceNumber - Payment reference from gateway
 * @returns {Promise} Transaction details
 */
export const processDepositCallback = async (amount, paymentGateway, referenceNumber) => {
  const response = await api.post('/wallet/deposit/callback', null, {
    params: { amount, paymentGateway, referenceNumber }
  });
  return response.data;
};

/**
 * Get transaction history with filters
 * @param {Object} filters - Filter parameters
 * @param {string} filters.transactionType - Transaction type filter (optional)
 * @param {string} filters.startDate - Start date filter (optional)
 * @param {string} filters.endDate - End date filter (optional)
 * @param {number} filters.page - Page number (default: 0)
 * @param {number} filters.size - Page size (default: 20)
 * @returns {Promise} Paginated transaction history
 */
export const getTransactionHistory = async (filters = {}) => {
  const { page = 0, size = 20, transactionType, startDate, endDate } = filters;

  const params = { page, size };
  if (transactionType) params.transactionType = transactionType;
  if (startDate) params.startDate = startDate;
  if (endDate) params.endDate = endDate;

  const response = await api.get('/wallet/transactions', { params });
  return response.data;
};

/**
 * Get single transaction by ID
 * @param {number} transactionId - Transaction ID
 * @returns {Promise} Transaction details
 */
export const getTransaction = async (transactionId) => {
  const response = await api.get(`/wallet/transactions/${transactionId}`);
  return response.data;
};

/**
 * Lock user wallet (admin only)
 * @param {number} userId - User ID to lock wallet
 * @returns {Promise} Success message
 */
export const lockWallet = async (userId) => {
  const response = await api.post(`/wallet/${userId}/lock`);
  return response.data;
};

/**
 * Unlock user wallet (admin only)
 * @param {number} userId - User ID to unlock wallet
 * @returns {Promise} Success message
 */
export const unlockWallet = async (userId) => {
  const response = await api.post(`/wallet/${userId}/unlock`);
  return response.data;
};
