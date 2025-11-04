package com.aptech.aptechMall.service.authentication;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final StringRedisTemplate redisTemplate;

    public void setToken(String token, String value, long ttl, TimeUnit unit){
        redisTemplate.opsForValue().set(token, value, ttl, unit);
        System.out.println("Token blacklist working: " + hasToken(token) + " for " + token);
    }

    public boolean hasToken(String token){
        return redisTemplate.hasKey(token);
    }
}
