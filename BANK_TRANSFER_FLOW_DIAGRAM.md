# 📊 LUỒNG CHẠY CHỨC NĂNG NẠP TIỀN QUA SMS

## 🎯 TỔNG QUAN

```
[USER] → [BANK] → [SMARTPHONE] → [SMS FORWARDER] → [BACKEND] → [WALLET]
```

---

## 📱 LUỒNG CHI TIẾT

### **BƯỚC 1: User Vào Trang Wallet**

```
┌─────────────────────────────────────────────────────────────┐
│ FRONTEND: http://localhost:5174/wallet                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  User đăng nhập                                             │
│     ↓                                                       │
│  Vào trang Wallet                                           │
│     ↓                                                       │
│  GET /api/wallet                                            │
│     ↓                                                       │
│  Backend trả về:                                            │
│    - userId: 3                                              │
│    - balance: 250,000 VND                                   │
│    - depositCode: "USER3"  ← QUAN TRỌNG                    │
│     ↓                                                       │
│  Hiển thị số dư + nút "Bank Transfer"                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

### **BƯỚC 2: User Click "Bank Transfer"**

```
┌─────────────────────────────────────────────────────────────┐
│ FRONTEND: BankTransferDepositPage.jsx                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Hiển thị:                                                  │
│    ┌──────────────────────────────────────┐                │
│    │  📱 QR CODE (VietQR)                 │                │
│    │  [QR Image with embedded info]       │                │
│    │                                       │                │
│    │  Ngân hàng: MBBank                   │                │
│    │  STK: 0975299279                     │                │
│    │  Tên: Nguyen Duc Luong               │                │
│    │  Nội dung: NAP TIEN USER3  ← KEY!   │                │
│    └──────────────────────────────────────┘                │
│                                                             │
│  User có thể:                                               │
│    1. Quét QR bằng app ngân hàng                           │
│    2. Hoặc nhập tay thông tin                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

### **BƯỚC 3: User Chuyển Khoản Từ App Ngân Hàng**

```
┌─────────────────────────────────────────────────────────────┐
│ BANKING APP (VD: MBBank)                                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  User mở app ngân hàng                                      │
│     ↓                                                       │
│  Quét QR hoặc nhập tay                                      │
│     ↓                                                       │
│  ┌─────────────────────────────────┐                       │
│  │ Chuyển khoản                     │                       │
│  │ ───────────────────────          │                       │
│  │ STK: 0975299279                  │                       │
│  │ Tên: Nguyen Duc Luong            │                       │
│  │ Số tiền: 500,000 VND             │                       │
│  │ Nội dung: NAP TIEN USER3         │                       │
│  │                                   │                       │
│  │ [Xác nhận]                        │                       │
│  └─────────────────────────────────┘                       │
│     ↓                                                       │
│  User xác nhận OTP/FaceID                                   │
│     ↓                                                       │
│  🎉 Chuyển khoản thành công!                                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

### **BƯỚC 4: Ngân Hàng Gửi SMS Xác Nhận**

```
┌─────────────────────────────────────────────────────────────┐
│ BANK SERVER → SMARTPHONE                                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Ngân hàng gửi SMS xác nhận:                                │
│                                                             │
│  ┌─────────────────────────────────────────┐               │
│  │ From: MBBank                             │               │
│  │ ────────────────────────────             │               │
│  │ TK 0975299279                            │               │
│  │ GD: +500,000VND                          │               │
│  │ 05/11/25 20:30                           │               │
│  │ SD: 750,000VND                           │               │
│  │ ND: MBVCB.11597844224.401854.           │               │
│  │     NAP TIEN USER3                       │               │
│  └─────────────────────────────────────────┘               │
│                                                             │
│  ⏱️ Thời gian: 10-30 giây sau khi chuyển khoản             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

### **BƯỚC 5: SMS Forwarder App Chuyển Tiếp**

```
┌─────────────────────────────────────────────────────────────┐
│ SMARTPHONE: SMS Forwarder App                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  App phát hiện SMS mới từ "MBBank"                          │
│     ↓                                                       │
│  Check rule:                                                │
│    ✅ Sender contains "MBBank"                              │
│     ↓                                                       │
│  Trigger webhook forward                                    │
│     ↓                                                       │
│  POST https://your-ngrok-url/api/bank-transfer/sms-webhook │
│  Content-Type: application/json                            │
│  Body:                                                      │
│  {                                                          │
│    "from": "MBBank",                                        │
│    "content": "TK 0975299279 GD: +500,000VND ..."          │
│  }                                                          │
│     ↓                                                       │
│  ⏱️ Thời gian: < 1 giây                                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

### **BƯỚC 6: Backend Nhận Webhook**

```
┌─────────────────────────────────────────────────────────────┐
│ BACKEND: BankTransferController                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  POST /api/bank-transfer/sms-webhook                        │
│     ↓                                                       │
│  1. Nhận request                                            │
│     ├─ from: "MBBank"                                       │
│     └─ content: "GD: +500,000VND ... USER3"                │
│     ↓                                                       │
│  2. Parse parameters                                        │
│     ├─ Check query params (GET)                            │
│     └─ Check JSON body (POST)                              │
│     ↓                                                       │
│  3. Extract từ JSON:                                        │
│     ├─ sender = "MBBank"                                    │
│     └─ message = "GD: +500,000VND ... USER3"               │
│     ↓                                                       │
│  4. Lưu SMS vào database (bank_sms table)                   │
│     ↓                                                       │
│  5. Gọi BankTransferService.processSingleSms()              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

### **BƯỚC 7: Parse SMS Content**

```
┌─────────────────────────────────────────────────────────────┐
│ BACKEND: BankSmsParserService                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Input: "GD: +500,000VND 05/11/25 20:30 ND: NAP TIEN USER3"│
│     ↓                                                       │
│  1. Extract Amount                                          │
│     ├─ Regex: [+]([0-9,]+)                                 │
│     ├─ Found: "+500,000"                                    │
│     └─ Result: 500000 (BigDecimal)                         │
│     ↓                                                       │
│  2. Extract Transaction Reference                           │
│     ├─ Regex: MBVCB\.[0-9]+\.[0-9]+                        │
│     ├─ Found: "MBVCB.11597844224.401854"                   │
│     └─ Result: "MBVCB_11597844224_401854"                  │
│     ↓                                                       │
│  3. Extract User Identifier                                 │
│     ├─ Try username: Pattern: ([A-Z0-9]{3,30})             │
│     │   → Not found (no standalone username)               │
│     ├─ Try userId: Pattern: USER\s*([0-9]+)                │
│     │   → Found: "USER3"                                    │
│     └─ Result: userId = 3                                   │
│     ↓                                                       │
│  Output:                                                    │
│    ├─ parsedAmount: 500000                                  │
│    ├─ transactionReference: "MBVCB_11597844224_401854"     │
│    ├─ extractedUserId: 3                                    │
│    ├─ extractedUsername: null                               │
│    └─ extractedEmail: null                                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

### **BƯỚC 8: Tìm User & Tạo Deposit**

```
┌─────────────────────────────────────────────────────────────┐
│ BACKEND: BankTransferService.createDepositFromSms()       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Input:                                                     │
│    ├─ extractedUserId: 3                                    │
│    ├─ parsedAmount: 500000                                  │
│    └─ transactionReference: "MBVCB_11597844224_401854"     │
│     ↓                                                       │
│  1. Try Username (Priority 1)                               │
│     └─ extractedUsername = null → Skip                      │
│     ↓                                                       │
│  2. Try UserId (Priority 2)                                 │
│     ├─ Query: SELECT * FROM users WHERE user_id = 3         │
│     ├─ Found: User(id=3, email="user3@example.com")        │
│     └─ ✅ Success!                                          │
│     ↓                                                       │
│  3. Check Duplicate Transaction                             │
│     ├─ Query: SELECT * FROM bank_sms                        │
│     │         WHERE transaction_reference = "MBVCB_..."     │
│     └─ ✅ Not found → OK to proceed                         │
│     ↓                                                       │
│  4. Call WalletService.processDeposit()                     │
│     ├─ userId: 3                                            │
│     ├─ amount: 500000                                       │
│     ├─ gateway: BANK_TRANSFER                               │
│     └─ reference: "MBVCB_11597844224_401854"               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

### **BƯỚC 9: Cộng Tiền Vào Ví**

```
┌─────────────────────────────────────────────────────────────┐
│ BACKEND: WalletService.processDeposit()                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. Get hoặc tạo wallet cho user                            │
│     ├─ Query: SELECT * FROM user_wallet                     │
│     │         WHERE user_id = 3                             │
│     └─ Found: wallet_id = 3, balance = 250000              │
│     ↓                                                       │
│  2. Check wallet không bị lock                              │
│     └─ ✅ is_locked = false                                 │
│     ↓                                                       │
│  3. Tính toán số dư mới                                     │
│     ├─ balanceBefore = 250000                               │
│     ├─ deposit = 500000                                     │
│     └─ balanceAfter = 750000                                │
│     ↓                                                       │
│  4. Update wallet balance                                   │
│     └─ UPDATE user_wallet                                   │
│        SET balance = 750000                                 │
│        WHERE id = 3                                         │
│     ↓                                                       │
│  5. Tạo transaction record                                  │
│     └─ INSERT INTO wallet_transaction:                      │
│        ├─ wallet_id: 3                                      │
│        ├─ transaction_type: DEPOSIT                         │
│        ├─ amount: 500000                                    │
│        ├─ balance_before: 250000                            │
│        ├─ balance_after: 750000                             │
│        ├─ description: "Deposit via BANK_TRANSFER"         │
│        └─ reference_number: "MBVCB_11597844224_401854"     │
│     ↓                                                       │
│  6. Update bank_sms record                                  │
│     └─ UPDATE bank_sms                                      │
│        SET deposit_created = true,                          │
│            wallet_transaction_id = 21                       │
│        WHERE id = 50                                        │
│     ↓                                                       │
│  ✅ SUCCESS!                                                │
│     └─ Return: WalletTransactionResponse(id=21)            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

### **BƯỚC 10: User Thấy Số Dư Cập Nhật**

```
┌─────────────────────────────────────────────────────────────┐
│ FRONTEND: WalletPage                                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Option 1: User click "Check for Deposit"                  │
│     ├─ Call: POST /api/bank-transfer/process-pending       │
│     ├─ Then: GET /api/wallet                                │
│     └─ Hiển thị số dư mới: 750,000 VND                      │
│                                                             │
│  Option 2: User refresh trang                               │
│     ├─ GET /api/wallet                                      │
│     └─ Hiển thị số dư mới: 750,000 VND                      │
│                                                             │
│  Option 3: User vào Transaction History                     │
│     ├─ GET /api/wallet/transactions                         │
│     └─ Thấy record mới:                                     │
│        ┌────────────────────────────────────┐              │
│        │ 📥 DEPOSIT                          │              │
│        │ +500,000 VND                        │              │
│        │ Deposit via BANK_TRANSFER          │              │
│        │ 05/11/2025 20:30                   │              │
│        │ Balance: 250,000 → 750,000 VND     │              │
│        └────────────────────────────────────┘              │
│                                                             │
│  🎉 User thấy tiền đã vào tài khoản!                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## ⏱️ THỜI GIAN XỬ LÝ

```
Bước 1: User vào wallet                    → < 1 giây
Bước 2: Hiển thị QR code                   → Tức thì
Bước 3: User chuyển khoản                  → 10-30 giây (tùy ngân hàng)
Bước 4: Bank gửi SMS                       → 10-30 giây
Bước 5: SMS Forwarder forward              → < 1 giây
Bước 6: Backend nhận webhook               → < 0.1 giây
Bước 7: Parse SMS                          → < 0.1 giây
Bước 8: Tìm user                           → < 0.1 giây
Bước 9: Cộng tiền vào ví                   → < 0.5 giây
Bước 10: User thấy số dư mới               → Khi refresh

TỔNG: Từ lúc chuyển khoản đến khi tiền vào ví: 1-2 PHÚT
```

---

## 🔄 XỬ LÝ LỖI (ERROR HANDLING)

### **Lỗi 1: Username Không Tồn Tại**
```
SMS: "GD +500k WRONGNAME USER3"
  ↓
⚠️ Try username "WRONGNAME" → Not found
  ↓
✅ Fallback to userId=3 → Found!
  ↓
✅ Deposit created successfully
```

### **Lỗi 2: Không Tìm Thấy User**
```
SMS: "GD +500k INVALIDUSER"
  ↓
❌ Try username "INVALIDUSER" → Not found
❌ No userId found
❌ No email found
  ↓
❌ Error: "User not found. Tried: username=INVALIDUSER, userId=null, email=null"
  ↓
SMS marked as processed with error
```

### **Lỗi 3: Duplicate Transaction**
```
SMS: "GD +500k USER3" (lần 2)
  ↓
Check transaction_reference = "MBVCB_123"
  ↓
❌ Already exists in database
  ↓
❌ Error: "Duplicate transaction reference"
  ↓
SMS marked as processed with error
```

---

## 📊 DATABASE TABLES INVOLVED

```
┌────────────────────┐
│ bank_sms           │  ← Lưu SMS từ ngân hàng
├────────────────────┤
│ id                 │
│ sender             │
│ message            │
│ parsed_amount      │
│ extracted_user_id  │
│ deposit_created    │
│ wallet_tx_id       │
└────────────────────┘
         ↓
         ↓ Link
         ↓
┌────────────────────┐
│ wallet_transaction │  ← Lịch sử giao dịch
├────────────────────┤
│ id                 │
│ wallet_id          │
│ transaction_type   │
│ amount             │
│ balance_before     │
│ balance_after      │
│ reference_number   │
└────────────────────┘
         ↓
         ↓ Link
         ↓
┌────────────────────┐
│ user_wallet        │  ← Ví của user
├────────────────────┤
│ id                 │
│ user_id            │
│ balance            │  ← Số dư hiện tại
│ is_locked          │
└────────────────────┘
         ↓
         ↓ Link
         ↓
┌────────────────────┐
│ users              │  ← Thông tin user
├────────────────────┤
│ user_id            │
│ username           │
│ email              │
│ full_name          │
└────────────────────┘
```

---

## 🎯 KEY POINTS

✅ **Format an toàn nhất:** `NAP TIEN USER{id}`
✅ **Fallback logic:** Username → UserId → Email
✅ **Duplicate prevention:** Check transaction_reference
✅ **Real-time processing:** < 1 phút từ chuyển khoản đến ví
✅ **Error handling:** Tất cả lỗi đều được log và lưu vào database

---

**📝 Tài liệu chi tiết:** `BANK_TRANSFER_SMS_INTEGRATION.md`
