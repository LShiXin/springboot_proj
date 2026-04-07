package com.shixin.serviceimpl;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.shixin.entity.MonitorTask;
import com.shixin.entity.MonitorTaskListDTO;
import com.shixin.entity.MonitorUrl;
import com.shixin.entity.MonitorUrlListDTO;
import com.shixin.entity.Notification;
import com.shixin.entity.ScheduleType;
import com.shixin.entity.TaskScheduleConfig;
import com.shixin.entity.UserAllInfoDTO;
import com.shixin.repository.MonitorTaskRepository;
import com.shixin.repository.MonitorUrlRepository;
import com.shixin.repository.NotificationRepository;
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
    private NotificationRepository notificationRepository;

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
        // 由于任务存储在用户完整信息中，需要先获取任务以知道用户ID
        MonitorTask taskFromDb = monitorTaskRepository.findById(id).orElse(null);
        if (taskFromDb == null) {
            return null;
        }
        return taskFromDb;
    }

    /**
     * 计算定时任务的cron表达式
     * 
     * @param config 定时任务配置
     * @return cron表达式，如果无法计算则返回null
     */
    private String calculateCronExpression(TaskScheduleConfig config) {
        if (config == null || config.getScheduleType() == null) {
            return null;
        }

        switch (config.getScheduleType()) {
            case CRON:
                // 如果已经提供了cron表达式，直接使用
                return config.getCronExpression();

            case FIXED_RATE:
            case FIXED_DELAY:
                // 对于固定间隔的任务，根据intervalMillis生成cron表达式
                return generateCronFromInterval(config.getIntervalMillis());

            case ONCE:
                // 一次性任务不需要cron表达式
                return null;

            default:
                log.warn("未知的调度类型: {}", config.getScheduleType());
                return null;
        }
    }

    /**
     * 根据间隔毫秒数生成cron表达式
     * 
     * @param intervalMillis 间隔毫秒数
     * @return cron表达式
     */
    private String generateCronFromInterval(Long intervalMillis) {
        if (intervalMillis == null || intervalMillis <= 0) {
            log.warn("无效的间隔时间: {}", intervalMillis);
            return null;
        }

        // 将毫秒转换为秒
        long intervalSeconds = intervalMillis / 1000;

        if (intervalSeconds < 60) {
            // 小于1分钟，按秒执行：每intervalSeconds秒执行一次
            return String.format("*/%d * * * * *", intervalSeconds);
        } else if (intervalSeconds < 3600) {
            // 小于1小时，按分钟执行：每intervalSeconds/60分钟执行一次
            long intervalMinutes = intervalSeconds / 60;
            return String.format("0 */%d * * * *", intervalMinutes);
        } else if (intervalSeconds < 86400) {
            // 小于1天，按小时执行：每intervalSeconds/3600小时执行一次
            long intervalHours = intervalSeconds / 3600;
            return String.format("0 0 */%d * * *", intervalHours);
        } else {
            // 大于等于1天，按天执行：每intervalSeconds/86400天执行一次
            long intervalDays = intervalSeconds / 86400;
            return String.format("0 0 0 */%d * *", intervalDays);
        }
    }

    /**
     * 计算定时任务的下次执行时间
     * 
     * @param config 定时任务配置
     * @return 下次执行时间，如果无法计算则返回null
     */
    private LocalDateTime calculateNextFireTime(TaskScheduleConfig config) {
        if (config == null || config.getScheduleType() == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();

        switch (config.getScheduleType()) {
            case CRON:
                // 对于CRON表达式，需要解析表达式计算下次执行时间
                // 这里简化处理：如果已经有下次执行时间且在未来，则保持不变
                // 否则从当前时间开始计算
                if (config.getNextFireTime() != null && config.getNextFireTime().isAfter(now)) {
                    return config.getNextFireTime();
                }
                // 简化处理：设置为1小时后
                return now.plusHours(1);

            case FIXED_RATE:
                // 固定频率任务：从当前时间开始计算下次执行时间
                if (config.getIntervalMillis() != null && config.getIntervalMillis() > 0) {
                    return now.plusSeconds(config.getIntervalMillis() / 1000);
                }
                break;

            case FIXED_DELAY:
                // 固定延迟任务：从当前时间开始计算下次执行时间
                if (config.getIntervalMillis() != null && config.getIntervalMillis() > 0) {
                    return now.plusSeconds(config.getIntervalMillis() / 1000);
                }
                break;

            case ONCE:
                // 一次性任务：如果startTime不为空且在未来，则使用startTime
                // 否则使用当前时间
                if (config.getStartTime() != null && config.getStartTime().isAfter(now)) {
                    return config.getStartTime();
                }
                return now;

            default:
                log.warn("未知的调度类型: {}", config.getScheduleType());
                break;
        }

        return null;
    }

    /**
     * 处理定时任务配置：计算cron表达式和下次执行时间
     * 
     * @param task 监控任务
     */
    private void processScheduleConfig(MonitorTask task) {
        if (task == null) {
            return;
        }

        TaskScheduleConfig config = task.getScheduleConfig();
        if (config == null) {
            return;
        }

        // 计算cron表达式
        String cronExpression = calculateCronExpression(config);
        if (cronExpression != null) {
            config.setCronExpression(cronExpression);
            log.debug("计算得到cron表达式: {}", cronExpression);
        }

        // 计算下次执行时间
        LocalDateTime nextFireTime = calculateNextFireTime(config);
        if (nextFireTime != null) {
            config.setNextFireTime(nextFireTime);
            log.debug("计算得到下次执行时间: {}", nextFireTime);
        }

        // 如果是一次性任务且已经执行过，则禁用
        if (config.getScheduleType() == ScheduleType.ONCE &&
                config.getLastFireTime() != null) {
            config.setEnabled(false);
            log.debug("一次性任务已执行，已禁用");
        }
    }

    // 保存或更新任务
    @Override
    public MonitorTask saveOrUpdateTask(MonitorTask task) {
        // 处理定时任务配置：计算cron表达式和下次执行时间
        processScheduleConfig(task);

        MonitorTask result = monitorTaskRepository.save(task);
        return result;
    }

    // 根据用户ID查询所有监控任务，并转换为 MonitorTaskListDTO
    @Override
    public List<MonitorTaskListDTO> getTasksByUserId(Long userId) {

        log.debug("正在从数据库中查询监控任务...", userId);
        return monitorTaskRepository.findAllByUserId(userId);

    }

    @Override
    public List<MonitorUrlListDTO> findListDtoByUserId(Long userId) {

        log.debug("正在从数据库中查询： {} 的监控链接，...", userId);
        return monitorTaskRepository.findListDtoByUserId(userId);

    }

    // 删除定时任务（连锁删除：删除任务、监控子链接和相关通知）
    @Override
    public Boolean deleteTaskById(Long taskId, Long userId) {
        try {
            MonitorTask entity = monitorTaskRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("该用户下未发现定时任务"));
            
            if (entity != null && entity.getUser() != null && entity.getUser().getId().equals(userId)) {
                // 1. 先删除该任务下的所有通知（连锁删除）
                List<Notification> taskNotifications = notificationRepository.findByTaskId(taskId);
                int deletedNotifications = 0;
                for (Notification notification : taskNotifications) {
                    notificationRepository.delete(notification);
                    deletedNotifications++;
                }
                log.info("删除任务 {} 的相关通知，共删除 {} 条通知", taskId, deletedNotifications);
                
                // 2. 删除该任务下的所有监控子链接
                List<MonitorUrl> taskUrls = monitorUrlRepository.findByTaskId(taskId);
                int deletedUrls = 0;
                for (MonitorUrl url : taskUrls) {
                    monitorUrlRepository.delete(url);
                    deletedUrls++;
                }
                log.info("删除任务 {} 的监控子链接，共删除 {} 条链接", taskId, deletedUrls);
                
                // 3. 最后删除定时任务本身
                monitorTaskRepository.delete(entity);
                
                log.info("定时任务删除成功: taskId={}, userId={}, 删除通知={}条, 删除链接={}条", 
                        taskId, userId, deletedNotifications, deletedUrls);
                return true;
            } else {
                log.warn("用户 {} 无权限删除任务 {} 或任务不存在", userId, taskId);
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

            // 更新调度类型（如果需要）
            if (monitorTask.getScheduleConfig().getScheduleType() != null) {
                entity.getScheduleConfig().setScheduleType(monitorTask.getScheduleConfig().getScheduleType());
            }

            // 处理定时任务配置：计算cron表达式和下次执行时间
            processScheduleConfig(entity);
        }

        MonitorTask result = monitorTaskRepository.save(entity);
        return result;
    }

    // 获取该用户，对应的监控任务的全部监控信息
    @Override
    public List<MonitorUrl> findAllTaskUrlsByTaskId(Long TaskID) {
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

            log.info("链接状态切换成功: linkId={}, userId={}, 新状态={}",
                    linkId, userId, updatedLink.isEnabled());
            return updatedLink;
        } catch (Exception e) {
            log.error("切换链接状态失败, linkId: {}, userId: {}", linkId, userId, e);
            return null;
        }
    }
}
