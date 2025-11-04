# YÊU CẦU SỬA LỖI BẢO MẬT VÀ CẢI THIỆN HỆ THỐNG

**Ngày tạo:** 2025-10-28
**Tác giả:** Claude Code Test Report
**Dự án:** AptechMall Spring Boot Application

---

## MỤC LỤC

1. [CRITICAL - Lỗi bảo mật nghiêm trọng](#1-critical---lỗi-bảo-mật-nghiêm-trọng)
2. [HIGH - Lỗi ảnh hưởng nghiêm trọng](#2-high---lỗi-ảnh-hưởng-nghiêm-trọng)
3. [MEDIUM - Cần cải thiện](#3-medium---cần-cải-thiện)
4. [LOW - Best practices](#4-low---best-practices)
5. [Checklist tổng hợp](#5-checklist-tổng-hợp)

---

## 1. CRITICAL - Lỗi bảo mật nghiêm trọng

### 1.1. LỖ HỔNG: Bất kỳ ai cũng có thể tự đăng ký với role ADMIN/STAFF

**Mức độ:** 🔴 CRITICAL
**File:** `src/main/java/com/aptech/aptechMall/service/authentication/AuthService.java`
**Dòng:** 128-149 (hàm `register()`)

#### Mô tả vấn đề:
Hàm register() hiện tại cho phép người dùng tự chọn role khi đăng ký. Attacker có thể tạo tài khoản ADMIN với một HTTP request đơn giản:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"hacker","password":"123","role":"ADMIN","email":"hack@evil.com"}'
```

#### Code hiện tại (SAI):
```java
// AuthService.java:128-149
public RegisterResponse register(RegisterRequest request) {
    if (userRepository.existsByUsername(request.getUsername())) {
        throw new UsernameAlreadyTaken("Username " +request.getUsername() + " already taken");
    }

    // ❌ NGUY HIỂM: Cho phép người dùng tự chọn role
    Role role = (request.getRole() == null || request.getRole().trim().isEmpty())
            ? Role.CUSTOMER
            : Role.fromString(request.getRole());

    User user = User.builder()
            .username(request.getUsername())
            .fullName(request.getFullName())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(role)
            .email(request.getEmail())
            .build();

    userRepository.save(user);
    return new RegisterResponse("Successfully registered the user " + user.getUsername());
}
```

#### Code đề xuất (ĐÚNG):
```java
// AuthService.java:128-149
public RegisterResponse register(RegisterRequest request) {
    if (userRepository.existsByUsername(request.getUsername())) {
        throw new UsernameAlreadyTaken("Username " +request.getUsername() + " already taken");
    }

    // ✅ BẢO MẬT: Public registration CHỈ cho phép role CUSTOMER
    Role role = Role.CUSTOMER;

    User user = User.builder()
            .username(request.getUsername())
            .fullName(request.getFullName())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(role)  // Luôn luôn là CUSTOMER
            .email(request.getEmail())
            .build();

    userRepository.save(user);
    return new RegisterResponse("Successfully registered the user " + user.getUsername());
}
```

#### Thêm endpoint mới cho ADMIN tạo user (TÙY CHỌN):
Nếu cần tạo ADMIN/STAFF, tạo endpoint riêng có authentication:

**File:** `src/main/java/com/aptech/aptechMall/Controller/UsersDataController.java`

```java
// Thêm vào UsersDataController.java
@PostMapping("/create")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<RegisterResponse> createUserWithRole(@RequestBody RegisterRequest request) {
    // Chỉ ADMIN mới có thể gọi endpoint này
    return ResponseEntity.ok(authService.registerWithRole(request));
}
```

**File:** `src/main/java/com/aptech/aptechMall/service/authentication/AuthService.java`

```java
// Thêm method mới trong AuthService.java
public RegisterResponse registerWithRole(RegisterRequest request) {
    if (userRepository.existsByUsername(request.getUsername())) {
        throw new UsernameAlreadyTaken("Username " +request.getUsername() + " already taken");
    }

    // Cho phép chỉ định role (chỉ được gọi bởi ADMIN)
    Role role = (request.getRole() == null || request.getRole().trim().isEmpty())
            ? Role.CUSTOMER
            : Role.fromString(request.getRole());

    User user = User.builder()
            .username(request.getUsername())
            .fullName(request.getFullName())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(role)
            .email(request.getEmail())
            .build();

    userRepository.save(user);
    return new RegisterResponse("Successfully registered the user " + user.getUsername() + " with role " + role);
}
```

---

### 1.2. REDIS là Single Point of Failure

**Mức độ:** 🔴 CRITICAL
**Files:** Multiple
**Impact:** Toàn bộ hệ thống không hoạt động khi Redis down

#### Mô tả vấn đề:
Khi test, Redis không chạy → Tất cả endpoints bị lỗi 500:
- `/api/auth/logout` → FAIL
- `/api/auth/refresh` → FAIL
- `/api/users/*` → FAIL (TokenBlacklistFilter gọi Redis)

```
RedisConnectionFailureException: Unable to connect to Redis
Connection refused: localhost/127.0.0.1:6379
```

#### Fix 1: Thêm Redis configuration

**File:** `src/main/resources/application.properties`

```properties
# Redis Configuration (THÊM DÒNG NÀY)
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.timeout=2000ms
spring.redis.lettuce.pool.max-active=8
spring.redis.lettuce.pool.max-idle=8
spring.redis.lettuce.pool.min-idle=0

# Redis connection retry
spring.redis.lettuce.shutdown-timeout=200ms
```

#### Fix 2: Add graceful error handling trong RedisService

**File:** `src/main/java/com/aptech/aptechMall/service/authentication/RedisService.java`

**Code hiện tại:**
```java
// Hiện tại RedisService không handle exception khi Redis down
public void setToken(String token, String value, long expirationTime, TimeUnit timeUnit) {
    redisTemplate.opsForValue().set(token, value, expirationTime, timeUnit);
}

public boolean hasToken(String token) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(token));
}
```

**Code đề xuất (thêm try-catch):**
```java
import lombok.extern.slf4j.Slf4j;

@Slf4j  // Thêm annotation này
@Service
@RequiredArgsConstructor
public class RedisService {
    private final RedisTemplate<String, String> redisTemplate;

    public void setToken(String token, String value, long expirationTime, TimeUnit timeUnit) {
        try {
            redisTemplate.opsForValue().set(token, value, expirationTime, timeUnit);
            log.info("Token blacklisted in Redis: {}", token.substring(0, 20) + "...");
        } catch (Exception e) {
            log.error("Failed to blacklist token in Redis: {}", e.getMessage());
            // QUAN TRỌNG: Không throw exception, cho phép request tiếp tục
            // Nhược điểm: Token không bị revoke khi Redis down
            // Cải thiện: Có thể lưu vào database backup
        }
    }

    public boolean hasToken(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(token));
        } catch (Exception e) {
            log.error("Failed to check token in Redis: {}", e.getMessage());
            // Trả về false để cho phép request tiếp tục
            // User experience > Security trong trường hợp này
            return false;
        }
    }
}
```

#### Fix 3: Update README.md để document Redis requirement

**File:** `README.md`

Thêm section:

```markdown
## Prerequisites

Before running the application, ensure you have the following installed:

1. **Java 17 or higher**
2. **MySQL 8.0+** running on `localhost:3306`
   - Database name: `test_db`
   - Username: `root`
   - Password: (empty or configure in application.properties)

3. **Redis 6.0+** running on `localhost:6379` ⚠️ **REQUIRED**
   - Used for JWT token blacklisting (logout functionality)
   - Without Redis, logout and refresh token features will not work properly

### Installing Redis:

**Windows:**
```bash
# Using Chocolatey
choco install redis-64

# Or download from: https://github.com/microsoftarchive/redis/releases
```

**Linux/Mac:**
```bash
# Ubuntu/Debian
sudo apt-get install redis-server

# Mac with Homebrew
brew install redis

# Start Redis
redis-server
```

**Verify Redis is running:**
```bash
redis-cli ping
# Should return: PONG
```
```

---

## 2. HIGH - Lỗi ảnh hưởng nghiêm trọng

### 2.1. Thiếu validate email trùng lặp

**Mức độ:** 🟠 HIGH
**File:** `src/main/java/com/aptech/aptechMall/service/authentication/AuthService.java`
**Dòng:** 128-149

#### Mô tả vấn đề:
Hiện tại chỉ check username trùng lặp, không check email. Khi email trùng, SQL constraint error lộ ra ngoài:

```json
{
  "error": "Duplicate entry 'testuser@example.com' for key 'users.UK6dotkott2kjsp8vw4d0m25fb7'"
}
```

#### Code hiện tại (THIẾU):
```java
public RegisterResponse register(RegisterRequest request) {
    if (userRepository.existsByUsername(request.getUsername())) {
        throw new UsernameAlreadyTaken("Username " +request.getUsername() + " already taken");
    }
    // ❌ THIẾU: Không check email trùng lặp

    Role role = Role.CUSTOMER;
    // ...
}
```

#### Code đề xuất:
```java
public RegisterResponse register(RegisterRequest request) {
    // ✅ Check username trùng lặp
    if (userRepository.existsByUsername(request.getUsername())) {
        throw new UsernameAlreadyTaken("Username " +request.getUsername() + " already taken");
    }

    // ✅ THÊM: Check email trùng lặp
    if (userRepository.existsByEmail(request.getEmail())) {
        throw new EmailAlreadyExistsException("Email " + request.getEmail() + " is already registered");
    }

    Role role = Role.CUSTOMER;
    // ...
}
```

#### Tạo Exception mới:

**File:** `src/main/java/com/aptech/aptechMall/Exception/EmailAlreadyExistsException.java`

```java
package com.aptech.aptechMall.Exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
```

#### Update GlobalExceptionHandler:

**File:** `src/main/java/com/aptech/aptechMall/Exception/GlobalExceptionHandler.java`

```java
@ExceptionHandler(EmailAlreadyExistsException.class)
public ResponseEntity<Map<String, Object>> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
    Map<String, Object> response = new HashMap<>();
    response.put("error", "Conflict");
    response.put("message", ex.getMessage());
    response.put("status", 409);
    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
}
```

---

### 2.2. User bị SUSPENDED/DELETED vẫn login được

**Mức độ:** 🟠 HIGH
**File:** `src/main/java/com/aptech/aptechMall/service/authentication/AuthService.java`
**Dòng:** 151-170

#### Mô tả vấn đề:
Hàm `authenticate()` không kiểm tra status của user. User có status SUSPENDED hoặc DELETED vẫn có thể login thành công và nhận JWT token.

#### Code hiện tại (THIẾU):
```java
public AuthResponse authenticate(AuthRequest request, HttpServletResponse response) {
    boolean existUsername = userRepository.existsByUsername(request.getUsername());
    User user = existUsername ?
            userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại")) :
            userRepository.findByEmail(request.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tồn tại"));

    if (!passwordEncoder.matches(request.getPassword(), user.getPassword()) || request.getPassword().isEmpty()) {
        throw new BadCredentialsException("Thông tin đăng nhập không hợp lệ");
    }

    // ❌ THIẾU: Không check user status

    String accessJwt = jwtService.generateToken(existUsername ? user.getUsername() : user.getEmail(), "access_token");
    // ...
}
```

#### Code đề xuất:
```java
public AuthResponse authenticate(AuthRequest request, HttpServletResponse response) {
    boolean existUsername = userRepository.existsByUsername(request.getUsername());
    User user = existUsername ?
            userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found")) :
            userRepository.findByEmail(request.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    if (!passwordEncoder.matches(request.getPassword(), user.getPassword()) || request.getPassword().isEmpty()) {
        throw new BadCredentialsException("Invalid credentials");
    }

    // ✅ THÊM: Kiểm tra user status
    if (user.getStatus() == Status.SUSPENDED) {
        throw new AccountSuspendedException("Your account has been suspended. Please contact support.");
    }

    if (user.getStatus() == Status.DELETED) {
        throw new AccountDeletedException("This account no longer exists");
    }

    if (user.getStatus() != Status.ACTIVE) {
        throw new AccountNotActiveException("Account is not active");
    }

    String accessJwt = jwtService.generateToken(existUsername ? user.getUsername() : user.getEmail(), "access_token");
    // ...
}
```

#### Tạo các Exception mới:

**File:** `src/main/java/com/aptech/aptechMall/Exception/AccountSuspendedException.java`

```java
package com.aptech.aptechMall.Exception;

public class AccountSuspendedException extends RuntimeException {
    public AccountSuspendedException(String message) {
        super(message);
    }
}
```

**File:** `src/main/java/com/aptech/aptechMall/Exception/AccountDeletedException.java`

```java
package com.aptech.aptechMall.Exception;

public class AccountDeletedException extends RuntimeException {
    public AccountDeletedException(String message) {
        super(message);
    }
}
```

**File:** `src/main/java/com/aptech/aptechMall/Exception/AccountNotActiveException.java`

```java
package com.aptech.aptechMall.Exception;

public class AccountNotActiveException extends RuntimeException {
    public AccountNotActiveException(String message) {
        super(message);
    }
}
```

#### Update GlobalExceptionHandler:

**File:** `src/main/java/com/aptech/aptechMall/Exception/GlobalExceptionHandler.java`

```java
@ExceptionHandler(AccountSuspendedException.class)
public ResponseEntity<Map<String, Object>> handleAccountSuspended(AccountSuspendedException ex) {
    Map<String, Object> response = new HashMap<>();
    response.put("error", "Account Suspended");
    response.put("message", ex.getMessage());
    response.put("status", 403);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
}

@ExceptionHandler(AccountDeletedException.class)
public ResponseEntity<Map<String, Object>> handleAccountDeleted(AccountDeletedException ex) {
    Map<String, Object> response = new HashMap<>();
    response.put("error", "Account Not Found");
    response.put("message", ex.getMessage());
    response.put("status", 404);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
}

@ExceptionHandler(AccountNotActiveException.class)
public ResponseEntity<Map<String, Object>> handleAccountNotActive(AccountNotActiveException ex) {
    Map<String, Object> response = new HashMap<>();
    response.put("error", "Account Not Active");
    response.put("message", ex.getMessage());
    response.put("status", 403);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
}
```

---

### 2.3. @PreAuthorize không hoạt động do thiếu @EnableMethodSecurity

**Mức độ:** 🟠 HIGH
**File:** `src/main/java/com/aptech/aptechMall/config/SecurityConfig.java`
**Dòng:** 24-27

#### Mô tả vấn đề:
`@PreAuthorize` annotations trong UsersDataController không có tác dụng vì SecurityConfig thiếu `@EnableMethodSecurity`.

Ví dụ: Dòng này không hoạt động:
```java
// UsersDataController.java:56
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
```

#### Code hiện tại (THIẾU):
```java
// SecurityConfig.java:24-27
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    // ❌ THIẾU: @EnableMethodSecurity
```

#### Code đề xuất:
```java
// SecurityConfig.java:24-28
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // ✅ THÊM annotation này
@RequiredArgsConstructor
public class SecurityConfig {
```

**LƯU Ý:** Sau khi thêm `@EnableMethodSecurity`, các annotation `@PreAuthorize` sẽ hoạt động:
- `UsersDataController.java:46` - `@PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")`
- `UsersDataController.java:56` - `@PreAuthorize("hasRole('ADMIN')")`

---

## 3. MEDIUM - Cần cải thiện

### 3.1. HTTP Status Codes không chuẩn RESTful

**Mức độ:** 🟡 MEDIUM
**File:** `src/main/java/com/aptech/aptechMall/Exception/GlobalExceptionHandler.java`

#### Mô tả vấn đề:
Các exception trả về HTTP 500 (Internal Server Error) thay vì status code phù hợp:
- Username trùng lặp → 500 (nên là 409 Conflict)
- Sai password → 500 (nên là 401 Unauthorized)
- User không tồn tại → 500 (nên là 404 Not Found)

#### Code đề xuất:

**File:** `src/main/java/com/aptech/aptechMall/Exception/GlobalExceptionHandler.java`

```java
package com.aptech.aptechMall.Exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ===== Authentication & Authorization Exceptions =====

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Unauthorized");
        response.put("message", "Invalid username or password");
        response.put("status", 401);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUsernameNotFound(UsernameNotFoundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Not Found");
        response.put("message", "User not found");
        response.put("status", 404);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(UsernameAlreadyTaken.class)
    public ResponseEntity<Map<String, Object>> handleUsernameAlreadyTaken(UsernameAlreadyTaken ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Conflict");
        response.put("message", ex.getMessage());
        response.put("status", 409);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // ===== Database Exceptions =====

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        Map<String, Object> response = new HashMap<>();
        String message = "Data integrity violation";

        // Parse message để tạo user-friendly error
        if (ex.getMessage().contains("Duplicate entry")) {
            if (ex.getMessage().contains("email")) {
                message = "Email address is already registered";
            } else if (ex.getMessage().contains("username")) {
                message = "Username is already taken";
            } else {
                message = "This record already exists";
            }
        }

        response.put("error", "Conflict");
        response.put("message", message);
        response.put("status", 409);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // ===== Cart & Order Exceptions =====

    @ExceptionHandler(CartNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCartNotFound(CartNotFoundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Not Found");
        response.put("message", ex.getMessage());
        response.put("status", 404);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(CartItemNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCartItemNotFound(CartItemNotFoundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Not Found");
        response.put("message", ex.getMessage());
        response.put("status", 404);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(EmptyCartException.class)
    public ResponseEntity<Map<String, Object>> handleEmptyCart(EmptyCartException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Bad Request");
        response.put("message", ex.getMessage());
        response.put("status", 400);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleOrderNotFound(OrderNotFoundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Not Found");
        response.put("message", ex.getMessage());
        response.put("status", 404);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(OrderNotCancellableException.class)
    public ResponseEntity<Map<String, Object>> handleOrderNotCancellable(OrderNotCancellableException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Bad Request");
        response.put("message", ex.getMessage());
        response.put("status", 400);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Not Found");
        response.put("message", ex.getMessage());
        response.put("status", 404);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // ===== Generic Exception Handler =====

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Internal Server Error");
        response.put("message", "An unexpected error occurred");
        response.put("status", 500);

        // Log full stack trace for debugging
        ex.printStackTrace();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
```

---

### 3.2. Error messages bằng tiếng Việt

**Mức độ:** 🟡 MEDIUM
**File:** `src/main/java/com/aptech/aptechMall/service/authentication/AuthService.java`
**Dòng:** 155, 160

#### Mô tả vấn đề:
Error messages trong code bằng tiếng Việt, khó maintain khi cần internationalization (i18n).

#### Code hiện tại:
```java
// AuthService.java:155
throw new UsernameNotFoundException("Người dùng không tồn tại");

// AuthService.java:160
throw new BadCredentialsException("Thông tin đăng nhập không hợp lệ");
```

#### Code đề xuất:
```java
// AuthService.java:155
throw new UsernameNotFoundException("User not found");

// AuthService.java:160
throw new BadCredentialsException("Invalid credentials");
```

**Áp dụng cho tất cả error messages trong các file:**
- `AuthService.java`
- `CartService.java`
- `OrderService.java`
- Custom Exception classes

---

### 3.3. ExpiredJwtException handling không đầy đủ

**Mức độ:** 🟡 MEDIUM
**File:** `src/main/java/com/aptech/aptechMall/security/filters/JwtAuthenticationFilter.java`
**Dòng:** 64-66

#### Mô tả vấn đề:
Khi JWT expired, filter chỉ set status 401 mà không trả về response body. Frontend không biết lý do cụ thể (expired vs invalid).

#### Code hiện tại:
```java
// JwtAuthenticationFilter.java:64-66
} catch (ExpiredJwtException ex){
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
    // ❌ Không có response body
}
```

#### Code đề xuất:
```java
} catch (ExpiredJwtException ex){
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    String jsonResponse = "{\"error\":\"Token Expired\",\"message\":\"JWT token has expired. Please refresh your token.\",\"status\":401}";
    response.getWriter().write(jsonResponse);
    return;  // Không gọi filterChain.doFilter()
}
```

---

## 4. LOW - Best practices

### 4.1. Thiếu input validation cho RegisterRequest

**Mức độ:** 🟢 LOW
**File:** `src/main/java/com/aptech/aptechMall/security/requests/RegisterRequest.java`

#### Mô tả vấn đề:
RegisterRequest không có validation annotations. Không validate:
- Email format
- Password strength (độ dài tối thiểu)
- Username format/length

#### Code đề xuất:

**File:** `src/main/java/com/aptech/aptechMall/security/requests/RegisterRequest.java`

```java
package com.aptech.aptechMall.security.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    // Tùy chọn: Thêm pattern cho password mạnh hơn
    // @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", message = "Password must contain uppercase, lowercase, and number")
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 191, message = "Full name must be between 2 and 191 characters")
    private String fullName;

    // Role không cần validate vì sẽ bị ignore trong public registration
    private String role;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 30, message = "Email must not exceed 30 characters")
    private String email;
}
```

#### Update Controller để enable validation:

**File:** `src/main/java/com/aptech/aptechMall/Controller/LoginController.java`

```java
import jakarta.validation.Valid;  // THÊM import

@PostMapping("/register")
public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
    // @Valid annotation sẽ trigger validation
    return ResponseEntity.ok(authService.register(request));
}
```

#### Update pom.xml (nếu chưa có):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

#### Thêm exception handler cho validation errors:

**File:** `src/main/java/com/aptech/aptechMall/Exception/GlobalExceptionHandler.java`

```java
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.FieldError;

@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
    Map<String, Object> response = new HashMap<>();
    Map<String, String> errors = new HashMap<>();

    ex.getBindingResult().getAllErrors().forEach((error) -> {
        String fieldName = ((FieldError) error).getField();
        String errorMessage = error.getDefaultMessage();
        errors.put(fieldName, errorMessage);
    });

    response.put("error", "Validation Failed");
    response.put("message", "Invalid input data");
    response.put("errors", errors);
    response.put("status", 400);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
}
```

---

### 4.2. Inconsistent authorization approach

**Mức độ:** 🟢 LOW
**Files:** `SecurityConfig.java`, `UsersDataController.java`

#### Mô tả vấn đề:
Có hai cách config authorization:
1. SecurityConfig matcher: `.requestMatchers("/api/users/**").hasAnyRole("ADMIN", "STAFF")`
2. Method annotation: `@PreAuthorize("hasRole('ADMIN')")`

Điều này gây confusion và khó maintain.

#### Recommendation:

**Approach 1: Chỉ dùng SecurityConfig (Đơn giản hơn)**

Remove tất cả `@PreAuthorize` trong controllers, config tất cả trong SecurityConfig:

```java
// SecurityConfig.java
@Bean
@Order(1)
public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
    return http
            .securityMatcher("/api/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    // Public endpoints
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/api/debug/**").permitAll()
                    .requestMatchers("/api/aliexpress/**").permitAll()
                    .requestMatchers("/api/products/**").permitAll()

                    // User management - ADMIN and STAFF can view/edit
                    .requestMatchers(HttpMethod.GET, "/api/users/**").hasAnyRole("ADMIN", "STAFF")
                    .requestMatchers(HttpMethod.PUT, "/api/users/**").hasAnyRole("ADMIN", "STAFF")
                    .requestMatchers(HttpMethod.PATCH, "/api/users/**").hasAnyRole("ADMIN", "STAFF")

                    // User management - Only ADMIN can delete
                    .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")

                    // All other API endpoints require authentication
                    .anyRequest().authenticated()
            )
            .logout(AbstractHttpConfigurer::disable)
            .addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(tokenBlacklistFilter, UsernamePasswordAuthenticationFilter.class)
            .userDetailsService(userDetailsService)
            .build();
}
```

**Approach 2: Chỉ dùng @PreAuthorize (Linh hoạt hơn)**

Remove authorization logic từ SecurityConfig, chỉ config trong controllers:

```java
// SecurityConfig.java - Đơn giản hóa
.authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/auth/**", "/api/debug/**", "/api/aliexpress/**", "/api/products/**").permitAll()
        .anyRequest().authenticated()  // Tất cả còn lại check ở controller level
)

// UsersDataController.java - Rõ ràng hơn
@GetMapping("/")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public ResponseEntity<List<UserResponseDTO>> getAllUsers() { ... }

@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Void> deleteUser(@PathVariable Long id) { ... }
```

**Khuyến nghị:** Approach 1 (SecurityConfig) đơn giản hơn cho project nhỏ/vừa.

---

### 4.3. Thêm Health Check endpoints

**Mức độ:** 🟢 LOW (Nhưng rất hữu ích cho monitoring)

#### Tạo HealthCheckController mới:

**File:** `src/main/java/com/aptech/aptechMall/Controller/HealthCheckController.java`

```java
package com.aptech.aptechMall.Controller;

import com.aptech.aptechMall.service.authentication.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthCheckController {

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;

    @GetMapping("")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("application", "AptechMall");

        return ResponseEntity.ok(health);
    }

    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealthCheck() {
        Map<String, Object> health = new HashMap<>();
        Map<String, String> services = new HashMap<>();

        // Check MySQL
        try (Connection conn = dataSource.getConnection()) {
            services.put("mysql", conn.isValid(2) ? "UP" : "DOWN");
        } catch (Exception e) {
            services.put("mysql", "DOWN");
        }

        // Check Redis
        try {
            redisConnectionFactory.getConnection().ping();
            services.put("redis", "UP");
        } catch (Exception e) {
            services.put("redis", "DOWN");
        }

        health.put("services", services);

        // Overall status
        boolean allUp = services.values().stream().allMatch(s -> s.equals("UP"));
        health.put("status", allUp ? "UP" : "DEGRADED");

        return ResponseEntity.ok(health);
    }
}
```

#### Update SecurityConfig để allow health endpoints:

```java
.authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/auth/**", "/api/debug/**", "/api/health/**").permitAll()
        // ...
)
```

---

## 5. CHECKLIST TỔNG HỢP

### Phase 1 - Security Critical (Làm ngay - Tuần này)

- [ ] **1.1** Fix registration role vulnerability - chỉ cho phép CUSTOMER
- [ ] **1.2** Add Redis configuration và error handling
- [ ] **2.1** Add email duplicate check trong register()
- [ ] **2.2** Add user status check trong authenticate()
- [ ] **2.3** Add @EnableMethodSecurity trong SecurityConfig
- [ ] **2.1** Tạo EmailAlreadyExistsException
- [ ] **2.2** Tạo AccountSuspendedException, AccountDeletedException, AccountNotActiveException

**Estimated time:** 2-3 hours

---

### Phase 2 - Error Handling (Tuần này)

- [ ] **3.1** Rewrite GlobalExceptionHandler với proper HTTP status codes
- [ ] **3.2** Đổi tất cả error messages sang tiếng Anh
- [ ] **3.3** Improve ExpiredJwtException handling trong JwtAuthenticationFilter
- [ ] Test lại tất cả error scenarios

**Estimated time:** 1-2 hours

---

### Phase 3 - Validation & Best Practices (Tuần sau)

- [ ] **4.1** Add validation annotations cho RegisterRequest
- [ ] **4.1** Add validation exception handler
- [ ] **4.1** Add spring-boot-starter-validation dependency (nếu chưa có)
- [ ] **4.2** Chọn một authorization approach và refactor
- [ ] **4.3** Add HealthCheckController
- [ ] Update README.md với Redis prerequisites
- [ ] Test toàn bộ hệ thống

**Estimated time:** 2-3 hours

---

### Phase 4 - Testing & Documentation (Optional - Khi có thời gian)

- [ ] Viết unit tests cho AuthService
- [ ] Viết integration tests cho authentication flow
- [ ] Add API documentation (Swagger/OpenAPI)
- [ ] Add logging cho security events
- [ ] Consider adding rate limiting cho login endpoint
- [ ] Consider adding account lockout after failed attempts
- [ ] Consider adding email verification flow

---

## 6. TESTING CHECKLIST

Sau khi fix xong, test các scenarios sau:

### Registration:
- [ ] Đăng ký user mới thành công → 200
- [ ] Đăng ký với username trùng → 409
- [ ] Đăng ký với email trùng → 409
- [ ] Đăng ký với role ADMIN trong request body → User vẫn là CUSTOMER
- [ ] Đăng ký với email invalid → 400
- [ ] Đăng ký với password ngắn → 400

### Login:
- [ ] Login với username hợp lệ → 200 + JWT token
- [ ] Login với email hợp lệ → 200 + JWT token
- [ ] Login với password sai → 401
- [ ] Login với user không tồn tại → 404
- [ ] Login với user SUSPENDED → 403
- [ ] Login với user DELETED → 404

### Authorization:
- [ ] CUSTOMER truy cập /api/users/ → 403
- [ ] STAFF truy cập /api/users/ → 200
- [ ] ADMIN truy cập /api/users/ → 200
- [ ] STAFF delete user → 403
- [ ] ADMIN delete user → 204

### Redis:
- [ ] Logout với Redis running → 200
- [ ] Logout với Redis down → 200 (graceful degradation)
- [ ] Sử dụng blacklisted token → 401

### Health:
- [ ] /api/health → 200 + status UP
- [ ] /api/health/detailed → 200 + all services status

---

## 7. ADDITIONAL RECOMMENDATIONS

### 7.1. Security Enhancements (Future)

1. **Rate Limiting:** Add rate limiting cho login endpoint để prevent brute force attacks
```java
// Using Bucket4j or Spring Rate Limiter
@RateLimiter(name = "loginLimiter")
@PostMapping("/login")
```

2. **Account Lockout:** Lock account sau N lần login failed
```java
// Track failed attempts in database
// Lock account for X minutes after 5 failed attempts
```

3. **Email Verification:** Require email verification khi đăng ký
```java
// Send verification email with token
// User status = PENDING until verified
```

4. **Password Reset:** Add forgot password flow
```java
// POST /api/auth/forgot-password
// Email reset token
// POST /api/auth/reset-password
```

### 7.2. Monitoring & Logging

1. **Security Event Logging:**
```java
@Slf4j
public class AuthService {
    public AuthResponse authenticate(...) {
        // ...
        log.info("User {} logged in successfully from IP {}",
                 user.getUsername(), request.getRemoteAddr());
        // ...
    }
}
```

2. **Failed Login Tracking:**
```java
log.warn("Failed login attempt for user {} from IP {}",
         username, request.getRemoteAddr());
```

### 7.3. Documentation

1. **API Documentation với Swagger:**
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.2.0</version>
</dependency>
```

2. **Update README.md với:**
- Setup instructions (MySQL, Redis)
- API endpoints documentation
- Authentication flow diagram
- Common error codes

---

## 8. CONTACT & SUPPORT

Nếu gặp vấn đề khi implement các fixes trên:

1. Check logs trong console application
2. Verify MySQL và Redis đang chạy
3. Test từng fix riêng lẻ trước khi combine
4. Use Postman/curl để test API endpoints

**Good luck! 🚀**

---

**Document Version:** 1.0
**Last Updated:** 2025-10-28
**Next Review:** After Phase 1 completion
