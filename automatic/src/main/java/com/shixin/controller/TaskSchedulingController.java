package com.shixin.controller;

import com.shixin.entity.ApiResponse;
import com.shixin.service.TaskSchedulingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 定时任务调度控制器
 */
@RestController
@RequestMapping("/api/scheduling")
public class TaskSchedulingController {

    private static final Logger log = LoggerFactory.getLogger(TaskSchedulingController.class);

    @Autowired
    private TaskSchedulingService taskSchedulingService;

    /**
     * 启动定时任务调度服务
     */
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<String>> startSchedulingService() {
        try {
            log.info("收到启动定时任务调度服务的请求");
            taskSchedulingService.startSchedulingService();
            
            return ResponseEntity.ok(ApiResponse.success("定时任务调度服务已启动"));
        } catch (Exception e) {
            log.error("启动定时任务调度服务失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "启动定时任务调度服务失败: " + e.getMessage()));
        }
    }

    /**
     * 停止定时任务调度服务
     */
    @PostMapping("/stop")
    public ResponseEntity<ApiResponse<String>> stopSchedulingService() {
        try {
            log.info("收到停止定时任务调度服务的请求");
            taskSchedulingService.stopSchedulingService();
            
            return ResponseEntity.ok(ApiResponse.success("定时任务调度服务已停止"));
        } catch (Exception e) {
            log.error("停止定时任务调度服务失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "停止定时任务调度服务失败: " + e.getMessage()));
        }
    }

    /**
     * 手动触发一次定时任务扫描
     */
    @PostMapping("/scan")
    public ResponseEntity<ApiResponse<String>> triggerManualScan() {
        try {
            log.info("收到手动触发定时任务扫描的请求");
            
            // 异步执行手动扫描，避免阻塞HTTP请求
            new Thread(() -> {
                try {
                    taskSchedulingService.triggerManualScan();
                    log.info("手动扫描执行完成");
                } catch (Exception e) {
                    log.error("手动扫描执行失败", e);
                }
            }).start();
            
            return ResponseEntity.ok(ApiResponse.success("已触发手动扫描，扫描将在后台执行，请查看日志了解扫描结果"));
        } catch (Exception e) {
            log.error("触发手动扫描失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "触发手动扫描失败: " + e.getMessage()));
        }
    }

    /**
     * 检查定时任务调度服务状态
     */
    @PostMapping("/status")
    public ResponseEntity<ApiResponse<String>> getSchedulingServiceStatus() {
        try {
            log.info("收到检查定时任务调度服务状态的请求");
            
            // 这里可以返回更详细的状态信息
            // 目前简化处理，返回服务运行状态
            String status = "定时任务调度服务状态查询成功";
            
            return ResponseEntity.ok(ApiResponse.success(status));
        } catch (Exception e) {
            log.error("检查定时任务调度服务状态失败", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(500, "检查定时任务调度服务状态失败: " + e.getMessage()));
        }
    }
}