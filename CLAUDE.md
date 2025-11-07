# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Aptechmall** is a full-stack e-commerce application that integrates with external marketplace APIs (AliExpress, Alibaba 1688) to provide product search and ordering capabilities. The project consists of a Java Spring Boot backend and a React frontend.

**Repository Structure:**
- `Backend/` - Spring Boot 3.5.6 REST API (Java 17)
- `Frontend/` - React 19 + Vite SPA (Node.js/npm)

## Key Features

### 1. Product Integration
- **Marketplace APIs:** AliExpress & Alibaba 1688 integration
- **Auto-Translation:** English/Chinese → Vietnamese product content
- **Smart Caching:** 7-day localStorage cache for translations
- **Rate Limit Handling:** 500ms delay between translation requests

### 2. E-Wallet System
- **Wallet Management:** User balance, deposits, transaction history
- **Payment Gateways:** VNPay, MoMo, ZaloPay, Bank Transfer
- **SMS Integration:** Automatic deposits via bank SMS forwarding
- **Admin Controls:** Lock/unlock wallets, manual adjustments

### 3. Authentication
- **JWT-Based Auth:** 5-minute access tokens, 8-day refresh tokens
- **Google OAuth:** Social login integration
- **Role-Based Access:** ADMIN, STAFF, CUSTOMER roles
- **Dual Portals:** Separate login pages for customers (`/login`) and admin (`/admin/login`)

### 4. Admin Dashboard
- **User Management:** View, edit roles, manage accounts
- **Order Management:** View all orders, update status, manage fees
- **Wallet Admin:** View user wallets, process adjustments
- **Fee Management:** Configure order processing fees

### 5. Shopping Features
- **Cart System:** User-specific carts with security isolation
- **Order Processing:** Checkout, payment, status tracking
- **Order Fees:** Configurable processing fees per order

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
- React 19 + Vite 7
- React Router v7
- Axios (API client)
- Tailwind CSS + lucide-react icons
- Context API for state management
- react-toastify (notifications)
- @react-oauth/google (Google login)
- qrcode.react (QR code generation)
- jwt-decode (token validation)

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
All cart, order, and wallet endpoints require authentication:
- `/api/cart/**` - Cart operations
- `/api/orders/**` - Order management
- `/api/wallet/**` - Wallet operations (balance, deposits, transactions)
- `/api/admin/**` - Admin operations (requires ADMIN or STAFF role)
- Backend extracts `userId` from JWT token (via `AuthenticationUtil.getCurrentUserId()`)
- Frontend sends JWT token in `Authorization` header (automatically via Axios interceptor)

**Public Endpoints:**
- `/api/auth/*` - Authentication (login, register, logout, refresh, Google OAuth)
- `/api/{platform}/products/{id}` - Product detail
- `/api/{platform}/search/simple` - Product search
- `/api/bank-transfer/**` - Bank SMS webhook (for wallet deposits)
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

## Feature-Specific Guides

### Translation Feature

**Auto-translate product content from English/Chinese to Vietnamese.**

**Location:** `Frontend/src/features/translation/`

**Key Components:**
- `hooks/useProductTranslation.js` - React hook for auto-translation
- `components/TranslationToggle.jsx` - UI toggle for language switching
- `services/translationApi.js` - RapidAPI Google Translator integration
- `services/translationCache.js` - 7-day localStorage caching

**Usage in Product Pages:**
```javascript
import useProductTranslation from '../../translation/hooks/useProductTranslation';
import TranslationToggle from '../../translation/components/TranslationToggle';

const { displayProduct, isTranslating, showOriginal, toggleLanguage } =
  useProductTranslation(product, platform, { autoTranslate: true });
```

**Performance:**
- First visit: ~15-20 seconds (translates all fields)
- Cached visits: <100ms (instant from localStorage)
- Translation rate limit: 500ms delay between requests

**API Configuration:**
- Provider: RapidAPI Free Google Translator
- Rate limit: 100-500 requests/day (free tier)
- Supported: `en→vi` (AliExpress), `zh-CN→vi` (1688)

**Documentation:** `TRANSLATION_FEATURE_COMPLETE.md`, `Frontend/src/features/translation/README.md`

### E-Wallet System

**Digital wallet for user deposits, payments, and transaction history.**

**Location:** `Backend/src/main/java/com/aptech/aptechMall/.../wallet/`

**Key Endpoints:**
- `GET /api/wallet` - Get wallet balance and info
- `POST /api/wallet/deposit/initiate` - Start deposit (redirects to payment gateway)
- `POST /api/wallet/deposit/callback` - Process payment callback
- `GET /api/wallet/transactions` - Get transaction history
- `GET /api/wallet/transactions/{id}` - Get specific transaction
- Admin endpoints: Lock/unlock wallet, manual adjustments

**Payment Gateways Supported:**
- VNPay, MoMo, ZaloPay (Vietnamese gateways)
- Bank Transfer (via SMS integration)
- Admin Manual (for manual adjustments)

**Transaction Types:**
- `DEPOSIT` - Add money to wallet
- `WITHDRAWAL` - Remove money from wallet
- `ORDER_PAYMENT` - Pay for order using wallet balance
- `ORDER_REFUND` - Refund to wallet when order cancelled
- `ADMIN_ADJUSTMENT` - Admin manual balance changes

**Security:**
- All wallet operations extract `userId` from JWT token
- Never accept `userId` from client parameters
- Transaction history includes `balance_before` and `balance_after` for audit trail
- Admin can lock wallets to prevent fraudulent activity

**Documentation:** `Backend/WALLET_FEATURE_IMPLEMENTATION.md`

### Bank Transfer SMS Integration

**Automatic wallet deposits via SMS forwarding from bank.**

**⚠️ Development/Testing Feature Only** - Use official payment gateways in production.

**How it works:**
1. User transfers money to your bank account with note: `Nap tien USER{userId}`
2. Bank sends SMS notification to your phone
3. SMS forwarding app (e.g., "SMS Forwarder") sends SMS to webhook
4. Backend parses SMS, extracts amount and user ID
5. Automatically credits user's wallet

**Webhook Endpoint:**
```bash
POST /api/bank-transfer/sms-webhook?sender=VIETCOMBANK&message=...
```

**SMS Format Examples:**
- `TK 1234567890 +500,000 VND. GD: 987654. ND: Nap tien USER123`
- `+500000d GD:987654 ND:NAPTIEN USER123`
- `GD 100k` (for testing)

**Parser Features:**
- Extracts amount (supports: `500,000`, `500k`, `500000d`)
- Extracts transaction reference (`GD: 123456`)
- Extracts user ID from content (`USER123` → `123`)
- Duplicate detection (same GD number rejected)

**Admin Monitoring:**
- `GET /api/bank-transfer/sms` - View all SMS
- `GET /api/bank-transfer/sms/errors` - View failed SMS
- `GET /api/bank-transfer/process-pending` - Retry unprocessed SMS

**Documentation:** `BANK_TRANSFER_SMS_INTEGRATION.md`, `BANK_TRANSFER_FLOW_DIAGRAM.md`

### Admin Portal

**Separate login portal for ADMIN and STAFF users.**

**Access:**
- URL: `http://localhost:5173/admin/login`
- From homepage: Scroll to footer → Click "Admin Portal"

**Features:**
- **Separate from customer login** - Different URL (`/admin/login` vs `/login`)
- **Role validation** - Only ADMIN and STAFF can login
- **Auto-redirect** - Successful login → `/admin/dashboard`
- **Dark theme UI** - Professional admin interface

**Admin Accounts:**
- `admin@pandamall.com` / `admin123` (ADMIN)
- `VanA` / `password` (ADMIN)
- `VanB` / `password` (STAFF)

**IMPORTANT:** Customer accounts (role: CUSTOMER) cannot access admin portal. They will receive "Access denied" error.

**Admin Dashboard Features:**
- User management (view, edit roles, manage accounts)
- Order management (view all orders, update status, manage fees)
- Wallet administration (view user wallets, lock/unlock, adjustments)
- Fee configuration (set order processing fees)

**Google OAuth Integration:**
- Google login available on customer login page (`/login`)
- Backend endpoint: `POST /api/auth/google`
- Requires `@react-oauth/google` package (already installed)

**Documentation:** `ADMIN_LOGIN_GUIDE.md`, `ADMIN_LOGIN_FIX.md`, `GOOGLE_LOGIN_MIGRATION.md`

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

*For Customer Portal (`/login`):*
- `demo.account@gmail.com` / `demo123` (CUSTOMER)
- VanC / password (CUSTOMER)

*For Admin Portal (`/admin/login`):*
- `admin@pandamall.com` / `admin123` (ADMIN)
- VanA / password (ADMIN)
- VanB / password (STAFF)

**Note:** Admin and Staff users can use either portal, but Customer users are restricted to customer portal only.

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

### Translation not working
- Check RapidAPI key in `Frontend/src/features/translation/services/translationApi.js`
- Verify rate limit not exceeded (100-500 requests/day free tier)
- Check console for translation errors
- Clear localStorage cache: `localStorage.clear()`
- First translation takes ~15-20 seconds (expected behavior)

### Wallet deposit fails
- Ensure user is logged in with valid JWT token
- Check payment gateway configuration in `Backend/application.properties`
- For SMS deposits: Verify SMS format matches supported patterns
- Check wallet is not locked: `GET /api/wallet`
- Review error logs: `GET /api/bank-transfer/sms/errors`

### Admin portal access denied
- Verify account role is ADMIN or STAFF (not CUSTOMER)
- Check database: `SELECT role FROM users WHERE email = 'your-email'`
- Customer accounts cannot access `/admin/login` (by design)
- Use customer login at `/login` instead

## Quick Reference

### Common Workflows

**Testing a full purchase flow:**
```bash
# 1. Start both servers
cd Backend && ./mvnw spring-boot:run &
cd Frontend && npm run dev

# 2. Login as customer
Open http://localhost:5173/login
Email: demo.account@gmail.com / demo123

# 3. Browse products → Add to cart → Checkout → Pay with wallet
```

**Testing translation feature:**
```bash
# Navigate to any product
http://localhost:5173/aliexpress/products/1005005244562338

# Translation auto-starts (15-20 sec first time)
# Toggle between EN/VI using button
# Second visit loads instantly from cache
```

**Testing wallet deposit:**
```bash
# Option 1: Via SMS (testing)
curl "http://localhost:8080/api/bank-transfer/sms-webhook?sender=TEST&message=GD+100k"

# Option 2: Via payment gateway
Login → Navigate to wallet → Deposit → Select gateway → Complete payment
```

**Accessing admin dashboard:**
```bash
# Navigate to admin login
http://localhost:5173/admin/login

# Login with admin account
Email: admin@pandamall.com / admin123

# Access features
- View all orders
- Manage users
- View wallets
- Configure fees
```

### Key File Locations

**Backend:**
- Controllers: `Backend/src/main/java/com/aptech/aptechMall/Controller/`
- Services: `Backend/src/main/java/com/aptech/aptechMall/service/`
- Entities: `Backend/src/main/java/com/aptech/aptechMall/entity/`
- Security: `Backend/src/main/java/com/aptech/aptechMall/config/SecurityConfig.java`
- Auth utils: `Backend/src/main/java/com/aptech/aptechMall/util/AuthenticationUtil.java`

**Frontend:**
- Features: `Frontend/src/features/{auth,cart,order,product,translation,admin}/`
- API client: `Frontend/src/shared/services/api.js`
- Contexts: `Frontend/src/features/{auth,cart}/context/`
- Routes: `Frontend/src/App.jsx`

## Additional Documentation

### Core Documentation
- `Backend/CLAUDE.md` - Backend-specific architecture and development
- `Frontend/CLAUDE.md` - Frontend-specific architecture and development
- `TESTING_GUIDE.md` - Testing procedures and checklists

### Feature Documentation
- `TRANSLATION_FEATURE_COMPLETE.md` - Auto-translation implementation details
- `Backend/WALLET_FEATURE_IMPLEMENTATION.md` - E-wallet system architecture
- `BANK_TRANSFER_SMS_INTEGRATION.md` - SMS-based deposit integration
- `BANK_TRANSFER_FLOW_DIAGRAM.md` - SMS webhook flow diagram
- `ADMIN_LOGIN_GUIDE.md` - Admin portal access and usage
- `GOOGLE_LOGIN_MIGRATION.md` - Google OAuth integration details

### Security Documentation
- `Backend/SECURITY_FIX_CART_ORDER_ISOLATION.md` - Cart/order isolation fix
- `Backend/SECURITY_FIX_REQUIREMENTS.md` - Security requirements checklist

### Project Status & Fixes
- `PROJECT_FLOWS_STATUS_REPORT.md` - Overall project status
- `IMPLEMENTATION_SUMMARY.md` - Implementation milestones
- `CHECKOUT_FLOW_BUGS_REPORT.md` - Known issues and fixes
- `ADMIN_LOGIN_FIX.md` - Admin login troubleshooting
