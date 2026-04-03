package com.shixin.serviceimpl;

import com.shixin.entity.ScheduleType;
import com.shixin.entity.TaskScheduleConfig;
import com.shixin.repository.TaskScheduleConfigRepository;
import com.shixin.service.RedisService;
import com.shixin.service.TaskSchedulingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务调度服务实现类
 */
@Service
@Transactional
public class TaskSchedulingServiceImpl implements TaskSchedulingService {

    private static final Logger log = LoggerFactory.getLogger(TaskSchedulingServiceImpl.class);
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final int SCAN_INTERVAL_MINUTES = 5;
    
    private ScheduledExecutorService scheduler;
    private volatile boolean isRunning = false;

    @Autowired
    private TaskScheduleConfigRepository taskScheduleConfigRepository;
    
    @Autowired
    private RedisService redisService;

    @Override
    public List<TaskScheduleConfig> scanAndLoadTasks() {
        LocalDateTime now = LocalDateTime.now();
        log.info("开始扫描定时任务，当前时间: {}", now);
        
        // 查询所有启用的定时任务配置
        List<TaskScheduleConfig> activeConfigs = taskScheduleConfigRepository.findActiveSchedules(now);
        
        log.info("找到 {} 个活跃的定时任务配置", activeConfigs.size());
        return activeConfigs;
    }

    @Override
    public String updateCronExpression(TaskScheduleConfig config) {
        if (config == null) {
            return null;
        }

        ScheduleType scheduleType = config.getScheduleType();
        String newCronExpression = null;

        switch (scheduleType) {
            case CRON:
                // 如果已经是CRON类型，保持原样
                newCronExpression = config.getCronExpression();
                break;
                
            case FIXED_RATE:
            case FIXED_DELAY:
                // 根据间隔时间生成cron表达式
                newCronExpression = generateCronFromInterval(config);
                break;
                
            case ONCE:
                // 一次性任务，生成特定的cron表达式
                newCronExpression = generateCronForOneTimeTask(config);
                break;
                
            default:
                log.warn("未知的调度类型: {}", scheduleType);
                break;
        }

        if (newCronExpression != null && !newCronExpression.equals(config.getCronExpression())) {
            config.setCronExpression(newCronExpression);
            log.info("更新任务 {} 的cron表达式为: {}", config.getId(), newCronExpression);
        }

        return newCronExpression;
    }

    @Override
    public LocalDateTime updateNextFireTime(TaskScheduleConfig config) {
        if (config == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextFireTime = null;

        // 根据调度类型计算下次执行时间
        switch (config.getScheduleType()) {
            case CRON:
                // 对于CRON表达式，需要解析并计算下次执行时间
                nextFireTime = calculateNextFireTimeFromCron(config, now);
                break;
                
            case FIXED_RATE:
                // 固定频率：从上一次执行时间开始计算
                nextFireTime = calculateNextFireTimeForFixedRate(config, now);
                break;
                
            case FIXED_DELAY:
                // 固定延迟：从当前时间开始计算
                nextFireTime = calculateNextFireTimeForFixedDelay(config, now);
                break;
                
            case ONCE:
                // 一次性任务：如果还没执行过，使用开始时间
                if (config.getLastFireTime() == null && config.getStartTime() != null) {
                    nextFireTime = config.getStartTime();
                } else {
                    nextFireTime = null; // 已经执行过的一次性任务不再执行
                }
                break;
        }

        // 检查是否在有效时间范围内
        if (nextFireTime != null) {
            if (config.getStartTime() != null && nextFireTime.isBefore(config.getStartTime())) {
                nextFireTime = config.getStartTime();
            }
            
            if (config.getEndTime() != null && nextFireTime.isAfter(config.getEndTime())) {
                nextFireTime = null; // 超过结束时间，不再执行
            }
        }

        if (nextFireTime != null && !nextFireTime.equals(config.getNextFireTime())) {
            config.setNextFireTime(nextFireTime);
            log.info("更新任务 {} 的下次执行时间为: {}", config.getId(), nextFireTime);
        }

        return nextFireTime;
    }

    @Override
    public void createExecutionQueue(List<TaskScheduleConfig> configs) {
        if (configs == null || configs.isEmpty()) {
            log.info("没有需要创建执行队列的任务");
            // 清空Redis中的执行队列
            redisService.clearExecutionQueue();
            return;
        }

        log.info("开始创建执行队列，共 {} 个任务", configs.size());
        
        // 按下次执行时间排序（最早执行的任务在前）
        configs.sort((c1, c2) -> {
            LocalDateTime t1 = c1.getNextFireTime();
            LocalDateTime t2 = c2.getNextFireTime();
            
            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return 1;
            if (t2 == null) return -1;
            
            return t1.compareTo(t2);
        });

        // 创建执行队列
        List<TaskScheduleConfig> executionQueue = new ArrayList<>();
        for (TaskScheduleConfig config : configs) {
            if (config.getNextFireTime() != null) {
                executionQueue.add(config);
                log.info("任务 {} 加入执行队列，计划执行时间: {}, cron表达式: {}", 
                        config.getId(), config.getNextFireTime(), config.getCronExpression());
            }
        }
        
        log.info("执行队列创建完成，共 {} 个任务加入队列", executionQueue.size());
        
        // 将执行队列保存到Redis中
        if (!executionQueue.isEmpty()) {
            try {
                // 设置队列过期时间为扫描间隔的2倍（10分钟），确保队列不会过期太快
                long timeout = SCAN_INTERVAL_MINUTES * 2 * 60; // 转换为秒
                redisService.saveExecutionQueue(executionQueue, timeout);
                log.info("执行队列已成功保存到Redis，过期时间: {} 秒", timeout);
                
                // 验证队列是否保存成功（可选）
                List<TaskScheduleConfig> savedQueue = redisService.getExecutionQueue();
                if (savedQueue != null && savedQueue.size() == executionQueue.size()) {
                    log.debug("Redis队列验证成功，保存了 {} 个任务", savedQueue.size());
                } else {
                    log.warn("Redis队列验证失败，预期 {} 个任务，实际保存 {} 个任务", 
                            executionQueue.size(), savedQueue != null ? savedQueue.size() : 0);
                }
            } catch (Exception e) {
                log.error("保存执行队列到Redis失败", e);
                // 即使Redis保存失败，也不影响后续逻辑，但需要记录错误
            }
        } else {
            // 如果没有任务需要执行，清空Redis中的队列
            redisService.clearExecutionQueue();
            log.debug("没有需要执行的任务，已清空Redis中的执行队列");
        }
    }

    @Override
    @Scheduled(fixedDelay = SCAN_INTERVAL_MINUTES * 60 * 1000) // 5分钟执行一次
    public void executeScheduledScan() {
        if (!isRunning) {
            log.debug("调度服务未运行，跳过本次扫描");
            return;
        }

        try {
            log.info("开始执行定时任务扫描...");
            
            // 1. 扫描并加载任务
            List<TaskScheduleConfig> tasks = scanAndLoadTasks();
            
            // 2. 更新每个任务的cron表达式和下次执行时间
            List<TaskScheduleConfig> updatedTasks = new ArrayList<>();
            for (TaskScheduleConfig task : tasks) {
                String oldCron = task.getCronExpression();
                LocalDateTime oldNextFireTime = task.getNextFireTime();
                
                updateCronExpression(task);
                updateNextFireTime(task);
                
                // 检查是否有更新
                boolean hasUpdates = !Objects.equals(oldCron, task.getCronExpression()) ||
                                    !Objects.equals(oldNextFireTime, task.getNextFireTime());
                
                if (hasUpdates) {
                    updatedTasks.add(task);
                    log.info("任务 {} 需要更新数据库: cron_expression={} -> {}, next_fire_time={} -> {}", 
                            task.getId(), oldCron, task.getCronExpression(), 
                            oldNextFireTime, task.getNextFireTime());
                }
            }
            
            // 3. 创建执行队列
            createExecutionQueue(tasks);
            
            // 4. 执行队列创建完毕后，更新数据库中的cron_expression、next_fire_time
            if (!updatedTasks.isEmpty()) {
                log.info("开始更新数据库，共 {} 个任务需要更新", updatedTasks.size());
                for (TaskScheduleConfig task : updatedTasks) {
                    try {
                        taskScheduleConfigRepository.save(task);
                        log.debug("任务 {} 的cron_expression和next_fire_time已更新到数据库", task.getId());
                    } catch (Exception e) {
                        log.error("更新任务 {} 到数据库失败", task.getId(), e);
                    }
                }
                log.info("数据库更新完成");
            } else {
                log.info("没有需要更新数据库的任务");
            }
            
            log.info("定时任务扫描完成，处理了 {} 个任务，更新了 {} 个任务到数据库", 
                    tasks.size(), updatedTasks.size());
            
        } catch (Exception e) {
            log.error("执行定时任务扫描时发生错误", e);
        }
    }

    @Override
    public void startSchedulingService() {
        if (isRunning) {
            log.warn("调度服务已经在运行中");
            return;
        }

        log.info("启动定时任务调度服务...");
        isRunning = true;
        
        // 启动时立即执行一次扫描
        executeScheduledScan();
        
        log.info("定时任务调度服务已启动，将每 {} 分钟扫描一次", SCAN_INTERVAL_MINUTES);
    }

    @Override
    public void stopSchedulingService() {
        if (!isRunning) {
            log.warn("调度服务已经停止");
            return;
        }

        log.info("停止定时任务调度服务...");
        isRunning = false;
        
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        log.info("定时任务调度服务已停止");
    }

    @Override
    public void triggerManualScan() {
        log.info("开始手动触发定时任务扫描...");
        
        try {
            // 1. 扫描并加载任务
            List<TaskScheduleConfig> tasks = scanAndLoadTasks();
            
            if (tasks.isEmpty()) {
                log.info("手动扫描完成：没有找到符合条件的定时任务");
                return;
            }
            
            log.info("手动扫描找到 {} 个符合条件的定时任务", tasks.size());
            
            // 2. 更新每个任务的cron表达式和下次执行时间
            List<TaskScheduleConfig> updatedTasks = new ArrayList<>();
            for (TaskScheduleConfig task : tasks) {
                String oldCron = task.getCronExpression();
                LocalDateTime oldNextFireTime = task.getNextFireTime();
                
                updateCronExpression(task);
                updateNextFireTime(task);
                
                // 检查是否有更新
                boolean hasUpdates = !Objects.equals(oldCron, task.getCronExpression()) ||
                                    !Objects.equals(oldNextFireTime, task.getNextFireTime());
                
                if (hasUpdates) {
                    updatedTasks.add(task);
                    log.info("任务 {} 需要更新数据库: cron_expression={} -> {}, next_fire_time={} -> {}", 
                            task.getId(), oldCron, task.getCronExpression(), 
                            oldNextFireTime, task.getNextFireTime());
                }
            }
            
            // 3. 创建执行队列
            createExecutionQueue(tasks);
            
            // 4. 执行队列创建完毕后，更新数据库中的cron_expression、next_fire_time
            if (!updatedTasks.isEmpty()) {
                log.info("开始更新数据库，共 {} 个任务需要更新", updatedTasks.size());
                for (TaskScheduleConfig task : updatedTasks) {
                    try {
                        taskScheduleConfigRepository.save(task);
                        log.debug("任务 {} 的cron_expression和next_fire_time已更新到数据库", task.getId());
                    } catch (Exception e) {
                        log.error("更新任务 {} 到数据库失败", task.getId(), e);
                    }
                }
                log.info("数据库更新完成");
            } else {
                log.info("没有需要更新数据库的任务");
            }
            
            log.info("手动定时任务扫描完成，处理了 {} 个任务，更新了 {} 个任务到数据库", 
                    tasks.size(), updatedTasks.size());
            
        } catch (Exception e) {
            log.error("手动触发定时任务扫描时发生错误", e);
            throw new RuntimeException("手动扫描失败: " + e.getMessage(), e);
        }
    }

    // ==================== 私有辅助方法 ====================

    private String generateCronFromInterval(TaskScheduleConfig config) {
        Long intervalMillis = config.getIntervalMillis();
        if (intervalMillis == null || intervalMillis <= 0) {
            log.warn("任务 {} 的间隔时间为空或无效: {}", config.getId(), intervalMillis);
            return null;
        }

        // 将毫秒转换为分钟
        long intervalMinutes = intervalMillis / (60 * 1000);
        
        if (intervalMinutes <= 0) {
            intervalMinutes = 1; // 最小间隔1分钟
        }

        // 生成cron表达式：每 intervalMinutes 分钟执行一次
        // 格式：秒 分 时 日 月 周
        return String.format("0 */%d * * * *", intervalMinutes);
    }

    private String generateCronForOneTimeTask(TaskScheduleConfig config) {
        LocalDateTime startTime = config.getStartTime();
        if (startTime == null) {
            log.warn("一次性任务 {} 没有设置开始时间", config.getId());
            return null;
        }

        // 生成特定时间的cron表达式
        // 格式：秒 分 时 日 月 周
        return String.format("0 %d %d %d %d ?",
                startTime.getMinute(),
                startTime.getHour(),
                startTime.getDayOfMonth(),
                startTime.getMonthValue());
    }

    private LocalDateTime calculateNextFireTimeFromCron(TaskScheduleConfig config, LocalDateTime now) {
        // 简化实现：假设cron表达式是简单的分钟级间隔
        // 实际项目中可以使用CronExpression解析器
        String cron = config.getCronExpression();
        if (cron == null || cron.isEmpty()) {
            return null;
        }

        // 简单解析：格式 "0 */X * * * *" 表示每X分钟执行一次
        try {
            if (cron.startsWith("0 */") && cron.contains("* * * *")) {
                String[] parts = cron.split(" ");
                if (parts.length >= 2) {
                    String minutePart = parts[1];
                    if (minutePart.startsWith("*/")) {
                        int interval = Integer.parseInt(minutePart.substring(2));
                        LocalDateTime lastFireTime = config.getLastFireTime();
                        
                        if (lastFireTime == null) {
                            // 第一次执行，从当前时间的下一分钟开始
                            return now.plusMinutes(1).withSecond(0).withNano(0);
                        } else {
                            // 从上一次执行时间开始计算
                            return lastFireTime.plusMinutes(interval);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析cron表达式失败: {}", cron, e);
        }

        // 默认返回当前时间加1小时
        return now.plusHours(1);
    }

    private LocalDateTime calculateNextFireTimeForFixedRate(TaskScheduleConfig config, LocalDateTime now) {
        Long intervalMillis = config.getIntervalMillis();
        if (intervalMillis == null || intervalMillis <= 0) {
            return null;
        }

        // 将毫秒转换为分钟
        long intervalMinutes = intervalMillis / (60 * 1000);
        if (intervalMinutes <= 0) {
            intervalMinutes = 1; // 最小间隔1分钟
        }

        LocalDateTime startTime = config.getStartTime();
        LocalDateTime lastFireTime = config.getLastFireTime();
        
        // 如果没有开始时间，使用当前时间作为参考
        if (startTime == null) {
            startTime = now.withSecond(0).withNano(0);
        }
        
        // 计算基于开始时间的下一个执行时间
        return calculateNextFireTimeBasedOnStartTime(startTime, lastFireTime, now, intervalMinutes);
    }
    
    /**
     * 计算基于开始时间的下一个执行时间
     * 算法：
     * 1. 如果上次执行时间为空，从开始时间计算下一个执行时间点
     * 2. 如果上次执行时间不为空，从上一次执行时间计算下一个执行时间点
     * 3. 如果计算出的时间已经过去，继续找下一个时间点
     * 
     * @param startTime 开始时间
     * @param lastFireTime 上次执行时间
     * @param now 当前扫描时间
     * @param intervalMinutes 间隔分钟数
     * @return 下一个执行时间
     */
    private LocalDateTime calculateNextFireTimeBasedOnStartTime(
            LocalDateTime startTime, 
            LocalDateTime lastFireTime, 
            LocalDateTime now, 
            long intervalMinutes) {
        
        if (intervalMinutes <= 0) {
            return null;
        }
        
        // 场景1：如果上次执行时间为空，从开始时间计算
        if (lastFireTime == null) {
            // 计算从开始时间到现在的分钟数
            long minutesFromStart = java.time.Duration.between(startTime, now).toMinutes();
            
            // 计算已经过去了多少个间隔
            long intervalsPassed = minutesFromStart / intervalMinutes;
            
            // 下一个执行时间 = 开始时间 + (intervalsPassed + 1) * intervalMinutes
            LocalDateTime nextTime = startTime.plusMinutes((intervalsPassed + 1) * intervalMinutes);
            
            // 确保下一个执行时间在当前时间之后
            while (nextTime.isBefore(now) || nextTime.isEqual(now)) {
                nextTime = nextTime.plusMinutes(intervalMinutes);
            }
            
            return nextTime;
        } 
        // 场景2：如果上次执行时间不为空
        else {
            // 计算从上一次执行时间开始的下一个时间点
            LocalDateTime nextTime = lastFireTime.plusMinutes(intervalMinutes);
            
            // 如果下一个时间点已经过去，继续找下一个
            while (nextTime.isBefore(now) || nextTime.isEqual(now)) {
                nextTime = nextTime.plusMinutes(intervalMinutes);
            }
            
            return nextTime;
        }
    }

    private LocalDateTime calculateNextFireTimeForFixedDelay(TaskScheduleConfig config, LocalDateTime now) {
        Long intervalMillis = config.getIntervalMillis();
        if (intervalMillis == null || intervalMillis <= 0) {
            return null;
        }

        // 固定延迟：从当前时间开始计算
        return now.plusNanos(intervalMillis * 1_000_000);
    }
}