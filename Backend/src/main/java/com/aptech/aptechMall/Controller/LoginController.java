package com.aptech.aptechMall.Controller;

import com.aptech.aptechMall.security.requests.*;
import com.aptech.aptechMall.service.authentication.AuthService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller xử lý xác thực và quản lý tài khoản người dùng
 *
 * Endpoint base: /api/auth
 * Tất cả endpoints trong controller này đều PUBLIC (không cần JWT token)
 *
 * Chức năng chính:
 * - Đăng ký tài khoản mới
 * - Đăng nhập (username/email + password hoặc Google OAuth)
 * - Đăng xuất (blacklist JWT token)
 * - Refresh access token
 * - Xem và cập nhật profile người dùng
 *
 * Hệ thống Token:
 * - Access Token: TTL 5 phút (gửi trong response body)
 * - Refresh Token: TTL 8 ngày (lưu trong httpOnly cookie)
 */
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"}) // Cho phép frontend truy cập
@RequestMapping("/api/auth")
public class LoginController {

    private final AuthService authService;

    /**
     * Đăng ký tài khoản người dùng mới
     *
     * @param request Thông tin đăng ký (username, email, password, fullName, role, phone)
     * @return RegisterResponse chứa thông tin user vừa tạo
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    /**
     * Đăng nhập vào hệ thống
     *
     * Hỗ trợ 2 phương thức:
     * 1. Đăng nhập thông thường: username/email + password
     * 2. Đăng nhập Google OAuth: sử dụng Google ID token
     *
     * @param request Thông tin đăng nhập (username/email, password hoặc Google token)
     * @param response HTTP response để set refresh token cookie
     * @param method Phương thức đăng nhập: null (thông thường) hoặc "google" (OAuth)
     * @return AuthResponse chứa access token và thông tin user
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request,
                                              HttpServletResponse response,
                                              @RequestParam(required = false, name="method") String method) {
        // Nếu là Google OAuth, xử lý riêng
        if (method != null && method.equals("google"))
            return ResponseEntity.ok(authService.authenticateGoogle(request));

        // Đăng nhập thông thường
        return ResponseEntity.ok(authService.authenticate(request, response));
    }

    /**
     * Đăng xuất khỏi hệ thống
     *
     * Thực hiện:
     * - Thêm access token vào blacklist trong Redis
     * - Xóa refresh token cookie
     *
     * @param request HTTP request chứa access token trong Authorization header
     * @param response HTTP response để xóa cookie
     * @return Thông báo đăng xuất thành công
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response){
        authService.logout(request, response);
        return ResponseEntity.ok("Logout Successfully");
    }

    /**
     * Refresh access token mới khi token cũ hết hạn
     *
     * Sử dụng refresh token từ httpOnly cookie để tạo access token mới
     * Access token có TTL ngắn (5 phút) nên cần refresh thường xuyên
     *
     * @param request HTTP request chứa refresh token trong cookie
     * @param response HTTP response để cập nhật cookie mới
     * @param method Phương thức: null (thông thường) hoặc "google" (OAuth)
     * @return AuthResponse chứa access token mới hoặc 401 nếu refresh token không hợp lệ
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response, @RequestParam(required = false, name="method") String method) {
        if (method != null && method.equals("google")){
            // Google OAuth refresh
            authService.generateRefreshTokenCookie(request, response);
        } else {
            // Refresh token thông thường
            AuthResponse accessToken = authService.refreshToken(request, response);
            if(accessToken != null)
                return ResponseEntity.ok(accessToken);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Refresh Token");
    }

    /**
     * Endpoint hỗ trợ Google OAuth
     *
     * Tạo refresh token cookie cho phiên OAuth
     *
     * @param request HTTP request
     * @param response HTTP response để set cookie
     * @return Thông báo khởi tạo thành công hoặc lỗi
     */
    @PostMapping("/oauth")
    public ResponseEntity<String> oAuthToken(HttpServletRequest request, HttpServletResponse response){
        try {
            authService.generateRefreshTokenCookie(request, response);
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("OAuth failed to generate refresh");
        }
        return ResponseEntity.ok("OAuth Refresh initiated successfully");
    }

    /**
     * Lấy thông tin profile người dùng hiện tại
     *
     * Yêu cầu: Access token hợp lệ trong Authorization header
     *
     * @param request HTTP request chứa JWT token
     * @param response HTTP response
     * @return ProfileResponse chứa thông tin user (id, email, fullName, role, status, v.v.)
     */
    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile(HttpServletRequest request, HttpServletResponse response){
        return ResponseEntity.ok(authService.getProfile(request, response));
    }

    /**
     * Lấy thông tin user hiện tại (alias của /profile)
     *
     * Endpoint tương đương với GET /profile
     * Được sử dụng theo chuẩn REST: /me để lấy thông tin chính mình
     *
     * @param request HTTP request chứa JWT token
     * @param response HTTP response
     * @return ProfileResponse chứa thông tin user
     */
    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getCurrentUser(HttpServletRequest request, HttpServletResponse response){
        return ResponseEntity.ok(authService.getProfile(request, response));
    }

    /**
     * Cập nhật thông tin profile người dùng
     *
     * Cho phép cập nhật: fullName, phone, address, avatar (hình đại diện)
     *
     * Lưu ý: Không thể thay đổi email/password tại đây
     * - Đổi password: POST /api/users/me/change-password
     * - Đổi email: POST /api/users/me/change-email
     *
     * @param request HTTP request chứa JWT token
     * @param response HTTP response
     * @param info Thông tin cập nhật (fullName, phone, v.v.)
     * @param avatar File ảnh đại diện (optional)
     * @return ProfileResponse với thông tin đã cập nhật
     */
    @PostMapping("/profile")
    public ResponseEntity<ProfileResponse> updateProfile(HttpServletRequest request, HttpServletResponse response, @RequestBody UpdateProfile info,
                                                         @RequestPart(value = "avatar", required = false) MultipartFile avatar){
        return ResponseEntity.ok(authService.updateProfile(request, response, info, avatar));
    }

    // Endpoint cũ /update-credentials đã bị xóa - thay thế bởi:
    // - POST /api/users/me/change-password (trong UserProfileController)
    // - POST /api/users/me/change-email (trong UserProfileController)

    /**
     * Tự động tạo các tài khoản demo khi khởi động ứng dụng
     *
     * Method này chạy 1 lần khi Spring Boot khởi động (@PostConstruct)
     * Tạo sẵn các tài khoản demo để test và admin hệ thống
     *
     * Danh sách tài khoản demo:
     * - VanA / 123456 (ADMIN) - Quản trị viên
     * - VanB / 123456 (STAFF) - Nhân viên
     * - VanC / 123456 (CUSTOMER) - Khách hàng
     * - demo.account@gmail.com / demo123 (CUSTOMER) - Tài khoản demo
     * - admin@aptechmall.com / admin123 (ADMIN) - Admin chính
     * - admin / 123456@ (ADMIN) - Admin với username "admin"
     *
     * Lưu ý:
     * - Chỉ tạo nếu email/username chưa tồn tại
     * - Không tạo trùng lặp khi restart server
     */
    @PostConstruct
    private void PreRegisterUsers(){
        List<RegisterRequest> preRegistrations = new ArrayList<>();

        // Tạo danh sách tài khoản demo
        RegisterRequest request1 = new RegisterRequest("VanA", "123456", "Nguyen Van A", "ADMIN", "NguyenVanA@gmail.com", null);
        RegisterRequest request2 = new RegisterRequest("VanB", "123456", "Nguyen Van B", "STAFF", "NguyenVanB@gmail.com", null);
        RegisterRequest request3 = new RegisterRequest("VanC", "123456", "Nguyen Van C", "CUSTOMER", "NguyenVanC@gmail.com", null);
        RegisterRequest request4 = new RegisterRequest(null, "demo123", "Demo User", "CUSTOMER", "demo.account@gmail.com", null);
        RegisterRequest request5 = new RegisterRequest(null, "admin123", "Demo Admin", "ADMIN", "admin@aptechmall.com", null);
        RegisterRequest adminRequest = new RegisterRequest("admin", "123456@", "Admin User", "ADMIN", "admin@aptechmall.com", null);

        preRegistrations.add(request1);
        preRegistrations.add(request2);
        preRegistrations.add(request3);
        preRegistrations.add(request4);
        preRegistrations.add(request5);
        preRegistrations.add(adminRequest);

        // Duyệt qua từng tài khoản và tạo nếu chưa tồn tại
        for(RegisterRequest request : preRegistrations){
            // Kiểm tra email và username đã tồn tại chưa
            boolean emailExists = authService.verifyEmailExists(request.getEmail());
            boolean usernameExists = request.getUsername() != null && authService.verifyUserExists(request.getUsername());

            // Chỉ tạo nếu cả email và username đều chưa tồn tại
            if (!emailExists && !usernameExists){
                authService.preRegister(request);
            }
        }
    }
}
