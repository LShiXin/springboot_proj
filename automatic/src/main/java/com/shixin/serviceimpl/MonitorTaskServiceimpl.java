package com.shixin.serviceimpl;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.shixin.entity.MonitorTask;
import com.shixin.entity.MonitorTaskListDTO;
import com.shixin.entity.MonitorUrl;
import com.shixin.entity.MonitorUrlListDTO;
import com.shixin.entity.UserAllInfoDTO;
import com.shixin.repository.MonitorTaskRepository;
import com.shixin.repository.MonitorUrlRepository;
import com.shixin.service.CacheService;
import com.shixin.service.MonitorTaskService;

import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class MonitorTaskServiceimpl implements MonitorTaskService {
    private static final Logger log = LoggerFactory.getLogger(MonitorTaskServiceimpl.class);

    @Autowired
    private MonitorTaskRepository monitorTaskRepository;

    @Autowired
    private MonitorUrlRepository monitorUrlRepository;

    @Autowired
    private CacheService cacheService;

    // 获取所有监控任务
    @Override
    public List<MonitorTask> getAllTasks() {
        // 暂时不实现Redis缓存，因为这是管理员功能且数据量可能较大
        // 可以考虑未来实现分页缓存或按用户分组缓存
        return monitorTaskRepository.findAll();
    }

    // 根据ID查询任务，获取该用户下的所有ID
    @Override
    public MonitorTask getTaskById(Long id) {
        // 先尝试从Redis中获取
        // 由于任务存储在用户完整信息中，需要先获取任务以知道用户ID
        MonitorTask taskFromDb = monitorTaskRepository.findById(id).orElse(null);
        if (taskFromDb == null) {
            return null;
        }
        
        // 如果任务有用户关联，尝试从Redis获取用户完整信息
        if (taskFromDb.getUser() != null) {
            UserAllInfoDTO userAllInfo = cacheService.getUserAllInfo(taskFromDb.getUser().getId());
            if (userAllInfo != null && userAllInfo.getTasks() != null) {
                // 从缓存的任务列表中查找该任务
                for (UserAllInfoDTO.TaskWithUrls cachedTask : userAllInfo.getTasks()) {
                    if (cachedTask.getId().equals(id)) {
                        // 将TaskWithUrls转换为MonitorTask
                        MonitorTask task = new MonitorTask();
                        task.setId(cachedTask.getId());
                        task.setName(cachedTask.getName());
                        task.setKeywords(cachedTask.getKeywords());
                        task.setEnabled(cachedTask.isEnabled());
                        // 注意：scheduleConfig等信息在TaskWithUrls中没有，所以需要从数据库获取
                        // 这里返回数据库中的完整任务对象
                        log.debug("从Redis缓存中找到任务ID: {}", id);
                        return taskFromDb;
                    }
                }
            }
        }
        
        return taskFromDb;
    }

    // 保存或更新任务
    @Override
    public MonitorTask saveOrUpdateTask(MonitorTask task) {
        MonitorTask result = monitorTaskRepository.save(task);
        if (result != null && task.getUser() != null) {
            // 重新缓存用户的完整信息到Redis
            cacheService.cacheUserAllInfoToRedis(task.getUser().getId());
        }
        return result;
    }

    // 根据用户ID查询所有监控任务，并转换为 MonitorTaskListDTO
    @Override
    public List<MonitorTaskListDTO> getTasksByUserId(Long userId) {
        // 先尝试从 Redis 中获取数据，如果没有再从数据库中查询
        List<MonitorTaskListDTO> cachedTasks = cacheService.getTasksByUserId(userId);
        if (cachedTasks != null) {
            log.debug("从 Redis 中获取用户 {} 的监控任务", userId);
            return cachedTasks;
        } else {
            log.debug("Redis 中没有用户 {} 的监控任务，正在从数据库中查询...", userId);
            return monitorTaskRepository.findAllByUserId(userId);
        }
    }

    @Override
    public List<MonitorUrlListDTO> findListDtoByUserId(Long userId) {
        // 先尝试从 Redis 中获取数据，如果没有再从数据库中查询
        List<MonitorUrlListDTO> cachedUrls = cacheService.findListDtoByUserId(userId);
        if (cachedUrls != null) {
            log.debug("从 Redis 中获取用户 {} 的监控链接", userId);
            return cachedUrls;
        } else {
            log.debug("Redis 中没有用户 {} 的监控链接，正在从数据库中查询...", userId);
            return monitorTaskRepository.findListDtoByUserId(userId);
        }
    }

    // 删除定时任务
    @Override
    public Boolean deleteTaskById(Long taskId, Long userId) {
        try {
            MonitorTask entity = monitorTaskRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("该用户下未发现定时任务"));
            if (entity != null && entity.getUser() != null && entity.getUser().getId().equals(userId)) {
                monitorTaskRepository.delete(entity);
                // 重新缓存用户的完整信息到Redis
                cacheService.cacheUserAllInfoToRedis(userId);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            log.error("删除定时任务失败, taskId: {}, userId: {}", taskId, userId, e);
            return false;
        }
    }

    @Override
    public MonitorTask updateTaskByUser(MonitorTask monitorTask, Long userId) {
        if (monitorTask == null || monitorTask.getId() == null) {
            throw new RuntimeException("任务参数不正确");
        }
        MonitorTask entity = monitorTaskRepository.findById(monitorTask.getId())
                .orElseThrow(() -> new RuntimeException("任务不存在"));
        if (entity.getUser() == null || !entity.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权限编辑该任务");
        }

        entity.setName(monitorTask.getName());
        entity.setKeywords(monitorTask.getKeywords());

        if (entity.getScheduleConfig() != null && monitorTask.getScheduleConfig() != null) {
            entity.getScheduleConfig().setStartTime(monitorTask.getScheduleConfig().getStartTime());
            entity.getScheduleConfig().setEndTime(monitorTask.getScheduleConfig().getEndTime());
            entity.getScheduleConfig().setIntervalMillis(monitorTask.getScheduleConfig().getIntervalMillis());
        }

        MonitorTask result = monitorTaskRepository.save(entity);
        // 重新缓存用户的完整信息到Redis
        cacheService.cacheUserAllInfoToRedis(userId);
        return result;
    }


    // 获取该用户，对应的监控任务的全部监控信息
    @Override
    public List<MonitorUrl> findAllTaskUrlsByTaskId(Long TaskID){
        // 先尝试从Redis中获取
        // 需要先获取任务以知道用户ID
        MonitorTask task = monitorTaskRepository.findById(TaskID).orElse(null);
        if (task == null) {
            return monitorUrlRepository.findByTaskId(TaskID);
        }
        
        // 如果任务有用户关联，尝试从Redis获取用户完整信息
        if (task.getUser() != null) {
            UserAllInfoDTO userAllInfo = cacheService.getUserAllInfo(task.getUser().getId());
            if (userAllInfo != null && userAllInfo.getTasks() != null) {
                // 从缓存的任务列表中查找该任务
                for (UserAllInfoDTO.TaskWithUrls cachedTask : userAllInfo.getTasks()) {
                    if (cachedTask.getId().equals(TaskID) && cachedTask.getUrls() != null) {
                        log.debug("从Redis缓存中找到任务ID: {} 的监控链接", TaskID);
                        // 将MonitorUrlListDTO转换为MonitorUrl
                        List<MonitorUrl> urls = new java.util.ArrayList<>();
                        for (MonitorUrlListDTO urlDto : cachedTask.getUrls()) {
                            MonitorUrl url = new MonitorUrl();
                            url.setId(urlDto.getId());
                            url.setUrl(urlDto.getUrl());
                            url.setEnabled(urlDto.isEnabled());
                            url.setRemark(urlDto.getRemark());
                            // 设置任务关联
                            url.setTask(task);
                            urls.add(url);
                        }
                        return urls;
                    }
                }
            }
        }
        
        // 如果Redis中没有，从数据库查询
        return monitorUrlRepository.findByTaskId(TaskID);
    }

    // 保存监控链接
    public MonitorUrl save_MonitorUrl(MonitorUrl url) {
        return monitorUrlRepository.save(url);
    }
    
    // 删除监控链接
    @Override
    public Boolean deleteLinkById(Long linkId, Long userId) {
        try {
            MonitorUrl link = monitorUrlRepository.findById(linkId)
                    .orElseThrow(() -> new RuntimeException("链接不存在"));
            
            // 验证链接所属的任务是否属于当前用户
            if (link.getTask() == null || link.getTask().getUser() == null || 
                !link.getTask().getUser().getId().equals(userId)) {
                log.error("用户 {} 无权限删除链接 {}", userId, linkId);
                return false;
            }
            
            monitorUrlRepository.delete(link);
            
            // 重新缓存用户的完整信息到Redis
            cacheService.cacheUserAllInfoToRedis(userId);
            
            log.info("链接删除成功: linkId={}, userId={}", linkId, userId);
            return true;
        } catch (Exception e) {
            log.error("删除链接失败, linkId: {}, userId: {}", linkId, userId, e);
            return false;
        }
    }
    
    // 切换链接状态
    @Override
    public MonitorUrl toggleLinkStatus(Long linkId, Long userId) {
        try {
            MonitorUrl link = monitorUrlRepository.findById(linkId)
                    .orElseThrow(() -> new RuntimeException("链接不存在"));
            
            // 验证链接所属的任务是否属于当前用户
            if (link.getTask() == null || link.getTask().getUser() == null || 
                !link.getTask().getUser().getId().equals(userId)) {
                log.error("用户 {} 无权限切换链接状态 {}", userId, linkId);
                return null;
            }
            
            // 切换链接状态
            link.setEnabled(!link.isEnabled());
            MonitorUrl updatedLink = monitorUrlRepository.save(link);
            
            // 重新缓存用户的完整信息到Redis
            cacheService.cacheUserAllInfoToRedis(userId);
            
            log.info("链接状态切换成功: linkId={}, userId={}, 新状态={}", 
                    linkId, userId, updatedLink.isEnabled());
            return updatedLink;
        } catch (Exception e) {
            log.error("切换链接状态失败, linkId: {}, userId: {}", linkId, userId, e);
            return null;
        }
    }
}
