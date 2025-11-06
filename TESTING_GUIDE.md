# 🧪 HƯỚNG DẪN TEST CHI TIẾT CÁC LUỒNG - APTECHMALL

**Ngày tạo:** 2025-11-06
**Mục đích:** Hướng dẫn từng bước để test tất cả các luồng trong project

---

## 📋 MỤC LỤC

1. [Setup môi trường](#1-setup-môi-trường)
2. [Test Authentication](#2-test-authentication)
3. [Test Product Browsing](#3-test-product-browsing)
4. [Test Shopping Cart](#4-test-shopping-cart)
5. [Test Wallet System](#5-test-wallet-system)
6. [Test Bank Transfer SMS](#6-test-bank-transfer-sms)
7. [Test Order & Checkout](#7-test-order--checkout)
8. [Test Admin Dashboard](#8-test-admin-dashboard)
9. [Test API với Postman](#9-test-api-với-postman)
10. [Troubleshooting](#10-troubleshooting)

---

## 1. SETUP MÔI TRƯỜNG

### 1.1. Prerequisites

#### Phần mềm cần cài đặt:
- ✅ **Java 17** - Để chạy backend
- ✅ **Node.js 18+** - Để chạy frontend
- ✅ **MySQL 8** - Database
- ✅ **Redis** - Token blacklist
- ✅ **Maven** - Build backend (có sẵn trong project: `./mvnw`)
- ⚠️ **Postman** (optional) - Test API

#### Kiểm tra đã cài đặt:
```bash
# Check Java
java -version
# Expected: java version "17.x.x"

# Check Node.js
node -v
# Expected: v18.x.x hoặc mới hơn

# Check MySQL
mysql --version
# Expected: mysql Ver 8.x.x

# Check Redis
redis-cli --version
# Expected: redis-cli x.x.x
```

### 1.2. Khởi động MySQL

#### Windows:
```bash
# Mở Services (Win + R, gõ "services.msc")
# Tìm "MySQL80" và click "Start"

# Hoặc command line:
net start MySQL80
```

#### Mac/Linux:
```bash
# Start MySQL
sudo systemctl start mysql
# hoặc
brew services start mysql
```

#### Tạo database:
```bash
# Login vào MySQL
mysql -u root -p

# Tạo database
CREATE DATABASE test_db;

# Kiểm tra
SHOW DATABASES;

# Thoát
EXIT;
```

### 1.3. Khởi động Redis

#### Windows:
```bash
# Download Redis for Windows từ GitHub
# https://github.com/microsoftarchive/redis/releases

# Giải nén và chạy:
redis-server.exe
```

#### Mac:
```bash
brew services start redis
```

#### Linux:
```bash
sudo systemctl start redis
```

#### Kiểm tra Redis đang chạy:
```bash
redis-cli ping
# Expected: PONG
```

### 1.4. Cấu hình Backend

#### Mở file `Backend/src/main/resources/application.properties`:
```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/test_db
spring.datasource.username=root
spring.datasource.password=       # Để trống nếu không có password

# RapidAPI Keys (Bắt buộc cho product search)
aliexpress.api.key=YOUR_RAPIDAPI_KEY
aliexpress.api.host=aliexpress-datahub.p.rapidapi.com

alibaba1688.api.key=YOUR_RAPIDAPI_KEY
alibaba1688.api.host=magic-aliababa.p.rapidapi.com

# JWT Secret
jwt.secret-key=your-secret-key-here
```

#### Lấy RapidAPI Key:
1. Truy cập https://rapidapi.com/
2. Đăng ký tài khoản (free)
3. Subscribe APIs:
   - AliExpress DataHub
   - Alibaba 1688
4. Copy API key và paste vào `application.properties`

### 1.5. Cấu hình Frontend

#### Tạo file `Frontend/.env`:
```bash
# Copy từ template
cd Frontend
cp .env.example .env
```

#### Mở file `Frontend/.env` và cấu hình:
```env
# Google OAuth Client ID (Optional - chỉ cần nếu test Google login)
VITE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com

# Backend API URL (default)
VITE_API_BASE_URL=http://localhost:8080/api
```

#### Lấy Google Client ID (Optional):
1. Truy cập https://console.cloud.google.com/
2. Tạo project mới
3. Enable Google+ API
4. Tạo OAuth 2.0 Client ID
5. Authorized redirect URIs: `http://localhost:5173`
6. Copy Client ID

### 1.6. Khởi động Backend

```bash
# Di chuyển vào thư mục Backend
cd Backend

# Compile và chạy
./mvnw spring-boot:run

# Windows:
mvnw.cmd spring-boot:run
```

#### Chờ backend khởi động:
```
Expected output:
...
Started AptechMallApplication in X.XXX seconds
Tomcat started on port 8080
```

#### Kiểm tra backend đang chạy:
Mở browser: http://localhost:8080/api/debug/health
Expected: `{"status":"UP"}`

### 1.7. Khởi động Frontend

Mở terminal mới (giữ backend đang chạy):

```bash
# Di chuyển vào thư mục Frontend
cd Frontend

# Install dependencies (chỉ lần đầu)
npm install

# Chạy dev server
npm run dev
```

#### Expected output:
```
VITE v5.x.x  ready in XXX ms

➜  Local:   http://localhost:5173/
➜  Network: use --host to expose
```

#### Kiểm tra frontend:
Mở browser: http://localhost:5173/
Expected: Thấy trang homepage PandaMall

---

## 2. TEST AUTHENTICATION

### 2.1. Test Đăng Ký (Register)

#### Bước 1: Vào trang đăng ký
1. Mở browser: http://localhost:5173/
2. Click nút "Register" ở góc phải header
3. Hoặc truy cập trực tiếp: http://localhost:5173/register

#### Bước 2: Điền form đăng ký
```
Full Name: Test User
Username: testuser001
Email: testuser001@example.com
Password: Test123!
Confirm Password: Test123!
```

#### Bước 3: Submit
- Click nút "Register"
- ✅ **Expected:**
  - Hiển thị success message
  - Redirect về trang login
  - Console log: "Registration successful"

#### Bước 4: Kiểm tra database
```sql
# Vào MySQL
mysql -u root -p test_db

# Check user mới tạo
SELECT user_id, username, email, role, status FROM users WHERE email = 'testuser001@example.com';

# Expected: 1 row với role='CUSTOMER', status='ACTIVE'
```

### 2.2. Test Đăng Nhập (Login)

#### Bước 1: Vào trang login
- URL: http://localhost:5173/login

#### Bước 2: Đăng nhập với tài khoản demo
```
Email/Username: demo.account@gmail.com
Password: demo123
```

#### Bước 3: Submit
- Click nút "Login"
- ✅ **Expected:**
  - Redirect về homepage (http://localhost:5173/)
  - Header hiển thị email user: "demo.account@gmail.com"
  - Thấy nút "Cart", "Orders", "Wallet"
  - Console log: "Login successful"

#### Bước 4: Kiểm tra localStorage
```javascript
// Mở browser DevTools (F12) → Console tab
// Chạy lệnh:
localStorage.getItem('token')
// Expected: Thấy JWT token string dài

localStorage.getItem('user')
// Expected: Thấy user object JSON
```

#### Bước 5: Kiểm tra token
```javascript
// Copy token từ localStorage
// Vào https://jwt.io/ và paste token

// Expected claims:
{
  "userId": 1,
  "email": "demo.account@gmail.com",
  "role": "CUSTOMER",
  "fullname": "Demo Account",
  "exp": 1234567890  // Expiry timestamp
}
```

### 2.3. Test Google OAuth Login

⚠️ **Yêu cầu:** Đã setup Google Client ID trong `.env`

#### Bước 1: Vào trang login
- URL: http://localhost:5173/login

#### Bước 2: Click "Sign in with Google"
- Click nút Google với icon

#### Bước 3: Chọn Google account
- Popup Google login mở ra
- Chọn tài khoản Google

#### Bước 4: Verify login
- ✅ **Expected:**
  - Popup đóng
  - Redirect về homepage
  - Header hiển thị email Google
  - User được tự động tạo trong database

#### Bước 5: Kiểm tra database
```sql
SELECT user_id, username, email, oAuth FROM users WHERE email = 'your-google-email@gmail.com';

# Expected: oAuth field chứa JSON với googleSub
```

### 2.4. Test Logout

#### Bước 1: Đang đăng nhập
- Đảm bảo đã login thành công

#### Bước 2: Click Logout
- Click "Logout" ở header dropdown
- Hoặc click icon user → Logout

#### Bước 3: Verify logout
- ✅ **Expected:**
  - Redirect về trang login
  - localStorage.token = null
  - localStorage.user = null
  - Không thể access protected routes

#### Bước 4: Test token blacklist
```javascript
// Copy token trước khi logout
const oldToken = 'your-old-token';

// Sau khi logout, thử gọi API với token cũ
fetch('http://localhost:8080/api/cart', {
  headers: {
    'Authorization': `Bearer ${oldToken}`
  }
})

// Expected: 401 Unauthorized (token đã bị blacklist)
```

### 2.5. Test Protected Routes

#### Bước 1: Logout
- Đảm bảo đã logout

#### Bước 2: Thử access protected route
- Vào URL: http://localhost:5173/cart

#### Bước 3: Verify redirect
- ✅ **Expected:**
  - Redirect về /login
  - URL có thêm `?redirect=/cart`
  - Sau khi login → redirect về /cart

---

## 3. TEST PRODUCT BROWSING

### 3.1. Test Search Products

#### Bước 1: Vào trang search
- URL: http://localhost:5173/search
- Hoặc click "Search Products" ở homepage

#### Bước 2: Tìm kiếm sản phẩm
```
Search keyword: "phone"
Click "Search" hoặc Enter
```

#### Bước 3: Verify results
- ✅ **Expected:**
  - Hiển thị grid sản phẩm (mặc định AliExpress)
  - Mỗi product card có:
    - Hình ảnh
    - Tên sản phẩm
    - Giá (USD)
    - Ratings & reviews
    - AliExpress badge
  - URL change: `?q=phone&page=1&sort=default`

#### Bước 4: Test filters

**Sort:**
```
Click dropdown "Sort by"
Chọn "Price: Low to High"

Expected:
- Products reorder theo giá tăng dần
- URL: ?q=phone&page=1&sort=price-asc
```

**Pagination:**
```
Scroll xuống cuối
Click nút "Next Page"

Expected:
- Load page 2
- URL: ?q=phone&page=2&sort=price-asc
```

**Switch marketplace:**
```
Click tab "1688"

Expected:
- Load 1688 products
- Giá hiển thị CNY (¥) thay vì USD ($)
- Badge đổi sang "1688"
```

#### Bước 5: Check console logs
```
Mở DevTools (F12) → Console

Expected logs:
- "Searching products: phone, page: 1"
- "API Response: 200 OK"
- Products array
```

### 3.2. Test Product Detail

#### Bước 1: Click vào 1 product từ search results
- Click bất kỳ product card nào

#### Bước 2: Verify product detail page
- ✅ **Expected:**
  - URL: `/aliexpress/products/{product_id}`
  - Product images carousel
  - Product title
  - Price with currency (USD or CNY)
  - Star ratings
  - Product attributes table
  - Add to Cart button

#### Bước 3: View images
```
Click vào thumbnail images

Expected:
- Main image thay đổi
- Hình lớn hiển thị
```

#### Bước 4: Test direct URL
```
Vào URL trực tiếp:
http://localhost:5173/aliexpress/products/1005005244562338

Expected:
- Load product detail đúng
- Không bị lỗi
```

#### Bước 5: Test 1688 product
```
URL:
http://localhost:5173/1688/products/123456

Expected:
- Product detail load
- Giá hiển thị CNY (¥)
- Badge "1688"
```

---

## 4. TEST SHOPPING CART

⚠️ **Yêu cầu:** Đã đăng nhập

### 4.1. Test Add to Cart

#### Bước 1: Vào product detail page
- URL: http://localhost:5173/aliexpress/products/1005005244562338

#### Bước 2: Add to cart
```
Quantity: 2
Click "Add to Cart"
```

#### Bước 3: Verify success
- ✅ **Expected:**
  - Success toast: "Added to cart"
  - Cart icon ở header có badge number (2)
  - Console log: "Item added to cart"

#### Bước 4: Check backend API
```javascript
// DevTools → Network tab
// Tìm request: POST /api/cart/items

Request payload:
{
  "productId": "1005005244562338",
  "productName": "...",
  "price": 99.99,
  "quantity": 2,
  "marketplace": "ALIEXPRESS",
  ...
}

Response: 200 OK
{
  "id": 1,
  "quantity": 2,
  ...
}
```

### 4.2. Test View Cart

#### Bước 1: Click cart icon
- Click icon giỏ hàng ở header
- Hoặc URL: http://localhost:5173/cart

#### Bước 2: Verify cart page
- ✅ **Expected:**
  - Hiển thị list items trong cart
  - Mỗi item có:
    - Hình ảnh
    - Tên sản phẩm
    - Giá gốc (USD/CNY)
    - Quantity selector
    - Subtotal
    - Remove button
  - Cart summary bên phải:
    - "Tiền hàng: XXX,XXX đ" (VND)
    - Nút "Proceed to Checkout"

#### Bước 3: Verify VND conversion
```
Nếu cart có:
- Item 1: $100 x 2 = $200
- Item 2: ¥50 x 1 = ¥50

Expected cart summary (với tỷ giá: USD=25,000, CNY=3,500):
- $200 × 25,000 = 5,000,000 VND
- ¥50 × 3,500 = 175,000 VND
- Total: 5,175,000 đ
```

### 4.3. Test Update Quantity

#### Bước 1: Ở cart page, thay đổi quantity
```
Click nút "+"

Expected:
- Quantity tăng lên 1
- Subtotal update
- Cart total update
- Cart badge ở header update
```

#### Bước 2: Giảm quantity
```
Click nút "-"

Expected:
- Quantity giảm 1
- Numbers update
```

#### Bước 3: Verify API call
```
Network tab:
PUT /api/cart/items/{id}
Body: { "quantity": 3 }

Response: 200 OK
```

### 4.4. Test Remove Item

#### Bước 1: Click "Remove" button
- Click icon trash/remove của 1 item

#### Bước 2: Verify removal
- ✅ **Expected:**
  - Item biến mất khỏi list
  - Cart total update
  - Cart badge giảm
  - Success toast: "Item removed"

#### Bước 3: Test remove tất cả
```
Remove hết items

Expected:
- Hiển thị "Your cart is empty"
- Cart badge = 0 hoặc ẩn
```

---

## 5. TEST WALLET SYSTEM

⚠️ **Yêu cầu:** Đã đăng nhập

### 5.1. Test View Wallet

#### Bước 1: Vào wallet page
```
URL: http://localhost:5173/wallet
Hoặc click "Wallet" ở header
```

#### Bước 2: Verify wallet info
- ✅ **Expected:**
  - Hiển thị số dư: "Balance: XXX,XXX đ"
  - Deposit code: "USER{id}" (ví dụ: USER1, USER3)
  - Nút "Bank Transfer"
  - Nút "View Transactions"
  - Recent transactions (nếu có)

#### Bước 3: Check API call
```
Network tab:
GET /api/wallet

Response: 200 OK
{
  "userId": 1,
  "balance": 250000,
  "depositCode": "USER1",
  "currency": "VND",
  "isLocked": false
}
```

### 5.2. Test Transaction History

#### Bước 1: Click "View Transactions"
- URL: http://localhost:5173/wallet/transactions

#### Bước 2: Verify transaction list
- ✅ **Expected:**
  - Table với columns:
    - Date & Time
    - Type (DEPOSIT / WITHDRAWAL)
    - Amount
    - Balance Before
    - Balance After
    - Description
  - Sorted by newest first

#### Bước 3: Check transaction details
```
Example row:
- Type: DEPOSIT (+)
- Amount: +500,000 đ
- Balance: 250,000 → 750,000 đ
- Description: "Deposit via BANK_TRANSFER"
- Date: 06/11/2025 20:30
```

### 5.3. Test Bank Transfer Page

#### Bước 1: Click "Bank Transfer" từ wallet page
- URL: http://localhost:5173/wallet/deposit/bank-transfer

#### Bước 2: Verify deposit instructions
- ✅ **Expected:**
  - QR Code image (VietQR)
  - Bank info:
    - Ngân hàng: MBBank
    - Số tài khoản: 0975299279
    - Tên: Nguyen Duc Luong
  - Nội dung CK: "NAP TIEN USER{id}"
  - Deposit code highlighted
  - Instructions text

#### Bước 3: Test QR code
```
QR code URL format:
https://img.vietqr.io/image/MB-0975299279-compact.png?amount=0&addInfo=NAP%20TIEN%20USER1&accountName=Nguyen%20Duc%20Luong

Expected:
- QR code hiển thị đúng
- Quét bằng app ngân hàng → điền sẵn thông tin
```

---

## 6. TEST BANK TRANSFER SMS

⚠️ **Yêu cầu:** SMS Forwarder app + Ngrok setup (Advanced)

### 6.1. Setup SMS Forwarder (One-time)

#### Bước 1: Install SMS Forwarder app
```
Android:
- Tìm "SMS Forwarder" trên Play Store
- Hoặc dùng app tương tự

iOS:
- Shortcuts app (built-in)
- Automation khi nhận SMS
```

#### Bước 2: Setup Ngrok (Expose localhost)
```bash
# Install ngrok
# Download từ: https://ngrok.com/download

# Chạy ngrok
ngrok http 8080

# Expected output:
Forwarding https://abc123.ngrok.io -> http://localhost:8080

# Copy HTTPS URL
```

#### Bước 3: Configure SMS Forwarder
```
SMS Forwarder settings:
- Sender filter: "MBBank" (hoặc tên ngân hàng của bạn)
- Webhook URL: https://abc123.ngrok.io/api/bank-transfer/sms-webhook
- Method: POST
- Content: JSON
  {
    "from": "{{sender}}",
    "content": "{{body}}"
  }
```

### 6.2. Test SMS Webhook

#### Bước 1: Transfer money qua bank app
```
Mở app ngân hàng (VD: MBBank)

Chuyển khoản:
- STK: 0975299279
- Tên: Nguyen Duc Luong
- Số tiền: 500,000 VND
- Nội dung: NAP TIEN USER1
           ^^^^^^^^^^^^^^^ (quan trọng!)

Confirm transfer
```

#### Bước 2: Nhận SMS từ ngân hàng
```
Expected SMS format:
"TK 0975299279 GD: +500,000VND 06/11/25 20:30 SD: 750,000VND ND: MBVCB.123.456.NAP TIEN USER1"
```

#### Bước 3: SMS Forwarder auto-forward
```
Check backend console log:

Expected logs:
[INFO] Received SMS webhook from MBBank
[INFO] SMS content: TK 0975299279 GD: +500,000VND...
[INFO] Parsed amount: 500000
[INFO] Extracted userId: 1
[INFO] User found: demo.account@gmail.com
[INFO] Deposit created: 500000 VND
[INFO] Wallet balance updated: 250000 -> 750000
```

#### Bước 4: Verify wallet updated
```
1. Vào wallet page: http://localhost:5173/wallet
2. Click "Check for Deposit" hoặc refresh page

Expected:
- Balance tăng lên 500,000 đ
- New transaction trong history
- Type: DEPOSIT
- Amount: +500,000 đ
- Description: "Deposit via BANK_TRANSFER"
```

### 6.3. Test Manual SMS Processing

#### Bước 1: Check pending SMS
```
DevTools Console:

// Call API
fetch('http://localhost:8080/api/bank-transfer/sms', {
  headers: {
    'Authorization': 'Bearer YOUR_TOKEN'
  }
}).then(r => r.json()).then(console.log)

Expected:
[
  {
    "id": 1,
    "sender": "MBBank",
    "message": "GD: +500,000VND...",
    "depositCreated": false
  }
]
```

#### Bước 2: Process pending manually
```
Click nút "Check for Deposit" ở wallet page

Hoặc call API:
POST http://localhost:8080/api/bank-transfer/process-pending

Expected response:
{
  "processed": 1,
  "failed": 0
}
```

### 6.4. Test SMS Formats

#### Format 1: UserId (Recommended)
```
Transfer với nội dung: "NAP TIEN USER3"

Expected:
✅ Parsed userId: 3
✅ Find user by userId = 3
✅ Deposit created successfully
```

#### Format 2: Username
```
Transfer với nội dung: "NAP TIEN testuser001"

Expected:
✅ Parsed username: testuser001
✅ Find user by username
✅ Deposit created
```

#### Format 3: Email (không khuyến khích)
```
Transfer với nội dung: "testuser001@example.com"

Expected:
✅ Parsed email
✅ Find user by email
✅ Deposit created
```

#### Format lỗi:
```
Transfer với nội dung: "Nap tien"

Expected:
❌ Cannot parse userId/username
❌ Error logged
❌ SMS marked as failed
```

---

## 7. TEST ORDER & CHECKOUT

⚠️ **Yêu cầu:** Đã đăng nhập, có items trong cart, có tiền trong wallet

### 7.1. Prepare Test Data

#### Bước 1: Đảm bảo có tiền trong wallet
```
Option 1: Bank transfer (như phần 6)
Option 2: Manual SQL insert

SQL:
UPDATE user_wallet SET balance = 10000000 WHERE user_id = 1;
-- Set balance = 10,000,000 VND
```

#### Bước 2: Đảm bảo có items trong cart
```
Add 2-3 products vào cart (như phần 4)

Example cart:
- Product A: $50 x 2 = $100
- Product B: ¥100 x 1 = ¥100
```

### 7.2. Test Checkout Success

#### Bước 1: Từ cart page, click "Proceed to Checkout"
- URL redirect: http://localhost:5173/checkout

#### Bước 2: Verify checkout page
- ✅ **Expected:**
  - Shipping address form
  - Order summary:
    - Product list
    - Product Total: X,XXX,XXX đ (VND)
    - Deposit Now (70%): Y,YYY,YYY đ
    - Remaining (30%): Z,ZZZ,ZZZ đ
  - Current wallet balance: W,WWW,WWW đ
  - Place Order button

#### Bước 3: Fill shipping address
```
Full Name: Test User
Phone: 0123456789
Address: 123 Test Street
City: Ho Chi Minh
```

#### Bước 4: Click "Place Order"
```
Expected loading state:
- Button disabled
- Loading spinner
- Text: "Processing..."
```

#### Bước 5: Verify success
- ✅ **Expected:**
  - Redirect: http://localhost:5173/orders/success
  - Success message: "Order placed successfully!"
  - Order number hiển thị
  - Nút "View Order Details"
  - Nút "Continue Shopping"

#### Bước 6: Check wallet deducted
```
Vào wallet page

Expected:
- Balance giảm đúng 70% deposit amount
- New transaction:
  - Type: WITHDRAWAL / ORDER_PAYMENT
  - Amount: -Y,YYY,YYY đ (deposit amount)
  - Description: "Order deposit payment for order #XXX"
```

#### Bước 7: Check order created
```
Click "View Order Details" hoặc vào Orders page

Expected:
- Order xuất hiện trong order list
- Status: PENDING
- Total amount đúng
- Items đúng
```

### 7.3. Test Insufficient Balance

#### Bước 1: Set wallet balance thấp
```sql
UPDATE user_wallet SET balance = 100000 WHERE user_id = 1;
-- Set balance = 100,000 VND (ít hơn deposit cần thiết)
```

#### Bước 2: Try checkout
```
Từ cart → Checkout → Place Order
```

#### Bước 3: Verify error
- ✅ **Expected:**
  - Error toast hiển thị
  - Message: "Insufficient wallet balance. You need XXX VND more."
  - Nút "Go to Wallet" trong toast
  - Order không được tạo
  - Wallet không bị trừ tiền

#### Bước 4: Click "Go to Wallet"
```
Expected:
- Redirect đến /wallet
- Có thể nạp thêm tiền
```

### 7.4. Test Order History

#### Bước 1: Vào orders page
```
URL: http://localhost:5173/orders
Hoặc click "Orders" ở header
```

#### Bước 2: Verify order list
- ✅ **Expected:**
  - List tất cả orders của user
  - Mỗi order card có:
    - Order number
    - Date
    - Total amount (VND)
    - Status badge (PENDING, PROCESSING, COMPLETED)
    - Number of items
    - "View Details" button

#### Bước 3: Filter by status
```
Click dropdown "All Orders"
Chọn "Pending"

Expected:
- Chỉ hiển thị orders PENDING
```

### 7.5. Test Order Detail

#### Bước 1: Click "View Details" của 1 order
- URL: http://localhost:5173/orders/{orderId}

#### Bước 2: Verify order detail page
- ✅ **Expected:**
  - Order header:
    - Order number
    - Date
    - Status badge
  - Order timeline (status history):
    - Các status với timestamp
    - Line connecting statuses
  - Shipping address
  - Payment info:
    - Deposit paid: Y,YYY,YYY đ
    - Remaining: Z,ZZZ,ZZZ đ
  - Product list:
    - Images
    - Names
    - Quantities
    - Prices
  - Order summary:
    - Subtotal
    - Shipping (nếu có)
    - Total

#### Bước 3: Check order timeline
```
Expected timeline example:
✅ PENDING - 06/11/2025 20:30
   Order placed and awaiting confirmation

⏳ PROCESSING - (future)
   Order is being prepared

⏳ SHIPPED - (future)
   Order has been shipped

⏳ COMPLETED - (future)
   Order delivered successfully
```

---

## 8. TEST ADMIN DASHBOARD

⚠️ **Yêu cầu:** Đăng nhập với ADMIN account

### 8.1. Login as Admin

#### Bước 1: Logout nếu đang login
- Click Logout

#### Bước 2: Login với admin account
```
Email: admin@pandamall.com
Password: admin123

Hoặc:
Username: VanA
Password: password
```

#### Bước 3: Verify admin access
- ✅ **Expected:**
  - Header có link "Admin"
  - URL available: /admin/dashboard

### 8.2. Test Admin Dashboard

#### Bước 1: Vào admin dashboard
```
URL: http://localhost:5173/admin/dashboard
Hoặc click "Admin" ở header
```

#### Bước 2: Verify dashboard stats
- ✅ **Expected:**
  - Stat cards:
    - Total Users: X
    - Total Orders: Y
    - Total Revenue: Z VND
    - Pending Orders: W
  - Charts:
    - Revenue chart (if implemented)
    - Orders chart
  - Recent orders table
  - Recent users table

#### Bước 3: Check API call
```
Network tab:
GET /api/admin/dashboard/stats

Response: 200 OK
{
  "totalUsers": 15,
  "totalOrders": 42,
  "totalRevenue": 125000000,
  "pendingOrders": 8,
  "todayOrders": 3,
  "monthlyRevenue": 25000000
}
```

### 8.3. Test Order Management

#### Bước 1: Vào order management
```
URL: http://localhost:5173/admin/orders
Hoặc click "Orders" trong admin nav
```

#### Bước 2: Verify orders table
- ✅ **Expected:**
  - Table với tất cả orders (all users)
  - Columns:
    - Order Number
    - Customer Name
    - Date
    - Total Amount
    - Status
    - Actions
  - Filter by status dropdown
  - Search box

#### Bước 3: Update order status
```
1. Click "Edit" button của 1 order
2. Chọn status mới: "PROCESSING"
3. Click "Save"

Expected:
- Status update thành công
- Toast: "Order status updated"
- Table refresh
- Order detail timeline updated
```

#### Bước 4: Check status history
```
Click "View" button → Order detail

Expected:
- Timeline shows new status
- Timestamp của update
```

### 8.4. Test User Management

#### Bước 1: Vào user management
```
URL: http://localhost:5173/admin/users
```

#### Bước 2: Verify users table
- ✅ **Expected:**
  - Table với tất cả users
  - Columns:
    - User ID
    - Username
    - Email
    - Role
    - Status
    - Created Date
    - Actions

#### Bước 3: Edit user
```
1. Click "Edit" button
2. Modal popup với form
3. Thay đổi:
   - Role: CUSTOMER → STAFF
   - Status: ACTIVE (giữ nguyên)
4. Click "Save"

Expected:
- User updated
- Toast success
- Table refresh
```

#### Bước 4: Lock user
```
1. Click "Lock" button
2. Confirm dialog
3. Click "Yes"

Expected:
- User status → LOCKED
- User không thể login
```

### 8.5. Test Wallet Management

#### Bước 1: Vào wallet management
```
URL: http://localhost:5173/admin/wallets
```

#### Bước 2: Verify wallets table
- ✅ **Expected:**
  - Table với tất cả wallets
  - Columns:
    - User ID
    - Username
    - Balance (VND)
    - Is Locked
    - Actions

#### Bước 3: View wallet transactions
```
Click "View Transactions" button

Expected:
- Modal popup
- Transaction history của user đó
- All transaction types
```

#### Bước 4: Adjust balance (nếu có feature)
```
1. Click "Adjust Balance"
2. Enter amount: +1000000
3. Reason: "Manual top-up for testing"
4. Click "Confirm"

Expected:
- Balance updated
- Transaction record created
- Type: ADMIN_ADJUSTMENT
```

### 8.6. Test Fee Configuration

#### Bước 1: Vào fee config
```
URL: http://localhost:5173/admin/fee-config
```

#### Bước 2: Verify config form
- ✅ **Expected:**
  - Form fields:
    - Service Fee (%): X%
    - Transaction Fee (VND): Y
    - Min Order Amount: Z
  - Current values displayed
  - Save button

#### Bước 3: Update fees
```
Change values:
- Service Fee: 5% → 7%
- Transaction Fee: 5000 → 10000

Click "Save Changes"

Expected:
- Success toast
- Config saved to database
- New orders use new fees
```

---

## 9. TEST API VỚI POSTMAN

### 9.1. Setup Postman

#### Bước 1: Import collection
```
Option 1: Create new collection manually

Option 2: Import từ file (nếu có)
- File → Import
- Select JSON file
```

#### Bước 2: Setup environment variables
```
Create new environment "Aptechmall Local"

Variables:
- base_url: http://localhost:8080/api
- token: (will be set after login)
- user_id: 1
```

### 9.2. Test Authentication APIs

#### Test 1: Register
```
Method: POST
URL: {{base_url}}/auth/register
Body (JSON):
{
  "username": "apitest001",
  "email": "apitest001@example.com",
  "password": "Test123!",
  "fullName": "API Test User"
}

Expected Response: 200 OK
{
  "message": "User registered successfully"
}
```

#### Test 2: Login
```
Method: POST
URL: {{base_url}}/auth/login
Body (JSON):
{
  "username": "demo.account@gmail.com",
  "password": "demo123"
}

Expected Response: 200 OK
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}

Action: Copy token → Set environment variable "token"
```

#### Test 3: Get Current User
```
Method: GET
URL: {{base_url}}/users/me
Headers:
- Authorization: Bearer {{token}}

Expected Response: 200 OK
{
  "userId": 1,
  "username": "demo.account",
  "email": "demo.account@gmail.com",
  "role": "CUSTOMER"
}
```

### 9.3. Test Cart APIs

#### Test 1: Get Cart
```
Method: GET
URL: {{base_url}}/cart
Headers:
- Authorization: Bearer {{token}}

Expected Response: 200 OK
{
  "userId": 1,
  "items": [...],
  "totalItems": 3
}
```

#### Test 2: Add to Cart
```
Method: POST
URL: {{base_url}}/cart/items
Headers:
- Authorization: Bearer {{token}}
Body (JSON):
{
  "productId": "1005005244562338",
  "productName": "Test Product",
  "price": 99.99,
  "quantity": 2,
  "currency": "USD",
  "marketplace": "ALIEXPRESS",
  "imageUrl": "https://example.com/image.jpg"
}

Expected Response: 200 OK
{
  "id": 10,
  "productId": "1005005244562338",
  "quantity": 2,
  ...
}
```

#### Test 3: Update Cart Item
```
Method: PUT
URL: {{base_url}}/cart/items/10
Headers:
- Authorization: Bearer {{token}}
Body (JSON):
{
  "quantity": 5
}

Expected Response: 200 OK
{
  "id": 10,
  "quantity": 5
}
```

#### Test 4: Remove Cart Item
```
Method: DELETE
URL: {{base_url}}/cart/items/10
Headers:
- Authorization: Bearer {{token}}

Expected Response: 200 OK
{
  "message": "Item removed from cart"
}
```

### 9.4. Test Order APIs

#### Test 1: Checkout
```
Method: POST
URL: {{base_url}}/orders/checkout
Headers:
- Authorization: Bearer {{token}}
Body (JSON):
{
  "shippingAddress": {
    "fullName": "Test User",
    "phone": "0123456789",
    "address": "123 Test St",
    "city": "Ho Chi Minh"
  },
  "paymentMethod": "WALLET"
}

Expected Response: 200 OK
{
  "orderId": 42,
  "orderNumber": "ORD-20251106-00042",
  "totalAmount": 5000000,
  "depositPaid": 3500000,
  "remaining": 1500000,
  "status": "PENDING"
}
```

#### Test 2: Get Orders
```
Method: GET
URL: {{base_url}}/orders
Headers:
- Authorization: Bearer {{token}}

Expected Response: 200 OK
{
  "orders": [
    {
      "orderId": 42,
      "orderNumber": "ORD-20251106-00042",
      "totalAmount": 5000000,
      "status": "PENDING",
      "createdAt": "2025-11-06T20:30:00"
    },
    ...
  ]
}
```

#### Test 3: Get Order Detail
```
Method: GET
URL: {{base_url}}/orders/42
Headers:
- Authorization: Bearer {{token}}

Expected Response: 200 OK
{
  "orderId": 42,
  "orderNumber": "ORD-20251106-00042",
  "items": [...],
  "shippingAddress": {...},
  "statusHistory": [...],
  "totalAmount": 5000000,
  "depositPaid": 3500000
}
```

### 9.5. Test Wallet APIs

#### Test 1: Get Wallet
```
Method: GET
URL: {{base_url}}/wallet
Headers:
- Authorization: Bearer {{token}}

Expected Response: 200 OK
{
  "userId": 1,
  "balance": 7500000,
  "depositCode": "USER1",
  "currency": "VND",
  "isLocked": false
}
```

#### Test 2: Get Transactions
```
Method: GET
URL: {{base_url}}/wallet/transactions
Headers:
- Authorization: Bearer {{token}}

Expected Response: 200 OK
{
  "transactions": [
    {
      "id": 21,
      "transactionType": "DEPOSIT",
      "amount": 500000,
      "balanceBefore": 7000000,
      "balanceAfter": 7500000,
      "description": "Deposit via BANK_TRANSFER",
      "createdAt": "2025-11-06T20:30:00"
    },
    ...
  ]
}
```

### 9.6. Test Bank Transfer APIs

#### Test 1: SMS Webhook
```
Method: POST
URL: {{base_url}}/bank-transfer/sms-webhook
Headers:
- Content-Type: application/json
Body (JSON):
{
  "from": "MBBank",
  "content": "TK 0975299279 GD: +500,000VND 06/11/25 20:30 SD: 750,000VND ND: MBVCB.123.456.NAP TIEN USER1"
}

Expected Response: 200 OK
{
  "message": "SMS received and processed",
  "depositCreated": true,
  "amount": 500000,
  "userId": 1
}
```

#### Test 2: Get SMS Records (Admin)
```
Method: GET
URL: {{base_url}}/bank-transfer/sms
Headers:
- Authorization: Bearer {{admin_token}}

Expected Response: 200 OK
{
  "smsRecords": [
    {
      "id": 1,
      "sender": "MBBank",
      "message": "...",
      "parsedAmount": 500000,
      "extractedUserId": 1,
      "depositCreated": true,
      "createdAt": "2025-11-06T20:30:00"
    },
    ...
  ]
}
```

---

## 10. TROUBLESHOOTING

### 10.1. Backend Issues

#### Problem: Backend không start
```
Error: Could not connect to database

Solution:
1. Check MySQL running:
   - Windows: net start MySQL80
   - Mac/Linux: sudo systemctl start mysql

2. Check database exists:
   mysql -u root -p
   SHOW DATABASES;

3. Check credentials trong application.properties
```

#### Problem: Redis connection failed
```
Error: Unable to connect to Redis

Solution:
1. Start Redis:
   - Windows: redis-server.exe
   - Mac: brew services start redis
   - Linux: sudo systemctl start redis

2. Check Redis running:
   redis-cli ping
   Expected: PONG
```

#### Problem: RapidAPI rate limit exceeded
```
Error: 429 Too Many Requests

Solution:
1. Check RapidAPI dashboard: https://rapidapi.com/developer/dashboard
2. Đợi rate limit reset (thường reset hàng tháng)
3. Upgrade plan (nếu cần)
4. Temporary: Dùng mock data (set USE_MOCK_API = true)
```

### 10.2. Frontend Issues

#### Problem: Frontend không start
```
Error: Module not found

Solution:
1. Delete node_modules:
   rm -rf node_modules

2. Delete package-lock.json:
   rm package-lock.json

3. Reinstall:
   npm install

4. Restart dev server:
   npm run dev
```

#### Problem: Google OAuth không hoạt động
```
Error: Invalid Client ID

Solution:
1. Check .env file exists: Frontend/.env
2. Check VITE_CLIENT_ID có đúng format:
   VITE_CLIENT_ID=xxx-xxx.apps.googleusercontent.com
3. Check authorized redirect URIs trong Google Console:
   - http://localhost:5173
4. Restart Vite server sau khi sửa .env
```

#### Problem: API calls bị CORS error
```
Error: CORS policy blocked

Solution:
1. Check backend CorsConfig:
   - Allowed origins: localhost:5173, localhost:3000

2. Check backend đang chạy trên port 8080

3. Clear browser cache:
   Ctrl + Shift + Delete → Clear cache
```

### 10.3. Testing Issues

#### Problem: Cart badge không update
```
Solution:
1. Check CartContext refreshCart() được gọi sau add/remove
2. Check browser console có errors
3. F5 refresh page
4. Clear localStorage:
   localStorage.clear()
   Refresh page
```

#### Problem: Order checkout failed với wallet error
```
Error: Insufficient balance (nhưng có đủ tiền)

Solution:
1. Check wallet balance trong database:
   SELECT * FROM user_wallet WHERE user_id = 1;

2. Check exchange rate tồn tại:
   SELECT * FROM exchange_rate;

3. Calculate deposit manually:
   Cart total × 0.7 = deposit needed
   Compare với wallet balance

4. Check backend logs để xem lỗi chi tiết
```

#### Problem: SMS webhook không nhận được
```
Solution:
1. Check ngrok đang chạy:
   ngrok http 8080

2. Copy HTTPS URL từ ngrok console

3. Update SMS Forwarder webhook URL

4. Test webhook manually:
   curl -X POST https://abc123.ngrok.io/api/bank-transfer/sms-webhook \
     -H "Content-Type: application/json" \
     -d '{"from":"MBBank","content":"GD: +500,000VND USER1"}'

5. Check backend console logs
```

### 10.4. Database Issues

#### Problem: Exchange rate empty
```
Solution:
1. Call exchange rate API manually:
   GET http://localhost:8080/api/exchange-rates/refresh

2. Check database:
   SELECT * FROM exchange_rate;

3. Insert fallback rates manually:
   INSERT INTO exchange_rate (currency, rate_to_vnd, source, updated_at)
   VALUES
   ('USD', 25000, 'MANUAL', NOW()),
   ('CNY', 3500, 'MANUAL', NOW());
```

#### Problem: User wallet không tồn tại
```
Error: Wallet not found for user

Solution:
Wallet auto-created khi user đăng ký, nhưng nếu user cũ:

INSERT INTO user_wallet (user_id, balance, is_locked, created_at, updated_at)
VALUES (1, 0, 0, NOW(), NOW());
```

---

## 📊 TESTING CHECKLIST

In danh sách này ra và check từng mục:

### Setup:
- [ ] MySQL running on port 3306
- [ ] Redis running on port 6379
- [ ] Backend started successfully (port 8080)
- [ ] Frontend started successfully (port 5173)
- [ ] RapidAPI keys configured

### Authentication:
- [ ] Register new account
- [ ] Login with username/password
- [ ] Login with Google OAuth
- [ ] Logout successfully
- [ ] Protected routes redirect to login

### Products:
- [ ] Search products on AliExpress
- [ ] Search products on 1688
- [ ] View product detail page
- [ ] Product images display correctly
- [ ] Sort and pagination work

### Cart:
- [ ] Add product to cart
- [ ] Cart badge updates
- [ ] View cart page
- [ ] Update quantity
- [ ] Remove item
- [ ] Cart total shows VND

### Wallet:
- [ ] View wallet balance
- [ ] View transaction history
- [ ] Bank transfer page shows QR code
- [ ] Deposit code displayed correctly

### Bank Transfer (Advanced):
- [ ] SMS Forwarder configured
- [ ] Ngrok running
- [ ] Transfer money with correct format
- [ ] SMS webhook receives notification
- [ ] Wallet balance updates automatically

### Orders:
- [ ] Checkout với sufficient balance
- [ ] Checkout với insufficient balance (error)
- [ ] Order appears in history
- [ ] View order detail
- [ ] Status timeline displays correctly
- [ ] Wallet deducted 70% deposit

### Admin:
- [ ] Login as admin
- [ ] View dashboard statistics
- [ ] View all orders
- [ ] Update order status
- [ ] View all users
- [ ] Edit user roles
- [ ] View all wallets
- [ ] Configure system fees

### API (Postman):
- [ ] All auth endpoints work
- [ ] All cart endpoints work
- [ ] All order endpoints work
- [ ] All wallet endpoints work
- [ ] All admin endpoints work

---

## 🎉 HOÀN THÀNH!

Nếu bạn đã test qua tất cả các luồng trên và đều PASS, xin chúc mừng!

Project đã sẵn sàng để:
- ✅ Demo cho khách hàng
- ✅ Deploy lên staging environment
- ✅ Tiến hành UAT testing
- ✅ Deploy production

---

**📝 Document version:** 1.0
**👨‍💻 Created by:** Claude Code
**📅 Date:** 2025-11-06
