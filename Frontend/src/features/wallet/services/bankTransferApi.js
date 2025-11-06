import api from '../../../shared/services/api';

/**
 * Bank Transfer SMS API Service
 * Handles SMS-based bank transfer endpoints
 */

/**
 * Get all SMS records
 * @returns {Promise} List of all SMS
 */
export const getAllSms = async () => {
  const response = await api.get('/bank-transfer/sms');
  return response.data;
};

/**
 * Get SMS by ID
 * @param {number} smsId - SMS ID
 * @returns {Promise} SMS details
 */
export const getSmsById = async (smsId) => {
  const response = await api.get(`/bank-transfer/sms/${smsId}`);
  return response.data;
};

/**
 * Get SMS with errors
 * @returns {Promise} List of SMS with errors
 */
export const getSmsWithErrors = async () => {
  const response = await api.get('/bank-transfer/sms/errors');
  return response.data;
};

/**
 * Process pending SMS
 * Triggers processing of unprocessed SMS
 * @returns {Promise} Processing result
 */
export const processPendingSms = async () => {
  const response = await api.get('/bank-transfer/process-pending');
  return response.data;
};

/**
 * Test webhook endpoint
 * @returns {Promise} Test response
 */
export const testWebhook = async () => {
  const response = await api.get('/bank-transfer/test');
  return response.data;
};

/**
 * Send test SMS (for development)
 * @param {Object} smsData - SMS data
 * @param {string} smsData.sender - SMS sender
 * @param {string} smsData.message - SMS message
 * @param {string} smsData.raw - Raw SMS data (optional)
 * @returns {Promise} Response
 */
export const sendTestSms = async (smsData) => {
  const response = await api.post('/bank-transfer/sms-webhook', null, {
    params: smsData
  });
  return response.data;
};
