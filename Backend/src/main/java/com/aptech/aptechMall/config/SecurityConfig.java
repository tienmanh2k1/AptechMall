package com.aptech.aptechMall.config;

import com.aptech.aptechMall.security.filters.JwtAuthenticationFilter;
import com.aptech.aptechMall.security.filters.TokenBlacklistFilter;
import com.aptech.aptechMall.service.authentication.JpaUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.CorsFilter;

/**
 * Cấu hình bảo mật Spring Security cho ứng dụng
 *
 * CHẾ ĐỘ BẢO MẬT:
 * - JWT-based authentication (stateless, không dùng session)
 * - BCrypt password encoding
 * - Token blacklist với Redis (cho logout)
 * - CORS enabled cho React frontend
 *
 * FILTER CHAIN:
 * 1. CorsFilter - Cho phép React frontend truy cập API
 * 2. TokenBlacklistFilter - Kiểm tra JWT có trong blacklist không (đã logout)
 * 3. JwtAuthenticationFilter - Validate JWT token và set SecurityContext
 * 4. UsernamePasswordAuthenticationFilter - Spring Security default filter
 *
 * AUTHORIZATION RULES:
 * - PUBLIC endpoints (không cần token):
 *   + /api/auth/** - Đăng ký, đăng nhập, refresh token
 *   + /api/debug/** - Debug endpoints
 *   + /api/aliexpress/**, /api/1688/**, /api/products/** - Xem sản phẩm
 *   + /api/exchange-rates/** - Tỷ giá
 *   + /api/bank-transfer/** - SMS webhook (nạp tiền qua chuyển khoản)
 *
 * - AUTHENTICATED endpoints (cần token):
 *   + /api/users/me/** - Xem/sửa profile của chính mình
 *   + /api/wallet/** - Quản lý ví điện tử
 *   + /api/cart/** - Giỏ hàng
 *   + /api/orders/** - Đơn hàng
 *
 * - ADMIN/STAFF only:
 *   + /api/users/** - Quản lý users (trừ /me)
 *   + /api/admin/orders/** - Quản lý tất cả đơn hàng
 *   + /api/wallet/lock, /api/wallet/unlock - Khóa/mở ví
 *
 * - ADMIN only:
 *   + /api/admin/** - Tất cả admin endpoints
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final JpaUserDetailsService userDetailsService;
    private final CorsFilter corsFilter;
    private final TokenBlacklistFilter tokenBlacklistFilter;

    /**
     * Cấu hình Security Filter Chain cho API endpoints
     *
     * @Order(1) - Filter chain này được ưu tiên xử lý đầu tiên
     *
     * Cấu hình chi tiết:
     * - CSRF disabled - không cần vì dùng JWT (stateless)
     * - Session STATELESS - không tạo HTTP session
     * - Authorization rules - xem class-level JavaDoc
     * - Custom filters - CORS, JWT validation, Token blacklist
     *
     * @param http HttpSecurity để cấu hình
     * @return SecurityFilterChain đã được cấu hình
     */
    @Bean
    @Order(1) // Ưu tiên cao nhất - xử lý tất cả /api/** requests
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/**") // Áp dụng cho tất cả /api/** endpoints
                .csrf(AbstractHttpConfigurer::disable) // Tắt CSRF vì dùng JWT
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Không dùng session
                .authorizeHttpRequests(auth -> auth
                        // ===== PUBLIC ENDPOINTS (không cần authentication) =====
                        .requestMatchers("/api/auth/*", "/api/auth/**").permitAll() // Auth: register, login, refresh
                        .requestMatchers("/api/debug/**").permitAll() // Debug endpoints
                        .requestMatchers("/api/aliexpress/**", "/api/1688/**", "/api/products/**").permitAll() // Xem sản phẩm
                        .requestMatchers("/api/exchange-rates/**").permitAll() // Tỷ giá
                        .requestMatchers("/api/bank-transfer/**").permitAll() // SMS webhook nạp tiền

                        // ===== AUTHENTICATED ENDPOINTS (cần JWT token) =====
                        .requestMatchers("/api/users/me/**").authenticated() // Profile của chính mình
                        .requestMatchers("/api/wallet/**").authenticated() // Ví điện tử
                        .requestMatchers("/api/cart/**", "/api/orders/**").authenticated() // Giỏ hàng, đơn hàng

                        // ===== ADMIN/STAFF ONLY =====
                        .requestMatchers("/api/users/**").hasAnyRole("ADMIN", "STAFF") // Quản lý users
                        .requestMatchers("/api/admin/orders/**").hasAnyRole("ADMIN", "STAFF") // Quản lý đơn hàng
                        .requestMatchers("/api/wallet/*/lock", "/api/wallet/*/unlock").hasRole("ADMIN") // Khóa/mở ví

                        // ===== ADMIN ONLY =====
                        .requestMatchers("/api/admin/**").hasRole("ADMIN") // Tất cả admin endpoints

                        // ===== MẶC ĐỊNH: Yêu cầu authentication cho các request còn lại =====
                        .anyRequest().authenticated()
                )
                .logout(AbstractHttpConfigurer::disable) // Logout tự xử lý trong controller (blacklist token)

                // ===== FILTER CHAIN (thứ tự quan trọng!) =====
                .addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class) // 1. CORS - cho phép React truy cập
                .addFilterBefore(tokenBlacklistFilter, UsernamePasswordAuthenticationFilter.class) // 2. Blacklist - kiểm tra token đã logout chưa
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class) // 3. JWT - validate token và set SecurityContext

                .userDetailsService(userDetailsService) // Load user details từ database
                .build();
    }

    /**
     * AuthenticationManager - quản lý quá trình authentication
     *
     * Được sử dụng trong AuthService để xác thực username/password
     *
     * @param config AuthenticationConfiguration từ Spring Security
     * @return AuthenticationManager instance
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * AuthenticationProvider - cung cấp logic authentication
     *
     * Sử dụng DaoAuthenticationProvider:
     * - Load user từ database qua JpaUserDetailsService
     * - So sánh password đã hash với BCryptPasswordEncoder
     *
     * @return DaoAuthenticationProvider đã cấu hình
     */
    @Bean
    public AuthenticationProvider authenticationProvider(){
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setPasswordEncoder(passwordEncoder());
        authProvider.setUserDetailsService(userDetailsService);
        return authProvider;
    }

    /**
     * PasswordEncoder - mã hóa mật khẩu người dùng
     *
     * Sử dụng BCrypt algorithm:
     * - Hash one-way (không thể decrypt)
     * - Mỗi lần hash cùng password cho kết quả khác nhau (salt ngẫu nhiên)
     * - Độ mạnh mặc định: 10 rounds
     *
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
