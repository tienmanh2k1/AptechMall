# Testing Checklist - Unified API Architecture

## Pre-Testing Setup

### Backend Requirements
Ensure your backend supports:
- ✅ Unified endpoint: `GET /api/products/{globalId}`
- ✅ Search endpoint returns `globalId` in each result
- ✅ Response includes `platform` field
- ✅ Native currency per marketplace (`USD` for AliExpress, `CNY` for Taobao/1688)
- ✅ Normalized error codes: `VALIDATION_ERROR`, `NOT_FOUND`, `UPSTREAM_ERROR`, `UPSTREAM_TIMEOUT`, `UPSTREAM_NO_DATA`

## 1. Unified Endpoint Testing

### Test 1.1: AliExpress Product Detail
- [ ] Open: `http://localhost:5174/product/ae:1005005244562338`
- [ ] Verify: Network tab shows `GET /api/products/ae:1005005244562338`
- [ ] Verify: Response includes `"platform": "aliexpress"`
- [ ] Verify: Badge shows "AliExpress" (red background)
- [ ] Verify: Currency is `USD`
- [ ] Console log shows:
  ```javascript
  {
    route: '/api/products/ae:1005005244562338',
    globalId: 'ae:1005005244562338',
    platform: 'aliexpress',
    status: 200,
    latencyMs: <number>,
    fromCache: <boolean>,
    priceCurrency: 'USD'
  }
  ```

### Test 1.2: Taobao Product Detail
- [ ] Open: `http://localhost:5174/product/tb:6543210987`
- [ ] Verify: Network tab shows `GET /api/products/tb:6543210987`
- [ ] Verify: Response includes `"platform": "taobao"`
- [ ] Verify: Badge shows "Taobao" (orange background)
- [ ] Verify: Currency is `CNY`

### Test 1.3: 1688 Product Detail
- [ ] Open: `http://localhost:5174/product/a1688:898144857257`
- [ ] Verify: Network tab shows `GET /api/products/a1688:898144857257`
- [ ] Verify: Response includes `"platform": "1688"`
- [ ] Verify: Badge shows "1688" (blue background)
- [ ] Verify: Currency is `CNY`

## 2. Error Handling Testing

### Test 2.1: Validation Error (Invalid Prefix)
- [ ] Open: `http://localhost:5174/product/xx:123456`
- [ ] Verify: Shows error: `"VALIDATION_ERROR: Unsupported marketplace prefix 'xx'"`
- [ ] Verify: Error displayed in ErrorMessage component
- [ ] Verify: Retry button available

### Test 2.2: Validation Error (Malformed ID)
- [ ] Open: `http://localhost:5174/product/ae:`
- [ ] Verify: Shows error: `"VALIDATION_ERROR: Product ID cannot be empty"`

### Test 2.3: Not Found Error
- [ ] Open: `http://localhost:5174/product/ae:999999999999`
- [ ] Verify: Backend returns 404
- [ ] Verify: Shows error: `"NOT_FOUND: Product not found"`
- [ ] Console log shows:
  ```javascript
  {
    route: '/api/products/ae:999999999999',
    globalId: 'ae:999999999999',
    status: 404,
    latencyMs: <number>,
    errorCode: 'NOT_FOUND'
  }
  ```

### Test 2.4: Upstream Error
- [ ] Mock backend to return 502 with provider error
- [ ] Verify: Shows error message with provider name
- [ ] Example: `"UPSTREAM_ERROR: Provider service error (Provider: aliexpress)"`
- [ ] Verify: Retry button available

### Test 2.5: Upstream Timeout
- [ ] Mock backend to return 504
- [ ] Verify: Shows error: `"UPSTREAM_TIMEOUT: Provider timeout"`
- [ ] Verify: Error includes provider name if available

### Test 2.6: Upstream No Data (Provider 205)
- [ ] Mock backend to map provider 205 → `UPSTREAM_NO_DATA`
- [ ] Verify: Shows error: `"UPSTREAM_NO_DATA: Provider returned no data"`
- [ ] Verify: Error includes provider name

## 3. Search Integration Testing

### Test 3.1: AliExpress Search Results
- [ ] Navigate to: `http://localhost:5174/search?q=phone&marketplace=aliexpress`
- [ ] Wait for results to load
- [ ] Open browser DevTools → Console
- [ ] Verify console log shows:
  ```javascript
  {
    route: '/aliexpress/search/simple',
    platform: 'aliexpress',
    keyword: 'phone',
    status: 200,
    latencyMs: <number>,
    resultCount: <number>
  }
  ```
- [ ] **Inspect first product card:**
  - Right-click → Inspect Element
  - Find the `<a href="...">` tag
  - Verify href matches: `/product/ae:{productId}` OR `/product/{globalId}`
- [ ] Click on a product card
- [ ] Verify URL is: `/product/ae:{id}` or `/product/{globalId}`
- [ ] Verify product detail loads correctly

### Test 3.2: Taobao Search Results
- [ ] Navigate to: `http://localhost:5174/search?q=phone&marketplace=taobao`
- [ ] Verify product cards link to `/product/tb:{id}` or use globalId from backend
- [ ] Click product → Verify badge shows "Taobao"

### Test 3.3: 1688 Search Results
- [ ] Navigate to: `http://localhost:5174/search?q=phone&marketplace=1688`
- [ ] Verify product cards link to `/product/a1688:{id}` or use globalId from backend
- [ ] Click product → Verify badge shows "1688"

## 4. Legacy URL Handling

### Test 4.1: Legacy URL Redirect
- [ ] Open: `http://localhost:5174/product/1005005244562338`
- [ ] Verify: Blue banner appears with message:
  ```
  Converting to Canonical Link
  This link is being updated to include marketplace information for better accuracy.
  Redirecting to the canonical URL...
  ```
- [ ] Wait 1.5 seconds
- [ ] Verify: Redirects to `/product/ae:1005005244562338`
- [ ] Verify: Browser history shows only canonical URL (use back button to test)
- [ ] Verify: Product detail loads correctly
- [ ] Verify: Badge shows "AliExpress"

### Test 4.2: Multiple Legacy Redirects
- [ ] Open: `/product/123456`
- [ ] Wait for redirect to `/product/ae:123456`
- [ ] Click browser back button
- [ ] Verify: Goes back to previous page (not to `/product/123456`)

## 5. Deep Linking & State Persistence

### Test 5.1: Refresh Persistence
- [ ] Open: `/product/tb:6543210987`
- [ ] Wait for page to load completely
- [ ] Press F5 (refresh)
- [ ] Verify: URL remains `/product/tb:6543210987`
- [ ] Verify: Badge still shows "Taobao"
- [ ] Verify: Platform is preserved

### Test 5.2: Bookmark Test
- [ ] Open: `/product/ae:1005005244562338`
- [ ] Bookmark the page (Ctrl+D)
- [ ] Close the tab
- [ ] Open bookmark
- [ ] Verify: Correct product loads
- [ ] Verify: Badge shows "AliExpress"

### Test 5.3: Share Link Test
- [ ] Open: `/product/ae:1005005244562338`
- [ ] Copy URL from address bar
- [ ] Open in new incognito window
- [ ] Paste URL and press Enter
- [ ] Verify: Correct product loads
- [ ] Verify: Marketplace is preserved

### Test 5.4: Direct Link in New Tab
- [ ] On search results page, right-click a product card
- [ ] Select "Open link in new tab"
- [ ] Verify: Product loads in new tab
- [ ] Verify: Marketplace badge is correct

## 6. Observability & Logging

### Test 6.1: Success Logs
- [ ] Open browser console (F12)
- [ ] Navigate to: `/product/ae:1005005244562338`
- [ ] Verify console log includes:
  - `route`
  - `globalId`
  - `platform`
  - `status: 200`
  - `latencyMs`
  - `fromCache` (true/false)
  - `priceCurrency`

### Test 6.2: Error Logs
- [ ] Open: `/product/ae:999999999`
- [ ] Verify console error log includes:
  - `route`
  - `globalId`
  - `status` (404, 502, etc.)
  - `latencyMs`
  - `errorCode`
  - `provider` (if applicable)

### Test 6.3: Search Logs
- [ ] Search for "phone" in AliExpress
- [ ] Verify console log includes:
  - `route`
  - `platform`
  - `keyword`
  - `page`
  - `status`
  - `latencyMs`
  - `resultCount`
  - `fromCache`

## 7. Currency Display

### Test 7.1: AliExpress Currency
- [ ] Open any AliExpress product
- [ ] Verify: Price shows `$` symbol (USD)
- [ ] Example: `$29.99`

### Test 7.2: Taobao Currency
- [ ] Open any Taobao product
- [ ] Verify: Price shows `¥` symbol (CNY)
- [ ] Example: `¥199`

### Test 7.3: 1688 Currency
- [ ] Open any 1688 product
- [ ] Verify: Price shows `¥` symbol (CNY)
- [ ] Example: `¥99`

## 8. Edge Cases

### Test 8.1: Empty GlobalId
- [ ] Open: `/product/`
- [ ] Verify: Appropriate error handling
- [ ] Should not crash

### Test 8.2: Special Characters in ID
- [ ] Open: `/product/ae:!@#$%`
- [ ] Verify: Shows validation error
- [ ] Should not crash

### Test 8.3: Very Long ID
- [ ] Open: `/product/ae:12345678901234567890123456789012345678901234567890`
- [ ] Verify: Request sent to backend
- [ ] Verify: Backend validates and returns appropriate error

### Test 8.4: Multiple Colons
- [ ] Open: `/product/ae:123:456:789`
- [ ] Verify: Shows validation error: `"Invalid globalId format"`

## 9. Performance Testing

### Test 9.1: Load Time
- [ ] Open: `/product/ae:1005005244562338`
- [ ] Check Network tab → Filter by `/api/products/`
- [ ] Verify: Request completes in reasonable time (<2s)
- [ ] Check console for `latencyMs` value

### Test 9.2: Cached Response
- [ ] Open: `/product/ae:1005005244562338`
- [ ] Wait for page to load
- [ ] Refresh page (F5)
- [ ] Check Network tab for 304 status OR check `x-from-cache` header
- [ ] Verify: Console log shows `fromCache: true` (if backend implements caching)

## 10. Legacy Route Migration Testing (30-Second Window)

### Test 10.0: Alias Support in GlobalId
- [ ] Frontend can parse `ali:123` (alias for AliExpress)
- [ ] Open: `/product/ali:1005005244562338`
- [ ] Verify: Parses correctly and resolves to AliExpress
- [ ] Verify: Badge shows "AliExpress"

### Test 10.1: Legacy Backend Route - Deprecation Headers (Within 30s)
- [ ] Backend deployed with `DEPLOY_TIME` set
- [ ] Call: `GET /api/aliexpress/products/1005005244562338`
- [ ] Verify: Returns HTTP 200 with normalized schema
- [ ] Verify: Response headers include:
  - `Deprecation: true` or `Deprecation: 1`
  - `Sunset: <RFC-7231 date>` (30 seconds after deploy)
  - `Link: </api/products/ae:1005005244562338>; rel="successor-version"`
- [ ] Browser console shows deprecation warning:
  ```
  ⚠️ DEPRECATED ENDPOINT: /api/aliexpress/products/1005005244562338
    Sunset: <date>
    Use instead: /api/products/ae:1005005244562338
  ```

### Test 10.2: Legacy Route with Aliases (Within 30s)
- [ ] Test all marketplace aliases:
  - `/api/ali/products/123` → Should work (alias for aliexpress)
  - `/api/ae/products/123` → Should work
  - `/api/aliexpress/products/123` → Should work
  - `/api/tb/products/456` → Should work
  - `/api/taobao/products/456` → Should work
  - `/api/a1688/products/789` → Should work
  - `/api/1688/products/789` → Should work
- [ ] All should return 200 with deprecation headers
- [ ] All should include correct `Link` header with canonical globalId

### Test 10.3: Legacy Route After 30s Window - Redirect
- [ ] Wait 31 seconds after deployment (or mock time)
- [ ] Call: `GET /api/aliexpress/products/1005005244562338`
- [ ] Verify: Returns HTTP 301 Moved Permanently
- [ ] Verify: `Location` header: `/api/products/ae:1005005244562338`
- [ ] Browser console shows:
  ```
  🔀 REDIRECT: /api/aliexpress/products/1005005244562338 → /api/products/ae:1005005244562338
    Please update to use canonical endpoint
  ```
- [ ] OR if using 307: Verify HTTP 307 Temporary Redirect

### Test 10.4: Legacy Route After Window - Validation Error (Alternative)
- [ ] If backend returns 400 instead of redirect:
- [ ] Call: `GET /api/aliexpress/products/1005005244562338`
- [ ] Verify: Returns HTTP 400
- [ ] Verify response body:
  ```json
  {
    "code": "VALIDATION_ERROR",
    "message": "Legacy endpoint retired. Use /api/products/{globalId}",
    "canonicalUrl": "/api/products/ae:1005005244562338",
    "globalId": "ae:1005005244562338"
  }
  ```

### Test 10.5: Unmappable Alias
- [ ] Call: `GET /api/invalid/products/123`
- [ ] Verify: Returns HTTP 400
- [ ] Verify response body:
  ```json
  {
    "code": "VALIDATION_ERROR",
    "message": "Unknown marketplace: 'invalid'",
    "allowedAliases": ["ali", "ae", "aliexpress", "tb", "taobao", "a1688", "1688"]
  }
  ```

## 11. Regression Testing

### Test 11.1: Existing Features Still Work
- [ ] Home page loads
- [ ] Search functionality works
- [ ] Filters work (category, price range, sort)
- [ ] Pagination works
- [ ] Product images display
- [ ] Add to cart button appears (even if not functional)

### Test 11.2: Other Pages Unaffected
- [ ] Navigate to `/cart`
- [ ] Verify: Cart page renders (even if placeholder)
- [ ] Navigate to `/` (home)
- [ ] Verify: Home page renders

## Test Results Summary

**Date:** ________________
**Tester:** ________________
**Environment:** ________________

**Total Tests:** 60+
**Passed:** ______
**Failed:** ______
**Blocked:** ______

### Critical Issues Found:
1. ________________________________________________
2. ________________________________________________
3. ________________________________________________

### Notes:
_________________________________________________________
_________________________________________________________
_________________________________________________________

---

## Quick Test Script (Browser Console)

Paste this in browser console for quick validation:

```javascript
// Test Global ID parsing
const testGlobalId = (id) => {
  console.log(`Testing: ${id}`);
  fetch(`/api/products/${id}`)
    .then(r => r.json())
    .then(data => console.log('✅ Success:', data))
    .catch(err => console.log('❌ Error:', err));
};

// Test multiple marketplaces
testGlobalId('ae:1005005244562338'); // AliExpress
testGlobalId('tb:6543210987');        // Taobao
testGlobalId('a1688:898144857257');   // 1688

// Test validation
testGlobalId('xx:123');  // Invalid prefix
testGlobalId('ae:');     // Empty ID
```

---

**Testing Tips:**
1. Use browser DevTools Network tab to inspect API calls
2. Check Console for observability logs
3. Test with real product IDs from your backend
4. Clear browser cache between tests if needed
5. Test in incognito mode for fresh state
6. Use different browsers (Chrome, Firefox, Safari)
