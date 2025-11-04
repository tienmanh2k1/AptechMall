# Cart & Order Features - Setup Guide

## 🔴 Lỗi "No static resource api/users/1/cart"

### Nguyên nhân
Backend của bạn **chưa có Cart và Order API endpoints**. Frontend đang gọi các API endpoint mà backend chưa implement.

---

## ✅ GIẢI PHÁP: Sử dụng Mock API

Tôi đã tạo **Mock API** để bạn có thể test frontend ngay lập tức mà không cần backend.

### Cách bật Mock API

**Mở file:** `src/config/api.config.js`

```javascript
// Set to true để dùng Mock API (test không cần backend)
// Set to false để dùng Real Backend API
export const USE_MOCK_API = true;  // ← Đặt true để test
```

### Restart Dev Server

```bash
# Stop server (Ctrl+C)
# Start lại
npm run dev
```

Bạn sẽ thấy console log:
```
[API CONFIG] Using MOCK API
[CART API] Using MOCK implementation
[ORDER API] Using MOCK implementation
```

---

## 🧪 Test Frontend với Mock API

### 1. **Test Add to Cart**
- Vào product detail page: `/aliexpress/products/1005005244562338`
- Click "Add to Cart"
- ✅ Sẽ thấy toast notification "Product added to cart!"
- ✅ Cart badge ở header tăng lên

### 2. **Test Cart Page**
- Click vào cart icon hoặc vào `/cart`
- ✅ Thấy sản phẩm vừa thêm
- ✅ Có thể tăng/giảm số lượng
- ✅ Có thể xóa item
- ✅ Thấy tổng tiền theo từng currency

### 3. **Test Checkout**
- Trong cart, click "Proceed to Checkout"
- ✅ Điền form shipping (address, phone)
- ✅ Click "Place Order"
- ✅ Redirect đến success page

### 4. **Test Orders List**
- Click "Orders" ở header hoặc vào `/orders`
- ✅ Thấy 3 sample orders (DELIVERED, SHIPPING, PENDING)
- ✅ Filter theo status
- ✅ Click vào order để xem detail

### 5. **Test Order Detail**
- Click vào một order
- ✅ Thấy đầy đủ thông tin order
- ✅ Nếu status = PENDING, có thể Cancel

---

## 🔌 Khi Backend Ready

### Backend cần implement các endpoints sau:

#### **Cart Endpoints**
```
GET    /api/users/{userId}/cart
POST   /api/users/{userId}/cart/items
PUT    /api/users/{userId}/cart/items/{itemId}
DELETE /api/users/{userId}/cart/items/{itemId}
DELETE /api/users/{userId}/cart
```

#### **Order Endpoints**
```
POST   /api/users/{userId}/orders
GET    /api/users/{userId}/orders
GET    /api/users/{userId}/orders/{orderId}
POST   /api/users/{userId}/orders/{orderId}/cancel
PATCH  /api/users/{userId}/orders/{orderId}/status
```

### Request/Response Examples

#### Add to Cart Request
```json
POST /api/users/1/cart/items
Content-Type: application/json

{
  "productId": "1005005244562338",
  "platform": "aliexpress",
  "title": "Sample Product",
  "price": 29.99,
  "currency": "USD",
  "image": "https://...",
  "quantity": 1,
  "selectedVariant": null
}
```

#### Get Cart Response
```json
{
  "userId": 1,
  "items": [
    {
      "id": 1,
      "productId": "1005005244562338",
      "platform": "aliexpress",
      "title": "Sample Product",
      "price": 29.99,
      "currency": "USD",
      "image": "https://...",
      "quantity": 2,
      "selectedVariant": null
    }
  ]
}
```

#### Create Order Request
```json
POST /api/users/1/orders
Content-Type: application/json

{
  "shippingAddress": "123 Main St, City, Country",
  "phone": "+1234567890",
  "note": "Please deliver in the morning"
}
```

#### Create Order Response
```json
{
  "id": 1,
  "orderNumber": 1001,
  "userId": 1,
  "status": "PENDING",
  "shippingAddress": "123 Main St, City, Country",
  "phone": "+1234567890",
  "note": "Please deliver in the morning",
  "items": [
    {
      "id": 1,
      "productId": "1005005244562338",
      "platform": "aliexpress",
      "title": "Sample Product",
      "price": 29.99,
      "currency": "USD",
      "quantity": 2,
      "image": "https://..."
    }
  ],
  "createdAt": "2025-01-15T10:30:00Z",
  "updatedAt": "2025-01-15T10:30:00Z"
}
```

### Switch sang Real Backend

1. **Đảm bảo backend đang chạy** trên `http://localhost:8080`

2. **Test backend trực tiếp**:
```bash
curl http://localhost:8080/api/users/1/cart
```

3. **Nếu backend trả về data**, mở `src/config/api.config.js`:
```javascript
export const USE_MOCK_API = false;  // ← Đổi thành false
```

4. **Restart dev server**:
```bash
npm run dev
```

5. **Kiểm tra console**:
```
[API CONFIG] Using REAL API
[CART API] Using REAL backend implementation
[ORDER API] Using REAL backend implementation
```

---

## 📁 File Structure

```
src/
├── config/
│   └── api.config.js              # ← Config mock/real API
│
├── features/
│   ├── cart/
│   │   ├── services/
│   │   │   ├── index.js           # ← Service selector
│   │   │   ├── cartApi.js         # Real backend API
│   │   │   └── cartApiMock.js     # Mock API (in-memory)
│   │   ├── components/
│   │   ├── context/
│   │   └── pages/
│   │
│   └── order/
│       ├── services/
│       │   ├── index.js           # ← Service selector
│       │   ├── orderApi.js        # Real backend API
│       │   └── orderApiMock.js    # Mock API (sample data)
│       ├── components/
│       └── pages/
```

---

## 🐛 Troubleshooting

### Lỗi: "Cannot find module './services'"
- Đảm bảo đã tạo `src/features/cart/services/index.js`
- Đảm bảo đã tạo `src/features/order/services/index.js`
- Restart dev server

### Mock API không hoạt động
- Kiểm tra `USE_MOCK_API = true` trong `api.config.js`
- Check browser console có log `[MOCK]` không
- Clear browser cache và restart

### Cart badge không update
- Kiểm tra `CartProvider` đã wrap `<App />` chưa (trong `App.jsx`)
- Check console có error không

### Backend ready nhưng vẫn dùng Mock
- Đổi `USE_MOCK_API = false` trong `api.config.js`
- **Restart dev server** (quan trọng!)

---

## 🎯 Next Steps

1. ✅ **Hiện tại**: Dùng Mock API để test frontend
2. 🔜 **Backend Team**: Implement Cart & Order endpoints
3. 🔜 **Integration**: Test với real backend
4. 🔜 **Production**: Deploy với real API

---

## 📞 Support

Nếu gặp vấn đề:
1. Check console logs (browser + terminal)
2. Verify `api.config.js` settings
3. Test backend endpoints với `curl` hoặc Postman
4. Check network tab trong DevTools

Good luck! 🚀
