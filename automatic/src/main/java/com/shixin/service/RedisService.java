package com.shixin.service;

import com.shixin.entity.User;
import com.shixin.entity.UserAllInfoDTO;
import com.shixin.entity.TaskScheduleConfig;

import java.util.List;


public interface RedisService {
   
    void saveToRedis(String key, String value, long timeout); // 设置过期时间，单位为秒

    String getFromRedis(String key);
    
    // 新的JSON存储方法
    void saveUserAllInfo(Long userId, UserAllInfoDTO userAllInfo, long timeout);
    
    UserAllInfoDTO getUserAllInfo(Long userId);
    
    // 局部更新方法
    void updateUserBaseInfo(Long userId, UserAllInfoDTO.UserBaseInfo baseInfo);
    
    void updateUserTasks(Long userId, java.util.List<UserAllInfoDTO.TaskWithUrls> tasks);
    
    void updateTaskUrls(Long userId, Long taskId, java.util.List<com.shixin.entity.MonitorUrlListDTO> urls);
    
    void deleteUserAllInfo(Long userId);
    
    // 执行队列相关方法
    void saveExecutionQueue(List<TaskScheduleConfig> tasks, long timeout);
    
    List<TaskScheduleConfig> getExecutionQueue();
    
    void clearExecutionQueue();

}
