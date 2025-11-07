# Admin Order Detail Page - Trang Chi Tiết Đơn Hàng Admin

**Ngày tạo:** 2025-11-07
**Tình trạng:** ✅ Hoàn thành và sẵn sàng sử dụng

---

## 📋 Tổng Quan

Trang **Admin Order Detail** cho phép Admin và Staff xem chi tiết đầy đủ của một đơn hàng, bao gồm:

✅ **Thông tin khách hàng** - User ID, số điện thoại, địa chỉ giao hàng
✅ **Danh sách sản phẩm** - Tất cả sản phẩm trong đơn hàng với hình ảnh, giá, số lượng
✅ **Chi tiết các loại phí** - Product cost, service fee, shipping fees, additional services
✅ **Trạng thái thanh toán** - Đã cọc, còn lại, payment status
✅ **Lịch sử thời gian** - Ngày tạo đơn, cập nhật lần cuối
✅ **Quick actions** - Buttons để cập nhật trạng thái và phí ngay trên trang

---

## 🎯 Files Đã Tạo/Chỉnh Sửa

### Files Mới:
1. ✅ **`Frontend/src/features/admin/pages/AdminOrderDetailPage.jsx`** (649 lines)
   - Component trang chi tiết đơn hàng cho admin
   - Hiển thị đầy đủ thông tin order
   - Tích hợp modals để update status và fees

### Files Đã Chỉnh Sửa:
2. ✅ **`Frontend/src/features/admin/pages/AdminOrderManagementPage.jsx`**
   - Thêm import `useNavigate` từ react-router-dom
   - Thêm nút **"View Details"** (màu indigo, icon Eye) trong cột Actions
   - Navigate đến `/admin/orders/${order.id}` khi click

3. ✅ **`Frontend/src/App.jsx`**
   - Import `AdminOrderDetailPage`
   - Thêm route: `<Route path="/admin/orders/:orderId" element={<AdminOrderDetailPage />} />`

### Backend (Đã có sẵn):
4. ✅ **`Backend/.../Controller/AdminOrderController.java`**
   - Endpoint: `GET /api/admin/orders/{orderId}`
   - Method: `getOrderById(@PathVariable Long orderId)`

5. ✅ **`Backend/.../service/OrderService.java`**
   - Method: `getOrderByIdAdmin(Long orderId)` (lines 551-558)

6. ✅ **`Frontend/src/features/admin/services/adminOrderApi.js`**
   - Function: `getOrderById(orderId)` (already exists)

---

## 🚀 Cách Sử Dụng

### Bước 1: Đăng Nhập Admin Portal
```bash
# Truy cập admin login
http://localhost:5173/admin/login

# Đăng nhập với tài khoản admin
Email: admin@pandamall.com
Password: admin123
```

### Bước 2: Vào Trang Quản Lý Đơn Hàng
```
Admin Dashboard → Orders
Hoặc: http://localhost:5173/admin/orders
```

### Bước 3: Xem Chi Tiết Đơn Hàng
```
1. Tìm đơn hàng cần xem
2. Click nút "View Details" (màu indigo, icon mắt)
3. Trang chi tiết sẽ mở ra với URL: /admin/orders/{orderId}
```

---

## 📊 Nội Dung Trang Chi Tiết

### Layout
Trang được chia thành **2 cột**:

#### Cột Trái (2/3 width):
1. **Thông tin khách hàng**
   - User ID
   - Số điện thoại
   - Địa chỉ giao hàng

2. **Sản phẩm trong đơn hàng**
   - Hiển thị bằng component `OrderItemsList`
   - Hình ảnh sản phẩm
   - Tên, giá, số lượng
   - Tổng tiền từng item

3. **Ghi chú** (nếu có)
   - Note từ customer hoặc admin

#### Cột Phải (1/3 width):
1. **Trạng thái thanh toán**
   - Badge: PAID / PARTIALLY_PAID / UNPAID
   - Đã cọc (depositAmount)
   - Còn lại (remainingAmount)

2. **Chi tiết phí** (Fee Breakdown)
   - Tiền hàng (productCost)
   - Phí dịch vụ 1.5% (serviceFee)
   - **Phí vận chuyển:**
     - Nội địa TQ (domesticShippingFee) - hoặc "Chưa cập nhật"
     - Quốc tế (internationalShippingFee) - hoặc "Chưa cập nhật"
     - Nội địa VN COD (vietnamDomesticShippingFee) - nếu có
   - **Phí dịch vụ bổ sung** (additionalServicesFee) - nếu có
     - Hiển thị cân nặng (estimatedWeight)
   - **Tổng cộng** (totalAmount) - màu đỏ, bold

3. **Thời gian**
   - Ngày tạo đơn (createdAt)
   - Cập nhật lần cuối (updatedAt)

---

## ⚡ Quick Actions

### Buttons ở đầu trang:

#### 1. Nút "Cập nhật trạng thái" (Blue)
- Icon: Edit
- Click → Mở modal update status
- Modal cho phép chọn status mới và thêm ghi chú
- Submit → API call → Refresh trang

#### 2. Nút "Cập nhật phí" (Green)
- Icon: Truck
- Click → Mở modal `UpdateOrderFeesModal`
- Modal cho phép nhập:
  - Phí vận chuyển nội địa TQ (CNY)
  - Phí vận chuyển quốc tế (VND)
  - Cân nặng (kg)
  - Dịch vụ bổ sung (checkboxes)
  - Ghi chú
- Submit → API call → Refresh trang

#### 3. Nút "Quay lại danh sách đơn hàng"
- Icon: ArrowLeft
- Navigate về `/admin/orders`

---

## 🎨 UI Features

### Design Highlights:
- ✅ Professional admin theme (màu xanh blue/indigo/green)
- ✅ Two-column responsive layout
- ✅ Card-based sections với shadow
- ✅ Icons cho mỗi section (User, ShoppingCart, CreditCard, etc.)
- ✅ Color-coded status badges
- ✅ Clear typography hierarchy
- ✅ Consistent spacing and padding

### Color Scheme:
| Element | Color |
|---------|-------|
| View Details button | Indigo (bg-indigo-600) |
| Update Status button | Blue (bg-blue-600) |
| Update Fees button | Green (bg-green-600) |
| Section icons | Blue (text-blue-600) |
| Total amount | Red (text-red-600) |
| Remaining amount | Orange (text-orange-600) |
| Status badges | Green/Yellow/Red based on status |

### Responsive:
- ✅ Desktop: 2-column layout (2/3 + 1/3)
- ✅ Tablet/Mobile: Stacks into single column
- ✅ Max width container (max-w-7xl)
- ✅ Proper padding on all screen sizes

---

## 🔄 Navigation Flow

```
Admin Orders Page (/admin/orders)
        ↓ Click "View Details"
Admin Order Detail (/admin/orders/{id})
        ↓ Click action buttons
    - Update Status Modal
    - Update Fees Modal
        ↓ Click "Quay lại"
Admin Orders Page (/admin/orders)
```

---

## 📡 API Integration

### Endpoint Used:
```
GET /api/admin/orders/{orderId}
```

**Authorization:** Requires ADMIN or STAFF role (JWT token)

**Response Structure:**
```javascript
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 123,
    "orderNumber": "ORD-2025-001",
    "status": "CONFIRMED",
    "totalAmount": 1500000,
    "shippingAddress": "123 Đường ABC, Q.1, TP.HCM",
    "phone": "0901234567",
    "note": "Giao hàng ngoài giờ",
    "totalItems": 15,
    "items": [...],  // Array of OrderItemDTO
    "paymentStatus": "PARTIALLY_PAID",
    "depositAmount": 700000,
    "remainingAmount": 800000,
    "productCost": 1000000,
    "serviceFee": 15000,
    "domesticShippingFee": 175000,
    "internationalShippingFee": 200000,
    "additionalServicesFee": 110000,
    "estimatedWeight": 3.5,
    "vietnamDomesticShippingFee": 0,
    "isCodShipping": true,
    "createdAt": "2025-01-01T10:00:00",
    "updatedAt": "2025-01-05T15:30:00"
  }
}
```

---

## ✨ Key Features

### 1. Complete Order Information
- ✅ Tất cả thông tin đơn hàng trong 1 trang
- ✅ Không cần chuyển trang để xem chi tiết
- ✅ Dễ dàng so sánh các loại phí

### 2. Customer Information Display
- ✅ Hiển thị User ID để admin có thể tra cứu
- ✅ Số điện thoại để liên hệ
- ✅ Địa chỉ đầy đủ để kiểm tra shipping

### 3. Product List with Details
- ✅ Tái sử dụng component `OrderItemsList` từ customer view
- ✅ Consistent UI giữa admin và customer
- ✅ Hiển thị marketplace badge, price, quantity

### 4. Fee Breakdown
- ✅ Chi tiết từng loại phí
- ✅ Phân biệt rõ giữa các phí shipping
- ✅ Hiển thị "Chưa cập nhật" cho phí chưa có
- ✅ Tổng cộng nổi bật

### 5. Quick Actions
- ✅ 2 buttons chính ngay đầu trang
- ✅ Không cần quay về list page để update
- ✅ Modal mở nhanh với thông tin order đã load
- ✅ Auto-refresh sau khi update thành công

### 6. Status & Payment Display
- ✅ Color-coded badges cho status
- ✅ Clear payment status với 3 states
- ✅ Deposit và remaining amount nổi bật
- ✅ Easy to see at a glance

---

## 🧪 Testing Checklist

### Test Case 1: View Order Detail
- [ ] Login admin portal
- [ ] Vào orders page
- [ ] Click "View Details" cho 1 order
- [ ] Expected:
  - ✅ Navigate to `/admin/orders/{id}`
  - ✅ Order detail loads
  - ✅ All information displays correctly
  - ✅ No console errors

### Test Case 2: Customer Information
- [ ] Check user ID displays
- [ ] Check phone number displays
- [ ] Check shipping address displays
- [ ] Verify all fields match order data

### Test Case 3: Product List
- [ ] Check all items display
- [ ] Check images load
- [ ] Check prices formatted correctly
- [ ] Check marketplace badges show

### Test Case 4: Fee Breakdown
- [ ] Product cost correct
- [ ] Service fee correct (1.5%)
- [ ] Shipping fees display or "Chưa cập nhật"
- [ ] Additional services fee displays if > 0
- [ ] Weight shows if set
- [ ] Total amount matches sum

### Test Case 5: Payment Status
- [ ] Badge color correct for status
- [ ] Deposit amount displays
- [ ] Remaining amount displays
- [ ] Colors appropriate (orange for remaining)

### Test Case 6: Quick Actions
- [ ] "Update Status" button opens modal
- [ ] Modal shows correct order info
- [ ] Can update status successfully
- [ ] "Update Fees" button opens modal
- [ ] Modal shows correct order info
- [ ] Can update fees successfully
- [ ] Page refreshes after update

### Test Case 7: Navigation
- [ ] "Quay lại" button works
- [ ] Navigate back to `/admin/orders`
- [ ] Breadcrumb/back button always visible

### Test Case 8: Responsive Design
- [ ] Desktop view (2 columns)
- [ ] Tablet view (responsive)
- [ ] Mobile view (stacked)
- [ ] All elements readable on all sizes

### Test Case 9: Edge Cases
- [ ] Order with no items (should still work)
- [ ] Order with no note (section hidden)
- [ ] Order with no fees set (shows "Chưa cập nhật")
- [ ] Order with COD shipping (shows VN shipping fee)
- [ ] Order without COD (VN shipping = 0)

### Test Case 10: Error Handling
- [ ] Invalid order ID → Show error message
- [ ] Network error → Show error with retry
- [ ] Order not found → Show not found message
- [ ] Loading state displays correctly

---

## 🐛 Troubleshooting

### Problem: Trang không load
**Solution:**
1. Check backend running on port 8080
2. Check JWT token valid (re-login if needed)
3. Check order ID in URL is valid number
4. Check browser console for errors

### Problem: "Order not found"
**Solution:**
1. Verify order exists in database
2. Check order ID in URL
3. Ensure user has ADMIN or STAFF role
4. Check backend logs for errors

### Problem: Some fields show "N/A" or empty
**Solution:**
1. Normal if data not set (e.g., fees not updated yet)
2. "Chưa cập nhật" is expected for fees before admin sets them
3. Check OrderResponse includes all fields

### Problem: Images không load
**Solution:**
1. Check item images có URL
2. Check network tab for image requests
3. Verify CORS for image URLs

### Problem: Buttons không work
**Solution:**
1. Check console for JavaScript errors
2. Verify modals imported correctly
3. Check API functions in adminOrderApi.js
4. Test with simple alert to isolate issue

---

## 📝 Code Structure

### Component Organization:
```
AdminOrderDetailPage
├── Header Section
│   ├── Back button
│   ├── Order title & ID
│   └── Status badge
├── Action Buttons
│   ├── Update Status
│   └── Update Fees
├── Left Column (2/3)
│   ├── Customer Info Card
│   ├── Order Items Card
│   └── Note Card (conditional)
└── Right Column (1/3)
    ├── Payment Status Card
    ├── Fee Breakdown Card
    └── Timeline Card
```

### State Management:
```javascript
const [order, setOrder] = useState(null);
const [loading, setLoading] = useState(true);
const [error, setError] = useState(null);
const [showStatusModal, setShowStatusModal] = useState(false);
const [showFeesModal, setShowFeesModal] = useState(false);
const [newStatus, setNewStatus] = useState('');
const [statusNote, setStatusNote] = useState('');
const [updatingStatus, setUpdatingStatus] = useState(false);
```

### Key Functions:
- `fetchOrder()` - Load order data from API
- `handleUpdateStatus()` - Update order status
- `handleUpdateFees()` - Update order fees
- `formatCurrency()` - Format VND currency
- `formatDate()` - Format datetime display

---

## 🔮 Future Enhancements

### Short Term:
- [ ] Add order status history timeline
- [ ] Show who updated status/fees (admin name)
- [ ] Add print order detail button
- [ ] Export to PDF functionality

### Medium Term:
- [ ] Add customer name (fetch from User service)
- [ ] Email customer notification after status update
- [ ] SMS notification for important updates
- [ ] Add internal notes (only visible to admin)

### Long Term:
- [ ] Real-time updates (WebSocket)
- [ ] Audit log for all changes
- [ ] Comparison view (before/after fee update)
- [ ] Bulk actions from detail page
- [ ] Integration with shipping tracking

---

## ✅ Summary

**Tính năng đã sẵn sàng sử dụng!**

**What's New:**
- ✅ Complete admin order detail page
- ✅ Full order information in one view
- ✅ Customer info, products, all fees
- ✅ Quick action buttons for update
- ✅ Professional admin UI theme
- ✅ Responsive design
- ✅ Error handling & loading states

**Benefits:**
🎯 Admin có đầy đủ thông tin đơn hàng
🎯 Không cần chuyển trang nhiều lần
🎯 Quick actions để update ngay
🎯 Clear fee breakdown dễ hiểu
🎯 Professional và easy to use

**Next Steps:**
1. ✅ Code complete
2. ⏳ Manual testing
3. ⏳ User acceptance testing
4. ⏳ Deploy to production

---

**Created by:** Claude Code
**Date:** 2025-11-07
**Version:** 1.0.0
