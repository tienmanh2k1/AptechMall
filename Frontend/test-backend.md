# Test Backend Cart API

## 1. Kiểm tra backend đang chạy
```bash
curl http://localhost:8080/api/health
# Hoặc mở browser: http://localhost:8080/api/
```

## 2. Test Cart GET endpoint
```bash
curl -X GET http://localhost:8080/api/users/1/cart
```

**Expected response:**
```json
{
  "userId": 1,
  "items": []
}
```

**Nếu lỗi 404:** Backend chưa có cart endpoints

## 3. Test Add to Cart endpoint
```bash
curl -X POST http://localhost:8080/api/users/1/cart/items \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "123",
    "platform": "aliexpress",
    "title": "Test Product",
    "price": 29.99,
    "currency": "USD",
    "image": "https://example.com/image.jpg",
    "quantity": 1
  }'
```

## 4. Kiểm tra backend logs
Xem backend console có báo lỗi gì không khi frontend call API.
