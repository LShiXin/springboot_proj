package com.shixin.serviceimpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.shixin.entity.MonitorTask;
import com.shixin.entity.MonitorTaskListDTO;
import com.shixin.entity.MonitorUrl;
import com.shixin.entity.MonitorUrlListDTO;
import com.shixin.entity.User;
import com.shixin.entity.UserAllInfoDTO;
import com.shixin.repository.MonitorTaskRepository;
import com.shixin.repository.MonitorUrlRepository;
import com.shixin.repository.UserRepository;
import com.shixin.service.CacheService;
import com.shixin.service.RedisService;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Slf4j
public class CacheServiceImpl implements CacheService {
    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    @Autowired
    private MonitorTaskRepository monitorTaskRepository;
    
    @Autowired
    private MonitorUrlRepository monitorUrlRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisService redisService;

    @Override
    @Async
    public void cacheUserAllInfoToRedis(Long userId) {
        log.info("正在将用户 {} 的完整信息缓存到 Redis 中...", userId);
        
        // 获取用户基本信息
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.error("用户 {} 不存在，无法缓存信息", userId);
            return;
        }
        
        // 创建用户基本信息
        UserAllInfoDTO.UserBaseInfo baseInfo = new UserAllInfoDTO.UserBaseInfo(user);
        
        // 获取用户的所有监控任务（通过查询所有任务然后过滤）
        List<MonitorTask> allTasks = monitorTaskRepository.findAll();
        List<MonitorTask> userTasks = allTasks.stream()
            .filter(task -> task.getUser() != null && task.getUser().getId().equals(userId))
            .collect(Collectors.toList());
        
        List<UserAllInfoDTO.TaskWithUrls> taskWithUrlsList = new ArrayList<>();
        
        for (MonitorTask task : userTasks) {
            // 获取每个任务的链接（先获取MonitorUrl实体，然后转换为MonitorUrlListDTO）
            List<MonitorUrl> monitorUrls = monitorUrlRepository.findByTaskId(task.getId());
            List<MonitorUrlListDTO> urls = monitorUrls.stream()
                .map(url -> new MonitorUrlListDTO(url.getId(), task.getId(), url.getUrl(), url.isEnabled(), url.getRemark()))
                .collect(Collectors.toList());
            
            UserAllInfoDTO.TaskWithUrls taskWithUrls = new UserAllInfoDTO.TaskWithUrls(task, urls);
            taskWithUrlsList.add(taskWithUrls);
        }
        
        // 创建完整用户信息对象
        UserAllInfoDTO userAllInfo = new UserAllInfoDTO(baseInfo, taskWithUrlsList);
        
        // 保存到Redis，设置过期时间为10分钟
        redisService.saveUserAllInfo(userId, userAllInfo, 600);
        log.info("用户 {} 的完整信息已成功缓存到 Redis 中，过期时间为 10 分钟", userId);
    }
    
    @Override
    public UserAllInfoDTO getUserAllInfo(Long userId) {
        return redisService.getUserAllInfo(userId);
    }
    
    @Override
    public void updateUserBaseInfo(Long userId, UserAllInfoDTO.UserBaseInfo baseInfo) {
        redisService.updateUserBaseInfo(userId, baseInfo);
        log.info("已更新用户 {} 的基本信息", userId);
    }
    
    @Override
    public void updateUserTasks(Long userId, List<UserAllInfoDTO.TaskWithUrls> tasks) {
        redisService.updateUserTasks(userId, tasks);
        log.info("已更新用户 {} 的定时任务列表", userId);
    }
    
    @Override
    public void updateTaskUrls(Long userId, Long taskId, List<MonitorUrlListDTO> urls) {
        redisService.updateTaskUrls(userId, taskId, urls);
        log.info("已更新用户 {} 的任务 {} 的链接", userId, taskId);
    }
    
    @Override
    public void deleteUserAllInfo(Long userId) {
        redisService.deleteUserAllInfo(userId);
        log.info("已删除用户 {} 的完整信息", userId);
    }
    
    // 向后兼容的方法实现
    @Override
    @Async
    @Deprecated
    public void cacheMonitorTasksToRedis(Long userId) {
        log.warn("cacheMonitorTasksToRedis 方法已过时，请使用 cacheUserAllInfoToRedis");
        cacheUserAllInfoToRedis(userId);
    }
    
    @Override
    @Async
    @Deprecated
    public void cacheMonitorTaskUrlsToRedis(Long userId) {
        log.warn("cacheMonitorTaskUrlsToRedis 方法已过时，请使用 cacheUserAllInfoToRedis");
        cacheUserAllInfoToRedis(userId);
    }
    
    @Override
    @Deprecated
    public List<MonitorTaskListDTO> getTasksByUserId(Long userId) {
        // log.warn("getTasksByUserId 方法已过时，请使用 getUserAllInfo");
        UserAllInfoDTO userAllInfo = getUserAllInfo(userId);
        if (userAllInfo != null && userAllInfo.getTasks() != null) {
            // 转换 TaskWithUrls 到 MonitorTaskListDTO
            return userAllInfo.getTasks().stream()
                .map(task -> {
                    // 现在TaskWithUrls包含了定时任务调度配置信息
                    return new MonitorTaskListDTO(
                        task.getId(),
                        task.getName(),
                        task.getKeywords(),
                        task.isEnabled(),
                        task.getStartTime(), // startTime
                        task.getTimePoint(), // timePoint
                        task.getIntervalMinutes(), // intervalMinutes
                        task.getEndTime()  // endTime
                    );
                })
                .collect(Collectors.toList());
        }
        return null;
    }
    
    @Override
    @Deprecated
    public List<MonitorUrlListDTO> findListDtoByUserId(Long userId) {
        // log.warn("findListDtoByUserId 方法已过时，请使用 getUserAllInfo");
        UserAllInfoDTO userAllInfo = getUserAllInfo(userId);
        if (userAllInfo != null && userAllInfo.getTasks() != null) {
            List<MonitorUrlListDTO> allUrls = new ArrayList<>();
            for (UserAllInfoDTO.TaskWithUrls task : userAllInfo.getTasks()) {
                if (task.getUrls() != null) {
                    allUrls.addAll(task.getUrls());
                }
            }
            return allUrls;
        }
        return null;
    }
}
