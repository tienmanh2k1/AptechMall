# Expired Token Login Fix

## Problem

When trying to login, user got error:
```
JWT token expired: JWT expired 1702967313 milliseconds ago at 2025-10-09T18:56:53.000Z. Current time: 2025-10-29T11:59:40.313Z. Allowed clock skew: 0 milliseconds.
```

## Root Cause

1. **Old token in localStorage**: An expired token from Oct 9, 2025 was still stored in the browser's localStorage
2. **Interceptor sending token on login**: The `api.js` request interceptor was automatically attaching the expired token to **ALL** requests, including login/register requests
3. **Backend rejecting request**: The backend validated the expired token in the Authorization header and rejected the login request before even checking credentials

## Solution Applied

### 1. Fixed Request Interceptor (`api.js`)

Modified the request interceptor to **skip attaching tokens** for public authentication endpoints:

```javascript
// Skip auth token for login/register/refresh endpoints
const publicEndpoints = ['/auth/login', '/auth/register', '/auth/refresh'];
const isPublicEndpoint = publicEndpoints.some(endpoint => config.url.includes(endpoint));

if (!isPublicEndpoint) {
  // Only add token for protected endpoints
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
}
```

**Benefits:**
- Login/register requests no longer send expired tokens
- Prevents authentication loop issues
- Cleaner API calls for public endpoints

### 2. Auto-clear Expired Tokens (`LoginPage.jsx`)

Added a `useEffect` hook that runs when the login page loads to automatically clear any expired or invalid tokens:

```javascript
useEffect(() => {
  const token = localStorage.getItem('token');
  if (token) {
    try {
      const decoded = jwtDecode(token);
      const currentTime = Date.now() / 1000;

      if (decoded.exp <= currentTime) {
        // Token expired, clear it
        localStorage.removeItem('token');
        localStorage.removeItem('user');
      }
    } catch (error) {
      // Invalid token, clear it
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    }
  }
}, []);
```

**Benefits:**
- Automatically cleans up stale tokens
- Prevents future login issues
- User-friendly (happens silently in the background)

## How to Test

1. **Test expired token cleanup:**
   - Manually add an expired token to localStorage:
     ```javascript
     localStorage.setItem('token', 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...')
     ```
   - Navigate to `/login`
   - Token should be automatically removed
   - Login should work normally

2. **Test login without old tokens:**
   - Clear localStorage: `localStorage.clear()`
   - Go to login page
   - Login with demo account: `demo.account@gmail.com` / `demo123`
   - Should login successfully without errors

3. **Test public endpoints don't send tokens:**
   - Open Network tab in DevTools
   - Submit login form
   - Check the `/api/auth/login` request
   - Verify **no** `Authorization` header is present

## Files Modified

1. `src/shared/services/api.js`
   - Lines 14-41: Modified request interceptor

2. `src/features/auth/pages/LoginPage.jsx`
   - Lines 1-37: Added useEffect to clear expired tokens
   - Added `jwtDecode` import

## Prevention for Future

The existing `AuthContext.jsx` already has token validation on app initialization (lines 20-46), but it doesn't prevent the issue when:
- User navigates directly to login page
- Token expired between page loads

The new `LoginPage` useEffect ensures tokens are validated **right before login**, providing an extra layer of protection.

## Related Issues

- ✅ Login now works even with expired tokens in localStorage
- ✅ No need for manual `localStorage.clear()` by users
- ✅ Public endpoints (login/register/refresh) no longer send unnecessary tokens
- ✅ Cleaner API request logs

## Date Fixed

October 29, 2025
