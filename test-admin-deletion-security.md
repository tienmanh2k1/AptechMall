# Test Security Fix - Admin Self-Deletion Prevention

## Test Date
2025-11-07

## Security Fix Applied
- **Backend**: Added validation in `UserManagementService.deleteUser()` to prevent:
  1. Admin from deleting their own account
  2. Deletion of the last admin account
- **Frontend**: UI now disables delete button for current user's own account

## Test Cases

### Test Case 1: Backend - Admin tries to delete their own account
**Expected Result**: 400 Bad Request with error message "You cannot delete your own account"

**Steps**:
```bash
# 1. Login as admin
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin@pandamall.com","password":"admin123"}'

# Expected response: Get JWT token and userId

# 2. Try to delete own account (replace {userId} and {token})
curl -X DELETE http://localhost:8080/api/users/{userId} \
  -H "Authorization: Bearer {token}"

# Expected response: 400 Bad Request
# {"message": "You cannot delete your own account"}
```

### Test Case 2: Backend - Admin tries to delete last admin account
**Expected Result**: 400 Bad Request with error message "Cannot delete the last admin account"

**Steps**:
```bash
# 1. Ensure only 1 ADMIN exists in database
# 2. Login as different admin or STAFF account
# 3. Try to delete the last admin account

curl -X DELETE http://localhost:8080/api/users/{last_admin_userId} \
  -H "Authorization: Bearer {staff_token}"

# Expected response: 400 Bad Request
# {"message": "Cannot delete the last admin account"}
```

### Test Case 3: Backend - Admin successfully deletes other user
**Expected Result**: 204 No Content (Success)

**Steps**:
```bash
# 1. Login as admin
# 2. Delete a CUSTOMER or STAFF account (not admin, not self)

curl -X DELETE http://localhost:8080/api/users/{other_userId} \
  -H "Authorization: Bearer {admin_token}"

# Expected response: 204 No Content
```

### Test Case 4: Frontend - Delete button is disabled for current user
**Expected Result**: Delete button is gray and disabled with tooltip "Cannot delete yourself"

**Steps**:
1. Login to admin portal: http://localhost:5173/admin/login
2. Navigate to User Management page
3. Find your own account in the list
4. **Expected**: Delete button (trash icon) is gray/disabled with tooltip
5. Try clicking it - nothing should happen

### Test Case 5: Frontend - Delete button works for other users
**Expected Result**: Delete button is red and clickable

**Steps**:
1. Login to admin portal
2. Navigate to User Management page
3. Find any other user's account
4. **Expected**: Delete button is red and clickable
5. Click it - should show confirmation dialog

## Files Modified

### Backend
1. `Backend/src/main/java/com/aptech/aptechMall/repository/UserRepository.java`
   - Added: `long countByRole(Role role);`

2. `Backend/src/main/java/com/aptech/aptechMall/service/admin/UserManagementService.java`
   - Added: Import `AuthenticationUtil`
   - Modified: `deleteUser()` method with security checks

### Frontend
1. `Frontend/src/features/admin/pages/AdminUserManagementPage.jsx`
   - Added: Import `useAuth` from AuthContext
   - Modified: `handleDelete()` - added validation
   - Modified: Delete button UI - conditional rendering based on currentUser

## Test Results

### Backend Tests
- [ ] Test Case 1: Admin self-deletion blocked ✅
- [ ] Test Case 2: Last admin deletion blocked ✅
- [ ] Test Case 3: Other user deletion works ✅

### Frontend Tests
- [ ] Test Case 4: Delete button disabled for self ✅
- [ ] Test Case 5: Delete button enabled for others ✅

## Notes
- Backend uses `AuthenticationUtil.getCurrentUserId()` to get authenticated user's ID
- Frontend uses `useAuth()` hook to get `currentUser.userId`
- Both layers provide defense-in-depth security
