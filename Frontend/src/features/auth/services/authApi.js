/**
 * Authentication API Service
 * Handles login, register, and logout API calls
 */

import api from '../../../shared/services/api';

/**
 * Login user
 * @param {Object} credentials
 * @param {string} credentials.username - Username or email
 * @param {string} credentials.password - User password
 * @returns {Promise<Object>} Response with token
 */
export const login = async (credentials) => {
  try {
    const response = await api.post('/auth/login', {
      username: credentials.username, // Backend expects "username" field (can be username or email)
      password: credentials.password,
    });

    // Backend returns: { token: "eyJhbGci..." }
    return response.data;
  } catch (error) {
    console.error('Login error:', error);
    throw error;
  }
};

/**
 * Register new user
 * @param {Object} userData
 * @param {string} userData.username - Username
 * @param {string} userData.password - User password
 * @param {string} userData.fullName - User full name
 * @param {string} userData.email - User email
 * @param {string} [userData.role] - User role (default: CUSTOMER)
 * @returns {Promise<Object>} Response with success message
 */
export const register = async (userData) => {
  try {
    const response = await api.post('/auth/register', {
      username: userData.username,     // REQUIRED
      password: userData.password,     // REQUIRED
      fullName: userData.fullName,     // REQUIRED (not "name")
      email: userData.email,           // REQUIRED
      role: userData.role || 'CUSTOMER' // OPTIONAL (default: CUSTOMER)
    });

    // Backend returns: { message: "Successfully registered the user {username}" }
    return response.data;
  } catch (error) {
    console.error('Register error:', error);
    throw error;
  }
};

/**
 * Logout user
 * @returns {Promise<void>}
 */
export const logout = async () => {
  try {
    // Call backend logout endpoint to clear refresh_token cookie
    await api.post('/auth/logout');

    // Clear local storage
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  } catch (error) {
    console.error('Logout error:', error);
    // Still clear local storage even if API call fails
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    throw error;
  }
};

/**
 * Get current user profile
 * @returns {Promise<Object>} User data
 */
export const getCurrentUser = async () => {
  try {
    const response = await api.get('/auth/me');
    return response.data;
  } catch (error) {
    console.error('Get current user error:', error);
    // Fallback to /auth/profile if /auth/me doesn't work
    try {
      const response = await api.get('/auth/profile');
      return response.data;
    } catch (fallbackError) {
      console.error('Get current user fallback error:', fallbackError);
      throw fallbackError;
    }
  }
};

/**
 * Refresh access token
 * @returns {Promise<Object>} New access token
 */
export const refreshToken = async () => {
  try {
    // No need to send refresh_token in body - it's in httpOnly cookie
    const response = await api.post('/auth/refresh');
    return response.data;
  } catch (error) {
    console.error('Refresh token error:', error);
    throw error;
  }
};

/**
 * Login user with Google OAuth
 * @param {Object} authRequest
 * @param {string} authRequest.email - Google email
 * @param {string} authRequest.fullName - Google Full Name
 * @param {string} authRequest.googleSub - Google Id
 * @param {string} username - Either from form's username or Username from Email
 * @returns {Promise<Object>} Response with token
 */
export const googleOauth = async (authRequest, username) => {
  try {
    console.log("Email in googleOauth: " + authRequest.email);
    // Backend expects AuthRequest with:
    // - username (used as email)
    // - fullname (lowercase 'n')
    const response = await api.post('/auth/login?method=google', {
      username: authRequest.email,  // Backend uses 'username' field to store email
      fullname: authRequest.fullName,  // Backend uses lowercase 'fullname'
      password: ""  // Empty password for OAuth
    });

    // Backend returns: { token: "eyJhbGci..." }
    return response.data;
  } catch (error) {
    console.error('Google OAuth login error:', error);
    throw error;
  }
};

/**
 * Generate Refresh Token for OAuth Users
 * @returns {Promise<Object>} Success response
 */
export const generateRefreshOauth = async () => {
  try {
    const response = await api.post('/auth/oauth');
    return response.data;
  } catch (error) {
    console.error('Generate OAuth refresh token error:', error);
    throw error;
  }
};
