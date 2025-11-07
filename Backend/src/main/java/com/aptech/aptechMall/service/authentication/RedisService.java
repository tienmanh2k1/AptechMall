package com.aptech.aptechMall.service.authentication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {
    private final StringRedisTemplate redisTemplate;

    public void setToken(String token, String value, long ttl, TimeUnit unit){
        redisTemplate.opsForValue().set(token, value, ttl, unit);
        log.debug("Token blacklisted successfully: {} (exists: {})", token.substring(0, Math.min(20, token.length())) + "...", hasToken(token));
    }

    public boolean hasToken(String token){
        return redisTemplate.hasKey(token);
    }
}
