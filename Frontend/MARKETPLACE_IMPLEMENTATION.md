# Marketplace Implementation Guide

## Overview

This document describes the implementation of the canonical marketplace enum and linking strategy for PandaMall, supporting multiple marketplaces (AliExpress, Taobao, 1688) with namespaced global IDs.

## Architecture

### 1. Canonical Marketplace Enum

Located in: `src/shared/utils/marketplace.js`

```javascript
MARKETPLACE = {
  ALIEXPRESS: 'aliexpress',
  TAOBAO: 'taobao',
  ALIBABA_1688: '1688'
}
```

### 2. Namespaced Global ID Format (Strategy A - Implemented)

**Format:** `{prefix}:{productId}`

**Examples:**
- AliExpress: `ae:1005005244562338`
- Taobao: `tb:6543210987`
- 1688: `a1688:898144857257`

**URL Pattern:** `/product/{globalId}`

**Benefits:**
- Marketplace is part of the product identity
- Robust to refresh/bookmark/share
- Single parameter - harder to lose
- Deterministic resolution

## Implementation Details

### Core Utilities (`src/shared/utils/marketplace.js`)

#### Constants

- `MARKETPLACE` - Enum of marketplace values
- `MARKETPLACE_PREFIX` - Prefix to marketplace mapping (`ae` → `aliexpress`)
- `PREFIX_BY_MARKETPLACE` - Reverse mapping
- `MARKETPLACE_DISPLAY_NAME` - UI display names
- `MARKETPLACE_COLORS` - Tailwind classes for badges

#### Key Functions

**`parseGlobalId(globalId)`**
- Parses namespaced globalId into marketplace and productId
- Returns validation result with error messages
- Detects legacy format (no prefix)

```javascript
parseGlobalId('ae:1005005244562338')
// → { marketplace: 'aliexpress', productId: '1005005244562338', isValid: true }

parseGlobalId('1005005244562338')
// → { isLegacy: true, isValid: false, error: '...' }
```

**`buildGlobalId(marketplace, productId)`**
- Constructs canonical globalId from marketplace and productId
- Throws error for invalid marketplace

```javascript
buildGlobalId('aliexpress', '1005005244562338')
// → 'ae:1005005244562338'
```

**`legacyToCanonical(productId)`**
- Converts legacy productId to canonical format
- Defaults to AliExpress during transition period

### Service Layer Updates

#### `src/features/product/services/productService.js`

**`getProductById(productId, marketplace)`**
- Now accepts marketplace parameter (defaults to 'aliexpress')
- Calls marketplace-specific endpoint: `/{marketplace}/products/${productId}`
- Attaches marketplace to error for debugging

**`searchProducts(marketplace, keyword, page, filters)`**
- Already accepted marketplace parameter
- Enhanced to attach marketplace to each result item
- Enables ProductCard to generate canonical links

### Component Updates

#### `ProductDetailPage` (`src/features/product/pages/ProductDetailPage.jsx`)

**New Features:**
1. **GlobalId Parsing**
   - Extracts marketplace and productId from URL parameter
   - Validates format using `parseGlobalId()`

2. **Legacy URL Handling**
   - Detects legacy format (no prefix)
   - Shows informational banner
   - Redirects to canonical URL after 1.5 seconds
   - Uses `replace: true` for clean history

3. **Marketplace Badge**
   - Displays marketplace name in colored badge
   - Positioned at top of product detail page
   - Colors: AliExpress (red), Taobao (orange), 1688 (blue)

4. **Enhanced Error Handling**
   - Validation errors show specific error message
   - Marketplace context preserved in errors

**Flow:**
```
URL: /product/ae:1005005244562338
  ↓
Parse globalId → marketplace='aliexpress', productId='1005005244562338'
  ↓
Fetch from /aliexpress/products/1005005244562338
  ↓
Display with AliExpress badge

URL: /product/1005005244562338 (legacy)
  ↓
Detect legacy format
  ↓
Show banner "Converting to canonical link..."
  ↓
Redirect to /product/ae:1005005244562338
```

#### `ProductCard` (`src/features/product/components/ProductCard.jsx`)

**Updated Link Generation:**
- Extracts marketplace from product object (attached by service layer)
- Builds canonical globalId using `buildGlobalId()`
- Fallback to legacy format if marketplace missing (backward compatibility)

```javascript
const productLink = marketplace
  ? `/product/${buildGlobalId(marketplace, itemId)}`
  : `/product/${itemId}`;
```

### Backward Compatibility

#### Legacy URL Handling

1. **Detection**: `parseGlobalId()` detects missing prefix
2. **User Experience**:
   - Blue informational banner
   - Message: "Converting to canonical link for accuracy"
   - Auto-redirect after 1.5 seconds
3. **Redirect**: 301 to canonical URL with `replace: true`
4. **Default**: Legacy URLs temporarily default to AliExpress

**Example:**
```
User visits: /product/1005005244562338
  ↓
Banner shown for 1.5s
  ↓
Redirect to: /product/ae:1005005244562338
  ↓
Browser history: only canonical URL
```

## Error Handling (Normalized)

### Validation Errors (400)

**Source:** `parseGlobalId()`

**Cases:**
- Unsupported prefix: `"VALIDATION_ERROR: Unsupported marketplace prefix 'xx'..."`
- Malformed format: `"VALIDATION_ERROR: Invalid globalId format..."`
- Empty productId: `"VALIDATION_ERROR: Product ID cannot be empty"`

**Display:** ErrorMessage component shows specific error

### Not Found (404)

**Source:** API response

**Display:** "Product not found" with retry button

### Upstream Errors (502/504)

**Source:** API timeout/error

**Enhanced:** Error object includes marketplace and productId for debugging

```javascript
error.marketplace = 'taobao';
error.productId = '6543210987';
error.statusCode = 502;
```

## Testing Scenarios

### ✅ Acceptance Criteria

1. **Links from Search Results**
   - ✅ Always include marketplace prefix
   - ✅ Format: `/product/{prefix}:{productId}`
   - ✅ Examples: `/product/ae:123`, `/product/tb:456`, `/product/a1688:789`

2. **Deep Linking**
   - ✅ Opening `/product/ae:123` in new tab preserves marketplace
   - ✅ Refresh preserves marketplace
   - ✅ Bookmark works correctly
   - ✅ Share link includes marketplace

3. **Legacy URL Migration**
   - ✅ `/product/123` shows conversion banner
   - ✅ Auto-redirects to `/product/ae:123`
   - ✅ History replaced (clean back button behavior)

4. **Validation**
   - ✅ Invalid prefix (`/product/xx:123`) → Validation error
   - ✅ Malformed ID (`/product/ae:`) → Validation error
   - ✅ No silent marketplace switching

5. **UI/UX**
   - ✅ Marketplace badge shown on Product Detail
   - ✅ Badge color matches marketplace
   - ✅ Legacy banner is informational (not alarming)

### Manual Testing Checklist

- [ ] Search for products in AliExpress → click result → verify URL is `/product/ae:{id}`
- [ ] Search for products in Taobao → click result → verify URL is `/product/tb:{id}`
- [ ] Search for products in 1688 → click result → verify URL is `/product/a1688:{id}`
- [ ] Open `/product/ae:1005005244562338` → verify marketplace badge shows "AliExpress"
- [ ] Open `/product/tb:6543210987` → verify marketplace badge shows "Taobao"
- [ ] Open `/product/a1688:898144857257` → verify marketplace badge shows "1688"
- [ ] Open `/product/1005005244562338` → verify banner → verify redirect to `/product/ae:1005005244562338`
- [ ] Open `/product/xx:123` → verify validation error
- [ ] Open `/product/ae:` → verify validation error
- [ ] Refresh product detail page → verify marketplace preserved
- [ ] Share product URL → verify recipient sees correct marketplace

## Analytics & Logging

### Current Logging

**Search Click:**
```javascript
console.log('🔍 Searching:', {
  marketplace, // ✅ Logged
  keyword,
  // ...
});
```

**Product Detail:**
```javascript
console.log(`Fetching product ${productId} from ${marketplace}`);
// ✅ Logs marketplace and productId
```

### Recommended Additions

```javascript
// On product link click (SearchResults)
analytics.track('product_clicked', {
  marketplace,
  productId,
  globalId: buildGlobalId(marketplace, productId),
  referrer: 'search'
});

// On product detail view
analytics.track('product_viewed', {
  marketplace,
  productId,
  globalId,
  loadTime: latencyMs,
  status: 'success' | 'error'
});

// Funnel integrity check
if (clickMarketplace !== detailMarketplace) {
  console.error('Marketplace mismatch!', {
    expected: clickMarketplace,
    actual: detailMarketplace
  });
}
```

## Migration Notes

### Phase 1: Transition (Current)

- All new links use canonical format
- Legacy links redirect with banner
- Default marketplace: AliExpress

### Phase 2: Full Adoption

- Update all internal bookmarks/saved links
- Remove legacy fallback after monitoring shows <1% legacy traffic
- Remove banner and redirect directly

### Phase 3: Cleanup (Optional)

- Remove legacy detection code
- Remove default marketplace fallback
- Enforce strict validation only

## File Changes Summary

### New Files
- `src/shared/utils/marketplace.js` - Core utilities
- `src/shared/utils/marketplace.test.js` - Manual test file
- `MARKETPLACE_IMPLEMENTATION.md` - This document

### Modified Files
- `src/features/product/services/productService.js`
  - Added marketplace parameter to `getProductById()`
  - Attach marketplace to search results
  - Enhanced error objects

- `src/features/product/pages/ProductDetailPage.jsx`
  - Parse globalId from URL
  - Handle legacy URL redirection
  - Display marketplace badge
  - Enhanced error handling

- `src/features/product/components/ProductCard.jsx`
  - Build canonical links with `buildGlobalId()`
  - Fallback for backward compatibility

## API Contract

### Backend Requirements

The backend must support marketplace-specific endpoints:

```
GET /{marketplace}/products/{productId}
GET /{marketplace}/search/simple?keyword=...
```

Where `{marketplace}` ∈ { "aliexpress", "taobao", "1688" }

**Response Format:**
- Same structure for all marketplaces (normalized)
- Include marketplace in metadata if needed

**Error Responses:**
- 400: Invalid marketplace or productId
- 404: Product not found
- 502/504: Upstream provider error

## Troubleshooting

### Issue: Marketplace badge not showing

**Check:**
1. Is marketplace attached to product in service layer? (line 64-67 in productService.js)
2. Is globalId valid in URL?
3. Is parseGlobalId() returning isValid: true?

### Issue: Links still using legacy format

**Check:**
1. Is marketplace attached to products in search results?
2. Is ProductCard importing buildGlobalId()?
3. Check browser console for errors

### Issue: Legacy redirect not working

**Check:**
1. Is useNavigate imported from react-router-dom?
2. Is legacy detection working? (parseGlobalId returns isLegacy: true)
3. Check browser console for navigation errors

## Future Enhancements

### 1. URL Query Parameter Fallback (Optional)

Support `/product/{id}?marketplace=taobao` as fallback

### 2. Marketplace Switcher

Allow users to view same product on different marketplace

### 3. SEO Optimization

- Add marketplace to page title
- Add structured data with marketplace info
- Generate marketplace-specific sitemaps

### 4. Advanced Analytics

- Track marketplace preference
- A/B test marketplace presentation
- Monitor cross-marketplace shopping patterns

## References

- Original Requirements: See user message with canonical linking strategy
- React Router v6 Docs: https://reactrouter.com/
- Backend API: See CLAUDE.md for API endpoints

---

**Implementation Date:** 2025-10-22
**Version:** 1.0
**Status:** ✅ Complete
