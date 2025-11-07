package com.aptech.aptechMall.security.filters;

import com.aptech.aptechMall.service.authentication.RedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter kiểm tra JWT Token có trong Blacklist không
 *
 * VỊ TRÍ TRONG SECURITY CHAIN:
 * 1. CorsFilter - Cho phép CORS
 * 2. TokenBlacklistFilter - FILTER NÀY - Kiểm tra token đã logout chưa
 * 3. JwtAuthenticationFilter - Validate JWT và set SecurityContext
 * 4. UsernamePasswordAuthenticationFilter - Spring Security default
 *
 * CHỨC NĂNG CHÍNH:
 * - Kiểm tra JWT token có trong Redis blacklist không
 * - Reject request nếu token đã bị blacklist (user đã logout)
 * - Skip check cho public endpoints
 *
 * CÁCH HOẠT ĐỘNG:
 * Khi user logout:
 * 1. AuthService thêm access token vào Redis với TTL
 * 2. TokenBlacklistFilter check Redis trước mỗi request
 * 3. Nếu token có trong Redis → reject với 401 Unauthorized
 * 4. Nếu không có → tiếp tục filter chain
 *
 * TẠI SAO KIỂM TRA BLACKLIST TRƯỚC JWT VALIDATION:
 * - Tránh lãng phí CPU validate token đã logout
 * - Logout có hiệu lực ngay lập tức
 * - Redis check rất nhanh (microseconds)
 *
 * XỬ LÝ LỖI REDIS:
 * - Nếu Redis không available → log warning và TIẾP TỤC
 * - Không block toàn bộ hệ thống nếu Redis down
 * - Trade-off: Logout có thể không hoạt động nếu Redis down
 *
 * QUAN TRỌNG:
 * - Filter này extends OncePerRequestFilter → chỉ chạy 1 lần per request
 * - PUBLIC endpoints được skip để tăng performance
 * - Token trong blacklist = token đã logout = user phải login lại
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenBlacklistFilter extends OncePerRequestFilter {

    private final RedisService redisService;

    /**
     * Kiểm tra JWT token có trong blacklist không
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain Filter chain để tiếp tục xử lý
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // === STEP 1: Skip blacklist check cho public endpoints ===
        String requestPath = request.getRequestURI();
        if (requestPath.startsWith("/api/auth/") ||       // Authentication endpoints
            requestPath.startsWith("/api/debug/") ||      // Debug endpoints
            requestPath.startsWith("/api/aliexpress/") || // Xem sản phẩm AliExpress
            requestPath.startsWith("/api/1688/") ||       // Xem sản phẩm 1688
            requestPath.startsWith("/api/products/")) {   // Xem sản phẩm chung
            filterChain.doFilter(request, response);
            return;
        }

        // === STEP 2: Lấy Authorization header ===
        String token = request.getHeader("Authorization");

        // === STEP 3: Kiểm tra token có trong blacklist không ===
        // Graceful degradation: Nếu Redis down, vẫn cho phép request tiếp tục
        if(token != null) {
            try {
                // Extract JWT token (bỏ "Bearer " prefix - 7 ký tự)
                String jwtToken = token.substring(7);

                // Kiểm tra Redis blacklist
                if(redisService.hasToken(jwtToken)) {
                    // Token có trong blacklist = user đã logout
                    // Reject request với 401 Unauthorized
                    response.getWriter().write("Token is blacklisted");
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token is blacklisted");
                    return; // KHÔNG tiếp tục filter chain
                }
            } catch (Exception e) {
                // Redis không available - log warning nhưng TIẾP TỤC
                // Không block toàn bộ hệ thống nếu Redis down
                log.warn("Redis not available for token blacklist check: {}", e.getMessage());
                // Tiếp tục filter chain - logout có thể không hoạt động đúng
            }
        }

        // === STEP 4: Token không có trong blacklist → tiếp tục filter chain ===
        filterChain.doFilter(request, response);
    }
}
