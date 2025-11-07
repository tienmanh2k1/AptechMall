package com.aptech.aptechMall.service.authentication;

import com.aptech.aptechMall.model.jpa.User;
import com.aptech.aptechMall.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service quản lý JWT (JSON Web Token) cho authentication
 *
 * Chức năng chính:
 * - Tạo JWT tokens (access token và refresh token)
 * - Validate và verify JWT tokens
 * - Trích xuất thông tin từ JWT claims (username, userId, role, v.v.)
 *
 * CẤU TRÚC JWT TOKEN:
 * JWT gồm 3 phần: Header.Payload.Signature
 *
 * Claims trong token:
 * - subject: username hoặc email của user
 * - userId: ID của user (QUAN TRỌNG cho authorization)
 * - role: Role của user (ADMIN, STAFF, CUSTOMER)
 * - type: Loại token (access_token hoặc refresh_token)
 * - email: Email của user
 * - fullname: Tên đầy đủ
 * - status: Trạng thái tài khoản
 * - iat: Thời gian phát hành (issued at)
 * - exp: Thời gian hết hạn (expiration)
 *
 * TTL (Time To Live):
 * - Access Token: 30 phút
 * - Refresh Token: 30 ngày
 *
 * BẢO MẬT:
 * - Token được ký bằng HMAC-SHA với secret key (từ application.properties)
 * - Secret key phải được mã hóa Base64
 * - Token có thể bị blacklist khi logout (quản lý bởi RedisService)
 *
 * Thư viện: io.jsonwebtoken (JJWT)
 */
@Service
public class JwtService {

    @Autowired
    private UserRepository userRepository;

    @Value("${jwt.secret-key}") // Đọc từ application.properties
    private String secretKey;

    // TTL của tokens - sử dụng Duration API để an toàn và rõ ràng
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(30); // 30 phút
    protected static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30); // 30 ngày

    /**
     * Tạo JWT token từ username/email
     *
     * @param username Username hoặc email của user
     * @param tokenType "access_token" hoặc "refresh_token"
     * @return JWT token string
     */
    public String generateToken(String username, String tokenType) {
        validateTokenType(tokenType);

        // Tìm user theo username hoặc email
        User user = userRepository.existsByUsername(username)
                ? userRepository.findByUsername(username).orElseThrow()
                : userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("No email found for Token Generation"));

        return buildToken(user, tokenType);
    }

    /**
     * Tạo JWT token từ User object
     *
     * @param user User object
     * @param tokenType "access_token" hoặc "refresh_token"
     * @return JWT token string
     */
    public String generateToken(User user, String tokenType) {
        validateTokenType(tokenType);
        return buildToken(user, tokenType);
    }

    /**
     * Validate token type - chỉ chấp nhận access_token hoặc refresh_token
     */
    private void validateTokenType(String tokenType) {
        if (!"access_token".equals(tokenType) && !"refresh_token".equals(tokenType)) {
            throw new IllegalArgumentException("Token type " + tokenType + " not supported");
        }
    }

    /**
     * Trích xuất claims từ User object để đưa vào JWT
     *
     * Claims bao gồm: userId, role, email, fullname, status, type
     *
     * @param user User object
     * @param tokenType Loại token
     * @return Map chứa các claims
     */
    private Map<String, Object> extractClaims(User user, String tokenType) {
        Map<String, Object> claims = new HashMap<>();

        claims.put("userId", user.getUserId()); // QUAN TRỌNG: dùng cho authorization
        claims.put("role", user.getRole().name()); // ADMIN, STAFF, CUSTOMER
        claims.put("type", tokenType); // access_token hoặc refresh_token
        claims.put("email", user.getEmail());
        claims.put("fullname", user.getFullName());
        claims.put("status", user.getStatus().name()); // ACTIVE, INACTIVE, v.v.

        return claims;
    }

    /**
     * Build JWT token từ User object và claims
     *
     * Quy trình:
     * 1. Trích xuất claims từ user
     * 2. Set subject = username (hoặc email nếu không có username)
     * 3. Tính thời gian hết hạn dựa vào tokenType
     * 4. Sign token với secret key
     *
     * @param user User object
     * @param tokenType Loại token
     * @return JWT token string đã được sign
     */
    private String buildToken(User user, String tokenType) {
        Map<String, Object> claims = extractClaims(user, tokenType);
        String subject = user.getUsername() != null ? user.getUsername() : user.getEmail();

        // Tính toán thời gian sử dụng Instant API (an toàn, tránh overflow)
        Instant now = Instant.now();
        Duration ttl = "refresh_token".equals(tokenType) ? REFRESH_TOKEN_TTL : ACCESS_TOKEN_TTL;
        Instant expirationTime = now.plus(ttl);

        // Build và sign JWT token
        return Jwts.builder()
                .claims()
                .add(claims) // Thêm custom claims
                .subject(subject) // Username hoặc email
                .issuedAt(Date.from(now)) // Thời gian phát hành
                .expiration(Date.from(expirationTime)) // Thời gian hết hạn
                .and()
                .signWith(getKey()) // Ký token với secret key
                .compact();
    }

    /**
     * Lấy SecretKey để sign và verify JWT
     *
     * Secret key được decode từ Base64 string trong application.properties
     *
     * @return SecretKey object
     */
    private SecretKey getKey(){
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Trích xuất username từ JWT token
     *
     * @param token JWT token
     * @return Username (subject claim)
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Trích xuất userId từ JWT token
     *
     * QUAN TRỌNG: userId được sử dụng để authorization trong toàn hệ thống
     *
     * Xử lý:
     * - Kiểm tra userId claim có tồn tại không
     * - Validate kiểu dữ liệu (phải là Number)
     * - Convert sang Long
     *
     * Nếu token cũ không có userId → throw exception yêu cầu re-authenticate
     *
     * @param token JWT token
     * @return User ID (Long)
     * @throws IllegalStateException nếu userId không tồn tại hoặc invalid format
     */
    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Object userIdObj = claims.get("userId");

        if (userIdObj == null) {
            throw new IllegalStateException("User ID not found in JWT token. Please re-authenticate to get a new token.");
        }

        if (!(userIdObj instanceof Number)) {
            throw new IllegalStateException("Invalid userId format in JWT token: expected Number, got " + userIdObj.getClass().getSimpleName());
        }

        return ((Number) userIdObj).longValue();
    }

    /**
     * Trích xuất một claim cụ thể từ JWT token
     *
     * @param token JWT token
     * @param claimResolver Function để lấy claim từ Claims object
     * @return Giá trị của claim
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    /**
     * Trích xuất tất cả claims từ JWT token
     *
     * Quy trình:
     * 1. Parse JWT token
     * 2. Verify signature với secret key
     * 3. Lấy payload (claims)
     *
     * @param token JWT token
     * @return Claims object chứa tất cả claims
     * @throws JwtException nếu token invalid hoặc signature không khớp
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey()) // Verify signature với secret key
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validate JWT token với UserDetails
     *
     * Kiểm tra:
     * - Username trong token khớp với UserDetails
     * - Token chưa hết hạn
     *
     * @param token JWT token
     * @param userDetails UserDetails từ database
     * @return true nếu token hợp lệ
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * Validate JWT token (chỉ kiểm tra expiration)
     *
     * @param jwtToken JWT token
     * @return true nếu token chưa hết hạn
     */
    public boolean validateToken(String jwtToken) {
        return extractExpiration(jwtToken).after(new Date());
    }

    /**
     * Kiểm tra token đã hết hạn chưa
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Lấy thời gian hết hạn của token
     *
     * @param token JWT token
     * @return Date expiration time
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
