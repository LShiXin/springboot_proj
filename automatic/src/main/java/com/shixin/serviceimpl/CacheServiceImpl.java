package com.shixin.serviceimpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.shixin.entity.MonitorTaskListDTO;
import com.shixin.entity.MonitorUrlListDTO;
import com.shixin.repository.MonitorTaskRepository;
import com.shixin.service.CacheService;
import com.shixin.service.MonitorTaskService;
import com.shixin.service.RedisService;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.lettuce.core.dynamic.annotation.Key;

@Service
@Slf4j
public class CacheServiceImpl implements CacheService {
    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    @Autowired
    private MonitorTaskRepository  monitorTaskRepository;

    @Autowired
    private RedisService redisService;

    /*
     * 获取当前用户下的所有监控任务,将数据放到redis中,并设置过期时间为10分钟
     */
    @Async
    public void cacheMonitorTasksToRedis(Long userId) {
        log.info("正在将用户 {} 的监控任务缓存到 Redis 中...", userId);
        // 获取用户的所有监控任务
        List<MonitorTaskListDTO> tasks = monitorTaskRepository.findAllByUserId(userId);
        // 将监控任务链接缓存到 Redis 中，设置过期时间为10min
        String redisKey = "tasks:" + userId;
        redisService.saveToRedis(redisKey, JSON.toJSONString(tasks), 600); // 设置过期时间为10分钟
        log.info("用户 {} 的监控任务已成功缓存到 Redis 中，过期时间为 10 分钟", userId);
    }


     /*
     * 获取当前用户下的所有监控链接,将数据放到redis中,并设置过期时间为10分钟
     */
    @Override
    @Async
    public void cacheMonitorTaskUrlsToRedis(Long userId) {
        log.info("正在将用户 {} 的监控任务链接缓存到 Redis 中...", userId);
        // 获取用户的所有监控任务
        List<MonitorUrlListDTO> urls = monitorTaskRepository.findListDtoByUserId(userId);

        // 将监控任务链接缓存到 Redis 中，设置过期时间为10min
        String redisKey = "task_urls:" + userId;
        redisService.saveToRedis(redisKey, JSON.toJSONString(urls), 600); // 设置过期时间为10分钟
        log.info("用户 {} 的监控任务链接已成功缓存到 Redis 中，过期时间为 10 分钟", userId);
    }

        // 根据用户ID查询所有监控任务，并转换为 MonitorTaskListDTO
    @Override
    public List<MonitorTaskListDTO> getTasksByUserId(Long userId) {
        String redisKey = "tasks:" + userId;
        String cachedData = redisService.getFromRedis(redisKey);
        if (cachedData != null) {
            log.info("从 Redis 中获取用户 {} 的监控任务", userId);
            return JSON.parseArray(cachedData, MonitorTaskListDTO.class);
        }else {
            log.info("Redis 中没有用户 {} 的监控任务", userId);
            return null;
        }
    }

    // 根据用户ID查询所有监控链接，并转换为 MonitorUrlListDTO
    public List<MonitorUrlListDTO> findListDtoByUserId(Long userId){
        String redisKey = "task_urls:" + userId;
        String cachedData = redisService.getFromRedis(redisKey);
        if (cachedData != null) {
            log.info("从 Redis 中获取用户 {} 的监控任务链接", userId);
            return JSON.parseArray(cachedData, MonitorUrlListDTO.class);
        }else {
            log.info("Redis 中没有用户 {} 的监控任务链接", userId);
            return null;
        }
    }

}
