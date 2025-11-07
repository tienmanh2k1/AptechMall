# Security Fix: Admin Self-Deletion Prevention

**Date:** 2025-11-07
**Severity:** 🔴 CRITICAL
**Status:** ✅ FIXED

---

## Vulnerability Description

Admin users could delete their own accounts through the User Management interface, leading to:
- Permanent loss of admin account (if no database backup)
- Immediate logout and service disruption
- Potential system lockout (if last admin deleted)
- Poor user experience (accidental self-deletion)

---

## Root Cause

### Backend
- `UserManagementService.deleteUser()` only checked if user exists
- Did NOT validate if the target user is the currently authenticated admin
- No protection against deleting the last admin account

### Frontend
- Delete button displayed for ALL users including current admin
- No client-side validation to prevent self-deletion
- Only relied on a generic confirmation dialog

---

## Fix Implementation

### ✅ 1. Backend Repository Enhancement
**File:** `Backend/src/main/java/com/aptech/aptechMall/repository/UserRepository.java:47`

**Added:**
```java
/**
 * Count users by role
 * @param role User role (ADMIN, STAFF, CUSTOMER)
 * @return Number of users with the given role
 */
long countByRole(Role role);
```

**Purpose:** Enable checking if admin is the last one before deletion.

---

### ✅ 2. Backend Service Validation
**File:** `Backend/src/main/java/com/aptech/aptechMall/service/admin/UserManagementService.java:88-112`

**Added:**
```java
public void deleteUser(Long id) {
    // Check if user exists
    if (!userRepository.existsById(id)) {
        throw new RuntimeException("User not found with id: " + id);
    }

    // ✅ NEW: Prevent admin from deleting their own account
    Long currentUserId = AuthenticationUtil.getCurrentUserId();
    if (id.equals(currentUserId)) {
        throw new RuntimeException("You cannot delete your own account");
    }

    // ✅ NEW: Prevent deletion of the last admin account
    User userToDelete = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

    if (userToDelete.getRole() == Role.ADMIN) {
        long adminCount = userRepository.countByRole(Role.ADMIN);
        if (adminCount <= 1) {
            throw new RuntimeException("Cannot delete the last admin account");
        }
    }

    userRepository.deleteById(id);
}
```

**Security Features:**
1. ✅ Extracts `currentUserId` from JWT token (not from client)
2. ✅ Blocks self-deletion with clear error message
3. ✅ Prevents deletion of last admin (system lockout protection)
4. ✅ Uses `AuthenticationUtil.getCurrentUserId()` following security best practices

---

### ✅ 3. Frontend UI Protection
**File:** `Frontend/src/features/admin/pages/AdminUserManagementPage.jsx`

**Changes:**

#### a) Import AuthContext
```javascript
import { useAuth } from '../../auth/context/AuthContext';

const AdminUserManagementPage = () => {
  const { user: currentUser } = useAuth();  // ✅ NEW
  // ...
```

#### b) Enhanced Delete Handler
```javascript
const handleDelete = async (userId, username) => {
  // ✅ NEW: Prevent admin from deleting their own account
  if (userId === currentUser?.userId) {
    toast.error('You cannot delete your own account');
    return;
  }

  if (!window.confirm(`Are you sure you want to delete user "${username}"?`)) {
    return;
  }
  // ... existing delete logic
};
```

#### c) Conditional Button Rendering (Line 382-398)
```javascript
{user.userId !== currentUser?.userId ? (
  <button
    onClick={() => handleDelete(user.userId, user.username)}
    className="text-red-600 hover:text-red-900 p-1 rounded hover:bg-red-50"
    title="Delete"
  >
    <Trash2 className="w-4 h-4" />
  </button>
) : (
  <button
    disabled
    className="text-gray-400 cursor-not-allowed p-1 rounded"
    title="Cannot delete yourself"
  >
    <Trash2 className="w-4 h-4" />
  </button>
)}
```

**UI Improvements:**
1. ✅ Delete button disabled (gray) for current user's account
2. ✅ Tooltip shows "Cannot delete yourself"
3. ✅ Additional validation in handler (defense-in-depth)
4. ✅ User-friendly error toast message

---

## Testing Instructions

### Prerequisites
```bash
# Start MySQL server
# MySQL must be running on localhost:3306 with database 'test_db'

# Start Redis server (required for auth)

# Start Backend
cd Backend
./mvnw spring-boot:run

# Start Frontend (in new terminal)
cd Frontend
npm run dev
```

### Test Case 1: Backend API Protection
```bash
# 1. Login as admin
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin@pandamall.com","password":"admin123"}'

# Response: { "token": "...", "user": { "userId": 1, ... } }

# 2. Try to delete own account (use userId from login response)
curl -X DELETE http://localhost:8080/api/users/1 \
  -H "Authorization: Bearer YOUR_TOKEN"

# ✅ Expected: HTTP 400 Bad Request
# {"message": "You cannot delete your own account"}
```

### Test Case 2: Frontend UI Protection
1. Navigate to: `http://localhost:5173/admin/login`
2. Login with: `admin@pandamall.com` / `admin123`
3. Go to: User Management page
4. Find your own account in the list
5. ✅ **Expected:** Delete button is **gray and disabled**
6. Hover over button → Tooltip shows "Cannot delete yourself"
7. Find another user's row
8. ✅ **Expected:** Delete button is **red and clickable**

### Test Case 3: Last Admin Protection
```bash
# Scenario: Only 1 admin exists in database

# 1. Login as STAFF user
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"VanB","password":"password"}'

# 2. Try to delete the last admin
curl -X DELETE http://localhost:8080/api/users/{last_admin_id} \
  -H "Authorization: Bearer STAFF_TOKEN"

# ✅ Expected: HTTP 400 Bad Request
# {"message": "Cannot delete the last admin account"}
```

---

## Verification Checklist

- [x] Backend: Added `countByRole()` to UserRepository
- [x] Backend: Added self-deletion validation in UserManagementService
- [x] Backend: Added last-admin protection
- [x] Backend: Uses `AuthenticationUtil.getCurrentUserId()` (secure)
- [x] Frontend: Imported `useAuth()` hook
- [x] Frontend: Added validation in `handleDelete()`
- [x] Frontend: Disabled delete button for current user
- [x] Frontend: Shows appropriate tooltip
- [x] Code compiles without errors
- [ ] **Manual testing required** (MySQL not running during fix)

---

## Files Modified

### Backend (3 files)
1. `Backend/src/main/java/com/aptech/aptechMall/repository/UserRepository.java`
   - Added `countByRole(Role role)` method

2. `Backend/src/main/java/com/aptech/aptechMall/service/admin/UserManagementService.java`
   - Added import: `AuthenticationUtil`
   - Enhanced `deleteUser()` with security checks

### Frontend (1 file)
3. `Frontend/src/features/admin/pages/AdminUserManagementPage.jsx`
   - Added import: `useAuth`
   - Added validation in `handleDelete()`
   - Conditional rendering for delete button

### Documentation (2 files)
4. `test-admin-deletion-security.md` - Test cases and steps
5. `SECURITY_FIX_ADMIN_SELF_DELETION.md` - This document

---

## Security Impact

### Before Fix
| Threat | Impact | Likelihood |
|--------|--------|------------|
| Admin self-deletion | 🔴 CRITICAL | 🟡 Medium |
| System lockout (last admin) | 🔴 CRITICAL | 🟢 Low |
| Accidental deletion | 🟠 High | 🟡 Medium |

### After Fix
| Threat | Impact | Likelihood |
|--------|--------|------------|
| Admin self-deletion | ✅ BLOCKED | N/A |
| System lockout (last admin) | ✅ BLOCKED | N/A |
| Accidental deletion | ✅ PREVENTED | N/A |

**Risk Reduction:** 🔴 CRITICAL → ✅ MITIGATED

---

## Additional Security Notes

### Defense-in-Depth
This fix implements **two layers of protection**:
1. **Backend validation** (primary) - Cannot be bypassed by client
2. **Frontend UI protection** (secondary) - Better UX, prevents accidental clicks

### Follows Project Security Standards
✅ Backend uses `AuthenticationUtil.getCurrentUserId()` from JWT token
✅ Never accepts `userId` from client parameters
✅ Consistent with existing cart/order security patterns
✅ See: `Backend/SECURITY_FIX_CART_ORDER_ISOLATION.md`

### Known Limitations
- **Not tested in production** - MySQL not running during development
- **Manual testing required** - Start MySQL and verify all test cases
- **No automated tests** - Consider adding unit/integration tests

---

## Recommendations

### Immediate Actions
1. ✅ **DONE:** Apply security fix to codebase
2. 🔄 **PENDING:** Start MySQL and run manual tests
3. 🔄 **PENDING:** Verify fix works in all scenarios

### Future Improvements
1. Add unit tests for `UserManagementService.deleteUser()`
2. Add integration tests for DELETE `/api/users/{id}` endpoint
3. Add E2E tests for admin user management UI
4. Consider soft-delete instead of hard-delete (preserve audit trail)
5. Add audit logging for user deletions
6. Send email notification when admin account is deleted by another admin

### Related Security Reviews
- Review other admin operations (update role, suspend user, etc.)
- Ensure similar protection exists for other critical operations
- Review STAFF permissions (can they delete admins?)

---

## Conclusion

✅ **Security fix successfully applied**
✅ **Code compiles without errors**
🔄 **Manual testing pending** (requires MySQL)

The vulnerability has been **mitigated** through defense-in-depth security measures. Both backend and frontend now prevent admin self-deletion and last-admin deletion.

**Next Step:** Start MySQL server and run test cases to verify fix behavior.

---

**Fixed by:** Claude Code
**Date:** 2025-11-07
**Issue Severity:** 🔴 CRITICAL
**Fix Status:** ✅ COMPLETE
