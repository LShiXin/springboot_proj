package com.shixin.serviceimpl;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.shixin.entity.User;
import com.shixin.service.RedisService;

@Service
public class RedisServceImpl implements RedisService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override 
    public void saveToRedis(String key, String value, long timeout) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(timeout)); // 设置过期时间，单位为秒
    }

    @Override 
    public String getFromRedis(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public User getUserinfoByUser_id(Long user_id) {
        String key = "user:" + user_id; // 构建Redis键，例如"user:123"
        String userInfoStr = getFromRedis(key); // 从Redis中获取用户信息字符串
        if (userInfoStr != null) {
            // 将字符串转换为User对象（假设User类有相应的构造方法或解析方法）
            return JSON.parseObject(userInfoStr, User.class); // 假设userInfoStr是User对象的字符串表示形式
        }
        return null;
    }
}
