package com.shixin.serviceimpl;

import com.shixin.entity.MonitorTask;
import com.shixin.entity.MonitorUrl;
import com.shixin.entity.TaskScheduleConfig;
import com.shixin.repository.TaskScheduleConfigRepository;
import com.shixin.service.RedisService;
import com.shixin.tool.crawler.beizhuxie.BeizhuxieTrainingCrawler;
import com.shixin.tool.crawler.cicpa.CicpaCrawler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务执行器实现类
 * 从Redis中的待执行队列获取任务，到对应时间点后开始执行，
 * 根据该定时任务的监控链接执行对应的爬虫类
 */
@Service
public class ScheduledTaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskExecutor.class);

    @Autowired
    private RedisService redisService;

    @Autowired
    private TaskScheduleConfigRepository taskScheduleConfigRepository;

    @Autowired
    private BeizhuxieTrainingCrawler beizhuxieTrainingCrawler;
    
    @Autowired
    private CicpaCrawler cicpaCrawler;

    /**
     * 每分钟检查一次待执行队列，执行到时间的任务
     */
    @Scheduled(fixedRate = 60000) // 每60秒执行一次
    public void executeScheduledTasks() {
        try {
            log.debug("开始检查待执行的定时任务...");

            // 从Redis获取执行队列
            List<TaskScheduleConfig> executionQueue = redisService.getExecutionQueue();

            if (executionQueue == null || executionQueue.isEmpty()) {
                log.debug("执行队列为空，没有待执行的任务");
                return;
            }

            log.info("获取到 {} 个待执行任务", executionQueue.size());

            LocalDateTime now = LocalDateTime.now();

            // 遍历执行队列，执行到时间的任务
            for (TaskScheduleConfig config : executionQueue) {
                if (config.getNextFireTime() == null) {
                    log.warn("任务 {} 的下次执行时间为空，跳过", config.getId());
                    continue;
                }

                // 检查是否到执行时间
                if (now.isAfter(config.getNextFireTime()) || now.isEqual(config.getNextFireTime())) {
                    log.info("任务 {} 到达执行时间，开始执行", config.getId());

                    // 执行任务
                    executeTask(config);

                    // 更新任务的下次执行时间（如果需要）
                    updateNextFireTime(config);

                    // 从Redis待执行列表中删除该任务
                    redisService.removeTaskFromExecutionQueue(config.getId());
                    log.info("已从Redis执行队列中删除任务: {}", config.getId());

                    // 更新数据库中的任务信息（上次执行时间和下次执行时间）
                    updateTaskInDatabase(config);
                    
                } else {
                    log.debug("任务 {} 还未到执行时间: {}", config.getId(), config.getNextFireTime());
                }
            }

        } catch (Exception e) {
            log.error("执行定时任务时发生错误", e);
        }
    }

    /**
     * 执行单个定时任务
     * @param config 定时任务配置
     */
    private void executeTask(TaskScheduleConfig config) {
        try {
            MonitorTask monitorTask = config.getMonitorTask();
            if (monitorTask == null) {
                log.error("任务配置 {} 关联的监控任务为空", config.getId());
                return;
            }

            log.info("开始执行监控任务: {} (ID: {})", monitorTask.getName(), monitorTask.getId());

            // 获取任务的监控链接
            List<MonitorUrl> urls = monitorTask.getUrls();
            if (urls == null || urls.isEmpty()) {
                log.warn("监控任务 {} 没有配置监控链接", monitorTask.getId());
                return;
            }

            // 遍历每个监控链接，执行对应的爬虫
            for (MonitorUrl url : urls) {
                if (!url.isEnabled()) {
                    log.debug("监控链接 {} 已禁用，跳过", url.getId());
                    continue;
                }

                log.info("执行监控链接: {} (ID: {})", url.getUrl(), url.getId());

                // 根据URL执行对应的爬虫类
                executeCrawlerForUrl(url, monitorTask);
            }

            // 更新任务的最后执行时间
            config.setLastFireTime(LocalDateTime.now());
            log.info("监控任务 {} 执行完成", monitorTask.getId());

        } catch (Exception e) {
            log.error("执行任务 {} 时发生错误", config.getId(), e);
        }
    }

    /**
     * 根据监控链接执行对应的爬虫类
     * @param url 监控链接
     * @param monitorTask 监控任务
     */
    private void executeCrawlerForUrl(MonitorUrl url, MonitorTask monitorTask) {
        String urlStr = url.getUrl().toLowerCase();

        try {
            // 根据URL判断使用哪个爬虫类
            if (urlStr.contains("bicpa.org.cn") || urlStr.contains("beizhuxie")) {
                // 北注协培训通知爬虫
                log.info("使用北注协培训爬虫处理链接: {}", url.getUrl());
                
                // 获取任务关键词
                String keywords = monitorTask.getKeywords();
                if (keywords == null || keywords.trim().isEmpty()) {
                    log.warn("监控任务 {} 未设置关键词，将处理所有通知", monitorTask.getId());
                    keywords = "";
                }
                
                // 执行爬虫，按照需求：先处理第一页，逐条检查关键词，一个月限制
                int crawledCount = beizhuxieTrainingCrawler.crawlTrainingNotificationsWithStrategy(
                    monitorTask.getUser().getId(),
                    monitorTask.getId(),
                    keywords
                );
                log.info("北注协爬虫完成，爬取到 {} 条相关通知", crawledCount);

            } else if (urlStr.contains("cicpa.org.cn")) {
                // 中注协爬虫
                log.info("使用中注协爬虫处理链接: {}", url.getUrl());
                
                // 获取任务关键词
                String keywords = monitorTask.getKeywords();
                if (keywords == null || keywords.trim().isEmpty()) {
                    log.warn("监控任务 {} 未设置关键词，将处理所有通知", monitorTask.getId());
                    keywords = "";
                }
                
                // 判断是要闻还是通知公告
                boolean isNews = urlStr.contains("/news/");
                
                // 执行爬虫，按照需求：先处理第一页，逐条检查关键词，一个月限制
                int crawledCount = cicpaCrawler.crawlNotificationsWithStrategy(
                    monitorTask.getUser().getId(),
                    monitorTask.getId(),
                    keywords,
                    isNews
                );
                log.info("中注协爬虫完成，爬取到 {} 条相关通知", crawledCount);

            } else {
                // 其他类型的链接，可以在这里添加更多的爬虫类
                log.warn("未找到适合的爬虫类处理链接: {}", url.getUrl());
            }

        } catch (Exception e) {
            log.error("执行爬虫时发生错误，链接: {}", url.getUrl(), e);
        }
    }

    /**
     * 更新任务的下次执行时间
     * @param config 定时任务配置
     */
    private void updateNextFireTime(TaskScheduleConfig config) {
        // 这里可以根据调度类型计算下次执行时间
        // 简化处理：对于固定间隔的任务，增加间隔时间
        if (config.getScheduleType() != null) {
            switch (config.getScheduleType()) {
                case FIXED_RATE:
                    if (config.getIntervalMillis() != null) {
                        LocalDateTime nextTime = LocalDateTime.now().plusSeconds(config.getIntervalMillis() / 1000);
                        config.setNextFireTime(nextTime);
                        log.info("更新任务 {} 的下次执行时间为: {}", config.getId(), nextTime);
                    }
                    break;
                case FIXED_DELAY:
                    // 固定延迟类型的任务，下次执行时间在执行完成后计算
                    // 这里暂时不处理，留给调度服务重新计算
                    break;
                case CRON:
                    // CRON表达式类型的任务，需要解析表达式计算下次时间
                    // 这里暂时不处理，留给调度服务重新计算
                    break;
                case ONCE:
                    // 一次性任务，执行后不再执行
                    config.setNextFireTime(null);
                    log.info("一次性任务 {} 执行完成，不再设置下次执行时间", config.getId());
                    break;
            }
        }
    }

    /**
     * 更新数据库中的任务信息（上次执行时间和下次执行时间）
     * @param config 定时任务配置
     */
    @Transactional
    private void updateTaskInDatabase(TaskScheduleConfig config) {
        try {
            // 从数据库获取最新的任务配置
            TaskScheduleConfig dbConfig = taskScheduleConfigRepository.findById(config.getId()).orElse(null);
            if (dbConfig == null) {
                log.warn("在数据库中未找到任务配置: {}", config.getId());
                return;
            }

            // 更新上次执行时间和下次执行时间
            dbConfig.setLastFireTime(config.getLastFireTime());
            dbConfig.setNextFireTime(config.getNextFireTime());

            // 保存到数据库
            taskScheduleConfigRepository.save(dbConfig);
            
            log.info("已更新数据库中的任务 {} 的执行时间信息: lastFireTime={}, nextFireTime={}", 
                    config.getId(), config.getLastFireTime(), config.getNextFireTime());
                    
        } catch (Exception e) {
            log.error("更新数据库中的任务 {} 执行时间信息时发生错误", config.getId(), e);
        }
    }
}
