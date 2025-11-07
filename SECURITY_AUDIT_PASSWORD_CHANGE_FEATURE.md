# Security Audit: Password Change Feature

**Date:** 2025-11-07
**Severity:** 🔴 CRITICAL
**Status:** ⚠️ BROKEN - Feature does NOT work and has security issues

---

## Executive Summary

The password change feature (`/api/auth/update-credentials`) has **CRITICAL security vulnerabilities** and **implementation bugs** that prevent it from working correctly. The feature is currently **BROKEN** and should be disabled or fixed immediately.

### Issues Found

| # | Issue | Severity | Impact |
|---|-------|----------|--------|
| 1 | User identified by client-provided email (not JWT) | 🔴 CRITICAL | Security bypass - can change other users' passwords |
| 2 | Frontend/Backend field name mismatch | 🔴 CRITICAL | Feature broken - NullPointerException |
| 3 | Missing null checks | 🟠 HIGH | Application crash |
| 4 | Poor error handling | 🟡 MEDIUM | Silent failures, no user feedback |
| 5 | Always sets password (even for email-only change) | 🟡 MEDIUM | Data corruption risk |

---

## Issue #1: Security Vulnerability - User ID from Client

### 🔴 CRITICAL SECURITY ISSUE

**File:** `Backend/src/main/java/com/aptech/aptechMall/service/authentication/AuthService.java:308`

**Problem:**
Backend identifies the user to modify by accepting `oldEmail` from the client request body instead of extracting the user from the JWT token.

**Vulnerable Code:**
```java
public void updateEmailOrPassword(HttpServletRequest request, HttpServletResponse response, UpdateCredential credential){
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        throw new IllegalArgumentException("Missing or invalid Authorization header");
    }
    try {
        // ❌ SECURITY ISSUE: Gets user from CLIENT-PROVIDED email
        User user = userRepository.findByEmail(credential.getOldEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Email not in Database"));

        // ❌ Only validates password, doesn't verify ownership
        if (!passwordEncoder.matches(credential.getOldPassword(), user.getPassword())){
            throw new BadCredentialsException("Old Password does not match");
        }

        // Changes password for user found by client email
        user.setPassword(passwordEncoder.encode(credential.getPassword()));
        // ...
    }
}
```

**Attack Scenario:**
```
Attacker knows:
- Victim's email: victim@example.com
- Victim's current password: oldpass123

Attack:
1. Attacker logs in with their own account
2. Attacker sends request to /api/auth/update-credentials:
   {
     "oldEmail": "victim@example.com",    // ❌ Victim's email
     "email": "victim@example.com",
     "oldPassword": "oldpass123",         // ❌ Victim's password
     "password": "hackedpass999"          // ✅ Attacker's new password
   }
3. Backend finds victim by email (not from JWT)
4. Backend validates victim's old password ✅
5. Backend sets victim's password to "hackedpass999" ✅
6. Attack successful! Victim's account compromised!
```

**Correct Implementation:**
```java
public void updateEmailOrPassword(HttpServletRequest request, HttpServletResponse response, UpdateCredential credential){
    // ✅ CORRECT: Get user from JWT token
    Long currentUserId = AuthenticationUtil.getCurrentUserId();
    User user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new RuntimeException("User not found"));

    // ✅ Verify current password
    if (!passwordEncoder.matches(credential.getCurrentPassword(), user.getPassword())){
        throw new BadCredentialsException("Current password is incorrect");
    }

    // ✅ Update password
    if (credential.getNewPassword() != null && !credential.getNewPassword().isEmpty()) {
        user.setPassword(passwordEncoder.encode(credential.getNewPassword()));
    }

    // ✅ Update email (if provided)
    if (credential.getEmail() != null && !credential.getEmail().equals(user.getEmail())) {
        user.setEmail(credential.getEmail());
        // Regenerate tokens...
    }

    userRepository.save(user);
}
```

---

## Issue #2: Frontend/Backend Field Mismatch

### 🔴 CRITICAL BUG - Feature Broken

**Problem:**
Frontend and backend use different field names for the request DTO, causing `NullPointerException`.

### Backend DTO
**File:** `Backend/src/main/java/com/aptech/aptechMall/security/requests/UpdateCredential.java`

```java
@Data
public class UpdateCredential {
    private String oldEmail;      // ❌ Required by backend
    private String email;
    private String oldPassword;   // ❌ Required by backend
    private String password;
}
```

### Frontend - Change Password Modal
**File:** `Frontend/src/features/user/components/ChangePasswordModal.jsx:71-74`

```javascript
const response = await updateCredentials({
  currentPassword: formData.currentPassword,  // ❌ Should be "oldPassword"
  newPassword: formData.newPassword,          // ❌ Should be "password"
  // ❌ MISSING: oldEmail
  // ❌ MISSING: email
});
```

**Result:**
- Backend receives: `{ currentPassword: "...", newPassword: "..." }`
- Backend expects: `{ oldEmail: "...", email: "...", oldPassword: "...", password: "..." }`
- Line 308: `credential.getOldEmail()` → **null**
- Line 308: `findByEmail(null)` → **NullPointerException!**
- Feature crashes! ❌

### Frontend - Change Email Modal
**File:** `Frontend/src/features/user/components/ChangeEmailModal.jsx:67-70`

```javascript
const response = await updateCredentials({
  email: formData.newEmail,                   // ✅ Matches backend
  currentPassword: formData.currentPassword,  // ❌ Should be "oldPassword"
  // ❌ MISSING: oldEmail
  // ❌ MISSING: password
});
```

**Result:**
- Backend receives: `{ email: "...", currentPassword: "..." }`
- Backend expects: `{ oldEmail: "...", email: "...", oldPassword: "...", password: "..." }`
- Line 308: `credential.getOldEmail()` → **null**
- Line 310: `credential.getOldPassword()` → **null**
- Line 313: `credential.getPassword()` → **null**
- Feature crashes! ❌

---

## Issue #3: Missing Null Checks

### 🟠 HIGH - Application Crash

**File:** `Backend/src/main/java/com/aptech/aptechMall/service/authentication/AuthService.java:308-315`

**Problems:**

1. **No null check for oldEmail:**
   ```java
   // Line 308
   User user = userRepository.findByEmail(credential.getOldEmail())  // ❌ Can be null
   ```

2. **No null check for oldPassword:**
   ```java
   // Line 310
   if (!passwordEncoder.matches(credential.getOldPassword(), user.getPassword()))  // ❌ Can be null
   ```

3. **Always sets password (even if null):**
   ```java
   // Line 313
   user.setPassword(passwordEncoder.encode(credential.getPassword()));  // ❌ Can be null
   ```

4. **No null check for email:**
   ```java
   // Line 315
   if(!credential.getEmail().equals(credential.getOldEmail()))  // ❌ Both can be null
   ```

**Result:** Multiple potential `NullPointerException` crashes.

---

## Issue #4: Poor Error Handling

### 🟡 MEDIUM - Silent Failures

**File:** `Backend/src/main/java/com/aptech/aptechMall/service/authentication/AuthService.java:327-329`

```java
} catch (Exception e) {
    System.err.println("Error extracting subject from JWT: " + e.getMessage());
    // ❌ No exception thrown - silent failure!
    // ❌ User sees "success" but nothing changed
}
```

**Problems:**
- Catches ALL exceptions (too broad)
- Only logs to console
- Does not throw exception back to controller
- Controller returns `ResponseEntity.ok("Credentials Updated")` even if it failed!
- User sees "success" message but password wasn't changed

**Correct Approach:**
```java
} catch (UsernameNotFoundException e) {
    throw new RuntimeException("User not found", e);
} catch (BadCredentialsException e) {
    throw new RuntimeException("Current password is incorrect", e);
} catch (Exception e) {
    log.error("Unexpected error updating credentials", e);
    throw new RuntimeException("Failed to update credentials", e);
}
```

---

## Issue #5: Always Sets Password

### 🟡 MEDIUM - Data Corruption Risk

**Problem:**
Backend always sets password (line 313), even when user only wants to change email.

**Code:**
```java
// Line 313 - ALWAYS executed
user.setPassword(passwordEncoder.encode(credential.getPassword()));

// Line 315 - THEN checks if email changed
if(!credential.getEmail().equals(credential.getOldEmail())){
    user.setEmail(credential.getEmail());
    // ...
}
```

**Issues:**
1. If user only changes email → password gets set to null/empty (corrupts account)
2. No conditional logic to skip password update
3. Should check if `password` field is provided before updating

**Correct Logic:**
```java
// Update password only if new password provided
if (credential.getPassword() != null && !credential.getPassword().isEmpty()) {
    user.setPassword(passwordEncoder.encode(credential.getPassword()));
}

// Update email only if new email provided and different
if (credential.getEmail() != null && !credential.getEmail().equals(user.getEmail())) {
    user.setEmail(credential.getEmail());
    // Regenerate tokens...
}
```

---

## Additional Observations

### JWT Token Not Used
**File:** `Backend/src/main/java/com/aptech/aptechMall/service/authentication/AuthService.java:303-306`

```java
String authHeader = request.getHeader("Authorization");
if (authHeader == null || !authHeader.startsWith("Bearer ")) {
    throw new IllegalArgumentException("Missing or invalid Authorization header");
}
// ❌ Gets Authorization header but NEVER USES IT!
// ❌ Should extract user from JWT token
```

The code checks for JWT token but never extracts the user from it. This is suspicious and suggests the implementation is incomplete or incorrect.

### DTO Design Issue
**File:** `Backend/src/main/java/com/aptech/aptechMall/security/requests/UpdateCredential.java`

The DTO combines two separate operations (change email + change password) into one. This creates confusion and makes validation complex.

**Better Design:**
```java
// Separate DTOs for separate operations
public class ChangePasswordRequest {
    private String currentPassword;  // Required
    private String newPassword;      // Required
}

public class ChangeEmailRequest {
    private String currentPassword;  // Required for verification
    private String newEmail;         // Required
}
```

---

## Security Best Practices Violated

1. ❌ **User identification from client input** (should be from JWT)
2. ❌ **No ownership verification** (anyone can change anyone's password if they know it)
3. ❌ **Sensitive operation without re-authentication** (should require recent login)
4. ❌ **Silent failures** (errors not propagated to user)
5. ❌ **Insufficient input validation** (no null checks)
6. ❌ **Incomplete token usage** (JWT checked but not used)

### Comparison with Secure Features

**Cart/Order Services (CORRECT):**
```java
// ✅ Get user from JWT token
Long userId = AuthenticationUtil.getCurrentUserId();
Cart cart = cartService.getCart(userId);
```

**This Feature (WRONG):**
```java
// ❌ Get user from client request
User user = userRepository.findByEmail(credential.getOldEmail());
```

---

## Recommended Fixes

### Priority 1: Security Fix (URGENT)

**File:** `Backend/src/main/java/com/aptech/aptechMall/service/authentication/AuthService.java`

```java
public void updateEmailOrPassword(HttpServletRequest request, HttpServletResponse response, UpdateCredential credential){
    // ✅ Get authenticated user from JWT token
    Long currentUserId = AuthenticationUtil.getCurrentUserId();
    User user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new RuntimeException("User not found"));

    // ✅ Validate current password
    if (credential.getCurrentPassword() == null || credential.getCurrentPassword().isEmpty()) {
        throw new IllegalArgumentException("Current password is required");
    }
    if (!passwordEncoder.matches(credential.getCurrentPassword(), user.getPassword())){
        throw new BadCredentialsException("Current password is incorrect");
    }

    // ✅ Update password (if provided)
    if (credential.getNewPassword() != null && !credential.getNewPassword().isEmpty()) {
        if (credential.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }
        user.setPassword(passwordEncoder.encode(credential.getNewPassword()));
    }

    // ✅ Update email (if provided and different)
    if (credential.getNewEmail() != null && !credential.getNewEmail().equals(user.getEmail())) {
        // Check if new email already exists
        if (userRepository.existsByEmail(credential.getNewEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        user.setEmail(credential.getNewEmail());

        // Regenerate tokens with new email
        boolean existUsername = userRepository.existsByUsername(user.getUsername());
        String refreshJwt = jwtService.generateToken(
            existUsername ? user.getUsername() : user.getEmail(),
            "refresh_token"
        );

        Cookie refreshTokenCookie = getRefreshTokenCookie(refreshJwt);
        setCookieAttribute(response, refreshTokenCookie);

        // Revoke old token
        var tokenCookie = Arrays.stream(request.getCookies())
            .filter(cookie -> cookie.getName().equals("refresh_token"))
            .findFirst()
            .orElse(null);
        if (tokenCookie != null) {
            revokeToken(tokenCookie.getValue());
        }
    }

    userRepository.save(user);
}
```

### Priority 2: Update DTO

**File:** `Backend/src/main/java/com/aptech/aptechMall/security/requests/UpdateCredential.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCredential {
    private String currentPassword;  // Required for verification
    private String newPassword;      // Optional - for password change
    private String newEmail;         // Optional - for email change
}
```

### Priority 3: Update Frontend

**ChangePasswordModal:**
```javascript
const response = await updateCredentials({
  currentPassword: formData.currentPassword,  // ✅ Matches new DTO
  newPassword: formData.newPassword,          // ✅ Matches new DTO
});
```

**ChangeEmailModal:**
```javascript
const response = await updateCredentials({
  currentPassword: formData.currentPassword,  // ✅ Matches new DTO
  newEmail: formData.newEmail,                // ✅ Matches new DTO
});
```

---

## Testing Plan

### Test Case 1: Change Password (Normal)
```bash
POST /api/auth/update-credentials
Authorization: Bearer USER_TOKEN
{
  "currentPassword": "oldpass123",
  "newPassword": "newpass456"
}

Expected: 200 OK, password updated
```

### Test Case 2: Change Password (Wrong Current Password)
```bash
POST /api/auth/update-credentials
Authorization: Bearer USER_TOKEN
{
  "currentPassword": "wrongpass",
  "newPassword": "newpass456"
}

Expected: 400 Bad Request, "Current password is incorrect"
```

### Test Case 3: Change Email
```bash
POST /api/auth/update-credentials
Authorization: Bearer USER_TOKEN
{
  "currentPassword": "oldpass123",
  "newEmail": "newemail@example.com"
}

Expected: 200 OK, email updated, new tokens issued
```

### Test Case 4: Security - Cannot Change Other User's Password
```bash
# Login as user1
POST /api/auth/login
{ "email": "user1@example.com", "password": "pass123" }

# Try to change user2's password
POST /api/auth/update-credentials
Authorization: Bearer USER1_TOKEN
{
  "currentPassword": "user2_oldpass",
  "newPassword": "hacked"
}

Expected: 400 Bad Request, "Current password is incorrect"
(Because backend uses user1 from JWT, not user2 from request)
```

---

## Files Affected

### Backend (3 files)
1. `Backend/src/main/java/com/aptech/aptechMall/service/authentication/AuthService.java:302-331`
   - Fix `updateEmailOrPassword()` method

2. `Backend/src/main/java/com/aptech/aptechMall/security/requests/UpdateCredential.java`
   - Update DTO fields

3. `Backend/src/main/java/com/aptech/aptechMall/Controller/LoginController.java:83-87`
   - Update endpoint documentation

### Frontend (3 files)
4. `Frontend/src/features/user/components/ChangePasswordModal.jsx:71-74`
   - Fix request payload

5. `Frontend/src/features/user/components/ChangeEmailModal.jsx:67-70`
   - Fix request payload

6. `Frontend/src/features/auth/services/authApi.js:184-192`
   - Update JSDoc comments

---

## Impact Assessment

### Current State
- ❌ **Feature is BROKEN** - NullPointerException on every request
- ❌ **Security vulnerability** - Can change other users' passwords
- ❌ **No user feedback** - Silent failures
- ❌ **Data corruption risk** - Can null out passwords

### After Fix
- ✅ **Feature works correctly**
- ✅ **Secure** - Uses JWT token for user identification
- ✅ **Proper error messages** - Users get clear feedback
- ✅ **Safe** - Proper validation and null checks

---

## Conclusion

The password change feature has **CRITICAL security vulnerabilities** and is currently **non-functional** due to implementation bugs.

**Immediate Action Required:**
1. 🔴 **URGENT:** Fix security issue (use JWT token, not client email)
2. 🔴 **URGENT:** Fix field name mismatch (frontend/backend)
3. 🟠 **HIGH:** Add proper null checks and validation
4. 🟡 **MEDIUM:** Improve error handling
5. 🟡 **MEDIUM:** Separate password/email change logic

**Estimated Fix Time:** 2-3 hours

---

**Audited by:** Claude Code
**Date:** 2025-11-07
**Severity:** 🔴 CRITICAL
**Status:** ⚠️ REQUIRES IMMEDIATE FIX
