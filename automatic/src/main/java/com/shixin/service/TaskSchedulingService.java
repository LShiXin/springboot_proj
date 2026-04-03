package com.shixin.service;

import com.shixin.entity.TaskScheduleConfig;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务调度服务接口
 */
public interface TaskSchedulingService {

    /**
     * 扫描所有定时任务，加载符合条件的任务
     * @return 符合条件的定时任务配置列表
     */
    List<TaskScheduleConfig> scanAndLoadTasks();

    /**
     * 根据执行间隔更新cron表达式
     * @param config 定时任务配置
     * @return 更新后的cron表达式
     */
    String updateCronExpression(TaskScheduleConfig config);

    /**
     * 更新下次执行时间
     * @param config 定时任务配置
     * @return 更新后的下次执行时间
     */
    LocalDateTime updateNextFireTime(TaskScheduleConfig config);

    /**
     * 根据执行时间创建执行队列
     * @param configs 定时任务配置列表
     */
    void createExecutionQueue(List<TaskScheduleConfig> configs);

    /**
     * 执行定时任务扫描和处理（5分钟执行一次）
     */
    void executeScheduledScan();

    /**
     * 启动定时任务调度服务
     */
    void startSchedulingService();

    /**
     * 停止定时任务调度服务
     */
    void stopSchedulingService();

    /**
     * 手动触发定时任务扫描
     */
    void triggerManualScan();
}
