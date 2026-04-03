package com.shixin.service;

import java.util.List;

import com.shixin.entity.MonitorTaskListDTO;
import com.shixin.entity.MonitorUrlListDTO;
import com.shixin.entity.UserAllInfoDTO;

public interface CacheService {

    // 缓存用户完整信息到Redis（包含用户基本信息和所有定时任务）
    void cacheUserAllInfoToRedis(Long userId);
    
    // 获取用户完整信息
    UserAllInfoDTO getUserAllInfo(Long userId);
    
    // 更新用户基本信息
    void updateUserBaseInfo(Long userId, UserAllInfoDTO.UserBaseInfo baseInfo);
    
    // 更新用户定时任务列表
    void updateUserTasks(Long userId, List<UserAllInfoDTO.TaskWithUrls> tasks);
    
    // 更新特定任务的链接
    void updateTaskUrls(Long userId, Long taskId, List<MonitorUrlListDTO> urls);
    
    // 删除用户完整信息
    void deleteUserAllInfo(Long userId);
    
    // 向后兼容的方法（可选）
    @Deprecated
    void cacheMonitorTasksToRedis(Long userId);
    
    @Deprecated
    void cacheMonitorTaskUrlsToRedis(Long userId);
    
    @Deprecated
    List<MonitorTaskListDTO> getTasksByUserId(Long userId);
    
    @Deprecated
    List<MonitorUrlListDTO> findListDtoByUserId(Long userId);
}
