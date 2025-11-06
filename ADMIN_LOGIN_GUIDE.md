# 🔐 HƯỚNG DẪN ĐĂNG NHẬP ADMIN/STAFF

**Ngày tạo:** 2025-11-06
**Phiên bản:** 1.0

---

## 📋 TỔNG QUAN

Hệ thống có **2 trang đăng nhập riêng biệt**:

### 1. **Customer Portal** (`/login`)
- Dành cho khách hàng thông thường
- Truy cập: Shopping cart, Orders, Wallet
- Sau khi login → Redirect về homepage

### 2. **Admin Portal** (`/admin/login`)
- Dành cho Admin và Staff
- Truy cập: Admin Dashboard, Management features
- Sau khi login → Redirect về `/admin/dashboard`
- **Kiểm tra role:** Chỉ cho phép ADMIN và STAFF

---

## 🚪 CÁCH TRUY CẬP ADMIN PORTAL

### Cách 1: Từ Homepage
```
1. Vào http://localhost:5173/
2. Scroll xuống cuối trang
3. Thấy phần "Are you an administrator or staff member?"
4. Click nút "Admin Portal"
5. Redirect đến /admin/login
```

### Cách 2: URL trực tiếp
```
Truy cập: http://localhost:5173/admin/login
```

---

## 👤 TÀI KHOẢN DEMO

### Admin Account:
```
Email: admin@pandamall.com
Password: admin123
Role: ADMIN
```

**Hoặc:**
```
Username: VanA
Password: password
Role: ADMIN
```

### Staff Account:
```
Username: VanB
Password: password
Role: STAFF
```

---

## 🔄 LUỒNG ĐĂNG NHẬP ADMIN

```
┌─────────────────────────────────────────┐
│ 1. User vào /admin/login                │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│ 2. Nhập username/email + password       │
│    - admin@pandamall.com / admin123     │
│    - VanA / password                    │
│    - VanB / password                    │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│ 3. Click "Sign In to Dashboard"        │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│ 4. Backend kiểm tra credentials         │
│    POST /api/auth/login                 │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│ 5. Frontend kiểm tra role               │
│    ✓ ADMIN → OK                         │
│    ✓ STAFF → OK                         │
│    ✗ CUSTOMER → Error: Access denied    │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│ 6. Save token + user to localStorage    │
│    Refresh cart context                 │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│ 7. Redirect to /admin/dashboard         │
│    Show success toast                   │
└─────────────────────────────────────────┘
```

---

## ✅ TESTING

### Test Case 1: Admin Login Success
```
Steps:
1. Vào http://localhost:5173/admin/login
2. Nhập: admin@pandamall.com / admin123
3. Click "Sign In to Dashboard"

Expected:
✅ Success toast: "Welcome, ADMIN!"
✅ Redirect to /admin/dashboard
✅ Dashboard loads successfully
✅ Admin menu visible
```

### Test Case 2: Staff Login Success
```
Steps:
1. Vào http://localhost:5173/admin/login
2. Nhập: VanB / password
3. Click "Sign In to Dashboard"

Expected:
✅ Success toast: "Welcome, STAFF!"
✅ Redirect to /admin/dashboard
✅ Dashboard loads successfully
✅ Staff can access management features
```

### Test Case 3: Customer Login Denied
```
Steps:
1. Vào http://localhost:5173/admin/login
2. Nhập: demo.account@gmail.com / demo123
3. Click "Sign In to Dashboard"

Expected:
❌ Error toast: "Access denied. Admin or Staff role required."
❌ Stay on /admin/login
❌ Not redirected to dashboard
```

### Test Case 4: Invalid Credentials
```
Steps:
1. Vào http://localhost:5173/admin/login
2. Nhập: wronguser / wrongpass
3. Click "Sign In to Dashboard"

Expected:
❌ Error toast: "Login failed" hoặc backend error
❌ Stay on /admin/login
```

### Test Case 5: Empty Fields
```
Steps:
1. Vào http://localhost:5173/admin/login
2. Để trống username hoặc password
3. Click "Sign In to Dashboard"

Expected:
❌ Error toast: "Please fill in all fields"
❌ Form validation error
```

---

## 🎨 UI FEATURES

### Design Highlights:
- ✅ Dark gradient background (professional admin look)
- ✅ Shield icon (security theme)
- ✅ "Admin Portal" branding
- ✅ Show/Hide password toggle
- ✅ Loading state during login
- ✅ Demo accounts info displayed
- ✅ "Back to Customer Portal" link

### Responsive Design:
- ✅ Mobile-friendly
- ✅ Centered card layout
- ✅ Smooth transitions
- ✅ Focus states for accessibility

---

## 🔒 SECURITY FEATURES

### Role-Based Access Control:
```javascript
// Frontend validation
const userRole = response.user?.role || response.role;

if (userRole !== 'ADMIN' && userRole !== 'STAFF') {
  toast.error('Access denied. Admin or Staff role required.');
  return; // Don't save token, don't redirect
}
```

### Backend Validation:
- Backend already validates JWT token
- Admin routes protected by `@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")`
- Double-layer security (frontend + backend)

### Token Management:
- Same JWT system as customer login
- Token contains role information
- AdminRoute component checks role before rendering

---

## 📁 FILES MODIFIED/CREATED

### Created:
1. **`Frontend/src/features/admin/pages/AdminLoginPage.jsx`**
   - New admin login page component
   - Role validation logic
   - Dark theme UI

### Modified:
2. **`Frontend/src/App.jsx`**
   - Added import: `AdminLoginPage`
   - Added route: `/admin/login`
   - Updated HomePage with admin portal link

---

## 🔧 CONFIGURATION

### No additional configuration needed!
- Uses existing AuthContext
- Uses existing login API
- Uses existing JWT system
- Only adds role check on frontend

---

## 🚨 IMPORTANT NOTES

### 1. Customer Cannot Access Admin Login
- Customer với role "CUSTOMER" sẽ bị reject
- Error message rõ ràng
- Không save token nếu role sai

### 2. Admin Can Still Use Customer Portal
- Admin có thể login qua `/login`
- Admin có thể shopping như customer
- Linh hoạt giữa 2 portals

### 3. Role Stored in JWT Token
- Token contains role claim
- Backend validates role on every request
- Frontend only checks for UX (backend is source of truth)

### 4. Existing Admin Accounts
Pre-registered in database (từ `LoginController @PostConstruct`):
- admin@pandamall.com (ADMIN)
- VanA (ADMIN)
- VanB (STAFF)
- VanC (CUSTOMER)

---

## 🎯 USER EXPERIENCE

### Admin Login Flow:
```
Homepage → "Admin Portal" link → /admin/login
                                      ↓
                              Enter credentials
                                      ↓
                              Role validation
                                      ↓
                              /admin/dashboard
```

### Customer Login Flow (unchanged):
```
Homepage → "Login" link → /login
                              ↓
                      Enter credentials
                              ↓
                          Homepage
```

---

## 📊 COMPARISON

| Feature | Customer Portal | Admin Portal |
|---------|----------------|--------------|
| **URL** | `/login` | `/admin/login` |
| **Allowed Roles** | ALL (ADMIN, STAFF, CUSTOMER) | ADMIN, STAFF only |
| **Redirect After Login** | `/` (Homepage) | `/admin/dashboard` |
| **UI Theme** | Light, customer-friendly | Dark, professional |
| **Features Access** | Shopping, Cart, Orders | Management, Analytics |
| **Demo Accounts** | demo.account@gmail.com | admin@pandamall.com, VanA, VanB |

---

## 🐛 TROUBLESHOOTING

### Problem: "Access denied" khi login với admin account
```
Solution:
1. Check account role trong database:
   SELECT user_id, username, email, role FROM users WHERE email = 'admin@pandamall.com';

2. Ensure role = 'ADMIN' hoặc 'STAFF'

3. If role = 'CUSTOMER', update:
   UPDATE users SET role = 'ADMIN' WHERE user_id = X;
```

### Problem: Redirect về homepage thay vì dashboard
```
Solution:
1. Check AdminLoginPage.jsx line 51-54
2. Ensure role check logic đúng
3. Check navigate('/admin/dashboard') được gọi
```

### Problem: 404 khi vào /admin/login
```
Solution:
1. Check App.jsx có route <Route path="/admin/login" element={<AdminLoginPage />} />
2. Restart Vite dev server: npm run dev
3. Clear browser cache
```

---

## 📝 EXAMPLE USAGE

### Scenario 1: Admin muốn quản lý orders
```
1. Vào http://localhost:5173/admin/login
2. Login: admin@pandamall.com / admin123
3. Redirect to /admin/dashboard
4. Click "Orders" trong admin nav
5. Xem tất cả orders, update status
```

### Scenario 2: Staff muốn quản lý users
```
1. Vào http://localhost:5173/admin/login
2. Login: VanB / password
3. Redirect to /admin/dashboard
4. Click "Users" trong admin nav
5. Xem users, edit roles
```

### Scenario 3: Customer thử access admin portal
```
1. Vào http://localhost:5173/admin/login
2. Login: demo.account@gmail.com / demo123
3. Error: "Access denied. Admin or Staff role required."
4. Stay on login page
5. Must use /login instead
```

---

## ✨ SUMMARY

### What's New:
✅ Separate admin login page (`/admin/login`)
✅ Role-based access control (ADMIN/STAFF only)
✅ Automatic redirect to admin dashboard
✅ Professional dark-themed UI
✅ Demo accounts displayed on login page
✅ "Back to Customer Portal" link

### What Stays Same:
✅ Same authentication API
✅ Same JWT token system
✅ Same AuthContext
✅ Customer login unchanged
✅ No database changes needed

### Benefits:
🎯 Clear separation of customer vs admin portals
🎯 Better security (role check before entry)
🎯 Better UX (direct access to dashboard)
🎯 Professional admin experience
🎯 Easy to test with demo accounts

---

**📅 Created:** 2025-11-06
**👨‍💻 Created By:** Claude Code
**✅ Status:** READY TO USE
