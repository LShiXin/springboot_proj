package com.shixin.config;

import com.shixin.service.BaseUrlsManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

/**
 * 可选URL自动重新加载配置
 * 应用启动时自动从JSON文件重新加载数据到Redis
 */
@Configuration
public class OptionalUrlsAutoReloadConfig implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OptionalUrlsAutoReloadConfig.class);

    @Autowired
    private BaseUrlsManagerService baseUrlsManagerService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            log.info("应用启动，正在从Optional_field.json重新加载数据到Redis...");
            var result = baseUrlsManagerService.reloadPotional_UrlsFromJson();
            log.info("可选URL数据重新加载成功，共加载了 {} 条记录", result.size());
        } catch (Exception e) {
            log.error("可选URL数据重新加载失败", e);
            // 不抛出异常，避免应用启动失败
        }
    }
}