package com.shixin.entity;

import java.time.LocalDateTime;

/**
 * 通知DTO，包含任务名称
 */
public class NotificationWithTaskNameDTO {
    
    private Long id;
    private Long userId;
    private Long taskId;
    private String taskName;
    private String title;
    private String url;
    private LocalDateTime notificationTime;
    private String originalContent;
    private String processedContent;
    private String matchedKeywords;
    private LocalDateTime createdAt;
    private boolean read;
    
    // 无参构造
    public NotificationWithTaskNameDTO() {}
    
    // 从Notification实体和任务名称构造
    public NotificationWithTaskNameDTO(Notification notification, String taskName) {
        this.id = notification.getId();
        this.userId = notification.getUserId();
        this.taskId = notification.getTaskId();
        this.taskName = taskName;
        this.title = notification.getTitle();
        this.url = notification.getUrl();
        this.notificationTime = notification.getNotificationTime();
        this.originalContent = notification.getOriginalContent();
        this.processedContent = notification.getProcessedContent();
        this.matchedKeywords = notification.getMatchedKeywords();
        this.createdAt = notification.getCreatedAt();
        this.read = notification.isRead();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getTaskId() {
        return taskId;
    }
    
    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }
    
    public String getTaskName() {
        return taskName;
    }
    
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public LocalDateTime getNotificationTime() {
        return notificationTime;
    }
    
    public void setNotificationTime(LocalDateTime notificationTime) {
        this.notificationTime = notificationTime;
    }
    
    public String getOriginalContent() {
        return originalContent;
    }
    
    public void setOriginalContent(String originalContent) {
        this.originalContent = originalContent;
    }
    
    public String getProcessedContent() {
        return processedContent;
    }
    
    public void setProcessedContent(String processedContent) {
        this.processedContent = processedContent;
    }
    
    public String getMatchedKeywords() {
        return matchedKeywords;
    }
    
    public void setMatchedKeywords(String matchedKeywords) {
        this.matchedKeywords = matchedKeywords;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public boolean isRead() {
        return read;
    }
    
    public void setRead(boolean read) {
        this.read = read;
    }
    
    @Override
    public String toString() {
        return "NotificationWithTaskNameDTO{" +
                "id=" + id +
                ", userId=" + userId +
                ", taskId=" + taskId +
                ", taskName='" + taskName + '\'' +
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", notificationTime=" + notificationTime +
                ", createdAt=" + createdAt +
                ", read=" + read +
                '}';
    }
}