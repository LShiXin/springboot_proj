package com.shixin.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * 监控任务实体
 * 对应数据库中的 monitor_task 表
 */
@Entity
@Table(name = "monitor_task")
public class MonitorTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 所属用户（多对一）
     */
    @ManyToOne(optional = false)  // 假设任务必须属于某个用户，设为 false
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    /**
     * 任务名称
     */
    @Column(nullable = false)
    private String name;

    /**
     * 关键词，多个用英文逗号分隔，例如 "培训,课程,讲座"
     */
    @Column(nullable = false)
    private String keywords;

    /**
     * Cron表达式，定义任务的执行时间，例如 "0 0 9 * * *" 表示每天9点
     */
    @Column(name = "cron_expression", nullable = false)
    private String cronExpression;

    /**
     * 是否启用该任务
     */
    @Column(nullable = false)
    private boolean enabled = true;

    /**
     * 一对多关联：一个任务对应多个监控链接
     * mappedBy 指向 MonitorUrl 中的 task 字段
     * cascade 和 orphanRemoval 保证任务增删时自动维护子表
     */
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MonitorUrl> urls = new ArrayList<>();

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

    // 无参构造方法（JPA 要求）
    public MonitorTask() {}

    // 带参构造方法（可选，便于快速创建）
    public MonitorTask(String name, String keywords, String cronExpression) {
        this.name = name;
        this.keywords = keywords;
        this.cronExpression = cronExpression;
    }

    // 生命周期回调：自动填充创建时间和更新时间
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== 辅助方法（维护双向关系）====================
    /**
     * 添加监控链接
     * 同时维护双向关系：设置链接所属的任务
     */
    public void addUrl(MonitorUrl url) {
        urls.add(url);
        url.setTask(this);
    }

    /**
     * 移除监控链接
     * 同时解除双向关系
     */
    public void removeUrl(MonitorUrl url) {
        urls.remove(url);
        url.setTask(null);
    }

    // ==================== Getters and Setters ====================
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public void setName(String name) {
        this.name = name;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<MonitorUrl> getUrls() {
        return urls;
    }

    public void setUrls(List<MonitorUrl> urls) {
        this.urls = urls;
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
        return "MonitorTask{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", keywords='" + keywords + '\'' +
                ", cronExpression='" + cronExpression + '\'' +
                ", enabled=" + enabled +
                ", urlsCount=" + (urls != null ? urls.size() : 0) +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}