# ĐÃ FIX LỖI PHÂN TRANG SẢN PHẨM BỊ LẶP LẠI

**Ngày fix:** 2025-10-28

---

## ✅ NHỮNG GÌ ĐÃ SỬA

### 1. AliExpressController.java
- ✅ **Endpoint `/api/aliexpress/search`** (dòng 31-51)
- ✅ **Endpoint `/api/aliexpress/search/simple`** (dòng 57-77)

### 2. Alibaba1688Controller.java
- ✅ **Endpoint `/api/1688/search`** (dòng 34-54)
- ✅ **Endpoint `/api/1688/search/simple`** (dòng 62-82)

---

## 🔧 THAY ĐỔI

### Trước đây (SAI):
```java
@RequestParam(defaultValue = "0") int framePosition
@RequestParam(defaultValue = "12") int frameSize
// Frontend phải tự tính offset → Dễ bị lỗi!
```

### Bây giờ (ĐÚNG):
```java
@RequestParam(defaultValue = "1") int page
@RequestParam(defaultValue = "12") int pageSize

// Backend tự động convert
int framePosition = (page - 1) * pageSize;
```

---

## 📝 CÁCH SỬ DỤNG MỚI

### Frontend chỉ cần gửi `page` number:

**AliExpress:**
```javascript
// Page 1
GET /api/aliexpress/search/simple?keyword=iphone&page=1&pageSize=12

// Page 2
GET /api/aliexpress/search/simple?keyword=iphone&page=2&pageSize=12

// Page 3
GET /api/aliexpress/search/simple?keyword=iphone&page=3&pageSize=12
```

**Alibaba 1688:**
```javascript
// Page 1
GET /api/1688/search/simple?keyword=联想&page=1&pageSize=12

// Page 2
GET /api/1688/search/simple?keyword=联想&page=2&pageSize=12
```

---

## 🧪 CÁCH TEST

### 1. Khởi động lại ứng dụng:
```bash
./mvnw spring-boot:run
```

### 2. Test AliExpress:
```bash
# Page 1 - Lấy items 0-11
curl "http://localhost:8080/api/aliexpress/search/simple?keyword=phone&page=1&pageSize=12"

# Page 2 - Lấy items 12-23 (PHẢI KHÁC với Page 1)
curl "http://localhost:8080/api/aliexpress/search/simple?keyword=phone&page=2&pageSize=12"

# Page 3 - Lấy items 24-35 (PHẢI KHÁC với Page 1, 2)
curl "http://localhost:8080/api/aliexpress/search/simple?keyword=phone&page=3&pageSize=12"
```

### 3. Test Alibaba 1688:
```bash
# Page 1
curl "http://localhost:8080/api/1688/search/simple?keyword=联想&page=1&pageSize=12"

# Page 2
curl "http://localhost:8080/api/1688/search/simple?keyword=联想&page=2&pageSize=12"
```

### 4. Kiểm tra kết quả:
- ✅ Page 2 phải có **sản phẩm khác hoàn toàn** với Page 1
- ✅ Không có product ID nào trùng lặp giữa các trang
- ✅ Mỗi trang có đúng 12 sản phẩm (hoặc pageSize đã set)

---

## 📊 CÔNG THỨC CHUYỂN ĐỔI

```
Page 1: framePosition = (1 - 1) × 12 = 0   → Items 0-11
Page 2: framePosition = (2 - 1) × 12 = 12  → Items 12-23
Page 3: framePosition = (3 - 1) × 12 = 24  → Items 24-35
Page 4: framePosition = (4 - 1) × 12 = 36  → Items 36-47
...
```

**Tổng quát:**
```
framePosition = (page - 1) × pageSize
```

---

## 🎯 KẾT QUẢ

- ✅ **Frontend đơn giản hơn**: Chỉ cần gửi page number (1, 2, 3...)
- ✅ **Không còn lặp lại sản phẩm**: Mỗi trang có items khác nhau
- ✅ **API dễ sử dụng hơn**: Phù hợp với chuẩn pagination thông thường
- ✅ **Tương thích ngược**: Frontend cũ vẫn hoạt động nếu update parameter names

---

## ⚠️ LƯU Ý CHO FRONTEND

**Nếu frontend đang dùng `framePosition` parameter:**

```javascript
// ❌ CŨ - Không còn hoạt động
fetch('/api/aliexpress/search/simple?framePosition=0&frameSize=12')

// ✅ MỚI - Phải đổi thành
fetch('/api/aliexpress/search/simple?page=1&pageSize=12')
```

**Đổi parameter names:**
- `framePosition` → `page` (bắt đầu từ 1, không phải 0)
- `frameSize` → `pageSize`

---

**Fix completed! 🚀**
