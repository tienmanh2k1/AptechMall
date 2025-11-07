package com.aptech.aptechMall.security.filters;

import com.aptech.aptechMall.service.authentication.JpaUserDetailsService;
import com.aptech.aptechMall.service.authentication.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter xác thực JWT Token cho mỗi HTTP request
 *
 * VỊ TRÍ TRONG SECURITY CHAIN:
 * 1. CorsFilter - Cho phép CORS
 * 2. TokenBlacklistFilter - Kiểm tra token blacklist
 * 3. JwtAuthenticationFilter - FILTER NÀY - Validate JWT và set SecurityContext
 * 4. UsernamePasswordAuthenticationFilter - Spring Security default
 *
 * CHỨC NĂNG CHÍNH:
 * - Trích xuất JWT token từ Authorization header
 * - Validate JWT token (signature, expiration)
 * - Load user details từ database
 * - Set Authentication vào SecurityContext
 * - Skip validation cho public endpoints
 *
 * LUỒNG XỬ LÝ:
 * 1. Kiểm tra URL có phải public endpoint không → skip nếu có
 * 2. Lấy Authorization header từ request
 * 3. Kiểm tra header có format "Bearer <token>" không
 * 4. Extract JWT token (bỏ "Bearer " prefix)
 * 5. Extract username từ token
 * 6. Load UserDetails từ database
 * 7. Validate token với UserDetails
 * 8. Tạo Authentication object và set vào SecurityContext
 * 9. Continue filter chain
 *
 * XỬ LÝ LỖI:
 * - ExpiredJwtException → 401 "Token expired"
 * - Các lỗi khác → 401 "Invalid token"
 * - Return JSON error response và KHÔNG tiếp tục filter chain
 *
 * SECURITY CONTEXT:
 * Sau khi filter này chạy, các controller có thể:
 * - Dùng @AuthenticationPrincipal để lấy UserDetails
 * - Dùng AuthenticationUtil.getCurrentUserId() để lấy userId
 * - Spring Security tự động check @PreAuthorize, @Secured
 *
 * QUAN TRỌNG:
 * - Filter này extends OncePerRequestFilter → chỉ chạy 1 lần per request
 * - PUBLIC endpoints được skip để tăng performance
 * - Token đã được kiểm tra blacklist ở TokenBlacklistFilter trước đó
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final JpaUserDetailsService userDetailsService;

    /**
     * Xử lý mỗi HTTP request để validate JWT token
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

        // === STEP 1: Skip JWT validation cho public endpoints ===
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
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        try {
            // === STEP 3: Kiểm tra Authorization header có hợp lệ không ===
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                // Không có token → tiếp tục filter chain
                // Spring Security sẽ reject request nếu endpoint cần authentication
                filterChain.doFilter(request, response);
                return;
            }

            // === STEP 4: Extract JWT token (bỏ "Bearer " prefix) ===
            jwt = authHeader.substring(7); // "Bearer " có 7 ký tự
            username = jwtService.extractUsername(jwt); // Extract username/email từ token

            // === STEP 5: Validate token và set Authentication ===
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Load user details từ database
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                // Validate token: check signature và expiration
                if (jwtService.validateToken(jwt, userDetails)) {
                    // Tạo Authentication object
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // credentials = null (không cần password)
                            userDetails.getAuthorities() // roles/permissions
                    );

                    // Set thêm request details (IP, session ID, v.v.)
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // === QUAN TRỌNG: Set Authentication vào SecurityContext ===
                    // Từ đây, Spring Security biết user đã authenticated
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            // === STEP 6: Tiếp tục filter chain ===
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException ex){
            // Token hết hạn → trả về 401 với message rõ ràng
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Token expired\",\"message\":\"Your session has expired. Please login again.\"}");
            return; // KHÔNG tiếp tục filter chain
        } catch (Exception ex) {
            // Token invalid (malformed, signature sai, v.v.) → 401
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid token\",\"message\":\"Authentication failed. Please login again.\"}");
            return; // KHÔNG tiếp tục filter chain
        }
    }
}
