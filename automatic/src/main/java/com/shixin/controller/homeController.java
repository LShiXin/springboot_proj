package com.shixin.controller;

import com.shixin.service.ServerStatusService;
import com.shixin.service.TaskExecutionStatsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api")
public class HomeController {

    @Autowired
    private ServerStatusService serverStatusService;

    @Autowired
    private TaskExecutionStatsService taskExecutionStatsService;

    @GetMapping("/home")
    public String getMethodName() {
        return "Hello World!";
    }

    @GetMapping("/service/status")
    public String getStatus() {
        return serverStatusService.getStatus();
    }

    /**
     * 获取定时任务执行统计
     */
    @GetMapping("/task-execution/stats")
    public Map<String, Object> getTaskExecutionStats() {
        return taskExecutionStatsService.getSystemTaskExecutionStats();
    }

    /**
     * 获取用户定时任务执行统计
     */
    @GetMapping("/task-execution/user-stats")
    public Map<String, Object> getUserTaskExecutionStats(HttpServletRequest request) {
        // 从请求属性中获取用户信息
        Object userInfoObj = request.getAttribute("userInfo");
        Object userIdObj = request.getAttribute("userId");
        
        Long userId = null;
        
        // 优先从userInfo获取用户ID
        if (userInfoObj != null && userInfoObj instanceof com.shixin.entity.User) {
            com.shixin.entity.User user = (com.shixin.entity.User) userInfoObj;
            userId = user.getId();
        } 
        // 如果userInfo不存在，尝试从userId属性获取
        else if (userIdObj != null) {
            if (userIdObj instanceof Long) {
                userId = (Long) userIdObj;
            } else if (userIdObj instanceof Integer) {
                userId = ((Integer) userIdObj).longValue();
            } else if (userIdObj instanceof String) {
                try {
                    userId = Long.parseLong((String) userIdObj);
                } catch (NumberFormatException e) {
                    // 处理格式错误
                }
            }
        }
        
        // 如果无法获取用户ID，返回错误信息
        if (userId == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", true);
            errorResponse.put("message", "用户未登录或Token无效");
            errorResponse.put("code", 401);
            return errorResponse;
        }
        
        return taskExecutionStatsService.getUserTaskExecutionStats(userId);
    }
}
