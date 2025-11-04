# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Aptechmall** is a full-stack e-commerce application that integrates with external marketplace APIs (AliExpress, Alibaba 1688) to provide product search and ordering capabilities. The project consists of a Java Spring Boot backend and a React frontend.

**Repository Structure:**
- `Backend/` - Spring Boot 3.5.6 REST API (Java 17)
- `Frontend/` - React 19 + Vite SPA (Node.js/npm)

## Quick Start

### Prerequisites
1. MySQL 8 server running on `localhost:3306` with database `test_db`
2. Redis server running (required for JWT token blacklist/logout)
3. RapidAPI keys for AliExpress and Alibaba 1688 (configured in Backend `application.properties`)

### Full Stack Development
```bash
# 1. Start Backend (from Backend/ directory)
cd Backend
./mvnw spring-boot:run

# 2. Start Frontend (from Frontend/ directory)
cd Frontend
npm install
npm run dev

# 3. Access application
# Frontend: http://localhost:5173
# Backend API: http://localhost:8080/api

# 4. Test with demo account
# Email: demo.account@gmail.com
# Password: demo123
```

## Key Architecture

### Technology Stack
**Backend:**
- Spring Boot 3.5.6, Java 17
- MySQL 8 (database), Redis (token blacklist)
- Spring Security + JWT authentication
- Spring Data JPA, WebFlux (for external APIs)

**Frontend:**
- React 19 + Vite
- React Router v6+
- Axios (API client)
- Tailwind CSS + lucide-react icons
- Context API for state management

### Security Architecture

**JWT-Based Authentication:**
- Access tokens: 5-minute TTL
- Refresh tokens: 8-day TTL
- Token claims include: `userId`, `role`, `email`, `fullname`, `status`, `username`
- Logout uses Redis token blacklist

**CRITICAL SECURITY PATTERN:**
Backend extracts `userId` from authenticated JWT token, NEVER from client parameters. This prevents users from accessing other users' data.

**Example (Backend):**
```java
// ✅ CORRECT - Extract from JWT
Long userId = AuthenticationUtil.getCurrentUserId();
CartResponse cart = cartService.getCart(userId);

// ❌ WRONG - Never accept userId from client
@GetMapping("/cart")
public ResponseEntity<?> getCart(@RequestParam Long userId) { // NO!
```

**Example (Frontend):**
```javascript
// ✅ CORRECT - Token in header, no userId param
export const getCart = async () => {
  const response = await api.get('/cart'); // JWT auto-attached by interceptor
  return response.data;
};

// ❌ WRONG - Never pass userId from client
export const getCart = async (userId) => { // NO!
  const response = await api.get(`/cart?userId=${userId}`);
```

### API Conventions

**Canonical Path Structure:**
Frontend URLs match backend API paths for product routes:
- Pattern: `/{platform}/products/{id}`
- Platforms: `aliexpress`, `1688`
- Example: `/aliexpress/products/1005005244562338` (same path on frontend and backend)

**Protected Endpoints:**
All cart and order endpoints require authentication:
- `/api/cart/**` - Cart operations
- `/api/orders/**` - Order management
- Backend extracts `userId` from JWT token (via `AuthenticationUtil.getCurrentUserId()`)
- Frontend sends JWT token in `Authorization` header (automatically via Axios interceptor)

**Public Endpoints:**
- `/api/auth/*` - Authentication (login, register, logout, refresh)
- `/api/{platform}/products/{id}` - Product detail
- `/api/{platform}/search/simple` - Product search
- `/api/debug/**` - Debug endpoints

### State Management

**Context Providers (Frontend):**
1. **AuthContext** (`features/auth/context/AuthContext.jsx`)
   - Manages authentication state: `user`, `token`, `loading`
   - Methods: `login()`, `logout()`, `updateUser()`, `isAuthenticated()`
   - Validates token expiry on app initialization

2. **CartContext** (`features/cart/context/CartContext.jsx`)
   - Manages cart count badge in header
   - Methods: `refreshCart()` - call after cart modifications
   - Resets on logout, refreshes on login

**Provider Hierarchy:**
```jsx
<AuthProvider>
  <CartProvider>
    <Routes />
  </CartProvider>
</AuthProvider>
```

## Development Workflow

### Backend Development (Java Spring Boot)

**Common Commands:**
```bash
cd Backend

# Compile and run
./mvnw spring-boot:run

# Run tests
./mvnw test

# Run specific test
./mvnw test -Dtest=ClassName#methodName

# Build JAR
./mvnw clean package -DskipTests
```

**Adding New Features:**
1. Define entity/model (if database changes needed)
2. Create/update repository interface
3. Implement service layer with business logic
4. Add DTOs for request/response
5. Create controller endpoints
6. Update `SecurityConfig` if endpoint needs specific authorization
7. **IMPORTANT:** For user-specific resources, always use `AuthenticationUtil.getCurrentUserId()` to get the authenticated user's ID

**Key Utility Classes:**
- `AuthenticationUtil` - Extract current user info from SecurityContext
- `JwtService` - Generate/validate JWT tokens
- `RedisService` - Manage token blacklist

### Frontend Development (React + Vite)

**Common Commands:**
```bash
cd Frontend

# Install dependencies
npm install

# Start dev server (port 5173)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Lint code
npm run lint
```

**Feature-Based Structure:**
```
src/features/
├── auth/      - Authentication (login, register, protected routes)
├── cart/      - Shopping cart
├── order/     - Order management (checkout, order history)
└── product/   - Product browsing (detail, search)
```

**Adding New API Calls:**
1. Define API function in feature's `services/` directory
2. **DO NOT** pass `userId` as parameter - JWT token contains user identity
3. Token automatically attached by `api.js` Axios interceptor
4. Handle 401 errors (interceptor redirects to login automatically)

**Example API Service:**
```javascript
// features/cart/services/cartApi.js
export const addToCart = async (product) => {
  const response = await api.post('/cart/items', product);
  return response.data;
};
```

### Mock API Configuration (Frontend Only)

For testing frontend without backend:
```javascript
// src/config/api.config.js
export const USE_MOCK_API = true; // Switch to mock mode
```

Mock implementations available for cart and order services.

## Common Tasks

### Running Tests
```bash
# Backend tests
cd Backend
./mvnw test

# Frontend (no tests configured yet)
cd Frontend
# npm test (not configured)
```

### Updating Security Configuration
When adding new protected endpoints:
1. Edit `Backend/src/main/java/com/aptech/aptechMall/config/SecurityConfig.java`
2. Add route pattern to appropriate section (public or role-restricted)
3. Test authentication is enforced

### Database Changes
- Backend uses JPA auto-update (`spring.jpa.hibernate.ddl-auto=update`)
- Changes to `@Entity` classes automatically update schema
- SQL queries logged to console (DEBUG mode enabled)

### Adding External API Integration
1. Implement `ProductMarketplaceService` interface
2. Use WebFlux `WebClient` with 10MB buffer for large responses
3. Configure API keys in `application.properties`
4. Map API responses to standardized DTOs

## Important Security Notes

### Recent Security Fixes

**Critical fixes implemented (see detailed docs):**
1. **Cart/Order Isolation** (`SECURITY_FIX_CART_ORDER_ISOLATION.md`)
   - Fixed: Users could access other users' carts/orders by manipulating URL parameters
   - Solution: Backend extracts `userId` from JWT token, never accepts from client

2. **Null Safety for Old Tokens** (`NULL_SAFETY_FIX.md`)
   - Fixed: Old JWT tokens without `userId` claim caused NullPointerException
   - Solution: Added null check with clear error message, returns 401 Unauthorized

3. **Code Review** (`CODE_REVIEW_SECURITY_FIX.md`)
   - Comprehensive security review of authentication and authorization
   - Grade: A (90/100)

### Security Best Practices

**When implementing user-specific features:**
1. Backend: Always use `AuthenticationUtil.getCurrentUserId()` to get authenticated user ID
2. Frontend: Never send `userId` as request parameter
3. Backend: Verify resource ownership in service layer
4. Frontend: Rely on 401 responses to detect auth failures

**Pre-registered Test Users:**
- `demo.account@gmail.com` / `demo123` (CUSTOMER)
- `admin@pandamall.com` / `admin123` (ADMIN)
- VanA / password (ADMIN)
- VanB / password (STAFF)
- VanC / password (CUSTOMER)

## Troubleshooting

### Backend won't start
- Check MySQL is running on port 3306 with database `test_db`
- Check Redis is running (required for logout functionality)
- Verify RapidAPI keys in `application.properties`

### Frontend shows 401 errors
- Ensure backend is running on port 8080
- Check JWT token is valid (may need to re-login)
- Old tokens without `userId` claim require fresh login

### Cart/Order data not showing
- Verify user is logged in (`localStorage` has `token` and `user`)
- Check browser console for API errors
- Ensure backend extracts `userId` from token (not URL params)

### CORS errors
- Backend CORS configured for `localhost:5173` and `localhost:3000`
- Check frontend is running on these ports
- Verify CORS configuration in `Backend/config/CorsConfig.java`

## Additional Documentation

Each subdirectory has its own detailed CLAUDE.md:
- `Backend/CLAUDE.md` - Backend-specific architecture and development
- `Frontend/CLAUDE.md` - Frontend-specific architecture and development

Security fix documentation:
- `SECURITY_FIX_CART_ORDER_ISOLATION.md` - Cart/order isolation fix details
- `NULL_SAFETY_FIX.md` - JWT token null safety fix
- `CODE_REVIEW_SECURITY_FIX.md` - Security review and recommendations
