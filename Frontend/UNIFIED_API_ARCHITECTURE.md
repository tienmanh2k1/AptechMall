# Unified API Architecture - Marketplace Implementation

## Overview

This document describes the **unified API architecture** for PandaMall's multi-marketplace integration (AliExpress, Taobao, 1688). The backend provides a single, normalized contract that accepts Global IDs, eliminating the need for marketplace-specific endpoints on the frontend.

## Architecture Principles

### 1. Single Source of Truth
- Backend is the authoritative source for marketplace/platform information
- Frontend validates globalId format for UX, but backend controls data resolution
- Platform field from API response always takes precedence

### 2. Unified Contract
- One endpoint for product details: `/api/products/{globalId}`
- Normalized response schema across all marketplaces
- Native currency per marketplace (no conversion)

### 3. Clean Separation of Concerns
- **Frontend**: GlobalId parsing, validation, and routing
- **Backend**: Provider resolution, data normalization, caching
- **Provider**: Native marketplace APIs (AliExpress, Taobao, 1688)

## Global ID Format

**Namespaced Format:** `{prefix}:{providerId}`

| Marketplace | Prefix | Example |
|------------|--------|---------|
| AliExpress | `ae` | `ae:1005005244562338` |
| Taobao | `tb` | `tb:6543210987` |
| 1688 | `a1688` | `a1688:898144857257` |

**URL Pattern:** `/product/{globalId}`

Examples:
- `/product/ae:1005005244562338` → AliExpress product
- `/product/tb:6543210987` → Taobao product
- `/product/a1688:898144857257` → 1688 product

## API Contracts

### Product Detail Endpoint

**Request:**
```
GET /api/products/{globalId}
```

**Unified Response Schema:**
```json
{
  "platform": "aliexpress" | "taobao" | "1688",
  "id": "1005005244562338",
  "globalId": "ae:1005005244562338",
  "title": "Product Title",
  "price": {
    "value": "29.99",
    "currency": "USD"
  },
  "images": ["url1", "url2"],
  "shop": {
    "name": "Shop Name",
    "id": "12345"
  },
  "url": "https://...",
  "attributes": {},
  "badge": "Hot Sale",
  "lastUpdated": "2025-10-22T12:00:00Z",

  // Legacy fields for backward compatibility (optional)
  "result": {
    "item": { /* existing structure */ },
    "delivery": { /* existing structure */ },
    "service": [ /* existing structure */ ],
    "seller": { /* existing structure */ }
  }
}
```

**Key Fields:**
- `platform`: Canonical marketplace enum (`aliexpress`, `taobao`, `1688`)
- `globalId`: Echo back the globalId for verification
- `price.currency`: Native currency (USD for AliExpress, CNY for Taobao/1688)
- `lastUpdated`: Timestamp for cache invalidation

### Search Endpoint

**Request:**
```
GET /api/{marketplace}/search/simple?keyword={keyword}&page={page}&sort={sort}
```

**Unified Response Schema:**
```json
{
  "platform": "aliexpress",
  "items": [
    {
      "id": "1005005244562338",
      "globalId": "ae:1005005244562338",
      "title": "Product Title",
      "price": {
        "value": "29.99",
        "currency": "USD"
      },
      "thumb": "thumbnail_url",
      "url": "product_url",
      "shop": {
        "name": "Shop Name"
      }
    }
  ],
  "badge": "Trending",
  "nextPage": 2,

  // Legacy fields for backward compatibility (optional)
  "result": {
    "items": [ /* existing itemId, imageUrl, etc */ ]
  }
}
```

**Important:** Backend should include `globalId` in each search result item for direct linking.

## Error Handling (Normalized)

### Error Response Format
```json
{
  "code": "VALIDATION_ERROR" | "NOT_FOUND" | "UPSTREAM_ERROR" | "UPSTREAM_TIMEOUT" | "UPSTREAM_NO_DATA",
  "message": "Human-readable error message",
  "provider": "aliexpress" | "taobao" | "1688",
  "globalId": "ae:1005005244562338",
  "timestamp": "2025-10-22T12:00:00Z"
}
```

### Error Codes

| HTTP Status | Error Code | Description | Frontend Behavior |
|------------|------------|-------------|-------------------|
| 400 | `VALIDATION_ERROR` | Invalid globalId format | Show validation error with retry |
| 404 | `NOT_FOUND` | Product not found at provider | Show "Product not found" |
| 502 | `UPSTREAM_ERROR` | Provider API error | Show provider-specific error with retry |
| 504 | `UPSTREAM_TIMEOUT` | Provider timeout | Show timeout error with retry |
| 200/502* | `UPSTREAM_NO_DATA` | Provider returned 205 (no data) | Policy-based: show error or empty state |

**Special Case - Provider 205 Response:**
- Backend receives HTTP 205 from provider (no content/data available)
- Backend maps to `UPSTREAM_NO_DATA` error code
- Backend decides policy: Return HTTP 200 with error body OR HTTP 502
- Frontend always checks for `code: "UPSTREAM_NO_DATA"` regardless of status
- Error message includes provider name

### Frontend Error Handling

**ProductService (`src/features/product/services/productService.js`):**

```javascript
try {
  const response = await api.get(`/api/products/${globalId}`);
  return response.data;
} catch (error) {
  const errorData = error.response?.data;

  // Map status codes to user-friendly errors
  if (statusCode === 400) {
    throw new Error('VALIDATION_ERROR: Invalid global ID');
  } else if (errorData?.code === 'UPSTREAM_NO_DATA') {
    throw new Error(`UPSTREAM_NO_DATA: Provider returned no data`);
  }
  // ... other error codes
}
```

## Observability & Logging

### Required Logging Fields

**Product Detail Load:**
```javascript
{
  route: '/api/products/{globalId}',
  globalId: 'ae:1005005244562338',
  platform: 'aliexpress',
  status: 200,
  latencyMs: 145,
  fromCache: true,
  priceCurrency: 'USD'
}
```

**Product Detail Error:**
```javascript
{
  route: '/api/products/{globalId}',
  globalId: 'ae:1005005244562338',
  status: 502,
  latencyMs: 3021,
  errorCode: 'UPSTREAM_ERROR',
  provider: 'aliexpress'
}
```

**Search Query:**
```javascript
{
  route: '/aliexpress/search/simple',
  platform: 'aliexpress',
  keyword: 'phone',
  page: 1,
  status: 200,
  latencyMs: 234,
  resultCount: 40,
  fromCache: false
}
```

### Analytics Events

**Product View:**
```javascript
analytics.track('product_viewed', {
  globalId: 'ae:1005005244562338',
  platform: 'aliexpress',
  source: 'search' | 'direct' | 'share',
  loadTimeMs: 145
});
```

**Funnel Integrity Check:**
```javascript
// Verify marketplace consistency from search to detail
if (clickPlatform !== detailPlatform) {
  console.error('Platform mismatch detected', {
    expected: clickPlatform,
    actual: detailPlatform,
    globalId
  });
}
```

## Migration Strategy (30-Second Window)

### Legacy Route Support

**Legacy Endpoints:**
```
GET /api/{marketplace}/products/{productId}
```

Examples:
- `/api/aliexpress/products/1005005244562338`
- `/api/taobao/products/6543210987`

### Migration Timeline

#### Phase 1: T0 to T0+30 seconds
**Behavior:**
- Legacy routes remain active and proxied to unified endpoint
- Return normalized schema (same as `/api/products/{globalId}`)
- Add deprecation headers:
  ```
  Deprecation: true
  Sunset: Wed, 22 Oct 2025 14:00:00 GMT
  Link: </api/products/ae:1005005244562338>; rel="alternate"
  ```

#### Phase 2: After T0+30 seconds
**Two Policy Options:**

**Option A - Redirect (Recommended):**
```
HTTP 301 Moved Permanently
Location: /api/products/ae:1005005244562338
```

**Option B - Hard Retirement:**
```
HTTP 410 Gone
{
  "code": "ENDPOINT_RETIRED",
  "message": "This endpoint has been retired. Use /api/products/{globalId}",
  "alternateUrl": "/api/products/ae:1005005244562338"
}
```

### Frontend Migration

**All new UI links MUST use globalId from day one:**
- ✅ `/product/ae:1005005244562338`
- ❌ `/product/1005005244562338` (legacy)

**Frontend handles legacy format:**
- User visits `/product/1005005244562338`
- Frontend detects legacy format
- Shows conversion banner
- Redirects to `/product/ae:1005005244562338`
- Backend never receives legacy requests from UI

## Implementation Details

### Service Layer (`productService.js`)

**Key Changes:**
1. `getProductById(globalId)` - Single parameter (globalId)
2. Calls `/api/products/{globalId}` - No splitting required
3. Logs observability data (latency, cache status, currency)
4. Enhanced error handling with provider context

```javascript
// Before (marketplace-specific)
await api.get(`/${marketplace}/products/${productId}`);

// After (unified)
await api.get(`/api/products/${globalId}`);
```

### Product Detail Page (`ProductDetailPage.jsx`)

**Flow:**
1. **Frontend Validation**: Parse globalId for format validation
2. **Legacy Detection**: Redirect legacy URLs to canonical format
3. **API Call**: Pass full globalId to service (no splitting)
4. **Platform from API**: Use `platform` field from response for badge
5. **Error Display**: Show provider name in error messages

```javascript
// Fetch with full globalId
const data = await productService.getProductById(id); // id = "ae:123"

// Use platform from response
if (data?.platform) {
  setMarketplace(data.platform);
}
```

### Product Card (`ProductCard.jsx`)

**Priority Order for Links:**
1. **Preferred**: Use `globalId` from backend search response
2. **Fallback**: Build `globalId` from `marketplace + itemId`
3. **Legacy**: Use `itemId` only (should rarely happen)

```javascript
if (globalId) {
  productLink = `/product/${globalId}`; // Backend provided
} else if (marketplace && itemId) {
  productLink = `/product/${buildGlobalId(marketplace, itemId)}`; // Build
} else {
  productLink = `/product/${itemId}`; // Legacy
}
```

## Testing Scenarios

### ✅ Acceptance Criteria

1. **Unified Endpoint**
   - [ ] Product detail calls `/api/products/{globalId}` with full globalId
   - [ ] Response includes `platform` field
   - [ ] Frontend uses platform from API for badge

2. **Native Currency**
   - [ ] AliExpress products show USD
   - [ ] Taobao products show CNY
   - [ ] 1688 products show CNY
   - [ ] No currency conversion occurs

3. **Error Handling**
   - [ ] `VALIDATION_ERROR` (400) → Clear frontend error
   - [ ] `NOT_FOUND` (404) → "Product not found"
   - [ ] `UPSTREAM_ERROR` (502) → Show provider name with retry
   - [ ] `UPSTREAM_TIMEOUT` (504) → Show timeout with retry
   - [ ] `UPSTREAM_NO_DATA` → Show "no data available" error

4. **Observability**
   - [ ] Logs include `latencyMs` for all requests
   - [ ] Logs include `fromCache` status
   - [ ] Logs include `priceCurrency` for products
   - [ ] Error logs include `provider` field

5. **Legacy Migration**
   - [ ] Frontend redirects legacy URLs to canonical
   - [ ] Backend proxies legacy endpoints during window
   - [ ] Deprecation headers present
   - [ ] After window: 301 redirect or 410 gone

6. **Deep Linking**
   - [ ] Refresh preserves marketplace
   - [ ] Bookmark works correctly
   - [ ] Share link includes platform badge

### Manual Test Cases

**Test Case 1: Unified Endpoint**
```
1. Open: /product/ae:1005005244562338
2. Verify: Network tab shows GET /api/products/ae:1005005244562338
3. Verify: Response includes "platform": "aliexpress"
4. Verify: Badge shows "AliExpress"
5. Verify: Currency is USD
```

**Test Case 2: Error Handling**
```
1. Open: /product/ae:999999999
2. Verify: Shows "NOT_FOUND: Product not found"
3. Verify: Console logs include provider: 'aliexpress'
4. Click retry
5. Verify: Makes new API call
```

**Test Case 3: Search to Detail**
```
1. Search for "phone" in Taobao
2. Verify: Search results include globalId field
3. Click product
4. Verify: URL is /product/tb:{id}
5. Verify: Detail shows "Taobao" badge
6. Verify: Currency is CNY
```

**Test Case 4: UPSTREAM_NO_DATA**
```
1. Backend mocks 205 from provider
2. Backend returns { code: 'UPSTREAM_NO_DATA', provider: 'taobao' }
3. Frontend shows: "Provider returned no data (Provider: taobao)"
4. Verify: Error includes provider name
```

## Backend Requirements

### Unified Endpoint Implementation

```javascript
// Pseudo-code for backend
app.get('/api/products/:globalId', async (req, res) => {
  const { globalId } = req.params;

  // Parse globalId to extract marketplace and providerId
  const { marketplace, providerId } = parseGlobalId(globalId);

  // Validate
  if (!marketplace || !providerId) {
    return res.status(400).json({
      code: 'VALIDATION_ERROR',
      message: 'Invalid globalId format',
      globalId
    });
  }

  // Fetch from provider
  try {
    const providerData = await providers[marketplace].getProduct(providerId);

    // Handle 205 response
    if (providerData.statusCode === 205) {
      return res.status(502).json({
        code: 'UPSTREAM_NO_DATA',
        message: 'Provider returned no data',
        provider: marketplace,
        globalId
      });
    }

    // Normalize response
    const normalized = {
      platform: marketplace,
      id: providerId,
      globalId,
      title: providerData.title,
      price: {
        value: providerData.price,
        currency: getCurrency(marketplace) // USD, CNY
      },
      images: providerData.images,
      // ... rest of normalized schema
    };

    res.json(normalized);
  } catch (error) {
    // Handle errors with proper codes
    res.status(502).json({
      code: 'UPSTREAM_ERROR',
      message: error.message,
      provider: marketplace,
      globalId
    });
  }
});
```

### Legacy Endpoint Proxy

```javascript
// Proxy legacy routes during migration window
app.get('/api/:marketplace/products/:productId', async (req, res) => {
  const { marketplace, productId } = req.params;
  const globalId = buildGlobalId(marketplace, productId);

  // Add deprecation headers
  res.setHeader('Deprecation', 'true');
  res.setHeader('Sunset', 'Wed, 22 Oct 2025 14:00:00 GMT');
  res.setHeader('Link', `</api/products/${globalId}>; rel="alternate"`);

  // Check if within 30-second window
  if (isWithinMigrationWindow()) {
    // Proxy to unified endpoint
    return proxyToUnified(globalId, res);
  } else {
    // Redirect or retire
    return res.redirect(301, `/api/products/${globalId}`);
  }
});
```

## Benefits of Unified Architecture

### 1. Simplified Frontend
- ✅ One service method for all marketplaces
- ✅ No marketplace branching logic
- ✅ Easier testing and maintenance

### 2. Backend Control
- ✅ Backend owns provider resolution
- ✅ Centralized caching strategy
- ✅ Easier to add new marketplaces

### 3. Consistent UX
- ✅ Normalized schema across platforms
- ✅ Consistent error handling
- ✅ Native currency per marketplace

### 4. Better Observability
- ✅ Single endpoint to monitor
- ✅ Consistent logging format
- ✅ Easier performance tracking

### 5. Clean Migration Path
- ✅ Legacy support during transition
- ✅ Clear deprecation timeline
- ✅ No breaking changes for users

## File Changes Summary

### Modified Files

**`src/features/product/services/productService.js`:**
- Changed: `getProductById(globalId)` - Single parameter
- Changed: Calls `/api/products/{globalId}` - Unified endpoint
- Added: Enhanced error handling with normalized codes
- Added: Observability logging (latency, cache, currency)

**`src/features/product/pages/ProductDetailPage.jsx`:**
- Changed: Passes full globalId to service (no splitting)
- Changed: Uses `platform` field from API response for badge
- Added: Provider name in error messages

**`src/features/product/components/ProductCard.jsx`:**
- Changed: Prefers `globalId` from backend
- Fallback: Builds globalId if not provided
- Last resort: Legacy format

### New Files

**`UNIFIED_API_ARCHITECTURE.md`:**
- Complete architecture documentation
- API contracts and schemas
- Error handling guide
- Migration strategy
- Testing scenarios

## References

- Global ID Format: `MARKETPLACE_IMPLEMENTATION.md`
- Frontend Utilities: `src/shared/utils/marketplace.js`
- Original Requirements: User message with unified contract
- Backend API: `/api/products/{globalId}`

---

**Implementation Date:** 2025-10-22
**Version:** 2.0 (Unified API)
**Status:** ✅ Complete
**Migration Window:** 30 seconds from deployment
