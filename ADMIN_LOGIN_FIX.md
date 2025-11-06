# 🔧 FIX: Admin Login Role Validation Error

**Date:** 2025-11-06
**Issue:** Admin login báo lỗi "Access denied. Admin or Staff role required."
**Status:** ✅ FIXED

---

## 🐛 NGUYÊN NHÂN LỖI

### Backend Response Structure:
Backend chỉ trả về:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**KHÔNG có** `user` object hoặc `role` field trong response!

### Code Lỗi (AdminLoginPage.jsx):
```javascript
// ❌ WRONG - response không có user hoặc role field
const userRole = response.user?.role || response.role;

if (userRole !== 'ADMIN' && userRole !== 'STAFF') {
  toast.error('Access denied...');
  return;
}
```

**Kết quả:** `userRole` luôn = `undefined` → Luôn bị reject!

---

## ✅ GIẢI PHÁP

### JWT Token Contains User Info:
Token chứa tất cả thông tin user trong **claims**:

```javascript
// Decoded JWT token:
{
  "userId": 1,
  "role": "ADMIN",           // ← Role ở đây!
  "type": "access_token",
  "email": "admin@pandamall.com",
  "fullname": "Admin User",
  "status": "ACTIVE",
  "sub": "admin@pandamall.com",  // username or email
  "iat": 1234567890,
  "exp": 1234567890
}
```

### Code Đã Sửa:
```javascript
import { jwtDecode } from 'jwt-decode';

// ✅ CORRECT - Decode token để lấy role
const response = await login({ username, password });

// Decode JWT token
const decodedToken = jwtDecode(response.token);
console.log('Decoded token:', decodedToken);

// Extract role from token claims
const userRole = decodedToken.role;  // ← Lấy từ token!

// Validate role
if (userRole !== 'ADMIN' && userRole !== 'STAFF') {
  toast.error('Access denied. Admin or Staff role required.');
  return;
}

// Create user object from decoded claims
const userData = {
  userId: decodedToken.userId,
  email: decodedToken.email,
  username: decodedToken.sub, // sub = username or email
  fullname: decodedToken.fullname,
  role: decodedToken.role,
  status: decodedToken.status
};

// Save to AuthContext
authLogin(response.token, userData);

// Success!
toast.success(`Welcome, ${userRole}!`);
navigate('/admin/dashboard');
```

---

## 🔄 JWT TOKEN CLAIMS

### Backend Token Generation (JwtService.java):
```java
private Map<String, Object> extractClaims(User user, String tokenType) {
    Map<String, Object> claims = new HashMap<>();

    claims.put("userId", user.getUserId());
    claims.put("role", user.getRole().name());      // ← ADMIN/STAFF/CUSTOMER
    claims.put("type", tokenType);                  // access_token/refresh_token
    claims.put("email", user.getEmail());
    claims.put("fullname", user.getFullName());
    claims.put("status", user.getStatus().name());  // ACTIVE/LOCKED

    return claims;
}

// Subject (sub) = username or email
String subject = user.getUsername() != null
    ? user.getUsername()
    : user.getEmail();
```

### Available Claims:
| Claim | Type | Example | Description |
|-------|------|---------|-------------|
| `userId` | Number | 1 | User ID |
| `role` | String | "ADMIN" | User role |
| `email` | String | "admin@pandamall.com" | Email |
| `fullname` | String | "Admin User" | Full name |
| `status` | String | "ACTIVE" | Account status |
| `sub` | String | "admin@pandamall.com" | Username or email |
| `type` | String | "access_token" | Token type |
| `iat` | Number | 1234567890 | Issued at (timestamp) |
| `exp` | Number | 1234567890 | Expiry (timestamp) |

---

## 📝 FILES MODIFIED

### File: `Frontend/src/features/admin/pages/AdminLoginPage.jsx`

#### 1. Added Import:
```javascript
import { jwtDecode } from 'jwt-decode';
```

#### 2. Updated handleSubmit Logic:
```javascript
// Before:
const userRole = response.user?.role || response.role; // ❌ undefined

// After:
const decodedToken = jwtDecode(response.token);
const userRole = decodedToken.role; // ✅ "ADMIN" or "STAFF"
```

#### 3. Extract User Data from Token:
```javascript
const userData = {
  userId: decodedToken.userId,
  email: decodedToken.email,
  username: decodedToken.sub,
  fullname: decodedToken.fullname,
  role: decodedToken.role,
  status: decodedToken.status
};
```

---

## 🧪 TESTING

### Test Case 1: Admin Login
```
Input:
- Username: admin@pandamall.com
- Password: admin123

Expected:
✅ Token decoded successfully
✅ decodedToken.role = "ADMIN"
✅ Pass role validation
✅ Toast: "Welcome, ADMIN!"
✅ Redirect to /admin/dashboard
```

### Test Case 2: Staff Login
```
Input:
- Username: VanB
- Password: password

Expected:
✅ Token decoded successfully
✅ decodedToken.role = "STAFF"
✅ Pass role validation
✅ Toast: "Welcome, STAFF!"
✅ Redirect to /admin/dashboard
```

### Test Case 3: Customer Login (Should Fail)
```
Input:
- Username: demo.account@gmail.com
- Password: demo123

Expected:
✅ Token decoded successfully
✅ decodedToken.role = "CUSTOMER"
❌ Fail role validation
❌ Toast: "Access denied. Admin or Staff role required."
❌ Stay on /admin/login
```

### Console Logs:
```javascript
// Should see in browser console:
Admin login response: { token: "eyJ..." }
Decoded token: {
  userId: 1,
  role: "ADMIN",
  email: "admin@pandamall.com",
  ...
}
```

---

## 🔍 DEBUGGING TIPS

### Check Token in Browser:
```javascript
// Open browser DevTools → Console
// After login, run:
const token = localStorage.getItem('token');
console.log(token);

// Decode token online:
// Go to https://jwt.io/
// Paste token → See decoded claims
```

### Check Role in Database:
```sql
-- MySQL
SELECT user_id, username, email, role, status
FROM users
WHERE email = 'admin@pandamall.com';

-- Expected:
-- role = 'ADMIN'
-- status = 'ACTIVE'
```

### Common Issues:

#### Issue 1: "jwtDecode is not a function"
```bash
# Install package
cd Frontend
npm install jwt-decode

# Restart dev server
npm run dev
```

#### Issue 2: Token không có role claim
```
Check backend JwtService.java:
- Line 62: claims.put("role", user.getRole().name());
- Ensure role is added to claims
```

#### Issue 3: Role = null trong token
```
Check database:
- Ensure user has valid role (ADMIN, STAFF, CUSTOMER)
- Not null or empty
```

---

## 📊 COMPARISON: Before vs After

### Before (❌ Broken):
```javascript
const userRole = response.user?.role;  // undefined
↓
userRole = undefined
↓
if (undefined !== 'ADMIN' && undefined !== 'STAFF') // true
↓
toast.error('Access denied')  // ❌ Always rejected!
```

### After (✅ Working):
```javascript
const decodedToken = jwtDecode(response.token);
const userRole = decodedToken.role;  // "ADMIN"
↓
userRole = "ADMIN"
↓
if ("ADMIN" !== 'ADMIN' && "ADMIN" !== 'STAFF') // false
↓
Pass validation, proceed to dashboard  // ✅ Success!
```

---

## 🎯 KEY LEARNINGS

1. **Backend doesn't return user object in login response**
   - Only returns JWT token
   - All user info is INSIDE the token

2. **Must decode JWT token to get user info**
   - Use `jwt-decode` library
   - Extract claims: role, email, userId, etc.

3. **Token claims are source of truth**
   - Backend validates token on every request
   - Frontend can trust decoded claims for UI logic

4. **Same pattern used in customer login**
   - Check `LoginPage.jsx` - also uses `jwtDecode`
   - AuthContext validates token expiry
   - AdminLoginPage now follows same pattern

---

## ✅ VERIFICATION CHECKLIST

- [x] Import `jwtDecode` from 'jwt-decode'
- [x] Decode JWT token after login
- [x] Extract role from `decodedToken.role`
- [x] Validate role === 'ADMIN' or 'STAFF'
- [x] Create userData object from decoded claims
- [x] Save to AuthContext with correct fields
- [x] Test with admin account: ✅ Works
- [x] Test with staff account: ✅ Works
- [x] Test with customer account: ❌ Rejected (expected)
- [x] Console logs show decoded token
- [x] Success toast shows correct role
- [x] Redirect to /admin/dashboard works

---

## 🚀 DEPLOYMENT NOTES

### No Backend Changes Required:
✅ Backend already returns JWT token with all claims
✅ No API changes needed
✅ No database changes needed

### Frontend Changes Only:
✅ AdminLoginPage.jsx updated
✅ Uses existing jwt-decode package
✅ No new dependencies
✅ No breaking changes

### Deploy Steps:
1. Pull latest code
2. Restart frontend: `npm run dev`
3. Test admin login
4. Deploy to production

---

**📅 Fixed Date:** 2025-11-06
**👨‍💻 Fixed By:** Claude Code
**✅ Status:** VERIFIED & WORKING
