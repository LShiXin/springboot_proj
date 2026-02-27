package com.shixin.service;


public interface RedisService {
   
    void saveToRedis(String key, String value, long timeout); // 设置过期时间，单位为秒

    String getFromRedis(String key);
}
