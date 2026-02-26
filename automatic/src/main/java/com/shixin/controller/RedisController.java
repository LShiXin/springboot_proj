package com.shixin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shixin.serviceimpl.RedisServceImpl;


@RestController
@RequestMapping("/redis")
public class RedisController {
    @Autowired
    private RedisServceImpl redisService;
    
    @GetMapping("/add/{key}/{value}/{timeout}")
    public String getMethodName(@PathVariable String key, @PathVariable String value, @PathVariable long timeout) {
        redisService.saveToRedis(key, value, timeout);
        return "Value added to Redis";
    }

    @GetMapping("/getredis/{key}")
    public String getValueFromRedis(@PathVariable String key) {
        String value = redisService.getFromRedis(key);
        return value != null ? value : "Key not found in Redis";
    }
    
}
