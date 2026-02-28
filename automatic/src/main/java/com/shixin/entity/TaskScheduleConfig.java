package com.shixin.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "task_schedule_config")
public class TaskScheduleConfig {

    @Id
    private Long id;  // 共享 MonitorTask 的主键，不使用自增

    @OneToOne
    @MapsId
    @JoinColumn(name = "monitor_task_id")
    private MonitorTask monitorTask;

    /**
     * 调度类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false, length = 20)
    private ScheduleType scheduleType;

    /**
     * Cron 表达式（当 scheduleType = CRON 时使用）
     */
    @Column(name = "cron_expression", length = 100)
    private String cronExpression;

    /**
     * 固定间隔（毫秒），用于 FIXED_RATE 或 FIXED_DELAY
     */
    @Column(name = "interval_millis")
    private Long intervalMillis;

    /**
     * 起始时间（可选，精确到分钟）
     */
    @Column(name = "start_time")
    private LocalDateTime startTime;

    /**
     * 结束时间（可选，精确到分钟）
     */
    @Column(name = "end_time")
    private LocalDateTime endTime;

    /**
     * 每天的执行时间点（格式 HH:mm），例如 "10:30"
     */
    @Column(name = "time_point", length = 5)
    private String timePoint;

    /**
     * 是否启用该调度配置
     */
    @Column(nullable = false)
    private boolean enabled = true;

    /**
     * 创建时间
     */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 无参构造
    public TaskScheduleConfig() {}

    // 带参构造
    public TaskScheduleConfig(MonitorTask monitorTask, ScheduleType scheduleType) {
        this.monitorTask = monitorTask;
        this.scheduleType = scheduleType;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public MonitorTask getMonitorTask() {
        return monitorTask;
    }
    public void setMonitorTask(MonitorTask monitorTask) {
        this.monitorTask = monitorTask;
    }
    public ScheduleType getScheduleType() {
        return scheduleType;
    }
    public void setScheduleType(ScheduleType scheduleType) {
        this.scheduleType = scheduleType;
    }
    public String getCronExpression() {
        return cronExpression;
    }
    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }
    public Long getIntervalMillis() {
        return intervalMillis;
    }
    public void setIntervalMillis(Long intervalMillis) {
        this.intervalMillis = intervalMillis;
    }
    public LocalDateTime getStartTime() {
        return startTime;
    }
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    public LocalDateTime getEndTime() {
        return endTime;
    }
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    public String getTimePoint() {
        return timePoint;
    }
    public void setTimePoint(String timePoint) {
        this.timePoint = timePoint;
    }
    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "TaskScheduleConfig{" +
                "id=" + id +
                ", monitorTask=" + (monitorTask != null ? monitorTask.getId() : null) +
                ", scheduleType=" + scheduleType +
                ", cronExpression='" + cronExpression + '\'' +
                ", intervalMillis=" + intervalMillis +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", timePoint='" + timePoint + '\'' +
                ", enabled=" + enabled +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}