package com.shixin.util;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.shixin.entity.MonitorTask;
import com.shixin.entity.MonitorUrlListDTO;
import com.shixin.entity.User;
import com.shixin.entity.UserAllInfoDTO;
import com.shixin.repository.MonitorTaskRepository;
import com.shixin.repository.MonitorUrlRepository;
import com.shixin.service.CacheService;

/**
 * Redis更新工具类，提供便捷的局部更新方法
 */
@Component
public class RedisUpdateUtil {
    
    @Autowired
    private CacheService cacheService;
    
    @Autowired
    private MonitorTaskRepository monitorTaskRepository;
    
    @Autowired
    private MonitorUrlRepository monitorUrlRepository;
    
    /**
     * 更新用户基本信息
     * @param user 用户对象
     */
    public void updateUserBaseInfo(User user) {
        if (user == null || user.getId() == null) {
            return;
        }
        
        UserAllInfoDTO.UserBaseInfo baseInfo = new UserAllInfoDTO.UserBaseInfo(user);
        cacheService.updateUserBaseInfo(user.getId(), baseInfo);
    }
    
    /**
     * 更新用户的所有定时任务
     * @param userId 用户ID
     */
    public void updateUserTasks(Long userId) {
        // 获取用户的所有监控任务
        List<MonitorTask> tasks = monitorTaskRepository.findAll().stream()
            .filter(task -> task.getUser() != null && task.getUser().getId().equals(userId))
            .collect(Collectors.toList());
        
        List<UserAllInfoDTO.TaskWithUrls> taskWithUrlsList = tasks.stream()
            .map(task -> {
                // 获取每个任务的链接
                List<MonitorUrlListDTO> urls = monitorUrlRepository.findByTaskId(task.getId()).stream()
                    .map(url -> new MonitorUrlListDTO(url.getId(), task.getId(), url.getUrl(), url.isEnabled(), url.getRemark()))
                    .collect(Collectors.toList());
                return new UserAllInfoDTO.TaskWithUrls(task, urls);
            })
            .collect(Collectors.toList());
        
        cacheService.updateUserTasks(userId, taskWithUrlsList);
    }
    
    /**
     * 更新特定任务的链接
     * @param userId 用户ID
     * @param taskId 任务ID
     */
    public void updateTaskUrls(Long userId, Long taskId) {
        List<MonitorUrlListDTO> urls = monitorUrlRepository.findByTaskId(taskId).stream()
            .map(url -> new MonitorUrlListDTO(url.getId(), taskId, url.getUrl(), url.isEnabled(), url.getRemark()))
            .collect(Collectors.toList());
        
        cacheService.updateTaskUrls(userId, taskId, urls);
    }
    
    /**
     * 添加或更新单个任务
     * @param task 任务对象
     */
    public void updateSingleTask(MonitorTask task) {
        if (task == null || task.getUser() == null) {
            return;
        }
        
        Long userId = task.getUser().getId();
        // 先获取当前的完整信息
        UserAllInfoDTO userAllInfo = cacheService.getUserAllInfo(userId);
        
        if (userAllInfo != null && userAllInfo.getTasks() != null) {
            // 查找是否已存在该任务
            List<UserAllInfoDTO.TaskWithUrls> tasks = userAllInfo.getTasks();
            boolean found = false;
            
            for (int i = 0; i < tasks.size(); i++) {
                if (tasks.get(i).getId().equals(task.getId())) {
                    // 更新现有任务
                    List<MonitorUrlListDTO> urls = monitorUrlRepository.findByTaskId(task.getId()).stream()
                        .map(url -> new MonitorUrlListDTO(url.getId(), task.getId(), url.getUrl(), url.isEnabled(), url.getRemark()))
                        .collect(Collectors.toList());
                    tasks.set(i, new UserAllInfoDTO.TaskWithUrls(task, urls));
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                // 添加新任务
                List<MonitorUrlListDTO> urls = monitorUrlRepository.findByTaskId(task.getId()).stream()
                    .map(url -> new MonitorUrlListDTO(url.getId(), task.getId(), url.getUrl(), url.isEnabled(), url.getRemark()))
                    .collect(Collectors.toList());
                tasks.add(new UserAllInfoDTO.TaskWithUrls(task, urls));
            }
            
            cacheService.updateUserTasks(userId, tasks);
        } else {
            // 如果Redis中没有缓存，则重新缓存完整信息
            cacheService.cacheUserAllInfoToRedis(userId);
        }
    }
    
    /**
     * 删除单个任务
     * @param userId 用户ID
     * @param taskId 任务ID
     */
    public void deleteSingleTask(Long userId, Long taskId) {
        UserAllInfoDTO userAllInfo = cacheService.getUserAllInfo(userId);
        
        if (userAllInfo != null && userAllInfo.getTasks() != null) {
            List<UserAllInfoDTO.TaskWithUrls> tasks = userAllInfo.getTasks().stream()
                .filter(task -> !task.getId().equals(taskId))
                .collect(Collectors.toList());
            
            cacheService.updateUserTasks(userId, tasks);
        }
    }
    
    /**
     * 刷新用户完整信息（完全重新加载）
     * @param userId 用户ID
     */
    public void refreshUserAllInfo(Long userId) {
        cacheService.cacheUserAllInfoToRedis(userId);
    }
}