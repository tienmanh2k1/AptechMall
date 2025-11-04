# ✅ ĐÃ SỬA CART API - FIX LỖI 403 FORBIDDEN

**Date**: 2025-10-29
**Root Cause**: Frontend gọi `/api/users/{userId}/cart/...` → Bị chặn bởi Security Rule (chỉ ADMIN/STAFF)

---

## 🔧 ĐÃ SỬA - CART ENDPOINTS

### ✅ GET CART

**❌ TRƯỚC (SAI):**
```javascript
GET /api/users/1/cart
```

**✅ SAU (ĐÚNG):**
```javascript
GET /api/cart?userId=1
```

**Code:**
```javascript
export const getCart = async (userId) => {
  const response = await api.get(`/cart?userId=${userId}`);
  return response.data;
};
```

---

### ✅ ADD TO CART

**❌ TRƯỚC (SAI):**
```javascript
POST /api/users/1/cart/items
```

**✅ SAU (ĐÚNG):**
```javascript
POST /api/cart/items?userId=1
```

**Code:**
```javascript
export const addToCart = async (userId, product) => {
  const response = await api.post(`/cart/items?userId=${userId}`, requestBody);
  return response.data;
};
```

---

### ✅ UPDATE CART ITEM

**❌ TRƯỚC (SAI):**
```javascript
PUT /api/users/1/cart/items/123
```

**✅ SAU (ĐÚNG):**
```javascript
PUT /api/cart/items/123?userId=1
```

**Code:**
```javascript
export const updateCartItem = async (userId, itemId, quantity) => {
  const response = await api.put(`/cart/items/${itemId}?userId=${userId}`, {
    quantity
  });
  return response.data;
};
```

---

### ✅ REMOVE CART ITEM

**❌ TRƯỚC (SAI):**
```javascript
DELETE /api/users/1/cart/items/123
```

**✅ SAU (ĐÚNG):**
```javascript
DELETE /api/cart/items/123?userId=1
```

**Code:**
```javascript
export const removeCartItem = async (userId, itemId) => {
  const response = await api.delete(`/cart/items/${itemId}?userId=${userId}`);
  return response.data;
};
```

---

### ✅ CLEAR CART

**❌ TRƯỚC (SAI):**
```javascript
DELETE /api/users/1/cart
```

**✅ SAU (ĐÚNG):**
```javascript
DELETE /api/cart/clear?userId=1
```

**Code:**
```javascript
export const clearCart = async (userId) => {
  const response = await api.delete(`/cart/clear?userId=${userId}`);
  return response.data;
};
```

---

## 🔑 REQUEST FORMAT ĐẦY ĐỦ

### **ADD TO CART Request Body:**

```javascript
{
  "productId": "1005005244562338",
  "platform": "aliexpress",
  "title": "Winter Jacket",
  "price": 45.99,
  "currency": "USD",
  "image": "https://...",
  "quantity": 1,

  // Variant info (optional - nếu có)
  "variantId": "config-123",
  "variantName": "White - Size M",
  "variantOptions": "Color: White, Size: M"
}
```

### **Headers (Tự Động):**

```javascript
{
  "Content-Type": "application/json",
  "Authorization": "Bearer eyJhbGci..."  // Tự động thêm bởi interceptor
}
```

---

## 🧪 TEST NGAY

### **1. Test Add to Cart:**

```bash
# Sau khi login và có token
# Vào product detail page bất kỳ
# Click "Add to Cart"

# Kiểm tra trong DevTools Network tab:
✅ Request URL: http://localhost:8080/api/cart/items?userId=1
✅ Request Method: POST
✅ Status: 200 (không còn 403!)
✅ Response: Cart data với item mới
```

### **2. Test Get Cart:**

```bash
# Click vào icon giỏ hàng trong header
# Hoặc truy cập: /cart

# Kiểm tra trong DevTools Network tab:
✅ Request URL: http://localhost:8080/api/cart?userId=1
✅ Request Method: GET
✅ Status: 200
✅ Response: { items: [...], totalPrice: ... }
```

### **3. Test Update Quantity:**

```bash
# Trong cart page, tăng/giảm quantity

# Kiểm tra trong DevTools Network tab:
✅ Request URL: http://localhost:8080/api/cart/items/123?userId=1
✅ Request Method: PUT
✅ Status: 200
```

### **4. Test Remove Item:**

```bash
# Click nút xóa item trong cart

# Kiểm tra trong DevTools Network tab:
✅ Request URL: http://localhost:8080/api/cart/items/123?userId=1
✅ Request Method: DELETE
✅ Status: 200
```

---

## 📋 BACKEND SECURITY RULE (Giải Thích)

**SecurityConfig.java:**
```java
.requestMatchers("/api/users/**").hasAnyRole("ADMIN", "STAFF")
.requestMatchers("/api/cart/**").authenticated()
.requestMatchers("/api/orders/**").authenticated()
```

**Ý Nghĩa:**
- `/api/users/**` → Chỉ ADMIN/STAFF mới được truy cập
- `/api/cart/**` → Bất kỳ user đã login (CUSTOMER cũng được)
- `/api/orders/**` → Bất kỳ user đã login

**Tại Sao Bị 403 Trước Đây?**
- Frontend gọi: `/api/users/1/cart/items`
- Backend check rule: `/api/users/**` → Cần role ADMIN/STAFF
- User login là: CUSTOMER
- Kết quả: 403 Forbidden ❌

**Tại Sao Giờ OK?**
- Frontend gọi: `/api/cart/items?userId=1`
- Backend check rule: `/api/cart/**` → Chỉ cần authenticated
- User login: CUSTOMER (đã có token)
- Kết quả: 200 OK ✅

---

## ⚠️ ORDER API CŨNG CẦN SỬA

File: `src/features/order/services/orderApi.js`

**Hiện tại cũng đang dùng:**
- `/api/users/${userId}/orders` ❌
- Sẽ bị 403 tương tự!

**Cần sửa thành** (đoán):
- `/api/orders?userId=${userId}` ✅
- Hoặc `/api/orders` (backend tự lấy userId từ JWT token)

**👉 Hãy kiểm tra backend endpoint thực tế cho orders!**

---

## 🎉 KẾT QUẢ

✅ **CART API ĐÃ ĐƯỢC SỬA HOÀN TOÀN**

**File đã sửa:**
- `src/features/cart/services/cartApi.js`

**Thay đổi:**
- ✅ GET cart: `/cart?userId={userId}`
- ✅ POST add to cart: `/cart/items?userId={userId}`
- ✅ PUT update: `/cart/items/{itemId}?userId={userId}`
- ✅ DELETE remove: `/cart/items/{itemId}?userId={userId}`
- ✅ DELETE clear: `/cart/clear?userId={userId}`

**Test ngay để xác nhận 403 error đã biến mất!** 🚀
