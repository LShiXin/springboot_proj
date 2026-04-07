package com.shixin.serviceimpl;

import com.shixin.entity.MonitorTask;
import com.shixin.entity.MonitorUrl;
import com.shixin.entity.TaskExecutionRecord;
import com.shixin.entity.TaskScheduleConfig;
import com.shixin.repository.NotificationRepository;
import com.shixin.repository.TaskExecutionRecordRepository;
import com.shixin.repository.TaskScheduleConfigRepository;
import com.shixin.tool.crawler.beizhuxie.BeizhuxieTrainingCrawler;
import com.shixin.tool.crawler.cicpa.CicpaCrawler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ScheduledTaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskExecutor.class);

    @Autowired
    private TaskScheduleConfigRepository taskScheduleConfigRepository;

    @Autowired
    private TaskExecutionRecordRepository taskExecutionRecordRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private BeizhuxieTrainingCrawler beizhuxieTrainingCrawler;

    @Autowired
    private CicpaCrawler cicpaCrawler;

    /**
     * 调度线程：每10秒扫描一次数据库，将到期的任务提交给异步执行器
     */
    @Scheduled(fixedRate = 10000)
    public void scanAndSubmitTasks() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<TaskScheduleConfig> tasksToExecute = taskScheduleConfigRepository.findSchedulesDueForExecution(now);

            if (tasksToExecute == null || tasksToExecute.isEmpty()) {
                log.debug("无待执行任务");
                return;
            }

            log.info("发现 {} 个待执行任务，准备异步执行", tasksToExecute.size());
            for (TaskScheduleConfig config : tasksToExecute) {
                // 异步执行，避免阻塞调度线程
                executeTaskWithOptimisticLockAsync(config, now);
            }
        } catch (Exception e) {
            log.error("扫描任务失败", e);
        }
    }

    /**
     * 异步执行单个任务（带乐观锁）
     */
    @Async("taskExecutor")  // 需要配置一个线程池 Bean，见下文
    public void executeTaskWithOptimisticLockAsync(TaskScheduleConfig config, LocalDateTime scanTime) {
        // 重试3次乐观锁更新
        int retryCount = 3;
        for (int i = 0; i < retryCount; i++) {
            try {
                if (tryExecuteTask(config, scanTime)) {
                    return;
                }
                Thread.sleep(100); // 短暂等待后重试
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("任务重试被中断", e);
                return;
            } catch (Exception e) {
                log.error("执行任务 {} 发生未知异常", config.getId(), e);
                return;
            }
        }
        log.error("任务 {} 乐观锁更新重试 {} 次仍失败，放弃本次执行", config.getId(), retryCount);
    }

    /**
     * 尝试执行任务（乐观锁事务）
     * @return true 表示成功执行并更新状态；false 表示乐观锁冲突或无需执行
     */
    @Transactional
    public boolean tryExecuteTask(TaskScheduleConfig config, LocalDateTime scanTime) {
        // 1. 重新查询最新数据（带版本号）及其关联
        TaskScheduleConfig dbConfig = taskScheduleConfigRepository.findByIdWithAssociations(config.getId()).orElse(null);
        if (dbConfig == null) {
            log.warn("任务 {} 不存在", config.getId());
            return false;
        }

        // 2. 乐观锁：版本号必须匹配
        if (!dbConfig.getVersion().equals(config.getVersion())) {
            log.debug("任务 {} 版本冲突（期望{}，实际{}），可能已被其他实例执行",
                    config.getId(), config.getVersion(), dbConfig.getVersion());
            return false;
        }

        // 3. 二次检查是否真的需要执行（防止并发）
        if (dbConfig.getNextFireTime() == null || dbConfig.getNextFireTime().isAfter(scanTime)) {
            log.debug("任务 {} 未到执行时间或已被处理", config.getId());
            return false;
        }

        // 记录执行开始时间
        long startTime = System.currentTimeMillis();
        TaskExecutionRecord executionRecord = new TaskExecutionRecord();
        executionRecord.setTaskId(dbConfig.getId());
        executionRecord.setExecutionTime(scanTime);
        // 设置初始状态为FAILED（避免null约束违反），执行成功后会更新为SUCCESS
        executionRecord.setStatus(TaskExecutionRecord.ExecutionStatus.FAILED);
        
        // 先保存执行记录以获取ID
        taskExecutionRecordRepository.save(executionRecord);
        Long executionRecordId = executionRecord.getId();
        
        try {
            // 在执行前查询当前任务的通知数量
            Long taskId = dbConfig.getId();
            long beforeCount = notificationRepository.countByTaskId(taskId);
            
            // 4. 执行真正的业务逻辑（爬虫），传递执行记录ID
            int notificationCount = executeTask(dbConfig, executionRecordId);
            
            // 在执行后查询当前任务的通知数量，计算新通知数量
            long afterCount = notificationRepository.countByTaskId(taskId);
            int newNotificationCount = (int) (afterCount - beforeCount);
            
            // 执行成功，记录成功结果
            executionRecord.setStatus(TaskExecutionRecord.ExecutionStatus.SUCCESS);
            executionRecord.setNotificationCount(notificationCount);
            executionRecord.setNewNotificationCount(newNotificationCount);
            executionRecord.setResultMessage(String.format("任务执行成功，抓取到 %d 条通知，其中 %d 条是新通知", 
                    notificationCount, newNotificationCount));
            executionRecord.setExecutionDuration(System.currentTimeMillis() - startTime);
            
            // 5. 更新上次执行时间和下次执行时间
            dbConfig.setLastFireTime(scanTime);
            LocalDateTime nextTime = computeNextFireTime(dbConfig, scanTime);
            dbConfig.setNextFireTime(nextTime);

            // 6. 保存任务配置和更新执行记录
            taskScheduleConfigRepository.save(dbConfig);
            taskExecutionRecordRepository.save(executionRecord);
            
            log.info("任务 {} 执行成功，抓取 {} 条通知，其中 {} 条是新通知，下次执行时间: {}", 
                    config.getId(), notificationCount, newNotificationCount, nextTime);
            return true;
            
        } catch (Exception e) {
            // 执行失败，记录失败结果
            log.error("执行任务 {} 业务逻辑失败", config.getId(), e);
            
            executionRecord.setStatus(TaskExecutionRecord.ExecutionStatus.FAILED);
            executionRecord.setErrorMessage(e.getMessage());
            executionRecord.setResultMessage("任务执行失败: " + e.getMessage());
            executionRecord.setExecutionDuration(System.currentTimeMillis() - startTime);
            
            // 保存执行记录（失败记录）
            taskExecutionRecordRepository.save(executionRecord);
            
            // 注意：这里不更新任务配置的lastFireTime和nextFireTime，让下次扫描继续尝试
            // 但为了不占用乐观锁版本号，我们直接返回true表示"本次乐观锁已消费，但不推进时间"
            return true;
        }
    }

    /**
     * 计算下次执行时间（核心修复点）
     */
    private LocalDateTime computeNextFireTime(TaskScheduleConfig config, LocalDateTime currentFireTime) {
        if (config.getScheduleType() == null) {
            return null;
        }
        switch (config.getScheduleType()) {
            case FIXED_RATE:
                // 基于当前执行开始时间 + 间隔
                if (config.getIntervalMillis() == null) return null;
                return currentFireTime.plus(config.getIntervalMillis(), ChronoUnit.MILLIS);

            case FIXED_DELAY:
                // 基于当前执行结束时间，但这里我们不知道结束时间，简单使用当前时间（实际应该在任务执行完成后计算）
                // 更精确的做法：在 executeTask 结束后记录结束时间，然后 + delay。这里简化：使用 currentFireTime + delay
                if (config.getIntervalMillis() == null) return null;
                return LocalDateTime.now().plus(config.getIntervalMillis(), ChronoUnit.MILLIS);

            case CRON:
                if (config.getCronExpression() == null || config.getCronExpression().isBlank()) return null;
                try {
                    CronExpression cron = CronExpression.parse(config.getCronExpression());
                    return cron.next(currentFireTime);
                } catch (Exception e) {
                    log.error("解析Cron表达式失败: {}", config.getCronExpression(), e);
                    return null;
                }

            case ONCE:
                return null; // 一次性任务，执行后不再调度

            default:
                return null;
        }
    }

    /**
     * 执行实际业务（爬虫），返回抓取到的通知总数
     */
    private int executeTask(TaskScheduleConfig config) throws Exception {
        return executeTask(config, null);
    }

    /**
     * 执行实际业务（爬虫），返回抓取到的通知总数（带执行记录ID）
     */
    private int executeTask(TaskScheduleConfig config, Long executionRecordId) throws Exception {
        MonitorTask monitorTask = config.getMonitorTask();
        if (monitorTask == null) {
            throw new IllegalStateException("任务配置未关联监控任务");
        }
        log.info("开始执行监控任务: {} (ID: {})", monitorTask.getName(), monitorTask.getId());

        List<MonitorUrl> urls = monitorTask.getUrls();
        if (urls == null || urls.isEmpty()) {
            log.warn("监控任务 {} 没有配置链接", monitorTask.getId());
            return 0;
        }

        int totalCount = 0;
        for (MonitorUrl url : urls) {
            if (!url.isEnabled()) {
                log.debug("链接 {} 已禁用，跳过", url.getId());
                continue;
            }
            totalCount += executeCrawlerForUrl(url, monitorTask, executionRecordId);
        }
        
        return totalCount;
    }

    /**
     * 执行爬虫（异常向上抛出），返回抓取到的通知数量
     */
    private int executeCrawlerForUrl(MonitorUrl url, MonitorTask monitorTask) throws Exception {
        return executeCrawlerForUrl(url, monitorTask, null);
    }

    /**
     * 执行爬虫（异常向上抛出），返回抓取到的通知数量（带执行记录ID）
     */
    private int executeCrawlerForUrl(MonitorUrl url, MonitorTask monitorTask, Long executionRecordId) throws Exception {
        String urlStr = url.getUrl().toLowerCase();
        String keywords = monitorTask.getKeywords();
        if (keywords == null) keywords = "";

        if (urlStr.contains("bicpa.org.cn") || urlStr.contains("beizhuxie")) {
            log.info("使用北注协爬虫处理: {}", url.getUrl());
            int count = beizhuxieTrainingCrawler.crawlTrainingNotificationsWithStrategy(
                    monitorTask.getUser().getId(),
                    monitorTask.getId(),
                    keywords,
                    executionRecordId
            );
            log.info("北注协爬虫完成，爬取 {} 条", count);
            return count;
        } else if (urlStr.contains("cicpa.org.cn")) {
            log.info("使用中注协爬虫处理: {}", url.getUrl());
            boolean isNews = urlStr.contains("/news/");
            int count = cicpaCrawler.crawlNotificationsWithStrategy(
                    monitorTask.getUser().getId(),
                    monitorTask.getId(),
                    keywords,
                    isNews,
                    executionRecordId
            );
            log.info("中注协爬虫完成，爬取 {} 条", count);
            return count;
        } else {
            log.warn("未找到匹配的爬虫，链接: {}", url.getUrl());
            return 0;
        }
    }
}