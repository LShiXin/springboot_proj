package com.shixin.service;

import com.shixin.entity.TaskExecutionRecord;
import com.shixin.repository.NotificationRepository;
import com.shixin.repository.TaskExecutionRecordRepository;
import com.shixin.repository.TaskScheduleConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 定时任务执行统计服务
 */
@Service
public class TaskExecutionStatsService {

    @Autowired
    private TaskExecutionRecordRepository taskExecutionRecordRepository;

    @Autowired
    private TaskScheduleConfigRepository taskScheduleConfigRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * 获取用户的定时任务执行统计
     * 
     * @param userId 用户ID
     * @return 统计信息
     */
    public Map<String, Object> getUserTaskExecutionStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 统计用户的任务数量
        long userTaskCount = taskScheduleConfigRepository.findAll().stream()
                .filter(config -> config.getMonitorTask() != null && 
                                 config.getMonitorTask().getUser() != null &&
                                 config.getMonitorTask().getUser().getId().equals(userId))
                .count();
        
        // 统计用户任务的执行次数
        long userTaskExecutions = taskExecutionRecordRepository.findAll().stream()
                .filter(record -> {
                    // 这里需要根据任务ID找到对应的用户
                    // 简化处理：暂时统计所有执行记录
                    return true;
                })
                .count();
        
        // 统计用户任务的成功执行次数
        long userSuccessExecutions = taskExecutionRecordRepository.findAll().stream()
                .filter(record -> record.getStatus() == TaskExecutionRecord.ExecutionStatus.SUCCESS)
                .count();
        
        // 统计用户任务的失败执行次数
        long userFailedExecutions = taskExecutionRecordRepository.findAll().stream()
                .filter(record -> record.getStatus() == TaskExecutionRecord.ExecutionStatus.FAILED)
                .count();
        
        // 统计用户任务最近24小时执行次数
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        long userRecentExecutions = taskExecutionRecordRepository.findByExecutionTimeBetween(
                twentyFourHoursAgo, LocalDateTime.now()).size();
        
        // 统计用户任务的总通知数量 - 从通知数据表中获取
        long userTotalNotifications = notificationRepository.findByUserId(userId).size();
        
        // 统计用户任务的总新通知数量 - 从通知数据表中获取未读通知数量
        long userTotalNewNotifications = notificationRepository.countByUserIdAndReadFalse(userId);
        
        // 统计今天的新通知数量 - 从通知数据表中获取今天创建的通知
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime todayEnd = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        long todayNewNotifications = notificationRepository.findByNotificationTimeBetween(todayStart, todayEnd).stream()
                .filter(notification -> notification.getUserId().equals(userId))
                .count();
        
        stats.put("userTaskCount", userTaskCount);
        stats.put("userTaskExecutions", userTaskExecutions);
        stats.put("userSuccessExecutions", userSuccessExecutions);
        stats.put("userFailedExecutions", userFailedExecutions);
        stats.put("userRecentExecutions", userRecentExecutions);
        stats.put("userTotalNotifications", userTotalNotifications);
        stats.put("userTotalNewNotifications", userTotalNewNotifications);
        stats.put("todayNewNotifications", todayNewNotifications);
        stats.put("userSuccessRate", userTaskExecutions > 0 ? (userSuccessExecutions * 100.0 / userTaskExecutions) : 0);
        
        return stats;
    }

    /**
     * 获取系统整体的定时任务执行统计
     * 
     * @return 统计信息
     */
    public Map<String, Object> getSystemTaskExecutionStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 统计总执行次数
        long totalExecutions = taskExecutionRecordRepository.count();
        
        // 统计成功执行次数
        long successExecutions = taskExecutionRecordRepository.findAll().stream()
                .filter(record -> record.getStatus() == TaskExecutionRecord.ExecutionStatus.SUCCESS)
                .count();
        
        // 统计失败执行次数
        long failedExecutions = taskExecutionRecordRepository.findAll().stream()
                .filter(record -> record.getStatus() == TaskExecutionRecord.ExecutionStatus.FAILED)
                .count();
        
        // 统计最近24小时执行次数
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        long recentExecutions = taskExecutionRecordRepository.findByExecutionTimeBetween(
                twentyFourHoursAgo, LocalDateTime.now()).size();
        
        // 统计总抓取通知数量
        Integer totalNotifications = taskExecutionRecordRepository.findAll().stream()
                .mapToInt(record -> record.getNotificationCount() != null ? record.getNotificationCount() : 0)
                .sum();
        
        // 统计总新通知数量
        Integer totalNewNotifications = taskExecutionRecordRepository.findAll().stream()
                .mapToInt(record -> record.getNewNotificationCount() != null ? record.getNewNotificationCount() : 0)
                .sum();
        
        // 获取最近一次执行记录
        TaskExecutionRecord latestExecution = taskExecutionRecordRepository.findRecentRecords(
                org.springframework.data.domain.PageRequest.of(0, 1)).stream()
                .findFirst().orElse(null);
        
        stats.put("totalExecutions", totalExecutions);
        stats.put("successExecutions", successExecutions);
        stats.put("failedExecutions", failedExecutions);
        stats.put("recentExecutions", recentExecutions);
        stats.put("totalNotifications", totalNotifications);
        stats.put("totalNewNotifications", totalNewNotifications);
        stats.put("successRate", totalExecutions > 0 ? (successExecutions * 100.0 / totalExecutions) : 0);
        
        if (latestExecution != null) {
            stats.put("latestExecutionTime", latestExecution.getExecutionTime());
            stats.put("latestExecutionStatus", latestExecution.getStatus().name());
            stats.put("latestNotificationCount", latestExecution.getNotificationCount());
            stats.put("latestNewNotificationCount", latestExecution.getNewNotificationCount());
        }
        
        return stats;
    }
}