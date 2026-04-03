package com.shixin.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public class MonitorTaskListDTO {

    private Long id;
    private String name;
    private String keywords;
    private boolean enabled;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    private String timePoint;          // 时间点，如 "10:30"

    private Long intervalMinutes;       // 间隔分钟数（原 intervalMillis / 60000）

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastExecutionTime;  // 上次执行时间

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextExecutionTime;  // 下次执行时间

    // 无参构造（可选，用于框架）
    public MonitorTaskListDTO() {}

    // 全参构造（可选，用于手动构建）
    public MonitorTaskListDTO(Long id, String name, String keywords, boolean enabled,
                          LocalDateTime startTime, String timePoint,
                          Long intervalMinutes, LocalDateTime endTime,
                          LocalDateTime lastExecutionTime, LocalDateTime nextExecutionTime) {
        this.id = id;
        this.name = name;
        this.keywords = keywords;
        this.enabled = enabled;
        this.startTime = startTime;
        this.timePoint = timePoint;
        this.intervalMinutes = intervalMinutes;
        this.endTime = endTime;
        this.lastExecutionTime = lastExecutionTime;
        this.nextExecutionTime = nextExecutionTime;
    }

    // Getter 和 Setter 方法
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

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public String getTimePoint() {
        return timePoint;
    }

    public void setTimePoint(String timePoint) {
        this.timePoint = timePoint;
    }

    public Long getIntervalMinutes() {
        return intervalMinutes;
    }

    public void setIntervalMinutes(Long intervalMinutes) {
        this.intervalMinutes = intervalMinutes;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getLastExecutionTime() {
        return lastExecutionTime;
    }

    public void setLastExecutionTime(LocalDateTime lastExecutionTime) {
        this.lastExecutionTime = lastExecutionTime;
    }

    public LocalDateTime getNextExecutionTime() {
        return nextExecutionTime;
    }

    public void setNextExecutionTime(LocalDateTime nextExecutionTime) {
        this.nextExecutionTime = nextExecutionTime;
    }

    @Override
    public String toString() {
        return "MonitorTaskListDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", keywords='" + keywords + '\'' +
                ", enabled=" + enabled +
                ", startTime=" + startTime +
                ", timePoint='" + timePoint + '\'' +
                ", intervalMinutes=" + intervalMinutes +
                ", endTime=" + endTime +
                ", lastExecutionTime=" + lastExecutionTime +
                ", nextExecutionTime=" + nextExecutionTime +
                '}';
    }
}