package com.shixin.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 定时任务执行记录实体类
 * 记录每次定时任务的执行结果
 */
@Entity
@Table(name = "task_execution_record", indexes = {
    @Index(name = "idx_task_execution_task_id", columnList = "taskId"),
    @Index(name = "idx_task_execution_execution_time", columnList = "executionTime"),
    @Index(name = "idx_task_execution_status", columnList = "status")
})
public class TaskExecutionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 任务ID（对应TaskScheduleConfig的id）
     */
    @Column(name = "task_id", nullable = false)
    private Long taskId;

    /**
     * 执行时间
     */
    @Column(name = "execution_time", nullable = false)
    private LocalDateTime executionTime;

    /**
     * 执行状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExecutionStatus status;

    /**
     * 执行结果描述
     */
    @Column(name = "result_message", length = 1000)
    private String resultMessage;

    /**
     * 抓取到的通知总数（爬虫返回的数量）
     */
    @Column(name = "notification_count")
    private Integer notificationCount;

    /**
     * 新通知数量（本次扫描后实际插入数据库的数量）
     */
    @Column(name = "new_notification_count")
    private Integer newNotificationCount;

    /**
     * 错误信息（如果执行失败）
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 执行耗时（毫秒）
     */
    @Column(name = "execution_duration")
    private Long executionDuration;

    /**
     * 创建时间
     */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 执行状态枚举
     */
    public enum ExecutionStatus {
        SUCCESS,    // 执行成功
        FAILED,     // 执行失败
        SKIPPED     // 跳过执行（如未到执行时间）
    }

    // 无参构造
    public TaskExecutionRecord() {}

    // 带参构造
    public TaskExecutionRecord(Long taskId, LocalDateTime executionTime, ExecutionStatus status) {
        this.taskId = taskId;
        this.executionTime = executionTime;
        this.status = status;
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

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public LocalDateTime getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(LocalDateTime executionTime) {
        this.executionTime = executionTime;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }

    public Integer getNotificationCount() {
        return notificationCount;
    }

    public void setNotificationCount(Integer notificationCount) {
        this.notificationCount = notificationCount;
    }

    public Integer getNewNotificationCount() {
        return newNotificationCount;
    }

    public void setNewNotificationCount(Integer newNotificationCount) {
        this.newNotificationCount = newNotificationCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getExecutionDuration() {
        return executionDuration;
    }

    public void setExecutionDuration(Long executionDuration) {
        this.executionDuration = executionDuration;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "TaskExecutionRecord{" +
                "id=" + id +
                ", taskId=" + taskId +
                ", executionTime=" + executionTime +
                ", status=" + status +
                ", resultMessage='" + resultMessage + '\'' +
                ", notificationCount=" + notificationCount +
                ", newNotificationCount=" + newNotificationCount +
                ", errorMessage='" + errorMessage + '\'' +
                ", executionDuration=" + executionDuration +
                ", createdAt=" + createdAt +
                '}';
    }
}