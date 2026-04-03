package com.shixin.entity;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 用户完整信息DTO，用于Redis存储
 * 存储结构：userAllInfo:{userId}
 * 包含：
 * - base: 用户基本信息
 * - tasks: 定时任务列表（包含任务链接）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserAllInfoDTO {
    
    /**
     * 用户基本信息
     */
    private UserBaseInfo base;
    
    /**
     * 定时任务列表，每个任务包含任务信息和链接
     */
    private List<TaskWithUrls> tasks;
    
    /**
     * 其他扩展信息
     */
    private Map<String, Object> extra;
    
    public UserAllInfoDTO() {
    }
    
    public UserAllInfoDTO(UserBaseInfo base, List<TaskWithUrls> tasks) {
        this.base = base;
        this.tasks = tasks;
    }
    
    public UserBaseInfo getBase() {
        return base;
    }
    
    public void setBase(UserBaseInfo base) {
        this.base = base;
    }
    
    public List<TaskWithUrls> getTasks() {
        return tasks;
    }
    
    public void setTasks(List<TaskWithUrls> tasks) {
        this.tasks = tasks;
    }
    
    public Map<String, Object> getExtra() {
        return extra;
    }
    
    public void setExtra(Map<String, Object> extra) {
        this.extra = extra;
    }
    
    @Override
    public String toString() {
        return "UserAllInfoDTO{" +
                "base=" + base +
                ", tasks=" + tasks +
                ", extra=" + extra +
                '}';
    }
    
    /**
     * 用户基本信息
     */
    public static class UserBaseInfo {
        private Long id;
        private String username;
        private String email;
        private String phone;
        private Boolean enabled;
        
        public UserBaseInfo() {
        }
        
        public UserBaseInfo(User user) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.email = user.getEmail();
            this.phone = user.getPhone();
            this.enabled = user.getEnabled();
        }
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getPhone() {
            return phone;
        }
        
        public void setPhone(String phone) {
            this.phone = phone;
        }
        
        public Boolean getEnabled() {
            return enabled;
        }
        
        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
        
        @Override
        public String toString() {
            return "UserBaseInfo{" +
                    "id=" + id +
                    ", username='" + username + '\'' +
                    ", email='" + email + '\'' +
                    ", phone='" + phone + '\'' +
                    ", enabled=" + enabled +
                    '}';
        }
    }
    
    /**
     * 带链接的定时任务
     */
    public static class TaskWithUrls {
        private Long id;
        private String name;
        private String keywords;
        private boolean enabled;
        private List<MonitorUrlListDTO> urls;
        private java.time.LocalDateTime startTime;
        private java.time.LocalDateTime endTime;
        private Long intervalMinutes;
        private String timePoint;
        
        public TaskWithUrls() {
        }
        
        public TaskWithUrls(MonitorTask task, List<MonitorUrlListDTO> urls) {
            this.id = task.getId();
            this.name = task.getName();
            this.keywords = task.getKeywords();
            this.enabled = task.isEnabled();
            this.urls = urls;
            
            // 设置定时任务调度配置信息
            if (task.getScheduleConfig() != null) {
                this.startTime = task.getScheduleConfig().getStartTime();
                this.endTime = task.getScheduleConfig().getEndTime();
                this.timePoint = task.getScheduleConfig().getTimePoint();
                
                // 将毫秒转换为分钟
                if (task.getScheduleConfig().getIntervalMillis() != null) {
                    this.intervalMinutes = task.getScheduleConfig().getIntervalMillis() / 60000;
                }
            }
        }
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getKeywords() {
            return keywords;
        }
        
        public void setKeywords(String keywords) {
            this.keywords = keywords;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public List<MonitorUrlListDTO> getUrls() {
            return urls;
        }
        
        public void setUrls(List<MonitorUrlListDTO> urls) {
            this.urls = urls;
        }
        
        public java.time.LocalDateTime getStartTime() {
            return startTime;
        }
        
        public void setStartTime(java.time.LocalDateTime startTime) {
            this.startTime = startTime;
        }
        
        public java.time.LocalDateTime getEndTime() {
            return endTime;
        }
        
        public void setEndTime(java.time.LocalDateTime endTime) {
            this.endTime = endTime;
        }
        
        public Long getIntervalMinutes() {
            return intervalMinutes;
        }
        
        public void setIntervalMinutes(Long intervalMinutes) {
            this.intervalMinutes = intervalMinutes;
        }
        
        public String getTimePoint() {
            return timePoint;
        }
        
        public void setTimePoint(String timePoint) {
            this.timePoint = timePoint;
        }
        
        @Override
        public String toString() {
            return "TaskWithUrls{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", keywords='" + keywords + '\'' +
                    ", enabled=" + enabled +
                    ", startTime=" + startTime +
                    ", endTime=" + endTime +
                    ", intervalMinutes=" + intervalMinutes +
                    ", timePoint='" + timePoint + '\'' +
                    ", urls=" + urls +
                    '}';
        }
    }
}