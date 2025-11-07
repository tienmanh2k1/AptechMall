package com.aptech.aptechMall.service.authentication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service quản lý JWT Token Blacklist với Redis
 *
 * Chức năng chính:
 * - Thêm JWT token vào blacklist khi user logout
 * - Kiểm tra xem token có trong blacklist không
 *
 * CÁCH HOẠT ĐỘNG:
 * Khi user logout, access token được thêm vào Redis với:
 * - Key: JWT token string
 * - Value: "blacklisted" hoặc username
 * - TTL: Thời gian còn lại đến khi token hết hạn
 *
 * Tại sao dùng Redis:
 * - In-memory database → cực nhanh (microseconds)
 * - Hỗ trợ TTL tự động → token tự xóa khỏi blacklist khi hết hạn
 * - Phù hợp cho session management và caching
 *
 * Luồng xử lý Logout:
 * 1. User gọi POST /api/auth/logout
 * 2. AuthService extract access token từ request
 * 3. Tính TTL còn lại của token (expiration - now)
 * 4. RedisService.setToken() → Thêm token vào Redis với TTL
 * 5. TokenBlacklistFilter kiểm tra token trong Redis trước khi cho phép request
 *
 * QUAN TRỌNG:
 * - Chỉ blacklist ACCESS TOKEN (5 phút)
 * - Không blacklist REFRESH TOKEN (quá lâu, tốn memory)
 * - Redis phải chạy để logout hoạt động
 *
 * Cấu hình Redis:
 * - Host/Port: Xem application.properties
 * - spring.redis.host=localhost
 * - spring.redis.port=6379
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {
    private final StringRedisTemplate redisTemplate;

    /**
     * Thêm JWT token vào blacklist
     *
     * Token sẽ được lưu trong Redis với TTL (Time To Live)
     * Sau khi hết TTL, Redis tự động xóa token
     *
     * Sử dụng:
     * - Khi user logout → blacklist access token
     * - TTL = thời gian còn lại đến khi token hết hạn
     * - Không cần lưu token đã hết hạn (tự invalid rồi)
     *
     * @param token JWT token string (full token)
     * @param value Giá trị lưu (thường là "blacklisted" hoặc username)
     * @param ttl Time to live
     * @param unit Đơn vị thời gian (MINUTES, SECONDS, v.v.)
     */
    public void setToken(String token, String value, long ttl, TimeUnit unit){
        redisTemplate.opsForValue().set(token, value, ttl, unit);
        log.debug("Token blacklisted successfully: {} (exists: {})",
                token.substring(0, Math.min(20, token.length())) + "...", // Log 20 ký tự đầu
                hasToken(token));
    }

    /**
     * Kiểm tra JWT token có trong blacklist không
     *
     * Được gọi bởi TokenBlacklistFilter trước mỗi request
     * Nếu token có trong blacklist → reject request với 401 Unauthorized
     *
     * @param token JWT token string
     * @return true nếu token có trong blacklist (đã logout)
     */
    public boolean hasToken(String token){
        return redisTemplate.hasKey(token);
    }
}
