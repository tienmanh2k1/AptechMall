import axios from 'axios';

const API_BASE_URL = 'http://neurophysiologically-unenthralled-jamey.ngrok-free.dev/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 100000,
  headers: {
    'Content-Type': 'application/json',
    'ngrok-skip-browser-warning': true
  },
  withCredentials: true, // Enable cookies for refresh_token
});

// Track if we're currently refreshing to avoid multiple refresh calls
let isRefreshing = false;
let failedQueue = [];
let refreshAttempts = 0;
const MAX_REFRESH_ATTEMPTS = 1; // Only try refresh once

const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

// Helper to check if refresh_token cookie exists
const hasRefreshToken = () => {
  return document.cookie.split(';').some(cookie => cookie.trim().startsWith('refresh_token='));
};

// Request interceptor
api.interceptors.request.use(
  (config) => {
    // Skip auth token for login/register/refresh endpoints
    const publicEndpoints = ['/auth/login', '/auth/register', '/auth/refresh'];
    const isPublicEndpoint = publicEndpoints.some(endpoint => config.url.includes(endpoint));

    if (!isPublicEndpoint) {
      // Add auth token if available
      const token = localStorage.getItem('token');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
        console.log('🔑 [API] Auth token attached:', token.substring(0, 20) + '...');
      } else {
        console.warn('⚠️ [API] No auth token found in localStorage');
      }
    } else {
      console.log('🔓 [API] Public endpoint - skipping token attachment');
    }

    console.log('API Request:', config.method.toUpperCase(), config.url);
    return config;
  },
  (error) => {
    console.error('Request error:', error);
    return Promise.reject(error);
  }
);

// Response interceptor with automatic token refresh
api.interceptors.response.use(
  (response) => {
    console.log('API Response:', response.status, response.config.url);
    return response;
  },
  async (error) => {
    const originalRequest = error.config;
    console.error('Response error:', error.response?.status, error.message);

    // Handle 401 Unauthorized errors with automatic token refresh
    if (error.response?.status === 401 && !originalRequest._retry) {
      // Skip refresh for auth endpoints to avoid infinite loops
      if (originalRequest.url?.includes('/auth/login') ||
          originalRequest.url?.includes('/auth/register') ||
          originalRequest.url?.includes('/auth/refresh')) {
        console.log('🚫 [API] Skipping refresh for auth endpoint');
        return Promise.reject(error);
      }

      // Check if we've exceeded max refresh attempts
      if (refreshAttempts >= MAX_REFRESH_ATTEMPTS) {
        console.error('❌ [API] Max refresh attempts reached, forcing logout');
        refreshAttempts = 0; // Reset for next session
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        window.dispatchEvent(new CustomEvent('token-expired'));
        if (window.location.pathname !== '/login' && window.location.pathname !== '/register') {
          window.location.href = '/login';
        }
        return Promise.reject(new Error('Max refresh attempts exceeded'));
      }

      // Check if refresh_token cookie exists
      if (!hasRefreshToken()) {
        console.error('❌ [API] No refresh_token cookie found');
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        window.dispatchEvent(new CustomEvent('token-expired'));
        if (window.location.pathname !== '/login' && window.location.pathname !== '/register') {
          window.location.href = '/login';
        }
        return Promise.reject(new Error('No refresh token available'));
      }

      if (isRefreshing) {
        // If already refreshing, queue this request
        console.log('⏳ [API] Token refresh in progress, queuing request...');
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then(token => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return api(originalRequest);
          })
          .catch(err => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;
      refreshAttempts++;

      try {
        console.log('🔄 [API] Access token expired, attempting to refresh...');

        // Call refresh endpoint (refresh_token is in httpOnly cookie)
        const response = await axios.post(
          `${API_BASE_URL}/auth/refresh`,
          {},
          {
            withCredentials: true,
            validateStatus: (status) => status < 500 // Accept all non-server errors
          }
        );

        // Check if response is successful and has token
        if (response.status === 200 && response.data && response.data.token) {
          const newToken = response.data.token;
          console.log('✅ [API] Token refreshed successfully');
          localStorage.setItem('token', newToken);

          // Reset refresh attempts on success
          refreshAttempts = 0;

          // Update the authorization header
          api.defaults.headers.common['Authorization'] = `Bearer ${newToken}`;
          originalRequest.headers.Authorization = `Bearer ${newToken}`;

          // Process all queued requests with new token
          processQueue(null, newToken);

          // Retry the original request
          return api(originalRequest);
        } else {
          // Refresh failed (401, 403, or invalid response)
          console.error('❌ [API] Token refresh failed - invalid response:', {
            status: response.status,
            data: response.data
          });
          throw new Error('Refresh token expired or invalid');
        }
      } catch (refreshError) {
        console.error('❌ [API] Token refresh failed:', refreshError.message);
        processQueue(refreshError, null);

        // Clear authentication data
        localStorage.removeItem('token');
        localStorage.removeItem('user');

        // Dispatch custom event for AuthContext to update state
        window.dispatchEvent(new CustomEvent('token-expired'));

        // Redirect to login page
        const currentPath = window.location.pathname;
        if (currentPath !== '/login' && currentPath !== '/register') {
          console.log('🔐 [API] Redirecting to login page...');
          window.location.href = '/login';
        }

        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    // Handle 403 Forbidden errors
    if (error.response?.status === 403) {
      console.error('❌ [API] 403 Forbidden:', {
        url: error.config?.url,
        method: error.config?.method,
        data: error.response?.data,
        hasToken: !!localStorage.getItem('token')
      });
    }

    return Promise.reject(error);
  }
);

export default api;