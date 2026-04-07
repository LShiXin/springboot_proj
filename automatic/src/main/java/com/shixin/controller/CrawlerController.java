package com.shixin.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shixin.entity.ApiResponse;
import com.shixin.entity.Notification;
import com.shixin.entity.NotificationWithTaskNameDTO;
import com.shixin.service.CrawlerService;

/**
 * 爬虫控制器
 */
@RestController
@RequestMapping("/api/crawler")
public class CrawlerController {

    private static final Logger logger = LoggerFactory.getLogger(CrawlerController.class);

    @Autowired
    private CrawlerService crawlerService;

    /**
     * 执行爬虫任务
     */
    @PostMapping("/execute")
    public ResponseEntity<ApiResponse<Map<String, Object>>> executeCrawlerTask(
            @RequestBody Map<String, Object> request) {
        
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            Long taskId = Long.valueOf(request.get("taskId").toString());
            String keywords = (String) request.get("keywords");
            String url = (String) request.get("url");

            logger.info("收到爬虫任务请求，用户ID: {}, 任务ID: {}, 关键词: {}, URL: {}", userId, taskId, keywords, url);

            int notificationCount = crawlerService.executeCrawlerTask(userId, taskId, keywords, url);

            Map<String, Object> result = new HashMap<>();
            result.put("notificationCount", notificationCount);
            result.put("userId", userId);
            result.put("taskId", taskId);
            result.put("message", "爬虫任务执行成功");

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            logger.error("执行爬虫任务时发生错误", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "执行爬虫任务失败: " + e.getMessage()));
        }
    }

    /**
     * 执行北注协培训通知爬虫任务
     */
    @PostMapping("/beizhuxie/execute")
    public ResponseEntity<ApiResponse<Map<String, Object>>> executeBeizhuxieCrawler(
            @RequestBody Map<String, Object> request) {
        
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            Long taskId = Long.valueOf(request.get("taskId").toString());
            String keywords = (String) request.get("keywords");

            logger.info("收到北注协爬虫任务请求，用户ID: {}, 任务ID: {}, 关键词: {}", userId, taskId, keywords);

            int notificationCount = crawlerService.executeBeizhuxieTrainingCrawler(userId, taskId, keywords);

            Map<String, Object> result = new HashMap<>();
            result.put("notificationCount", notificationCount);
            result.put("userId", userId);
            result.put("taskId", taskId);
            result.put("message", "北注协爬虫任务执行成功");

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            logger.error("执行北注协爬虫任务时发生错误", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "执行北注协爬虫任务失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户通知列表（包含任务名称）
     */
    @GetMapping("/notifications/user/{userId}")
    public ResponseEntity<ApiResponse<List<NotificationWithTaskNameDTO>>> getUserNotifications(
            @PathVariable Long userId) {
        
        try {
            logger.debug("获取用户通知列表，用户ID: {}", userId);
            List<Notification> notifications = crawlerService.getUserNotifications(userId);
            
            // 转换为DTO，包含任务名称
            List<NotificationWithTaskNameDTO> notificationDTOs = new java.util.ArrayList<>();
            for (Notification notification : notifications) {
                // 获取任务名称
                String taskName = "未知任务"; // 默认值
                try {
                    // 这里需要获取任务名称，暂时使用默认值
                    // 实际应该从MonitorTaskService获取
                    taskName = "任务" + notification.getTaskId();
                } catch (Exception e) {
                    logger.warn("获取任务名称失败，任务ID: {}", notification.getTaskId(), e);
                }
                
                NotificationWithTaskNameDTO dto = new NotificationWithTaskNameDTO(notification, taskName);
                notificationDTOs.add(dto);
            }
            
            return ResponseEntity.ok(ApiResponse.success(notificationDTOs));
        } catch (Exception e) {
            logger.error("获取用户通知列表时发生错误", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "获取通知列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取任务通知列表
     */
    @GetMapping("/notifications/task/{taskId}")
    public ResponseEntity<ApiResponse<List<Notification>>> getTaskNotifications(
            @PathVariable Long taskId) {
        
        try {
            logger.debug("获取任务通知列表，任务ID: {}", taskId);
            List<Notification> notifications = crawlerService.getTaskNotifications(taskId);
            return ResponseEntity.ok(ApiResponse.success(notifications));
        } catch (Exception e) {
            logger.error("获取任务通知列表时发生错误", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "获取任务通知列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户未读通知数量
     */
    @GetMapping("/notifications/unread/count/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUnreadNotificationCount(
            @PathVariable Long userId) {
        
        try {
            logger.debug("获取用户未读通知数量，用户ID: {}", userId);
            long count = crawlerService.getUnreadNotificationCount(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("unreadCount", count);
            
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            logger.error("获取未读通知数量时发生错误", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "获取未读通知数量失败: " + e.getMessage()));
        }
    }

    /**
     * 标记通知为已读
     */
    @PutMapping("/notifications/{notificationId}/read")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markNotificationAsRead(
            @PathVariable Long notificationId) {
        
        try {
            logger.debug("标记通知为已读，通知ID: {}", notificationId);
            boolean success = crawlerService.markNotificationAsRead(notificationId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("notificationId", notificationId);
            result.put("success", success);
            
            if (success) {
                return ResponseEntity.ok(ApiResponse.success(result));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error(404, "通知不存在或标记失败"));
            }
        } catch (Exception e) {
            logger.error("标记通知为已读时发生错误", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "标记通知为已读失败: " + e.getMessage()));
        }
    }

    /**
     * 标记用户所有通知为已读
     */
    @PutMapping("/notifications/user/{userId}/read-all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAllNotificationsAsRead(
            @PathVariable Long userId) {
        
        try {
            logger.debug("标记用户所有通知为已读，用户ID: {}", userId);
            int count = crawlerService.markAllNotificationsAsRead(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("markedCount", count);
            
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            logger.error("标记所有通知为已读时发生错误", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "标记所有通知为已读失败: " + e.getMessage()));
        }
    }

    /**
     * 删除通知
     */
    @DeleteMapping("/notifications/{notificationId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteNotification(
            @PathVariable Long notificationId) {
        
        try {
            logger.debug("删除通知，通知ID: {}", notificationId);
            boolean success = crawlerService.deleteNotification(notificationId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("notificationId", notificationId);
            result.put("success", success);
            
            if (success) {
                return ResponseEntity.ok(ApiResponse.success(result));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error(404, "通知不存在或删除失败"));
            }
        } catch (Exception e) {
            logger.error("删除通知时发生错误", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "删除通知失败: " + e.getMessage()));
        }
    }

    /**
     * 删除任务的所有通知
     */
    @DeleteMapping("/notifications/task/{taskId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteTaskNotifications(
            @PathVariable Long taskId) {
        
        try {
            logger.debug("删除任务的所有通知，任务ID: {}", taskId);
            int count = crawlerService.deleteTaskNotifications(taskId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("taskId", taskId);
            result.put("deletedCount", count);
            
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            logger.error("删除任务通知时发生错误", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "删除任务通知失败: " + e.getMessage()));
        }
    }

   
    /**
     * 搜索通知
     */
    @GetMapping("/notifications/search")
    public ResponseEntity<ApiResponse<List<Notification>>> searchNotifications(
            @RequestParam Long userId,
            @RequestParam(required = false) String keyword) {
        
        try {
            logger.debug("搜索通知，用户ID: {}, 关键词: {}", userId, keyword);
            
            List<Notification> notifications;
            if (keyword == null || keyword.trim().isEmpty()) {
                notifications = crawlerService.getUserNotifications(userId);
            } else {
                notifications = crawlerService.getUserNotifications(userId).stream()
                        .filter(notification -> 
                            notification.getTitle().contains(keyword) || 
                            (notification.getOriginalContent() != null && notification.getOriginalContent().contains(keyword)))
                        .toList();
            }
            
            return ResponseEntity.ok(ApiResponse.success(notifications));
        } catch (Exception e) {
            logger.error("搜索通知时发生错误", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "搜索通知失败: " + e.getMessage()));
        }
    }
}