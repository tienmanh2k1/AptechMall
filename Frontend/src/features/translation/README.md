# Product Translation Feature

## Tổng Quan

Tính năng tự động dịch nội dung sản phẩm từ tiếng Anh (AliExpress) và tiếng Trung (1688) sang tiếng Việt sử dụng **RapidAPI Free Google Translator**.

## Cấu Trúc

```
features/translation/
├── services/
│   ├── translationApi.js          # API calls đến RapidAPI
│   └── translationCache.js        # Cache management (7 days TTL)
├── hooks/
│   └── useProductTranslation.js   # Custom hook cho auto-translation
├── utils/
│   ├── productTranslator.js       # Extract & translate product content
│   └── productAdapter.js          # Transform backend format
├── components/
│   └── TranslationToggle.jsx      # UI toggle button
└── README.md
```

## Tích Hợp Vào ProductDetailPage

### Bước 1: Import Hook và Component

```jsx
import useProductTranslation from '../../translation/hooks/useProductTranslation';
import TranslationToggle from '../../translation/components/TranslationToggle';
import { transformForTranslation, applyTranslationsToBackend } from '../../translation/utils/productAdapter';
```

### Bước 2: Sử Dụng Hook

```jsx
const ProductDetailPage = () => {
  const { platform, id } = useParams();
  const [product, setProduct] = useState(null);

  // Transform product for translation
  const flatProduct = product ? transformForTranslation(product) : null;

  // Use translation hook
  const {
    displayProduct,
    isTranslating,
    showOriginal,
    toggleLanguage,
    hasTranslation
  } = useProductTranslation(flatProduct, platform, {
    autoTranslate: true,  // Tự động dịch khi load
    delayMs: 500          // Delay 500ms giữa các request
  });

  // Apply translations back to backend format
  const translatedBackendProduct = displayProduct && product
    ? applyTranslationsToBackend(displayProduct, product)
    : product;

  // Use translatedBackendProduct instead of product
  const backendItem = translatedBackendProduct?.Result?.Item;

  // ...rest of code
};
```

### Bước 3: Thêm UI Toggle Button

```jsx
return (
  <div className="container mx-auto px-4 py-8">
    {/* Translation Toggle - đặt ở đầu trang */}
    <TranslationToggle
      showOriginal={showOriginal}
      onToggle={toggleLanguage}
      isTranslating={isTranslating}
      hasTranslation={hasTranslation}
      sourceLang={platform === '1688' ? 'ZH' : 'EN'}
      targetLang="VI"
    />

    {/* Rest of product UI */}
    <ProductImages images={backendItem.Pictures} />
    <ProductInfo title={backendItem.Title} />
    {/* ... */}
  </div>
);
```

## API Configuration

### RapidAPI Details

- **API**: Free Google Translator
- **Endpoint**: `https://free-google-translator.p.rapidapi.com/external-api/free-google-translator`
- **Key**: Đã configure trong `translationApi.js`
- **Rate Limit**: Free tier có giới hạn requests/day

### Supported Languages

| Platform | Source Language | Target Language |
|----------|----------------|-----------------|
| AliExpress | English (en) | Vietnamese (vi) |
| 1688 | Chinese (zh-CN) | Vietnamese (vi) |

## Cache System

### Cache Storage

- **Location**: `localStorage`
- **Key Format**: `translation_{platform}_{productId}`
- **TTL**: 7 days
- **Data Structure**:
```json
{
  "productId": "aliexpress_1005005244562338",
  "platform": "aliexpress",
  "translatedAt": 1704067200000,
  "expiresAt": 1704672000000,
  "data": { ...translated product... }
}
```

### Cache Functions

```javascript
import { getCachedTranslation, saveCachedTranslation, clearCachedTranslation } from '../services/translationCache';

// Get cached translation
const cached = getCachedTranslation('aliexpress', '1005005244562338');

// Save translation
saveCachedTranslation('aliexpress', '1005005244562338', translatedData);

// Clear specific cache
clearCachedTranslation('aliexpress', '1005005244562338');

// Clear all caches
clearAllTranslationCaches();
```

## Translatable Fields

Các trường được dịch tự động:

### 1. Product Info
- `title` - Tên sản phẩm
- `description` - Mô tả sản phẩm

### 2. Attributes
- `attributes[].name` - Tên thuộc tính (Color, Size, Material...)
- `attributes[].value` - Giá trị thuộc tính (Red, Large, Cotton...)

### 3. Variants
- `variants[].name` - Tên biến thể
- `variants[].value` - Giá trị biến thể

### 4. Shop/Seller
- `shop.name` - Tên shop
- `shop.description` - Mô tả shop

### 5. Shipping
- `shipping.method` - Phương thức vận chuyển
- `shipping.description` - Mô tả vận chuyển

### 6. Category
- `breadcrumbs[].title` - Danh mục sản phẩm

## Performance

### Estimation

```javascript
import { estimateTranslationTime } from '../utils/productTranslator';

// Estimate translation time for a product
const estimatedMs = estimateTranslationTime(product, 500);
console.log(`Estimated: ${estimatedMs / 1000} seconds`);
```

**Average Time:**
- Product với 20 fields: ~15-20 seconds
- Product với 50 fields: ~30-40 seconds

### Optimization

1. **Caching**: Lần đầu dịch sẽ lâu, lần sau dùng cache (instant)
2. **Delay**: Default 500ms giữa các request để tránh rate limit
3. **Batch**: Không dùng batch API (RapidAPI không support)

## Error Handling

```jsx
const {
  displayProduct,
  error,
  retryTranslation
} = useProductTranslation(product, platform);

// Check error
if (error) {
  console.error('Translation failed:', error);
}

// Retry manually
<button onClick={retryTranslation}>Retry Translation</button>
```

**Error Cases:**
- API rate limit exceeded → Fallback to original
- Network error → Fallback to original
- Invalid API key → Fallback to original

## Testing

### Manual Test

1. Navigate to product page: `/aliexpress/products/1005005244562338`
2. Wait for auto-translation (check console logs)
3. Click "Xem bản gốc" to toggle
4. Check cache in DevTools → Application → Local Storage

### Console Logs

```
[Translation] Translating from en to vi: iPhone 15 Pro Max...
[Translation Cache] Miss: translation_aliexpress_1005005244562338
[Product Translator] Extracted 25 texts to translate
[Product Translator] Translated 25 texts
[Translation Cache] Saved: translation_aliexpress_1005005244562338
[useProductTranslation] Translation complete
```

## Troubleshooting

### Translation không hoạt động

1. **Check console for errors**
   - API key invalid?
   - Rate limit exceeded?
   - Network error?

2. **Check platform**
   - Chỉ support: `aliexpress`, `1688`
   - Không support các platform khác

3. **Clear cache**
```javascript
import { clearAllTranslationCaches } from '../services/translationCache';
clearAllTranslationCaches();
```

### Translation quá lâu

1. **Giảm delay**
```javascript
useProductTranslation(product, platform, { delayMs: 300 }); // Faster but risky
```

2. **Check số lượng fields**
```javascript
import { extractTranslatableTexts } from '../utils/productTranslator';
const { texts } = extractTranslatableTexts(product);
console.log(`${texts.length} fields to translate`);
```

## Future Enhancements

- [ ] Backend translation API (tránh rate limit)
- [ ] WebSocket real-time translation
- [ ] Translation progress bar
- [ ] Batch translation API
- [ ] Multiple target languages
- [ ] Translation quality rating

## API Cost

**RapidAPI Free Tier:**
- 100-500 requests/day (depends on API)
- Nếu vượt quota → Auto fallback to original

**Optimization:**
- Cache 7 days → Reduce API calls
- Share cache across users (future: Redis)
