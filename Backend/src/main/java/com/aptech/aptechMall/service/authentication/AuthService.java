package com.aptech.aptechMall.service.authentication;

import com.aptech.aptechMall.Exception.UsernameAlreadyTaken;
import com.aptech.aptechMall.model.jpa.User;
import com.aptech.aptechMall.repository.UserRepository;
import com.aptech.aptechMall.security.Role;
import com.aptech.aptechMall.security.requests.*;
import com.aptech.aptechMall.service.FileUploadService;
import io.jsonwebtoken.*;
import jakarta.persistence.EntityExistsException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Service xử lý xác thực và quản lý người dùng (Authentication Service)
 *
 * Chức năng chính:
 * - Đăng ký tài khoản mới (register)
 * - Đăng nhập (authenticate) - username/email + password
 * - Đăng nhập Google OAuth (authenticateGoogle)
 * - Đăng xuất (logout) - blacklist JWT token trong Redis
 * - Refresh access token - tạo token mới từ refresh token
 * - Quản lý profile người dùng (getProfile, updateProfile)
 *
 * Hệ thống JWT Token:
 * - Access Token: TTL 5 phút, chứa userId, email, role, fullname, status
 * - Refresh Token: TTL 8 ngày, lưu trong httpOnly cookie
 * - Logout: Thêm access token vào Redis blacklist với TTL = thời gian còn lại của token
 *
 * Bảo mật:
 * - Password được hash bằng BCrypt (PasswordEncoder)
 * - JWT signed bằng secret key (cấu hình trong application.properties)
 * - Refresh token lưu trong httpOnly cookie (không thể truy cập từ JavaScript)
 * - Token blacklist trong Redis để logout ngay lập tức
 *
 * Dependencies:
 * - UserRepository: Truy vấn database users
 * - PasswordEncoder: Hash và verify password
 * - JwtService: Tạo và validate JWT tokens
 * - RedisService: Quản lý token blacklist
 * - FileUploadService: Upload avatar người dùng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RedisService redisService;
    private final FileUploadService fileUploadService;

    /**
     * Lấy thông tin profile của user hiện tại từ JWT token
     *
     * @param request HttpServletRequest chứa Authorization header với JWT token
     * @param response HttpServletResponse để set status code khi có lỗi
     * @return ProfileResponse chứa thông tin user hoặc null nếu có lỗi
     */
    public ProfileResponse getProfile(HttpServletRequest request, HttpServletResponse response){
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }

        try {
            String subject = jwtService.extractUsername(authHeader.replace("Bearer ", ""));
            boolean hasUsername = userRepository.existsByUsername(subject);

            User user = hasUsername ? userRepository.findByUsername(subject).orElseThrow() :
                    userRepository.findByEmail(subject).orElseThrow(() -> new UsernameNotFoundException("Can't retrieve Profile because User is null"));

            return ProfileResponse.builder()
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .emailVerified(user.isEmailVerified())
                    .phone(user.getPhone())
                    .avatarUrl(user.getAvatarUrl())
                    .fullName(user.getFullName())
                    .role(user.getRole())
                    .registeredAt(user.getRegisteredAt())
                    .lastLogin(user.getLastLogin())
                    .build();

        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        } catch (JwtException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        } catch (UsernameNotFoundException e) {
            log.warn("User not found: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error in getProfile: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    public ProfileResponse updateProfile(HttpServletRequest request, HttpServletResponse response, UpdateProfile updatedProfile, MultipartFile avatar){
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }
        try {
            String subject = jwtService.extractUsername(authHeader.substring(7));
            boolean hasUsername = userRepository.existsByUsername(subject);
            String avatarFilePath = avatar != null ? fileUploadService.saveAvatar(avatar) : updatedProfile.getAvatarUrl();

            User user = hasUsername ? userRepository.findByUsername(subject).orElseThrow() :
                    userRepository.findByEmail(subject).orElseThrow(() -> new UsernameNotFoundException("Can't retrieve Profile because User is null"));
            user.setFullName(updatedProfile.getFullName());
            user.setPhone(updatedProfile.getPhone());
            user.setUsername(updatedProfile.getUsername());
            user.setAvatarUrl(avatarFilePath);

            userRepository.save(user);

            return ProfileResponse.builder()
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .emailVerified(user.isEmailVerified())
                    .phone(user.getPhone())
                    .avatarUrl(user.getAvatarUrl())
                    .fullName(user.getFullName())
                    .role(user.getRole())
                    .registeredAt(user.getRegisteredAt())
                    .lastLogin(user.getLastLogin())
                    .build();

        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        } catch (JwtException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        } catch (UsernameNotFoundException e) {
            log.warn("User not found: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error in getProfile: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyTaken("Username " +request.getUsername() + " already taken");
        }

        // Validate Vietnam phone number if provided
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            if (!isValidVietnamPhone(request.getPhone())) {
                throw new IllegalArgumentException("Số điện thoại không hợp lệ. Phải là số điện thoại Việt Nam (10 số, đầu số hợp lệ)");
            }
        }

        // Set default role to CUSTOMER if not provided or use provided role
        Role role = (request.getRole() == null || request.getRole().trim().isEmpty())
                ? Role.CUSTOMER
                : Role.fromString(request.getRole());

        User user = User.builder()
                .username(request.getUsername())
                .fullName(request.getFullName())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .email(request.getEmail())
                .phone(request.getPhone())
                .build();

        userRepository.save(user);

        return new RegisterResponse("Successfully registered the user " + user.getUsername());
    }

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

        String accessJwt = jwtService.generateToken(existUsername ? user.getUsername() : user.getEmail(), "access_token");
        String refreshJwt = jwtService.generateToken(existUsername ? user.getUsername() : user.getEmail(), "refresh_token");
        Cookie refreshTokenCookie = getRefreshTokenCookie(refreshJwt);
        setCookieAttribute(response, refreshTokenCookie);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        return new AuthResponse(accessJwt);
    }

    public AuthResponse refreshToken(HttpServletRequest request, HttpServletResponse response) {
        var refreshTokenCookie = Arrays.stream(request.getCookies()).filter(cookie -> cookie.getName().equals("refresh_token")).findFirst().orElseThrow();
        var currentRefreshToken = refreshTokenCookie.getValue();

        // Check token blacklist if Redis is available
        try {
            if(redisService.hasToken(currentRefreshToken))
                throw new EntityExistsException("Refresh Token haven't yet been invalidated or expired");
        } catch (Exception e) {
            log.warn("Redis not available for token blacklist check: " + e.getMessage());
        }

        revokeToken(currentRefreshToken);
        if(currentRefreshToken != null) {
            if(jwtService.validateToken(currentRefreshToken)) {
                var username = jwtService.extractUsername(currentRefreshToken);
                String jwt = jwtService.generateToken(username, "access_token");
                String newRefreshToken = jwtService.generateToken(username, "refresh_token");
                refreshTokenCookie = getRefreshTokenCookie(newRefreshToken);
                setCookieAttribute(response, refreshTokenCookie);
                return new AuthResponse(jwt);
            }
        }
        return null;
    }

    private Cookie getRefreshTokenCookie(String refreshToken){
        var cookieMaxAge = (int) (jwtService.extractExpiration(refreshToken).getTime() - System.currentTimeMillis()) / 1000;
        if(cookieMaxAge < 0)
            cookieMaxAge = 0;

        Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
        refreshTokenCookie.setMaxAge(cookieMaxAge);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");

        return refreshTokenCookie;
    }

    private void setCookieAttribute(HttpServletResponse response, Cookie refreshTokenCookie){
        response.addCookie(refreshTokenCookie); // add cookie vao React frontend
        String sameSiteAttribute = "; SameSite=None";
        String header = response.getHeader("Set-Cookie");
        if (header != null && !header.contains("SameSite=")) {
            response.setHeader("Set-Cookie", header + sameSiteAttribute);
        }
    }

    public void preRegister(RegisterRequest request) {
        Role role = Role.fromString(request.getRole());

        User user = User.builder()
                .username(request.getUsername())
                .fullName(request.getFullName())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(role)
                .build();

        userRepository.save(user);
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = request.getHeader("Authorization");
        Cookie refreshTokenCookie = Arrays.stream(request.getCookies()).filter(cookie -> cookie.getName().equals("refresh_token")).findFirst().orElseThrow();
        revokeToken(accessToken.substring(7));
        revokeToken(refreshTokenCookie.getValue());
        revokeRefreshTokenCookie(response, refreshTokenCookie);
    }

    private void revokeToken(String token) {
        try {
            var expirationTime = (jwtService.extractExpiration(token).getTime() - System.currentTimeMillis()) / 1000;
            redisService.setToken(token, "blacklisted", expirationTime, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis not available for token revocation: " + e.getMessage());
            // Token won't be blacklisted, but app will continue to work
        }
    }

    private void revokeRefreshTokenCookie(HttpServletResponse response, Cookie refreshTokenCookie) {
        refreshTokenCookie.setMaxAge(0);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");
        setCookieAttribute(response, refreshTokenCookie);
    }

    public boolean verifyUserExists(String username){
        return userRepository.existsByUsername(username);
    }
    public boolean verifyEmailExists(String email){
        return userRepository.existsByEmail(email);
    }

    public AuthResponse authenticateGoogle(AuthRequest request){
        User user; String accessJwt;

        user = userRepository.findByEmail(request.getUsername()).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(request.getUsername());
            newUser.setFullName(request.getFullname());
            newUser.setPassword(passwordEncoder.encode("")); //temporarily set as blank as OAuth doesn't set password for you, login will not authenticate if password is blank
            newUser.setEmailVerified(true);
            return userRepository.save(newUser);
        });;
        accessJwt = jwtService.generateToken(user, "access_token");
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        return new AuthResponse(accessJwt);

    }

    public void generateRefreshTokenCookie(HttpServletRequest request, HttpServletResponse response){
        String accessToken = request.getHeader("Authorization");
        try {
            String subject = jwtService.extractUsername(accessToken.substring(7));
            String refreshJwt = jwtService.generateToken(subject, "refresh_token");

            Cookie refreshTokenCookie = getRefreshTokenCookie(refreshJwt);
            setCookieAttribute(response, refreshTokenCookie);

        } catch (Exception e) {
            log.error("Error extracting subject from JWT: {}", e.getMessage(), e);
        }
    }

    // Old updateEmailOrPassword method removed - replaced by:
    // - UserProfileService.changePassword()
    // - UserProfileService.changeEmail()

    /**
     * Validate Vietnam phone number
     * Must be exactly 10 digits and start with valid prefix
     */
    private boolean isValidVietnamPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return true; // Optional field
        }

        // Remove all non-digit characters
        String cleanPhone = phone.replaceAll("\\D", "");

        // Must be exactly 10 digits
        if (cleanPhone.length() != 10) {
            return false;
        }

        // Must start with 0
        if (!cleanPhone.startsWith("0")) {
            return false;
        }

        // Valid Vietnam phone prefixes (first 3 digits)
        String[] validPrefixes = {
                // Viettel
                "032", "033", "034", "035", "036", "037", "038", "039", "096", "097", "098", "086",
                // Vinaphone
                "083", "084", "085", "081", "082", "088", "091", "094",
                // Mobifone
                "070", "079", "077", "076", "078", "090", "093", "089",
                // Vietnamobile
                "056", "058", "092",
                // Gmobile
                "059", "099"
        };

        String prefix = cleanPhone.substring(0, 3);
        for (String validPrefix : validPrefixes) {
            if (prefix.equals(validPrefix)) {
                return true;
            }
        }

        return false;
    }
}
