# Backend Migration Guide - 30-Second Window Strategy

## Overview

This guide describes the backend migration from marketplace-specific routes to a unified canonical endpoint, with a **30-second transition window** for backward compatibility.

**Goal:** Move to single canonical route while preserving native currency per marketplace and ensuring zero downtime for clients.

## Migration Timeline

```
T0 (Deploy Time)
├─ T0 to T0+30s: Legacy routes active with deprecation headers
└─ T0+30s onwards: Legacy routes redirect (301/307) or return 400
```

## API Routes

### Canonical Route (Primary)

**Endpoint:**
```
GET /api/products/{globalId}
```

**GlobalId Format:**
```
{prefix}:{providerId}

Where prefix ∈ { ae, tb, a1688 }
```

**Examples:**
- `/api/products/ae:1005005244562338` → AliExpress product
- `/api/products/tb:6543210987` → Taobao product
- `/api/products/a1688:898144857257` → 1688 product

**Required Response:**
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
  "images": [...],
  "shop": {...},
  "url": "...",
  "attributes": {...},
  "badge": "Hot Sale",
  "lastUpdated": "2025-10-22T12:00:00Z"
}
```

**Key Requirements:**
- ✅ Accept full globalId in path parameter
- ✅ Return `platform` field (backend is source of truth)
- ✅ Return native currency (`USD` for AliExpress, `CNY` for Taobao/1688)
- ✅ Echo back `globalId` for verification

### Legacy Routes (Transitional)

**Endpoints:**
```
GET /api/{platform}/products/{id}
```

**Examples:**
- `/api/aliexpress/products/1005005244562338`
- `/api/ae/products/1005005244562338`
- `/api/ali/products/1005005244562338`
- `/api/taobao/products/6543210987`
- `/api/tb/products/6543210987`
- `/api/1688/products/898144857257`
- `/api/a1688/products/898144857257`

## Marketplace Aliases

### Canonical Names → Prefixes

| Canonical | Prefix | Aliases |
|-----------|--------|---------|
| `aliexpress` | `ae` | `ali`, `ae` |
| `taobao` | `tb` | `tb` |
| `1688` | `a1688` | `a1688` |

### Alias Mapping Table

```javascript
const MARKETPLACE_ALIASES = {
  'ali': 'aliexpress',
  'ae': 'aliexpress',
  'aliexpress': 'aliexpress',
  'tb': 'taobao',
  'taobao': 'taobao',
  'a1688': '1688',
  '1688': '1688'
};
```

### Normalization Logic

```javascript
function normalizeMarketplace(input) {
  const normalized = MARKETPLACE_ALIASES[input.toLowerCase()];
  if (!normalized) {
    throw new Error('VALIDATION_ERROR: Invalid marketplace');
  }
  return normalized;
}
```

## Migration Strategy: 30-Second Window

### Phase 1: T0 to T0+30 seconds

**Behavior:**
Legacy routes remain active and return data with deprecation headers.

**Response:**
```http
HTTP/1.1 200 OK
Deprecation: true
Sunset: Wed, 22 Oct 2025 14:00:30 GMT
Link: </api/products/ae:1005005244562338>; rel="successor-version"
Content-Type: application/json

{
  "platform": "aliexpress",
  "id": "1005005244562338",
  "globalId": "ae:1005005244562338",
  ...
}
```

**Headers:**
- `Deprecation: true` - Indicates endpoint is deprecated
- `Sunset: <RFC-7231 date>` - Exact time endpoint will be retired
- `Link: <canonical-url>; rel="successor-version"` - Points to new endpoint

**Implementation:**
```javascript
app.get('/api/:platform/products/:id', (req, res) => {
  const { platform, id } = req.params;

  // 1. Normalize marketplace alias
  const normalizedPlatform = normalizeMarketplace(platform);

  // 2. Build canonical globalId
  const prefix = getPrefixByMarketplace(normalizedPlatform);
  const globalId = `${prefix}:${id}`;

  // 3. Check if within 30-second window
  const deployTime = process.env.DEPLOY_TIME || Date.now();
  const elapsed = Date.now() - deployTime;
  const withinWindow = elapsed <= 30000; // 30 seconds

  if (withinWindow) {
    // Proxy to canonical endpoint
    const data = await getProductByGlobalId(globalId);

    // Add deprecation headers
    res.setHeader('Deprecation', 'true');
    res.setHeader('Sunset', new Date(deployTime + 30000).toUTCString());
    res.setHeader('Link', `</api/products/${globalId}>; rel="successor-version"`);

    return res.json(data);
  } else {
    // After window: redirect or error
    return handlePostWindowRequest(req, res, globalId);
  }
});
```

### Phase 2: After T0+30 seconds

**Behavior Options:**

#### Option A: Permanent Redirect (Recommended)

```http
HTTP/1.1 301 Moved Permanently
Location: /api/products/ae:1005005244562338
Content-Type: application/json

{
  "code": "ENDPOINT_MOVED",
  "message": "This endpoint has moved. Please use /api/products/{globalId}",
  "location": "/api/products/ae:1005005244562338"
}
```

**Implementation:**
```javascript
function handlePostWindowRequest(req, res, globalId) {
  const canonicalUrl = `/api/products/${globalId}`;

  res.status(301)
    .location(canonicalUrl)
    .json({
      code: 'ENDPOINT_MOVED',
      message: 'This endpoint has moved permanently',
      location: canonicalUrl
    });
}
```

#### Option B: Temporary Redirect

```http
HTTP/1.1 307 Temporary Redirect
Location: /api/products/ae:1005005244562338
```

#### Option C: Validation Error

```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "code": "VALIDATION_ERROR",
  "message": "Legacy endpoint retired. Use /api/products/{globalId}",
  "canonicalUrl": "/api/products/ae:1005005244562338",
  "globalId": "ae:1005005244562338"
}
```

**Recommendation:** Use Option A (301 redirect) for best backward compatibility.

### Special Case: Unmappable Aliases

If platform parameter doesn't match any known alias:

```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "code": "VALIDATION_ERROR",
  "message": "Unknown marketplace: 'xyz'",
  "allowedAliases": ["ali", "ae", "aliexpress", "tb", "taobao", "a1688", "1688"]
}
```

## Canonical Endpoint Implementation

### Request Handler

```javascript
app.get('/api/products/:globalId', async (req, res) => {
  const { globalId } = req.params;
  const startTime = Date.now();

  try {
    // 1. Parse globalId
    const parsed = parseGlobalId(globalId);

    if (!parsed.isValid) {
      return res.status(400).json({
        code: 'VALIDATION_ERROR',
        message: parsed.error,
        globalId
      });
    }

    const { marketplace, productId } = parsed;

    // 2. Fetch from provider
    const providerData = await fetchFromProvider(marketplace, productId);

    // 3. Handle provider 205 (no data)
    if (providerData.statusCode === 205) {
      return res.status(502).json({
        code: 'UPSTREAM_NO_DATA',
        message: 'Provider returned no data',
        provider: marketplace,
        globalId
      });
    }

    // 4. Normalize response
    const normalized = normalizeProviderResponse(marketplace, productId, providerData);

    // 5. Add observability headers
    const latencyMs = Date.now() - startTime;
    res.setHeader('X-Response-Time', latencyMs);
    res.setHeader('X-Platform', marketplace);

    // 6. Return normalized data
    res.json(normalized);

    // 7. Log telemetry
    logTelemetry({
      route: `/api/products/${globalId}`,
      globalId,
      platform: marketplace,
      status: 200,
      latencyMs,
      fromCache: providerData.fromCache || false
    });

  } catch (error) {
    const latencyMs = Date.now() - startTime;

    const errorResponse = normalizeError(error, globalId);
    res.status(errorResponse.status).json(errorResponse.body);

    logTelemetry({
      route: `/api/products/${globalId}`,
      globalId,
      status: errorResponse.status,
      latencyMs,
      errorCode: errorResponse.body.code,
      provider: errorResponse.body.provider
    });
  }
});
```

### GlobalId Parsing

```javascript
function parseGlobalId(globalId) {
  if (!globalId || !globalId.includes(':')) {
    return {
      isValid: false,
      error: 'Invalid globalId format. Expected "prefix:productId"'
    };
  }

  const [prefix, productId] = globalId.split(':', 2);

  const prefixMap = {
    'ae': 'aliexpress',
    'ali': 'aliexpress',  // Support alias
    'tb': 'taobao',
    'a1688': '1688'
  };

  const marketplace = prefixMap[prefix.toLowerCase()];

  if (!marketplace) {
    return {
      isValid: false,
      error: `Unsupported prefix "${prefix}". Allowed: ae, ali, tb, a1688`
    };
  }

  if (!productId) {
    return {
      isValid: false,
      error: 'Product ID cannot be empty'
    };
  }

  return {
    isValid: true,
    marketplace,
    productId,
    prefix
  };
}
```

### Response Normalization

```javascript
function normalizeProviderResponse(marketplace, productId, providerData) {
  const prefix = {
    'aliexpress': 'ae',
    'taobao': 'tb',
    '1688': 'a1688'
  }[marketplace];

  const currency = marketplace === 'aliexpress' ? 'USD' : 'CNY';

  return {
    platform: marketplace,
    id: productId,
    globalId: `${prefix}:${productId}`,
    title: providerData.title,
    price: {
      value: providerData.price,
      currency: currency  // Native currency per marketplace
    },
    images: providerData.images || [],
    shop: {
      name: providerData.shopName,
      id: providerData.shopId
    },
    url: providerData.url,
    attributes: providerData.attributes || {},
    badge: providerData.badge,
    lastUpdated: new Date().toISOString()
  };
}
```

## Error Normalization

### Error Codes

| Code | HTTP Status | Description | When to Use |
|------|-------------|-------------|-------------|
| `VALIDATION_ERROR` | 400 | Invalid globalId format | Malformed globalId, invalid prefix, empty productId |
| `NOT_FOUND` | 404 | Product not found | Provider returns 404 |
| `UPSTREAM_ERROR` | 502 | Provider API error | Provider returns 5xx, network error |
| `UPSTREAM_TIMEOUT` | 504 | Provider timeout | Request to provider times out |
| `UPSTREAM_NO_DATA` | 502* | Provider returned 205 | Provider returns 205 (reset content/no data) |

*Note: `UPSTREAM_NO_DATA` can return 200 or 502 based on policy. Recommend 502 for clarity.

### Error Response Format

```javascript
function normalizeError(error, globalId) {
  // Provider 404
  if (error.statusCode === 404) {
    return {
      status: 404,
      body: {
        code: 'NOT_FOUND',
        message: 'Product not found',
        globalId,
        provider: error.provider
      }
    };
  }

  // Provider 205 (no data)
  if (error.statusCode === 205) {
    return {
      status: 502,
      body: {
        code: 'UPSTREAM_NO_DATA',
        message: 'Provider returned no data available',
        globalId,
        provider: error.provider
      }
    };
  }

  // Provider timeout
  if (error.code === 'ETIMEDOUT' || error.statusCode === 504) {
    return {
      status: 504,
      body: {
        code: 'UPSTREAM_TIMEOUT',
        message: 'Provider request timed out',
        globalId,
        provider: error.provider
      }
    };
  }

  // Provider error (5xx)
  if (error.statusCode >= 500) {
    return {
      status: 502,
      body: {
        code: 'UPSTREAM_ERROR',
        message: 'Provider service error',
        globalId,
        provider: error.provider,
        details: error.message
      }
    };
  }

  // Unknown error
  return {
    status: 500,
    body: {
      code: 'INTERNAL_ERROR',
      message: 'An unexpected error occurred',
      globalId
    }
  };
}
```

## Telemetry & Logging

### Required Fields

```javascript
{
  route: string,           // e.g., "/api/products/ae:123"
  globalId: string,        // e.g., "ae:123"
  platform: string,        // e.g., "aliexpress"
  status: number,          // HTTP status code
  latencyMs: number,       // Request duration
  fromCache: boolean,      // Whether response from cache
  priceCurrency: string,   // "USD" or "CNY"
  timestamp: string        // ISO 8601
}
```

### Success Log Example

```json
{
  "route": "/api/products/ae:1005005244562338",
  "globalId": "ae:1005005244562338",
  "platform": "aliexpress",
  "status": 200,
  "latencyMs": 145,
  "fromCache": true,
  "priceCurrency": "USD",
  "timestamp": "2025-10-22T14:30:15.123Z"
}
```

### Error Log Example

```json
{
  "route": "/api/products/ae:999999999",
  "globalId": "ae:999999999",
  "platform": "aliexpress",
  "status": 404,
  "latencyMs": 234,
  "fromCache": false,
  "errorCode": "NOT_FOUND",
  "provider": "aliexpress",
  "timestamp": "2025-10-22T14:30:15.456Z"
}
```

## Native Currency Handling

### Currency by Marketplace

| Marketplace | Currency | Symbol |
|------------|----------|--------|
| AliExpress | `USD` | $ |
| Taobao | `CNY` | ¥ |
| 1688 | `CNY` | ¥ |

### Implementation

```javascript
function getCurrency(marketplace) {
  return marketplace === 'aliexpress' ? 'USD' : 'CNY';
}

// In response normalization
price: {
  value: providerData.price.toString(),
  currency: getCurrency(marketplace)
}
```

**Important:** Never convert currencies. Return native price from provider.

## Frontend Integration

### Deprecation Header Detection

Frontend automatically logs deprecation warnings:

```javascript
// Browser console output when legacy endpoint used:
⚠️ DEPRECATED ENDPOINT: /api/aliexpress/products/123
  Sunset: Wed, 22 Oct 2025 14:00:30 GMT
  Use instead: /api/products/ae:123
```

### Redirect Handling

Frontend logs redirects:

```javascript
// Browser console output when redirect occurs:
🔀 REDIRECT: /api/aliexpress/products/123 → /api/products/ae:123
  Please update to use canonical endpoint
```

## Testing Strategy

### Unit Tests

```javascript
describe('parseGlobalId', () => {
  it('should parse canonical ae prefix', () => {
    const result = parseGlobalId('ae:123');
    expect(result.marketplace).toBe('aliexpress');
    expect(result.productId).toBe('123');
  });

  it('should parse alias ali prefix', () => {
    const result = parseGlobalId('ali:123');
    expect(result.marketplace).toBe('aliexpress');
  });

  it('should reject invalid prefix', () => {
    const result = parseGlobalId('xx:123');
    expect(result.isValid).toBe(false);
  });
});
```

### Integration Tests

```javascript
describe('Legacy Route Migration', () => {
  it('should return data with deprecation headers within 30s', async () => {
    const res = await request(app)
      .get('/api/aliexpress/products/123')
      .expect(200);

    expect(res.headers['deprecation']).toBe('true');
    expect(res.headers['sunset']).toBeDefined();
    expect(res.headers['link']).toContain('/api/products/ae:123');
  });

  it('should redirect after 30s window', async () => {
    // Mock time > 30s after deploy
    await request(app)
      .get('/api/aliexpress/products/123')
      .expect(301)
      .expect('Location', '/api/products/ae:123');
  });
});
```

## Deployment Checklist

### Pre-Deployment

- [ ] Implement canonical endpoint `/api/products/{globalId}`
- [ ] Implement legacy route handler with time check
- [ ] Add deprecation header logic
- [ ] Implement redirect logic for post-window
- [ ] Add telemetry logging
- [ ] Test all marketplace aliases
- [ ] Test error normalization
- [ ] Verify native currency handling

### Deployment

- [ ] Set `DEPLOY_TIME` environment variable
- [ ] Deploy backend with both routes active
- [ ] Monitor logs for deprecation warnings
- [ ] Verify 30-second window starts correctly

### Post-Deployment

- [ ] Monitor legacy route usage
- [ ] Check for deprecation header presence
- [ ] Verify redirects work after 30s
- [ ] Monitor error rates
- [ ] Check telemetry logs

### After 30 Seconds

- [ ] Verify legacy routes return 301
- [ ] Check no 5xx errors from redirects
- [ ] Monitor for any clients still using legacy routes
- [ ] Plan eventual removal of legacy code (after monitoring period)

## Monitoring & Alerts

### Metrics to Track

1. **Legacy Route Usage:**
   - Count of requests to `/api/{platform}/products/{id}`
   - Should drop to zero within minutes after 30s window

2. **Canonical Route Adoption:**
   - Count of requests to `/api/products/{globalId}`
   - Should be 100% of product detail requests

3. **Error Rates:**
   - 400 errors (validation)
   - 404 errors (not found)
   - 502/504 errors (upstream issues)

4. **Response Times:**
   - P50, P95, P99 latency
   - Track by marketplace

5. **Cache Hit Rate:**
   - Percentage of requests served from cache
   - Track by marketplace

### Alert Thresholds

```yaml
alerts:
  - name: LegacyRouteUsageAfterWindow
    condition: legacy_route_count > 10 after 60s
    severity: warning

  - name: HighErrorRate
    condition: error_rate > 5%
    severity: critical

  - name: HighLatency
    condition: p95_latency > 2000ms
    severity: warning

  - name: UpstreamNoDataSpike
    condition: upstream_no_data_count > 100 in 5min
    severity: warning
```

## Rollback Strategy

If issues occur:

1. **Within 30 seconds:**
   - Extend window by updating `DEPLOY_TIME`
   - Deploy fix
   - Reset timer

2. **After 30 seconds:**
   - Revert redirect logic to return data instead
   - Extend window to 5 minutes for emergency fix
   - Deploy corrected version

3. **Full Rollback:**
   - Keep legacy routes active indefinitely
   - Fix canonical endpoint issues
   - Re-plan migration with longer window

## FAQs

**Q: Why 30 seconds?**
A: Short enough to force quick adoption, long enough for in-flight requests to complete.

**Q: What if clients cache DNS/routes?**
A: Deprecation headers warn clients. After window, 301 redirects are automatically followed by HTTP clients.

**Q: Should we support `ali` alias in globalId?**
A: Frontend supports parsing `ali:123`, but canonical output should always use `ae:123`.

**Q: What about search results?**
A: Search endpoints should return `globalId: "ae:123"` in each result for direct linking.

**Q: How to test locally?**
A: Set `DEPLOY_TIME=<timestamp-30s-ago>` to simulate post-window behavior.

---

**Version:** 1.0
**Last Updated:** 2025-10-22
**Status:** Ready for Implementation
