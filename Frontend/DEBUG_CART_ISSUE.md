# 🐛 Debug: Không xem được trang giỏ hàng

## Vấn đề
Sau khi add sản phẩm vào giỏ hàng thành công, không xem được trang `/cart`

---

## ✅ Đã Fix

### 1. **Top-level await issue**
- ❌ **Old:** Service selectors dùng top-level `await` → crash module loading
- ✅ **Fixed:** Refactor sang lazy loading pattern

### 2. **Added debug logs**
- Console sẽ hiển thị chi tiết mỗi bước
- Dễ dàng track được lỗi ở đâu

### 3. **Created debug page**
- Page riêng để test cart service trực tiếp
- URL: `http://localhost:5173/cart-debug`

---

## 🔍 Cách Debug

### **Bước 1: Kiểm tra Browser Console**

1. **Mở DevTools** (F12)
2. **Vào tab Console**
3. **Reload trang** `/cart`
4. **Tìm logs:**

```
[API CONFIG] Using MOCK API
[CART API] Using MOCK implementation
[CartContext] Fetching cart count for user: 1
[CartContext] Cart data: {...}
[CartPage] Fetching cart for user: 1
[CartPage] Cart data received: {...}
```

### **Bước 2: Check Errors**

Nếu có lỗi, console sẽ hiện:
```
[CartPage] Error fetching cart: ...
```

**Các lỗi thường gặp:**

#### ❌ **Error: "Cannot read properties of undefined"**
**Nguyên nhân:** Mock API chưa load
**Fix:** Restart dev server
```bash
# Ctrl+C để stop
npm run dev
```

#### ❌ **Error: "Failed to fetch"**
**Nguyên nhân:** Backend API fail (nếu `USE_MOCK_API = false`)
**Fix:**
1. Check `src/config/api.config.js`
2. Đặt `USE_MOCK_API = true`
3. Restart dev server

#### ❌ **Error: "cart.items is not iterable"**
**Nguyên nhân:** Cart data structure sai
**Fix:** Kiểm tra mock API response

---

## 🧪 Test với Debug Page

### **Vào Debug Page**
```
http://localhost:5173/cart-debug
```

### **Test các functions:**

1. **Click "Test Get Cart"**
   - ✅ Should return: `{ userId: 1, items: [...] }`
   - ❌ If error: Check console

2. **Click "Test Add to Cart"**
   - ✅ Should add a test product
   - ✅ Check result displayed

3. **Test real flow:**
   - Add product từ debug page
   - Vào `/cart`
   - Should see product

---

## 🔧 Manual Fixes

### **Fix 1: Verify Mock API Config**

**File:** `src/config/api.config.js`
```javascript
// MUST be true for testing without backend
export const USE_MOCK_API = true;
```

### **Fix 2: Clear Cache**

```bash
# Stop server
Ctrl+C

# Clear cache
rm -rf node_modules/.vite

# Restart
npm run dev
```

### **Fix 3: Check CartProvider**

**File:** `src/App.jsx`

Đảm bảo có `<CartProvider>`:
```jsx
<Router>
  <CartProvider>  {/* ← MUST have this */}
    <Layout>
      <Routes>
        ...
      </Routes>
    </Layout>
  </CartProvider>
</Router>
```

### **Fix 4: Verify imports**

**File:** `src/features/cart/pages/CartPage.jsx`

```javascript
import { getCart, updateCartItem, removeCartItem } from '../services';
// NOT from '../services/cartApi' or '../services/cartApiMock'
```

---

## 📊 Expected Console Output

### **Khi vào /cart (Success):**

```
[API CONFIG] Using MOCK API
[CART API] Using MOCK implementation
[CartContext] Fetching cart count for user: 1
[MOCK] Getting cart for user: 1
[CartContext] Cart data: { userId: 1, items: [...] }
[CartContext] Cart count: 2
[CartPage] Fetching cart for user: 1
[MOCK] Getting cart for user: 1
[CartPage] Cart data received: { userId: 1, items: [...] }
```

### **Khi Add to Cart (Success):**

```
[ProductDetailPage] Adding to cart: {...}
[MOCK] Adding to cart: {...}
✅ Product added to cart!
[CartContext] Fetching cart count for user: 1
[MOCK] Getting cart for user: 1
[CartContext] Cart count: 3
```

---

## 🚑 Emergency Quick Fix

Nếu vẫn không work, thử **hard reset:**

```bash
# 1. Stop server
Ctrl+C

# 2. Delete cache
rmdir /s /q node_modules\.vite

# 3. Restart
npm run dev

# 4. Clear browser cache
# Browser: Ctrl+Shift+Delete → Clear cache

# 5. Hard reload page
# Browser: Ctrl+Shift+R
```

---

## 📞 Still Not Working?

### **Checklist:**

- [ ] `USE_MOCK_API = true` trong `api.config.js`?
- [ ] Dev server đã restart?
- [ ] Browser console có error gì?
- [ ] `/cart-debug` page có work không?
- [ ] CartProvider wrap App trong `App.jsx`?
- [ ] Clear cache rồi?

### **Share info này để debug:**

1. **Console logs** (copy toàn bộ)
2. **Error message** (nếu có)
3. **Browser**: Chrome/Firefox/Edge?
4. **Node version**: `node -v`

---

## 🎯 Test Flow Hoàn Chỉnh

```bash
# 1. Restart server
npm run dev

# 2. Test debug page
http://localhost:5173/cart-debug
→ Click "Test Add to Cart"
→ Should see result

# 3. Test real cart page
http://localhost:5173/cart
→ Should see product added

# 4. Test add from product page
http://localhost:5173/aliexpress/products/1005005244562338
→ Click "Add to Cart"
→ Cart badge should increase
→ Go to /cart
→ Should see product
```

---

## ✅ Success Indicators

Khi mọi thứ OK, bạn sẽ thấy:

1. ✅ Console không có error màu đỏ
2. ✅ Cart page load được
3. ✅ Cart badge hiển thị số items
4. ✅ Có thể add/update/remove items
5. ✅ Toast notifications hoạt động

Good luck debugging! 🚀
