// src/shared/utils/storage.js
// Session storage utilities for marketplace and other preferences

const STORAGE_KEYS = {
  MARKETPLACE: 'panda_marketplace',
  AUTH_TOKEN: 'token',
};

/**
 * Get marketplace from session storage or return default
 * @param {string} defaultValue - Default marketplace if not found
 * @returns {string} Marketplace value
 */
export const getMarketplace = (defaultValue = 'aliexpress') => {
  try {
    const stored = sessionStorage.getItem(STORAGE_KEYS.MARKETPLACE);
    return stored || defaultValue;
  } catch (error) {
    console.warn('Failed to read from sessionStorage:', error);
    return defaultValue;
  }
};

/**
 * Set marketplace in session storage
 * @param {string} marketplace - Marketplace to store
 */
export const setMarketplace = (marketplace) => {
  try {
    sessionStorage.setItem(STORAGE_KEYS.MARKETPLACE, marketplace);
  } catch (error) {
    console.warn('Failed to write to sessionStorage:', error);
  }
};

/**
 * Clear marketplace from session storage
 */
export const clearMarketplace = () => {
  try {
    sessionStorage.removeItem(STORAGE_KEYS.MARKETPLACE);
  } catch (error) {
    console.warn('Failed to clear from sessionStorage:', error);
  }
};

/**
 * Get auth token from localStorage
 * @returns {string|null} Auth token or null
 */
export const getAuthToken = () => {
  try {
    return localStorage.getItem(STORAGE_KEYS.AUTH_TOKEN);
  } catch (error) {
    console.warn('Failed to read auth token:', error);
    return null;
  }
};

/**
 * Set auth token in localStorage
 * @param {string} token - Auth token to store
 */
export const setAuthToken = (token) => {
  try {
    localStorage.setItem(STORAGE_KEYS.AUTH_TOKEN, token);
  } catch (error) {
    console.warn('Failed to write auth token:', error);
  }
};

/**
 * Clear auth token from localStorage
 */
export const clearAuthToken = () => {
  try {
    localStorage.removeItem(STORAGE_KEYS.AUTH_TOKEN);
  } catch (error) {
    console.warn('Failed to clear auth token:', error);
  }
};
