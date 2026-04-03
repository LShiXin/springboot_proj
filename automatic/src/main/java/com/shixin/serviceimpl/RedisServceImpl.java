package com.shixin.serviceimpl;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.shixin.entity.MonitorTask;
import com.shixin.entity.MonitorUrlListDTO;
import com.shixin.entity.User;
import com.shixin.entity.UserAllInfoDTO;
import com.shixin.entity.TaskScheduleConfig;
import com.shixin.service.RedisService;

@Service
public class RedisServceImpl implements RedisService {

    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private static final String USER_ALL_INFO_KEY_PREFIX = "userAllInfo:";
    private static final String EXECUTION_QUEUE_KEY = "executionQueue:current";

    @Override 
    public void saveToRedis(String key, String value, long timeout) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(timeout)); // 设置过期时间，单位为秒
    }

    @Override 
    public String getFromRedis(String key) {
        return redisTemplate.opsForValue().get(key);
    }
    
    @Override
    public void saveUserAllInfo(Long userId, UserAllInfoDTO userAllInfo, long timeout) {
        String key = USER_ALL_INFO_KEY_PREFIX + userId;
        String value = JSON.toJSONString(userAllInfo);
        saveToRedis(key, value, timeout);
    }
    
    @Override
    public UserAllInfoDTO getUserAllInfo(Long userId) {
        String key = USER_ALL_INFO_KEY_PREFIX + userId;
        String value = getFromRedis(key);
        if (value != null) {
            return JSON.parseObject(value, UserAllInfoDTO.class);
        }
        return null;
    }
    
    @Override
    public void updateUserBaseInfo(Long userId, UserAllInfoDTO.UserBaseInfo baseInfo) {
        UserAllInfoDTO userAllInfo = getUserAllInfo(userId);
        if (userAllInfo != null) {
            userAllInfo.setBase(baseInfo);
            // 获取剩余过期时间
            Long ttl = redisTemplate.getExpire(USER_ALL_INFO_KEY_PREFIX + userId);
            if (ttl != null && ttl > 0) {
                saveUserAllInfo(userId, userAllInfo, ttl);
            } else {
                saveUserAllInfo(userId, userAllInfo, 600); // 默认10分钟
            }
        }
    }
    
    @Override
    public void updateUserTasks(Long userId, List<UserAllInfoDTO.TaskWithUrls> tasks) {
        UserAllInfoDTO userAllInfo = getUserAllInfo(userId);
        if (userAllInfo != null) {
            userAllInfo.setTasks(tasks);
            // 获取剩余过期时间
            Long ttl = redisTemplate.getExpire(USER_ALL_INFO_KEY_PREFIX + userId);
            if (ttl != null && ttl > 0) {
                saveUserAllInfo(userId, userAllInfo, ttl);
            } else {
                saveUserAllInfo(userId, userAllInfo, 600); // 默认10分钟
            }
        }
    }
    
    @Override
    public void updateTaskUrls(Long userId, Long taskId, List<MonitorUrlListDTO> urls) {
        UserAllInfoDTO userAllInfo = getUserAllInfo(userId);
        if (userAllInfo != null && userAllInfo.getTasks() != null) {
            List<UserAllInfoDTO.TaskWithUrls> tasks = userAllInfo.getTasks();
            for (UserAllInfoDTO.TaskWithUrls task : tasks) {
                if (task.getId().equals(taskId)) {
                    task.setUrls(urls);
                    break;
                }
            }
            userAllInfo.setTasks(tasks);
            // 获取剩余过期时间
            Long ttl = redisTemplate.getExpire(USER_ALL_INFO_KEY_PREFIX + userId);
            if (ttl != null && ttl > 0) {
                saveUserAllInfo(userId, userAllInfo, ttl);
            } else {
                saveUserAllInfo(userId, userAllInfo, 600); // 默认10分钟
            }
        }
    }
    
    @Override
    public void deleteUserAllInfo(Long userId) {
        String key = USER_ALL_INFO_KEY_PREFIX + userId;
        redisTemplate.delete(key);
    }
    
    @Override
    public void saveExecutionQueue(List<TaskScheduleConfig> tasks, long timeout) {
        // 先清空现有队列
        clearExecutionQueue();
        
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        
        // 将任务列表转换为JSON字符串列表
        List<String> taskJsonList = tasks.stream()
            .map(JSON::toJSONString)
            .collect(Collectors.toList());
        
        // 使用Redis List存储，从左到右按执行时间排序（最早执行的在最右边）
        // 使用rightPushAll确保顺序：最早执行的任务在列表尾部
        redisTemplate.opsForList().rightPushAll(EXECUTION_QUEUE_KEY, taskJsonList);
        
        // 设置队列过期时间
        redisTemplate.expire(EXECUTION_QUEUE_KEY, Duration.ofSeconds(timeout));
        
        // 记录日志
        org.slf4j.LoggerFactory.getLogger(RedisServceImpl.class)
            .info("执行队列已保存到Redis，共 {} 个任务，过期时间: {} 秒", tasks.size(), timeout);
    }
    
    @Override
    public List<TaskScheduleConfig> getExecutionQueue() {
        // 获取整个队列
        List<String> taskJsonList = redisTemplate.opsForList().range(EXECUTION_QUEUE_KEY, 0, -1);
        
        if (taskJsonList == null || taskJsonList.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 将JSON字符串转换回TaskScheduleConfig对象
        return taskJsonList.stream()
            .map(json -> JSON.parseObject(json, TaskScheduleConfig.class))
            .collect(Collectors.toList());
    }
    
    @Override
    public void clearExecutionQueue() {
        redisTemplate.delete(EXECUTION_QUEUE_KEY);
        org.slf4j.LoggerFactory.getLogger(RedisServceImpl.class)
            .debug("执行队列已从Redis中清除");
    }
    
    @Override
    public void removeTaskFromExecutionQueue(Long taskId) {
        // 获取当前队列
        List<String> taskJsonList = redisTemplate.opsForList().range(EXECUTION_QUEUE_KEY, 0, -1);
        
        if (taskJsonList == null || taskJsonList.isEmpty()) {
            return;
        }
        
        // 查找要删除的任务
        String taskToRemove = null;
        for (String taskJson : taskJsonList) {
            TaskScheduleConfig config = JSON.parseObject(taskJson, TaskScheduleConfig.class);
            if (config.getId() != null && config.getId().equals(taskId)) {
                taskToRemove = taskJson;
                break;
            }
        }
        
        if (taskToRemove != null) {
            // 从列表中移除该任务
            // 注意：Redis的LREM命令会移除所有匹配的元素，但我们只移除第一个匹配的
            redisTemplate.opsForList().remove(EXECUTION_QUEUE_KEY, 1, taskToRemove);
            
            org.slf4j.LoggerFactory.getLogger(RedisServceImpl.class)
                .debug("已从执行队列中删除任务: {}", taskId);
        } else {
            org.slf4j.LoggerFactory.getLogger(RedisServceImpl.class)
                .debug("未在执行队列中找到任务: {}", taskId);
        }
    }
}
