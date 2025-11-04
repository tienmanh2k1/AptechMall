# Integration Guide: Thêm Translation vào ProductDetailPage

## Bước 1: Import Dependencies

Thêm vào đầu file `ProductDetailPage.jsx`:

```javascript
// Existing imports...
import { useParams } from 'react-router-dom';
import { productService } from '../services/productService';
// ...

// ✅ ADD: Translation imports
import useProductTranslation from '../../translation/hooks/useProductTranslation';
import TranslationToggle from '../../translation/components/TranslationToggle';
import { transformForTranslation, applyTranslationsToBackend } from '../../translation/utils/productAdapter';
```

## Bước 2: Update State & Hook Setup

Trong component `ProductDetailPage`, thêm translation logic:

```javascript
const ProductDetailPage = () => {
  const { platform, id } = useParams();
  const { refreshCart } = useCart();
  const { formatPrice } = useCurrency();

  // Existing states
  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedVariant, setSelectedVariant] = useState(null);
  const [currentImages, setCurrentImages] = useState([]);
  const [currentPrice, setCurrentPrice] = useState(null);
  const [addingToCart, setAddingToCart] = useState(false);

  // ✅ ADD: Transform product for translation
  const flatProduct = product ? transformForTranslation(product) : null;

  // ✅ ADD: Translation hook
  const {
    displayProduct,
    isTranslating,
    showOriginal,
    toggleLanguage,
    hasTranslation,
    error: translationError
  } = useProductTranslation(flatProduct, platform, {
    autoTranslate: true,  // Tự động dịch khi load page
    delayMs: 500          // Delay 500ms giữa mỗi request
  });

  // ✅ ADD: Apply translations back to backend format
  const translatedBackendProduct = displayProduct && product
    ? applyTranslationsToBackend(displayProduct, product)
    : product;

  // Existing fetchProduct function...
  const fetchProduct = useCallback(async () => {
    // ... existing code không thay đổi
  }, [platform, id]);

  // ... rest of existing code
};
```

## Bước 3: Update Product Data Usage

Replace tất cả `product` references bằng `translatedBackendProduct`:

```javascript
// ❌ BEFORE:
const backendItem = product?.Result?.Item;
const backendVendor = product?.Result?.Vendor;
const backendRootPath = product?.Result?.RootPath;

// ✅ AFTER:
const backendItem = translatedBackendProduct?.Result?.Item;
const backendVendor = translatedBackendProduct?.Result?.Vendor;
const backendRootPath = translatedBackendProduct?.Result?.RootPath;
```

**⚠️ QUAN TRỌNG:** Tất cả chỗ sử dụng `product.Result.Item`, `product.Result.Vendor` đều phải đổi thành `translatedBackendProduct.Result.Item`, `translatedBackendProduct.Result.Vendor`

## Bước 4: Add Translation Toggle UI

Thêm Translation Toggle button vào UI (đặt sau Marketplace Badge):

```jsx
return (
  <div className="container mx-auto px-4 py-8">
    {/* Existing Marketplace Badge */}
    {marketplaceInfo && (
      <div className="mb-4">
        <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${marketplaceInfo.colors.bg} ${marketplaceInfo.colors.text}`}>
          {marketplaceInfo.name}
        </span>
      </div>
    )}

    {/* ✅ ADD: Translation Toggle */}
    <div className="mb-6">
      <TranslationToggle
        showOriginal={showOriginal}
        onToggle={toggleLanguage}
        isTranslating={isTranslating}
        hasTranslation={hasTranslation}
        sourceLang={platform === '1688' ? 'ZH' : 'EN'}
        targetLang="VI"
      />
    </div>

    {/* Rest of existing UI... */}
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-8">
      {/* ... */}
    </div>
  </div>
);
```

## Bước 5: Handle Translation Error (Optional)

Thêm error handling cho translation:

```jsx
// Inside component
useEffect(() => {
  if (translationError) {
    console.warn('Translation failed:', translationError);
    // Optional: Show toast
    // toast.warning('Không thể dịch sản phẩm. Hiển thị bản gốc.');
  }
}, [translationError]);
```

## Complete Code Example

```jsx
import React, { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import { productService } from '../services/productService';
import { addToCart } from '../../cart/services';
import { useCart } from '../../cart/context/CartContext';
import { useCurrency } from '../../currency/context/CurrencyContext';
import ProductImages from '../components/ProductImages';
import ProductInfo from '../components/ProductInfo';
// ... other imports

// ✅ Translation imports
import useProductTranslation from '../../translation/hooks/useProductTranslation';
import TranslationToggle from '../../translation/components/TranslationToggle';
import { transformForTranslation, applyTranslationsToBackend } from '../../translation/utils/productAdapter';

const ProductDetailPage = () => {
  const { platform, id } = useParams();
  const { refreshCart } = useCart();
  const { formatPrice } = useCurrency();

  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedVariant, setSelectedVariant] = useState(null);
  const [currentImages, setCurrentImages] = useState([]);
  const [currentPrice, setCurrentPrice] = useState(null);
  const [addingToCart, setAddingToCart] = useState(false);

  // ✅ Translation setup
  const flatProduct = product ? transformForTranslation(product) : null;
  const {
    displayProduct,
    isTranslating,
    showOriginal,
    toggleLanguage,
    hasTranslation
  } = useProductTranslation(flatProduct, platform, {
    autoTranslate: true,
    delayMs: 500
  });

  const translatedBackendProduct = displayProduct && product
    ? applyTranslationsToBackend(displayProduct, product)
    : product;

  // Existing fetch function
  const fetchProduct = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      if (!platform || !id) {
        throw new Error('VALIDATION_ERROR: Platform and ID are required');
      }

      if (!isValidMarketplace(platform)) {
        throw new Error(`VALIDATION_ERROR: Invalid platform "${platform}"`);
      }

      const data = await productService.getProductById(platform, id);
      setProduct(data);

      if (data.Result?.Item) {
        const item = data.Result.Item;
        setCurrentImages(item.Pictures?.map(p => p.Url) || []);
        setCurrentPrice(item.Price?.ConvertedPriceWithoutSign || item.Price?.OriginalPrice);
      }
    } catch (err) {
      console.error('Error fetching product:', err);
      let errorMessage = err.message || err.toString();
      if (err.code && err.provider) {
        errorMessage = `${errorMessage} (Provider: ${err.provider})`;
      }
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  }, [platform, id]);

  useEffect(() => {
    fetchProduct();
  }, [fetchProduct]);

  // ... rest of existing functions (handleVariantChange, handleAddToCart, etc.)

  if (loading) {
    return <Loading message="Loading product details..." />;
  }

  if (error) {
    return <ErrorMessage message={error} onRetry={fetchProduct} />;
  }

  if (!translatedBackendProduct || !translatedBackendProduct.Result || !translatedBackendProduct.Result.Item) {
    return <ErrorMessage message="Product not found" onRetry={fetchProduct} />;
  }

  // ✅ Use translatedBackendProduct instead of product
  const backendItem = translatedBackendProduct.Result.Item;
  const backendVendor = translatedBackendProduct.Result.Vendor;
  const backendRootPath = translatedBackendProduct.Result.RootPath;

  // ... normalize product (existing code)

  const marketplaceInfo = getMarketplaceInfo(translatedBackendProduct.platform || platform);

  return (
    <div className="container mx-auto px-4 py-8">
      {/* Marketplace Badge */}
      {marketplaceInfo && (
        <div className="mb-4">
          <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${marketplaceInfo.colors.bg} ${marketplaceInfo.colors.text}`}>
            {marketplaceInfo.name}
          </span>
        </div>
      )}

      {/* ✅ Translation Toggle */}
      <div className="mb-6">
        <TranslationToggle
          showOriginal={showOriginal}
          onToggle={toggleLanguage}
          isTranslating={isTranslating}
          hasTranslation={hasTranslation}
          sourceLang={platform === '1688' ? 'ZH' : 'EN'}
          targetLang="VI"
        />
      </div>

      {/* Rest of existing UI */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-8">
        <div>
          <ProductImages images={item.images} title={item.title} />
        </div>

        <div className="space-y-6">
          <ProductInfo
            product={normalizedProduct}
            seller={seller}
            platform={platform}
            productId={backendItem?.Id}
          />

          {/* ... rest of UI */}
        </div>
      </div>

      {/* ... rest of page */}
    </div>
  );
};

export default ProductDetailPage;
```

## Verification Checklist

After integration, verify:

- [ ] Product title được dịch sang tiếng Việt
- [ ] Attributes được dịch (Color, Size, etc.)
- [ ] Shop name được dịch
- [ ] Toggle button hiển thị
- [ ] Click toggle chuyển đổi giữa EN/ZH ↔ VI
- [ ] Translation được cache (check localStorage)
- [ ] Lần 2 vào trang load instant (từ cache)
- [ ] Console logs hiển thị translation progress
- [ ] Error handling hoạt động (fallback to original)

## Testing

```bash
# 1. Start frontend
cd Frontend
npm run dev

# 2. Navigate to product page
http://localhost:5173/aliexpress/products/1005005244562338

# 3. Check console for logs:
[Translation] Translating from en to vi...
[Product Translator] Extracted 25 texts
[Translation Cache] Saved: translation_aliexpress_1005005244562338

# 4. Reload page → should load from cache instantly
```

## Troubleshooting

**Problem:** Translation không chạy
- Check console errors
- Verify API key trong `translationApi.js`
- Check platform support (`aliexpress` hoặc `1688` only)

**Problem:** Translation quá lâu
- Giảm `delayMs` (risk: rate limit)
- Check số lượng fields cần dịch (console logs)

**Problem:** Cache không work
- Clear localStorage: DevTools → Application → Local Storage → Clear
- Check cache key format: `translation_{platform}_{productId}`

**Problem:** Toggle button không hiện
- Check `hasTranslation` flag
- Verify translation đã complete
- Check `isTranslating` state
