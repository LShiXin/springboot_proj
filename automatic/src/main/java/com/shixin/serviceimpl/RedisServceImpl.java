package com.shixin.serviceimpl;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisServceImpl {

    @Autowired
    private StringRedisTemplate redisTemplate;

    public void saveToRedis(String key, String value, long timeout) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(timeout)); // 设置过期时间，单位为秒
    }

    public String getFromRedis(String key) {
        return redisTemplate.opsForValue().get(key);
    }
}
