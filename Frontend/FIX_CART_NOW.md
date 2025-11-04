# ⚡ Fix Cart Issue - Quick Guide

## 🔴 Vấn đề
Không xem được trang `/cart` sau khi add sản phẩm

---

## ✅ GIẢI PHÁP NHANH (2 phút)

### **Bước 1: Check Config**
```bash
# Mở file:
src/config/api.config.js

# Đảm bảo:
export const USE_MOCK_API = true;
```

### **Bước 2: Restart Server**
```bash
# Trong terminal (Ctrl+C để stop nếu đang chạy)
npm run dev
```

### **Bước 3: Test**
1. Mở: `http://localhost:5173/cart-debug`
2. Click **"Test Add to Cart"**
3. Nếu thấy result JSON → Mock API đang hoạt động ✅
4. Vào: `http://localhost:5173/cart`
5. Should see product ✅

---

## 🔍 Kiểm tra Console

**Mở DevTools (F12) → Console tab**

### ✅ Success logs:
```
[API CONFIG] Using MOCK API
[CART API] Using MOCK implementation
[CartPage] Cart data received: {...}
```

### ❌ Nếu thấy error:

#### Error: "Cannot find module"
→ **Fix:** Restart dev server

#### Error: "Failed to fetch"
→ **Fix:** Check `USE_MOCK_API = true`

#### Error: "cart.items is not iterable"
→ **Fix:** Clear cache:
```bash
# Stop server (Ctrl+C)
rmdir /s /q node_modules\.vite
npm run dev
```

---

## 🎯 Test Steps

```
1. Add product từ product detail page
   ↓
2. See toast "Product added to cart!" ✅
   ↓
3. Cart badge increases ✅
   ↓
4. Click cart icon
   ↓
5. Cart page loads với product ✅
```

---

## 🚨 Vẫn không work?

### **Hard Reset:**
```bash
# 1. Stop server
Ctrl+C

# 2. Clear all cache
rmdir /s /q node_modules\.vite

# 3. Verify config
# Check: src/config/api.config.js
# USE_MOCK_API = true

# 4. Restart
npm run dev

# 5. Clear browser (Ctrl+Shift+Delete)

# 6. Test debug page
http://localhost:5173/cart-debug
```

---

## 📁 Files Changed

Đã fix các files sau:
- ✅ `src/features/cart/services/index.js` - Fixed top-level await
- ✅ `src/features/order/services/index.js` - Fixed top-level await
- ✅ `src/features/cart/context/CartContext.jsx` - Added debug logs
- ✅ `src/features/cart/pages/CartPage.jsx` - Added debug logs
- ✅ Created: `src/features/cart/pages/CartDebugPage.jsx`

---

## 💡 Debug URLs

- **Cart Debug:** http://localhost:5173/cart-debug
- **Cart Page:** http://localhost:5173/cart
- **Sample Product:** http://localhost:5173/aliexpress/products/1005005244562338

---

## ✅ Khi OK

Bạn sẽ thấy:
- ✅ No errors in console
- ✅ Cart page loads
- ✅ Can add/remove items
- ✅ Cart badge updates
- ✅ Toast notifications work

---

**Chi tiết:** Xem [DEBUG_CART_ISSUE.md](./DEBUG_CART_ISSUE.md)
