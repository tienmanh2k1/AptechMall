# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is **PandaMall**, a React-based e-commerce application that integrates with AliExpress API for product browsing and searching. Built with Vite, React Router, Tailwind CSS, and Axios.

## Development Commands

```bash
# Install dependencies (first time setup)
npm install

# Start development server (runs on default Vite port, usually 5173)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Lint codebase
npm run lint
```

## Quick Start

**Full Stack Development:**
1. Start MySQL server (localhost:3306, database: `test_db`)
2. Start Redis server (required for auth logout)
3. Start Backend: `cd ../Backend && ./mvnw spring-boot:run` (runs on port 8080)
4. Start Frontend: `npm install && npm run dev` (runs on port 5173)
5. Access app at `http://localhost:5173`
6. Login with demo account: `demo.account@gmail.com` / `demo123`

**Frontend Only (with Mock API):**
1. Edit `src/config/api.config.js` - set `USE_MOCK_API = true`
2. Run `npm run dev`
3. Cart and order features will use mock data; product search requires backend

## Architecture

### Feature-Based Structure

The codebase uses a feature-based architecture rather than traditional type-based (components, pages, services):

```
src/
├── features/
│   ├── auth/                 # Authentication feature
│   │   ├── components/       # ProtectedRoute
│   │   ├── context/          # AuthContext provider
│   │   ├── pages/            # LoginPage, RegisterPage
│   │   └── services/         # authApi.js
│   ├── cart/                 # Shopping cart feature
│   │   ├── components/       # CartItem, CartSummary, CartEmpty
│   │   ├── context/          # CartContext provider
│   │   ├── pages/            # CartPage, CartDebugPage
│   │   └── services/         # cartApi.js, cartApiMock.js
│   ├── order/                # Order management feature
│   │   ├── components/       # OrderItem, OrderItemsList, OrderStatusBadge
│   │   ├── pages/            # CheckoutPage, CheckoutSuccessPage, OrderListPage, OrderDetailPage
│   │   └── services/         # orderApi.js, orderApiMock.js
│   └── product/              # Product feature module
│       ├── components/       # ProductCard, ProductImages, ProductInfo, ProductAttributes,
│       │                     # ProductVariantSelector, ProductSKU, ProductShipping, ProductSeller
│       │   └── search/       # SearchBar, SearchFilters, SearchResults, SearchSuggestions, SortDropdown
│       ├── pages/            # ProductDetailPage, SearchPage
│       └── services/         # productService.js
├── shared/
│   ├── components/           # Layout, Header, Footer, Loading, ErrorMessage
│   ├── services/             # api.js (Axios instance with interceptors)
│   └── utils/                # formatters.js, constants.js, marketplace.js, storage.js
├── config/                   # api.config.js (mock API toggle)
├── App.jsx                   # Root component with routing
└── main.jsx                  # React entry point
```

### Backend API Integration

The app expects a backend API running on `http://localhost:8080/api` with the following endpoints:

**Product Detail Endpoint (Canonical):**
- `GET /api/{platform}/products/{id}` - Fetch product details
  - `platform` ∈ {aliexpress, 1688}
  - `id` = provider's original product ID (string)
  - Example: `/api/aliexpress/products/1005005244562338`

**Search Endpoint:**
- `GET /api/{platform}/search/simple?keyword=...&page=...&sort=...` - Search products
  - Example: `/api/aliexpress/search/simple?keyword=phone&page=1&sort=0`

**Important**:
- The backend is NOT included in this repository. Start the backend server separately before running the frontend.
- Base URL is `http://localhost:8080/api` - paths should NOT start with `/api` to avoid duplication

### API Service Layer

All API calls go through two service layers:

1. **`src/shared/services/api.js`**: Base Axios instance with:
   - Request/response interceptors
   - Auto-attaches Bearer token from localStorage if available
   - Handles 401 errors by clearing token
   - Console logging for debugging
   - Base URL: `http://localhost:8080/api`

2. **`src/features/product/services/productService.js`**: Product-specific API methods wrapping the base API instance
   - `getProductById(platform, id)` - Calls `/{platform}/products/{id}`
   - `searchProducts(marketplace, keyword, page, filters)` - Calls `/{marketplace}/search/simple`

### Canonical Route Structure

The app uses a **canonical path** where frontend URLs exactly match backend API endpoints:

**Supported Platforms:**
| Platform Value | Display Name |
|---------------|--------------|
| `aliexpress` | AliExpress |
| `1688` | 1688 |

**Canonical Route (Frontend = Backend):**
- **Pattern**: `/{platform}/products/{id}`
- **Examples**:
  - `/aliexpress/products/1005005244562338` (frontend URL = backend API path)
  - `/1688/products/898144857257`

**Benefits:**
- Clean deep linking (URLs are self-describing with embedded marketplace)
- Better SEO (marketplace visible in URL path)
- Simpler caching strategies (CDN can cache by marketplace)
- Clearer logging (platform in every request path)
- No URL transformation needed (frontend path = API path)

**Utilities:** See [Shared Utilities](#shared-utilities) section below for details on:
- `marketplace.js` - Platform validation, globalId parsing, display info
- `constants.js` - App-wide constants (routes, marketplaces, sort options)
- `formatters.js` - Currency and number formatting
- `storage.js` - LocalStorage helpers

### Routing

Uses React Router v6+ with the following routes:

**Public Routes (No Layout):**
- `/login` - Login page
- `/register` - Register page

**Public Routes (With Layout):**
- `/` - Home page
- `/search` - Search page with filters (uses URL search params: `?q=keyword&page=1&sort=relevance`)
- `/:platform/products/:id` - Product detail page (e.g., `/aliexpress/products/1005005244562338`)

**Protected Routes (Require Authentication):**
- `/cart` - Shopping cart page
- `/checkout` - Checkout page
- `/orders/success` - Order confirmation page
- `/orders` - Order history page
- `/orders/:orderId` - Order detail page
- `/cart-debug` - Cart debug page (development only)

**Catch-All:**
- `*` - 404 Not Found page

**Note**: The product detail route uses the canonical path structure where the frontend URL exactly matches the backend API endpoint.

### State Management

No global state management library (Redux, Zustand, etc.) is used. State is managed via:
- Local component state (`useState`)
- URL search parameters for search filters/pagination
- `useParams` for route parameters (`platform` and `id`)
- Backend response `platform` field for marketplace badge display

### Key UI Patterns

**Loading States**: All async operations use the shared `<Loading />` component with custom messages.

**Error Handling**: All errors use the shared `<ErrorMessage />` component with optional retry callback.

**Search Page**: Uses URL search params (`useSearchParams`) to manage filters, sorting, and pagination. This allows:
- Direct linking to search results
- Browser back/forward navigation
- State persistence across page refreshes

### Styling

**Tailwind CSS Configuration:**
- Utility-first CSS framework configured in `tailwind.config.js`
- Custom primary color palette (red shades: 50, 100, 500, 600, 700)
- Scans all `.jsx`, `.tsx`, `.js`, `.ts` files in `src/` for class names
- PostCSS configured with Tailwind and Autoprefixer

**Custom CSS Classes (in `src/index.css`):**
- `.btn-primary` - Red primary button (matches brand color)
- `.btn-secondary` - Gray secondary button
- `.card` - White card with shadow and padding
- Base styles: Light gray background (`bg-gray-50`), dark gray text

**Icons:**
- Uses `lucide-react` package for all icons
- Consistent sizing and styling across components

## Key Implementation Details

### Product Detail Page (`ProductDetailPage.jsx`)

**Key Implementation:**
1. Reads `platform` and `id` from URL route params (`:platform/:id`)
2. Validates platform using `isValidMarketplace()` from marketplace utils
3. If platform or id is missing/invalid, shows validation error
4. Passes platform and id separately to `productService.getProductById(platform, id)`
5. Uses `platform` field from API response for marketplace badge display
6. Displays provider name in error messages for better debugging

**Displays:**
- Product images gallery
- Product info (title, price, ratings with native currency)
- SKU variants
- Shipping information
- Seller details
- Service guarantees
- Marketplace badge (based on `platform` field from backend)

**Response Structure:**
- **Normalized schema**: `platform`, `id`, `title`, `price.{value,currency}`, `images[]`, `shop.{name,id}`, `url`, `attributes`, `badge`, `lastUpdated`
- **Legacy fields** (for compatibility): `result.item`, `result.delivery`, `result.service`, `result.seller`

### Search Page (`SearchPage.jsx`)

Complex page with:
- Search bar with real-time search
- Filters sidebar (category, price range)
- Sort dropdown (relevance, price, ratings)
- Results grid with pagination
- URL-driven state (all filters/page in URL params)

## Important Architecture Details

### Currency Handling
- **Native currency per marketplace**: No currency conversion is performed
- AliExpress products display in USD (`$`)
- 1688 products display in CNY (`¥`)
- Currency symbol and code come from `response.price.currency` field

### Error Handling
Backend returns normalized error codes that frontend must handle:

| Error Code | HTTP Status | Meaning | Frontend Action |
|-----------|-------------|---------|-----------------|
| `VALIDATION_ERROR` | 400 | Missing/invalid platform or id | Show validation error |
| `NOT_FOUND` | 404 | Product not found | Show "Product not found" |
| `UPSTREAM_ERROR` | 502 | Provider API error | Show error with retry |
| `UPSTREAM_TIMEOUT` | 504 | Provider timeout | Show timeout with retry |
| `UPSTREAM_NO_DATA` | 200/502 | Provider returned 205 (no data) | Show "No data available" |

All errors include `provider` field for debugging.

### Product Card Link Generation
Search results **must include both `platform` and `id`** fields for each item. ProductCard builds links as:
- **Canonical path**: Use `platform` and `id` from backend → `/{platform}/products/{id}`
- **Validation**: If platform or id is missing, do not render link; show validation hint instead
- **No defaults**: Never default to AliExpress; always use the item's own platform
- **Deep linking**: URLs are self-describing and work without session context (refresh/share/new tab)

### Analytics
Track platform and id separately:
- **On click**: `{ platform, id, listContext }`
- **On detail load**: `{ platform, id, status, latencyMs }`

## Context Providers

### AuthContext (`features/auth/context/AuthContext.jsx`)
Provides authentication state and methods:
- `user` - Current user object from localStorage
- `token` - JWT access token
- `loading` - Initial auth state loading flag
- `login(token, userData)` - Store token and user, update state
- `logout()` - Clear auth data from localStorage and state
- `updateUser(userData)` - Update user data
- `isAuthenticated()` - Check if user is logged in

**Token Management:**
- Validates token expiry using `jwt-decode` on app initialization
- Auto-clears expired tokens from localStorage
- Token intercepted and attached to API requests via `api.js`

### CartContext (`features/cart/context/CartContext.jsx`)
Provides cart state management:
- `cartCount` - Total item quantity in cart
- `loading` - Cart loading state
- `refreshCart()` - Refresh cart count from API

**Implementation Details:**
- Uses hardcoded `CURRENT_USER_ID = 1` (TODO: replace with actual user ID)
- Fetches cart count on mount and when user logs in
- Resets cart count to 0 when user logs out
- All cart operations should call `refreshCart()` to update count

## Authentication & Authorization

### Protected Routes
The following routes require authentication (wrapped in `<ProtectedRoute>`):
- `/cart` - Cart page
- `/checkout` - Checkout page
- `/orders/success` - Order success page
- `/orders` - Order list page
- `/orders/:orderId` - Order detail page
- `/cart-debug` - Cart debug page

**ProtectedRoute Component:**
- Checks `isAuthenticated()` from AuthContext
- Redirects to `/login` if not authenticated
- Shows loading state while auth is initializing

### Backend Integration
**Authentication Endpoints (from Backend):**
- `POST /api/auth/login` - Login with username/email + password
- `POST /api/auth/register` - Register new user
- `POST /api/auth/logout` - Logout (blacklists token in Redis)
- `POST /api/auth/refresh` - Refresh access token

**Pre-registered Demo Users (Backend):**
- `admin@pandamall.com` / `admin123` (ADMIN role)
- `demo.account@gmail.com` / `demo123` (CUSTOMER role)
- VanA / password (ADMIN), VanB / password (STAFF), VanC / password (CUSTOMER)

**Token System:**
- Access tokens: 5-minute TTL
- Refresh tokens: 8-day TTL (stored in httpOnly cookies)
- Tokens contain: role, email, fullname, status, username
- 401 responses automatically clear token and redirect to `/login` (handled in `api.js`)

## Mock API Configuration

The app supports switching between real and mock APIs via `src/config/api.config.js`:
- Set `USE_MOCK_API = true` to use mock API (for testing without backend)
- Set `USE_MOCK_API = false` to use real backend API
- Mock implementations available for cart and order services

## Backend Architecture (Java Spring Boot)

The backend is a separate Spring Boot 3.5.6 application located in `../Backend/`:

**Tech Stack:**
- Java 17, Spring Boot 3.5.6, MySQL 8, Redis
- Spring Security + JWT authentication
- Spring Data JPA, WebFlux (for external API calls)

**Run Backend:**
```bash
cd ../Backend
./mvnw spring-boot:run
```

**Prerequisites:**
- MySQL server on localhost:3306 with database `test_db`
- Redis server running (required for token blacklist)
- RapidAPI keys configured in application.properties

**Backend Endpoints:**
- `/api/auth/*` - Authentication (login, register, logout, refresh)
- `/api/users/**` - User management (ADMIN/STAFF only)
- `/api/cart/*` - Cart operations (requires auth)
- `/api/orders/*` - Order management (requires auth)
- `/api/{platform}/products/{id}` - Product detail
- `/api/{platform}/search/simple` - Product search
- `/api/debug/**` - Debug endpoints

## Shared Utilities

### Marketplace Utilities (`shared/utils/marketplace.js`)

**Constants:**
- `MARKETPLACE` - Canonical platform enum: `{ ALIEXPRESS: 'aliexpress', ALIBABA_1688: '1688' }`
- `MARKETPLACE_DISPLAY_NAME` - UI display names for each platform
- `MARKETPLACE_COLORS` - Tailwind color classes for platform badges
- `MARKETPLACE_PREFIX` - Prefix mapping for globalId format (ae, ali, a1688)
- `MARKETPLACE_ALIASES` - All recognized aliases for backward compatibility

**Key Functions:**
- `isValidMarketplace(marketplace)` - Validate platform enum value
- `getMarketplaceInfo(marketplace)` - Returns `{ name, colors }` for UI display
- `parseGlobalId(globalId)` - Parse namespaced ID (format: `"ae:1005005244562338"`)
  - Returns: `{ marketplace, productId, prefix, isValid, error? }`
- `buildGlobalId(marketplace, productId)` - Build canonical globalId
- `normalizeMarketplaceAlias(alias)` - Convert alias to canonical marketplace
- `legacyToCanonical(productId)` - Convert legacy ID to canonical format (defaults to AliExpress)

**Usage Example:**
```javascript
import { MARKETPLACE, isValidMarketplace, getMarketplaceInfo } from '@/shared/utils/marketplace';

// Validate platform from URL params
if (!isValidMarketplace(platform)) {
  setError('Invalid marketplace');
  return;
}

// Get display info for badge
const { name, colors } = getMarketplaceInfo(MARKETPLACE.ALIEXPRESS);
// Returns: { name: 'AliExpress', colors: { bg: 'bg-red-500', text: 'text-white' } }
```

### Constants (`shared/utils/constants.js`)

**App Configuration:**
- `APP_NAME` - 'PandaMall'
- `API_TIMEOUT` - 10000ms
- `DEFAULT_MARKETPLACE` - 'aliexpress'

**Marketplace Configuration:**
- `MARKETPLACES` - Platform enum (same as `MARKETPLACE` in marketplace.js)
- `MARKETPLACE_CONFIG` - Extended config with label and color classes

**Sort Options:**
- `SORT_OPTIONS` - Maps frontend strings to backend integers:
  - `'default'` → 0 (Most Relevant)
  - `'price-asc'` → 1 (Price: Low to High)
  - `'price-desc'` → 2 (Price: High to Low)
  - `'sales'` → 3 (Best Selling)
- `SORT_LABELS` - Display labels for each sort option

**Routes:**
- `ROUTES` - Route path constants (HOME, PRODUCT_DETAIL, CART, etc.)

### Formatters (`shared/utils/formatters.js`)

Utility functions for formatting currency, numbers, dates, etc. (Implementation details to be discovered as needed)

### Storage (`shared/utils/storage.js`)

LocalStorage helper functions for consistent data access patterns (Implementation details to be discovered as needed)

## Notes

- The app uses React 19 with StrictMode enabled
- All API responses are logged to console for debugging
- API base URL must be `http://localhost:8080/api` - ensure paths don't duplicate `/api` prefix
- Context providers wrap the entire app: AuthProvider → CartProvider → Routes
- Cart operations use a hardcoded user ID (TODO: integrate with AuthContext)
- ESLint configured with React Hooks and React Refresh plugins