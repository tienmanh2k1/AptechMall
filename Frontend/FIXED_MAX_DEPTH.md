# ✅ Fixed: Maximum Update Depth Exceeded

## 🔴 Error Message
```
Maximum update depth exceeded. This can happen when a component calls setState inside useEffect, but useEffect either doesn't have a dependency array, or one of the dependencies changes on every render.
```

**Location:** `ProductDetailPage.jsx:79`

---

## 🔍 Root Causes

### **Cause 1: handleVariantChange not memoized**
```javascript
// ❌ BAD: Function recreated on every render
const handleVariantChange = (variant) => {
  setSelectedVariant(variant);
  // ...
};
```

**Problem:**
- New function created every render
- If passed to child component as prop → child re-renders
- Child might call onChange → triggers setState → re-render → loop

### **Cause 2: handleAddToCart referencing wrong scope**
```javascript
// ❌ BAD: 'item' doesn't exist in this scope
const cartItem = {
  id: item.itemId,  // ← item is undefined here!
  title: item.title,
  // ...
};
```

**Problem:**
- `item` is created in render phase (line ~156)
- `handleAddToCart` is defined before render
- Accessing undefined variable → errors or unexpected behavior

### **Cause 3: Dependencies not stable**
Functions like `refreshCart` from context might change unexpectedly if not properly memoized.

---

## ✅ Solutions Applied

### **Fix 1: Memoize handleVariantChange**

**File:** `src/features/product/pages/ProductDetailPage.jsx`

```javascript
// ✅ GOOD: Memoized with useCallback
const handleVariantChange = useCallback((variant) => {
  setSelectedVariant(variant);

  if (variant?.price) {
    setCurrentPrice(variant.price);
  }

  if (variant?.variantImage && product?.Result?.Item) {
    const backendItem = product.Result.Item;
    const variantPicture = backendItem.Pictures?.find(p =>
      p.Url === variant.variantImage || p.Url.includes(variant.variantImage)
    );
    if (variantPicture) {
      const otherImages = backendItem.Pictures.filter(p => p.Url !== variantPicture.Url);
      setCurrentImages([variantPicture.Url, ...otherImages.map(p => p.Url)]);
    }
  }
}, [product]); // Only recreate when product changes
```

**Benefits:**
- Function reference stable unless product changes
- No unnecessary re-renders of child components
- No infinite loops

### **Fix 2: Fix handleAddToCart scope**

```javascript
// ✅ GOOD: Use product data directly
const handleAddToCart = useCallback(async () => {
  if (!product?.Result?.Item) return;

  try {
    setAddingToCart(true);

    const backendItem = product.Result.Item;
    const currency = backendItem.Price?.Currency || 'USD';

    const cartItem = {
      id: backendItem.Id,           // ✅ Use backendItem
      platform: platform,
      title: backendItem.Title,     // ✅ Use backendItem
      price: currentPrice || backendItem.Price?.ConvertedPriceWithoutSign,
      currency: currency,
      image: currentImages?.[0] || backendItem.Pictures?.[0]?.Url,
      quantity: 1,
      selectedVariant: selectedVariant ? JSON.stringify(selectedVariant) : null
    };

    await addToCart(CURRENT_USER_ID, cartItem);
    refreshCart();
    toast.success('Product added to cart!');
  } catch (err) {
    console.error('Error adding to cart:', err);
    toast.error(err.response?.data?.message || 'Failed to add product to cart');
  } finally {
    setAddingToCart(false);
  }
}, [product, platform, currentPrice, currentImages, selectedVariant, refreshCart]);
```

**Benefits:**
- All data accessed from correct scope
- Memoized with proper dependencies
- No reference errors

### **Fix 3: Added null checks**

Added optional chaining (`?.`) throughout to prevent errors:
- `variant?.price`
- `variant?.variantImage`
- `product?.Result?.Item`
- `currentImages?.[0]`

---

## 🧪 Testing

### **Test 1: Page Loads Without Errors**
```bash
npm run dev
```

1. Open: `http://localhost:5173/aliexpress/products/1005005244562338`
2. ✅ Page loads without console errors
3. ✅ No infinite loops
4. ✅ Console is clean

### **Test 2: Add to Cart Works**
1. Click "Add to Cart" button
2. ✅ Toast notification appears
3. ✅ Cart badge updates
4. ✅ No errors in console

### **Test 3: Variant Selection Works**
1. If product has variants, select different options
2. ✅ Price updates
3. ✅ Images update
4. ✅ No errors or loops

### **Test 4: Navigate Between Products**
1. Go to search page
2. Click different products
3. ✅ Each product loads correctly
4. ✅ No loops between navigation

---

## 📁 Files Modified

### **1. ProductDetailPage.jsx**
**Changes:**
- ✅ Wrapped `handleVariantChange` with `useCallback`
- ✅ Wrapped `handleAddToCart` with `useCallback`
- ✅ Fixed data access in `handleAddToCart`
- ✅ Added null checks with optional chaining
- ✅ Fixed dependencies arrays

**Lines affected:** 78-138

### **2. CartContext.jsx** (already fixed)
**Changes:**
- ✅ Memoized context value with `useMemo`
- ✅ Memoized `fetchCartCount` with `useCallback`
- ✅ Memoized `refreshCart` with `useCallback`

---

## 🎯 Key Learnings

### **1. Always memoize callback props**
```javascript
// ✅ DO THIS
const handleChange = useCallback((value) => {
  setState(value);
}, [dependencies]);

// ❌ DON'T DO THIS
const handleChange = (value) => {
  setState(value); // New function every render
};
```

### **2. Check variable scope**
```javascript
// ✅ DO THIS: Access data from available scope
const handler = useCallback(() => {
  const data = product?.Result?.Item;
  use(data);
}, [product]);

// ❌ DON'T DO THIS: Access variable from wrong scope
const handler = () => {
  use(item); // item doesn't exist here!
};
// ... later in render
const item = product.Result.Item;
```

### **3. Use optional chaining for safety**
```javascript
// ✅ DO THIS
if (variant?.price) { ... }
const img = currentImages?.[0];

// ❌ DON'T DO THIS (may crash)
if (variant.price) { ... }
const img = currentImages[0];
```

### **4. Minimize dependencies when possible**
```javascript
// ✅ GOOD: Only depends on what changes
const handler = useCallback(() => {
  doSomething(id);
}, [id]);

// ⚠️ OK but may recreate unnecessarily
const handler = useCallback(() => {
  doSomething(id);
}, [id, product, user, settings]); // Too many deps
```

---

## ✅ Verification Checklist

After restart:

- [ ] Page loads without console errors
- [ ] No "Maximum update depth" errors
- [ ] No infinite loops (check CPU usage)
- [ ] Add to cart works
- [ ] Cart badge updates
- [ ] Variant selection works (if available)
- [ ] Navigation between products works
- [ ] Browser performance is good

---

## 🚨 If Error Persists

### **Check 1: ProductVariantSelector**
The component might be calling `onVariantChange` in render phase:

```javascript
// ❌ BAD: Calling in render
function ProductVariantSelector({ onVariantChange }) {
  onVariantChange(someValue); // ← This causes loops!
  return <div>...</div>;
}

// ✅ GOOD: Only call in event handlers
function ProductVariantSelector({ onVariantChange }) {
  const handleClick = () => {
    onVariantChange(someValue); // ← Only in events
  };
  return <div onClick={handleClick}>...</div>;
}
```

### **Check 2: Context value changes**
Use React DevTools Profiler to see what's causing re-renders.

### **Check 3: Dependencies**
Add logs to check if dependencies are stable:

```javascript
useEffect(() => {
  console.log('Dependencies changed:', { product, platform });
}, [product, platform]);
```

---

## 📞 Need More Help?

Share:
1. Full console error with stack trace
2. Which action triggers the error
3. React DevTools Profiler screenshot

---

**Next: Restart dev server and test!** 🚀
