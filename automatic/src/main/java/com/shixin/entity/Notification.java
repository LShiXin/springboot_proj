package com.shixin.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * 通知实体类
 * 存储爬取到的通知信息
 */
@Entity
@Table(name = "notification", indexes = {
    @Index(name = "idx_user_task", columnList = "userId, taskId"),
    @Index(name = "idx_notification_time", columnList = "notificationTime"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 任务ID
     */
    @Column(name = "task_id", nullable = false)
    private Long taskId;

    /**
     * 通知名称
     */
    @Column(name = "title", nullable = false, length = 500)
    private String title;

    /**
     * 通知链接
     */
    @Column(name = "url", nullable = false, length = 1000)
    private String url;

    /**
     * 通知时间
     */
    @Column(name = "notification_time", nullable = false)
    private LocalDateTime notificationTime;

    /**
     * 原始通知内容（未处理）
     */
    @Column(name = "original_content", columnDefinition = "TEXT")
    private String originalContent;

    /**
     * 处理后的通知内容（关键词变色处理）
     */
    @Column(name = "processed_content", columnDefinition = "TEXT")
    private String processedContent;

    /**
     * 匹配到的关键词
     */
    @Column(name = "matched_keywords", length = 500)
    private String matchedKeywords;

    /**
     * 创建时间
     */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 是否已读
     */
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    /**
     * 任务执行记录ID（关联到TaskExecutionRecord）
     * 表明这个通知是哪一次执行任务添加的
     */
    @Column(name = "execution_record_id")
    private Long executionRecordId;

    // 无参构造
    public Notification() {}

    // 带参构造
    public Notification(Long userId, Long taskId, String title, String url, LocalDateTime notificationTime) {
        this.userId = userId;
        this.taskId = taskId;
        this.title = title;
        this.url = url;
        this.notificationTime = notificationTime;
    }

    // 带执行记录ID的构造
    public Notification(Long userId, Long taskId, String title, String url, LocalDateTime notificationTime, Long executionRecordId) {
        this.userId = userId;
        this.taskId = taskId;
        this.title = title;
        this.url = url;
        this.notificationTime = notificationTime;
        this.executionRecordId = executionRecordId;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
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

    public Long getExecutionRecordId() {
        return executionRecordId;
    }

    public void setExecutionRecordId(Long executionRecordId) {
        this.executionRecordId = executionRecordId;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", userId=" + userId +
                ", taskId=" + taskId +
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", notificationTime=" + notificationTime +
                ", matchedKeywords='" + matchedKeywords + '\'' +
                ", createdAt=" + createdAt +
                ", read=" + read +
                ", executionRecordId=" + executionRecordId +
                '}';
    }
}