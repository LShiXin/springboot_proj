package com.shixin.service;

import com.shixin.entity.User;


public interface RedisService {
   
    void saveToRedis(String key, String value, long timeout); // 设置过期时间，单位为秒

    String getFromRedis(String key);

    User getUserinfoByUser_id(Long user_id);
}
