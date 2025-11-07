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

@Service
public class JwtService {

    @Autowired
    private UserRepository userRepository;

    @Value("${jwt.secret-key}")
    private String secretKey;

    // Dùng Duration API để tránh integer overflow và rõ ràng về đơn vị thời gian
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(30); // 30 phút
    protected static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30); // 30 ngày

    public String generateToken(String username, String tokenType) {
        validateTokenType(tokenType);

        User user = userRepository.existsByUsername(username)
                ? userRepository.findByUsername(username).orElseThrow()
                : userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("No email found for Token Generation"));

        return buildToken(user, tokenType);
    }

    public String generateToken(User user, String tokenType) {
        validateTokenType(tokenType);
        return buildToken(user, tokenType);
    }

    private void validateTokenType(String tokenType) {
        if (!"access_token".equals(tokenType) && !"refresh_token".equals(tokenType)) {
            throw new IllegalArgumentException("Token type " + tokenType + " not supported");
        }
    }

    private Map<String, Object> extractClaims(User user, String tokenType) {
        Map<String, Object> claims = new HashMap<>();

        claims.put("userId", user.getUserId());
        claims.put("role", user.getRole().name());
        claims.put("type", tokenType);
        claims.put("email", user.getEmail());
        claims.put("fullname", user.getFullName());
        claims.put("status", user.getStatus().name());

        return claims;
    }

    private String buildToken(User user, String tokenType) {
        Map<String, Object> claims = extractClaims(user, tokenType);
        String subject = user.getUsername() != null ? user.getUsername() : user.getEmail();

        // Dùng Instant API để tính toán thời gian một cách an toàn
        Instant now = Instant.now();
        Duration ttl = "refresh_token".equals(tokenType) ? REFRESH_TOKEN_TTL : ACCESS_TOKEN_TTL;
        Instant expirationTime = now.plus(ttl);

        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expirationTime))
                .and()
                .signWith(getKey())
                .compact();
    }

    private SecretKey getKey(){
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

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

    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public boolean validateToken(String jwtToken) {
        return extractExpiration(jwtToken).after(new Date());
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
