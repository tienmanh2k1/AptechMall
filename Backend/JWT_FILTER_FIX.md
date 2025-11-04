# JWT Filter Login Fix

## Problem

Login endpoint returns 401 Unauthorized even though `/api/auth/login` is configured as permitAll in SecurityConfig.

**Error Response:**
```json
{
  "error": "Token Expired",
  "message": "Your session has expired. Please login again.",
  "status": 401
}
```

## Root Cause

The `JwtAuthenticationFilter` runs for **ALL** requests (extends `OncePerRequestFilter`) and executes **BEFORE** the security configuration checks permitAll endpoints.

**Filter Chain Order:**
1. CorsFilter
2. **JwtAuthenticationFilter** ← Validates tokens here
3. TokenBlacklistFilter
4. Spring Security authorization checks (permitAll, authenticated, etc.)

**The Problem:**
- Even though SecurityConfig line 43 marks `/api/auth/*` as permitAll
- JwtAuthenticationFilter still processes the request FIRST
- If an Authorization header with expired token exists (from localStorage or cookies), the filter:
  - Catches `ExpiredJwtException` (line 64)
  - Returns 401 immediately (line 65-68)
  - Never reaches the permitAll check

## Solution Applied

Modified `JwtAuthenticationFilter.java` to skip JWT validation for public endpoints:

```java
@Override
protected void doFilterInternal(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull FilterChain filterChain)
        throws ServletException, IOException {

    // Skip JWT validation for public endpoints
    String requestPath = request.getRequestURI();
    if (requestPath.startsWith("/api/auth/") ||
        requestPath.startsWith("/api/debug/") ||
        requestPath.startsWith("/api/aliexpress/") ||
        requestPath.startsWith("/api/1688/") ||
        requestPath.startsWith("/api/products/")) {
        filterChain.doFilter(request, response);
        return;
    }

    // ... rest of JWT validation logic
}
```

**Benefits:**
- Public endpoints bypass JWT validation entirely
- Expired tokens in Authorization header won't block login requests
- Matches the permitAll configuration in SecurityConfig
- No performance overhead for public endpoints

## How to Apply

1. **Stop the backend server** (Ctrl+C in terminal)
2. **Restart backend:**
   ```bash
   cd Backend
   ./mvnw spring-boot:run
   ```
3. **Clear browser localStorage** (in DevTools Console):
   ```javascript
   localStorage.clear()
   ```
4. **Try logging in again** with demo credentials:
   - Email: `demo.account@gmail.com`
   - Password: `demo123`

## Testing

### Test 1: Login without token
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo.account@gmail.com","password":"demo123"}'
```

**Expected Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### Test 2: Login with expired token (should still work)
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer expired_token_here" \
  -d '{"username":"demo.account@gmail.com","password":"demo123"}'
```

**Expected:** Same success response (expired token ignored for login endpoint)

### Test 3: Protected endpoint without token
```bash
curl http://localhost:8080/api/cart/1
```

**Expected:** 403 Forbidden (no token provided)

### Test 4: Protected endpoint with expired token
```bash
curl http://localhost:8080/api/cart/1 \
  -H "Authorization: Bearer expired_token_here"
```

**Expected:** 401 Unauthorized with "Token expired" message

## Files Modified

1. `Backend/src/main/java/com/aptech/aptechMall/security/filters/JwtAuthenticationFilter.java`
   - Lines 35-44: Added public endpoint skip logic

2. `Frontend/src/shared/services/api.js` (from previous fix)
   - Lines 17-32: Skip attaching token for public endpoints

3. `Frontend/src/features/auth/pages/LoginPage.jsx` (from previous fix)
   - Lines 17-37: Auto-clear expired tokens on mount

## Related Configuration

**SecurityConfig.java** (line 43):
```java
.requestMatchers("/api/auth/*", "/api/auth/**", "/api/debug/**",
                 "/api/aliexpress/**", "/api/1688/**", "/api/products/**")
.permitAll()
```

These paths should now work consistently across both:
- Security configuration (permitAll)
- JWT filter (skip validation)

## Prevention for Future

When adding new public endpoints:
1. Add to `SecurityConfig.java` permitAll list
2. Add to `JwtAuthenticationFilter.java` skip list
3. Keep both lists synchronized

## Date Fixed

October 29, 2025
