package com.shixin.entity;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * 监控链接实体，该实体记录了每个监控任务下的具体URL地址及其状态等信息
 * 对应数据库中的 monitor_url 表
 */
@Entity
@Table(name = "monitor_url")
public class MonitorUrl {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    /**
     * 具体的URL地址
     */
    @Column(nullable = false, length = 500)
    private String url;

    /**
     * 所属监控任务（多对一）
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private MonitorTask task;

    /**
     * 是否启用该链接（默认启用）
     */
    @Column(nullable = false)
    private boolean enabled = true;

    /**
     * 备注（可选）
     */
    private String remark;

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

    public MonitorUrl() {}

    public MonitorUrl(String url, MonitorTask task) {
        this.url = url;
        this.task = task;
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

    // Getters and Setters (generated)

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public MonitorTask getTask() {
        return task;
    }

    public void setTask(MonitorTask task) {
        this.task = task;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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
        return "MonitorUrl{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", taskId=" + (task != null ? task.getId() : null) +
                ", enabled=" + enabled +
                ", remark='" + remark + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}

