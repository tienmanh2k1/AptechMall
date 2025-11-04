package com.aptech.aptechMall.security.filters;

import com.aptech.aptechMall.service.authentication.RedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TokenBlacklistFilter extends OncePerRequestFilter {

    private final RedisService redisService;
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        // Skip blacklist check for public endpoints
        String requestPath = request.getRequestURI();
        if (requestPath.startsWith("/api/auth/") ||
            requestPath.startsWith("/api/debug/") ||
            requestPath.startsWith("/api/aliexpress/") ||
            requestPath.startsWith("/api/1688/") ||
            requestPath.startsWith("/api/products/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getHeader("Authorization");

        // Try to check Redis, but continue if Redis is unavailable
        if(token != null) {
            try {
                if(redisService.hasToken(token.substring(7))) {
                    response.getWriter().write("Token is blacklisted");
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token is blacklisted");
                    return;
                }
            } catch (Exception e) {
                // Redis not available - log warning but continue
                System.err.println("WARNING: Redis not available for token blacklist check: " + e.getMessage());
                // Continue without blacklist check
            }
        }

        filterChain.doFilter(request, response);
    }
}
