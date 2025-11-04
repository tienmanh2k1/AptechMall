package com.aptech.aptechMall.service.authentication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisService {
    private final StringRedisTemplate redisTemplate;

    public void setToken(String token, String value, long ttl, TimeUnit unit){
        try {
            redisTemplate.opsForValue().set(token, value, ttl, unit);
            log.info("Token blacklist working: " + hasToken(token) + " for " + token.substring(0, 20) + "...");
        } catch (Exception e) {
            log.error("Failed to blacklist token in Redis: {}", e.getMessage());
        }

    }

    public boolean hasToken(String token){
        try {
            return redisTemplate.hasKey(token);
        } catch (Exception e){
            log.error("Failed to reference token status in Redis: {}", e.getMessage());
            return false;
        }
    }

    public void saveToken(String email, String token, long expirationSeconds) {
        try {
            redisTemplate.opsForValue().set("refresh:" + email, token, expirationSeconds, TimeUnit.SECONDS);
        } catch(Exception e) {
            log.error("Failed to save refresh token in Redis: {}", e.getMessage());
        }
    }

    public boolean deleteToken(String email) {
        try {
            return redisTemplate.delete("refresh:" + email);
        } catch(Exception e) {
            log.error("Failed to delete refresh token in Redis: {}", e.getMessage());
            return false;
        }

    }

    public String getToken(String email) {
        try {
            return redisTemplate.opsForValue().get("refresh:" + email);
        } catch (Exception e){
            log.error("Failed to retrieve Token from redis: {}", e.getMessage());
            return null;
        }

    }

}
