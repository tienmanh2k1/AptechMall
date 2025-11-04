# 🚀 Quick Start - Test Cart & Order

## Lỗi bạn gặp phải

```
No static resource api/users/1/cart
```

**Nguyên nhân:** Backend chưa có Cart/Order API endpoints.

**Giải pháp:** Đã tạo Mock API để test frontend ngay!

---

## ✅ Bật Mock API (1 phút)

### Bước 1: Mở file config
```bash
D:\Documents\React\myapp\src\config\api.config.js
```

### Bước 2: Set USE_MOCK_API = true
```javascript
export const USE_MOCK_API = true;  // ← Đảm bảo = true
```

### Bước 3: Restart server
```bash
# Ctrl+C để stop
npm run dev
```

### Bước 4: Check console
Bạn sẽ thấy:
```
[API CONFIG] Using MOCK API
[CART API] Using MOCK implementation
[ORDER API] Using MOCK implementation
```

---

## 🧪 Test ngay (5 phút)

### 1. Add to Cart
- Vào: http://localhost:5173/aliexpress/products/1005005244562338
- Click **"Add to Cart"**
- ✅ Thấy toast "Product added to cart!"
- ✅ Cart badge tăng lên

### 2. View Cart
- Click cart icon (góc trên phải)
- ✅ Thấy sản phẩm vừa add
- ✅ Tăng/giảm số lượng
- ✅ Xóa item

### 3. Checkout
- Click **"Proceed to Checkout"**
- Điền form:
  ```
  Address: 123 Test Street
  Phone: +84123456789
  ```
- Click **"Place Order"**
- ✅ Redirect đến success page

### 4. View Orders
- Click **"Orders"** ở header
- ✅ Thấy 3 sample orders + order vừa tạo
- ✅ Click vào order để xem chi tiết

---

## 🔄 Khi Backend Ready

### Check backend đã có API chưa
```bash
curl http://localhost:8080/api/users/1/cart
```

Nếu trả về JSON → Backend ready!

### Switch sang Real API
1. Mở: `src/config/api.config.js`
2. Đổi: `export const USE_MOCK_API = false;`
3. Restart: `npm run dev`

---

## 📚 Đọc thêm

- **Chi tiết:** [CART_ORDER_SETUP.md](./CART_ORDER_SETUP.md)
- **Backend API Spec:** [CART_ORDER_SETUP.md#backend-cần-implement](./CART_ORDER_SETUP.md)
- **Test Backend:** [test-backend.md](./test-backend.md)

---

## 🎯 Summary

| Trạng thái | Action |
|-----------|--------|
| ✅ Frontend hoàn thành | Cart + Order features |
| ✅ Mock API ready | Test không cần backend |
| 🔜 Backend cần implement | 10 API endpoints |
| 🔜 Production ready | Switch `USE_MOCK_API = false` |

**Bây giờ bạn có thể test toàn bộ flow mà không cần backend!** 🎉
