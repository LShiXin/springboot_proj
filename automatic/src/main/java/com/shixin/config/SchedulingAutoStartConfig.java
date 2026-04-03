package com.shixin.config;

import com.shixin.service.TaskSchedulingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

/**
 * 定时任务调度自动启动配置
 * 应用启动时自动启动定时任务调度服务
 */
@Configuration
public class SchedulingAutoStartConfig implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchedulingAutoStartConfig.class);

    @Autowired
    private TaskSchedulingService taskSchedulingService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            log.info("应用启动，正在启动定时任务调度服务...");
            taskSchedulingService.startSchedulingService();
            log.info("定时任务调度服务启动成功");
        } catch (Exception e) {
            log.error("定时任务调度服务启动失败", e);
            // 不抛出异常，避免应用启动失败
        }
    }
}