# 🐛 Báo Cáo Lỗi Luồng Đặt Hàng (Checkout Flow)

**Ngày:** 2025-11-06
**Người kiểm tra:** Claude Code
**Trạng thái:** 3 lỗi NGHIÊM TRỌNG, 2 lỗi vừa

---

## ⚠️ LỖI NGHIÊM TRỌNG

### 1. **CRITICAL: Exchange Rate Not Found Exception**
**Location:** `OrderService.java:143`
```java
ExchangeRateResponse exchangeRate = exchangeRateService.getRate(currency);
```

**Vấn đề:**
- Nếu `currency` không tồn tại trong database → throw `RuntimeException`
- Không có fallback hoặc default exchange rate
- User sẽ không thể checkout nếu tỷ giá chưa được load

**Kịch bản lỗi:**
1. Database mới, chưa có exchange rates
2. RapidAPI failed và không fetch được tỷ giá
3. Currency code không match (ví dụ: "US" thay vì "USD")

**Impact:** ⚠️ **HIGH** - User không thể đặt hàng, application crash

**Fix đề xuất:**
```java
try {
    ExchangeRateResponse exchangeRate = exchangeRateService.getRate(currency);
    // ... existing code
} catch (RuntimeException e) {
    // Fallback to default rates
    log.error("Exchange rate not found for {}, using fallback rate", currency);
    throw new IllegalStateException(
        "Exchange rates are temporarily unavailable. Please try again later."
    );
}
```

**Hoặc tốt hơn:** Implement fallback rates in `ExchangeRateService`:
```java
public ExchangeRateResponse getRate(String currency) {
    return exchangeRateRepository
        .findByCurrency(currency.toUpperCase())
        .map(rate -> ExchangeRateResponse.builder()...)
        .orElseGet(() -> getFallbackRate(currency)); // Fallback
}

private ExchangeRateResponse getFallbackRate(String currency) {
    // Default rates if API failed
    BigDecimal defaultRate = currency.equals("USD")
        ? BigDecimal.valueOf(25000)
        : BigDecimal.valueOf(3500);

    return ExchangeRateResponse.builder()
        .currency(currency)
        .rateToVnd(defaultRate)
        .source("FALLBACK")
        .updatedAt(LocalDateTime.now())
        .build();
}
```

---

### 2. **CRITICAL: Transaction Rollback Issue**
**Location:** `OrderService.java:186-189`
```java
wallet.withdraw(depositAmount);        // Line 186: Wallet updated
Order savedOrder = orderRepository.save(order);  // Line 189: Could fail
```

**Vấn đề:**
- `wallet.withdraw()` thay đổi balance trong memory
- Nếu `orderRepository.save()` thất bại → wallet vẫn bị trừ tiền
- `@Transactional` ở class level có thể không rollback vì JPA detached entities

**Kịch bản lỗi:**
1. Wallet withdraw thành công
2. Database connection timeout khi save order
3. Transaction rollback NHƯNG wallet object trong memory đã bị modify
4. Nếu wallet được save sau đó → user mất tiền nhưng không có order

**Impact:** ⚠️ **CRITICAL** - User mất tiền, data inconsistency

**Fix đề xuất:**
```java
// Don't call wallet.withdraw() directly
// Let database transaction handle everything

// Record balance before
BigDecimal balanceBefore = wallet.getBalance();
BigDecimal balanceAfter = balanceBefore.subtract(depositAmount);

// Create transaction record FIRST
WalletTransaction transaction = WalletTransaction.builder()
    .wallet(wallet)
    .transactionType(TransactionType.ORDER_PAYMENT)
    .amount(depositAmount)
    .balanceBefore(balanceBefore)
    .balanceAfter(balanceAfter)
    .order(null) // Will be set after order is saved
    .description("Order deposit payment - PENDING")
    .build();

// Save order first
Order savedOrder = orderRepository.save(order);

// Update transaction with order
transaction.setOrder(savedOrder);
transaction.setDescription(String.format("Order deposit payment for order #%s", savedOrder.getOrderNumber()));

// NOW update wallet and save everything atomically
wallet.setBalance(balanceAfter);
walletRepository.save(wallet);
walletTransactionRepository.save(transaction);

log.info("Order {} created, wallet deducted {} VND",
    savedOrder.getOrderNumber(), depositAmount);
```

---

### 3. **CRITICAL: Null Marketplace Handling**
**Location:** `OrderService.java:141`
```java
String currency = inferCurrency(cartItem.getMarketplace());
```

**Vấn đề:**
- `cartItem.getMarketplace()` có thể null
- `inferCurrency()` trả về "USD" nếu null → **KHÔNG AN TOÀN**
- Nếu marketplace null → sản phẩm có thể là CNY nhưng bị tính USD

**Kịch bản lỗi:**
1. Cart item cũ từ trước khi có marketplace field
2. Data migration không đúng
3. Manual insert vào database

**Impact:** ⚠️ **HIGH** - Tính tiền sai, user trả thiếu/thừa

**Fix đề xuất:**
```java
// Validate marketplace exists
if (cartItem.getMarketplace() == null) {
    log.error("Cart item {} has null marketplace", cartItem.getId());
    throw new IllegalStateException(
        "Invalid cart item: missing marketplace information. Please remove and re-add this item."
    );
}

String currency = inferCurrency(cartItem.getMarketplace());
```

---

## ⚠️ LỖI VỪA

### 4. **Cart Item Currency Mismatch**
**Location:** `Frontend/CheckoutPage.jsx:143-144`
```javascript
const currency = item.currency || 'USD';
const itemTotal = item.price * item.quantity;
```

**Vấn đề:**
- Frontend assume currency là 'USD' nếu null
- Backend infer từ marketplace
- **Có thể khác nhau!**

**Impact:** MEDIUM - Hiển thị giá sai trên frontend

**Fix:** Ensure backend ALWAYS set currency field trong CartItem response

---

### 5. **Exchange Rate Null Check**
**Location:** `OrderService.java:144`
```java
BigDecimal itemTotalVND = itemTotal.multiply(exchangeRate.getRateToVnd())
```

**Vấn đề:**
- Không check `exchangeRate.getRateToVnd()` có thể null
- Sẽ throw NullPointerException

**Fix:**
```java
if (exchangeRate.getRateToVnd() == null ||
    exchangeRate.getRateToVnd().compareTo(BigDecimal.ZERO) <= 0) {
    throw new IllegalStateException(
        "Invalid exchange rate for " + currency
    );
}
```

---

## ✅ NHỮNG GÌ ĐÃ TỐT

1. ✅ Kiểm tra wallet balance trước khi trừ tiền
2. ✅ Validate cart không rỗng
3. ✅ Log đầy đủ để debug
4. ✅ Error message rõ ràng cho user
5. ✅ Frontend xử lý insufficient funds error tốt

---

## 🔧 HÀNH ĐỘNG CẦN THỰC HIỆN

**Ưu tiên cao (Fix ngay):**
1. ✅ Thêm fallback exchange rates trong `ExchangeRateService`
2. ✅ Fix transaction rollback issue trong `OrderService`
3. ✅ Validate marketplace không null

**Ưu tiên vừa (Fix trong sprint tiếp):**
4. Ensure backend set currency field trong CartItem
5. Add null check cho exchange rate value

**Ưu tiên thấp (Nice to have):**
- Add retry logic cho exchange rate API
- Add cache cho exchange rates
- Add monitoring/alerts cho failed transactions

---

## 📊 TESTING CHECKLIST

Sau khi fix, test các scenario sau:

- [ ] **Happy path:** User có đủ tiền, checkout thành công
- [ ] **Insufficient funds:** User không đủ tiền → error message đúng
- [ ] **No exchange rates:** Database rỗng → fallback rates hoạt động
- [ ] **Null marketplace:** Cart item không có marketplace → error rõ ràng
- [ ] **Database timeout:** Simulate DB error → transaction rollback đúng
- [ ] **Mixed currencies:** Cart có cả USD và CNY → tính tổng đúng
- [ ] **Wallet locked:** User wallet bị khóa → không cho checkout
- [ ] **Empty cart:** Cart rỗng → error message đúng

---

## 📝 NOTES

- Các fix này cần **testing kỹ lưỡng** trước khi deploy production
- Nên tạo **migration script** để fix cart items có null marketplace
- Cần **monitor** exchange rate API health
- Xem xét implement **circuit breaker** cho external API calls
