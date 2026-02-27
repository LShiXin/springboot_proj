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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "sys_user")  // 避免使用 user 等数据库关键字
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;       // 用户名（登录账号）

    @Column(nullable = false)
    private String password;        // 密码（建议存储加密后的密文）

    @Column(length = 100)
    private String email;           // 邮箱（可选）

    @Column(name = "phone", length = 20)
    private String phone;           // 手机号（可选）

    @Column(nullable = false)
    private Boolean enabled = true; // 是否启用

    // 一对多关联：一个用户拥有多个监控任务
    // mappedBy 指向 MonitorTask 实体中的 user 字段
    // cascade 根据业务需求设置，通常对用户的操作不应级联删除任务，所以可设为 CascadeType.ALL 或谨慎使用
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MonitorTask> tasks = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public User() {}

    public User(String username, String password) {
        this.username = username;
        this.password = password;
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

    // 辅助方法：添加任务（维护双向关系）
    public void addTask(MonitorTask task) {
        tasks.add(task);
        task.setUser(this);
    }

    // 辅助方法：移除任务
    public void removeTask(MonitorTask task) {
        tasks.remove(task);
        task.setUser(null);
    }

    // Getters and Setters (可使用 IDE 生成或 Lombok)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public List<MonitorTask> getTasks() { return tasks; }
    public void setTasks(List<MonitorTask> tasks) { this.tasks = tasks; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', email='" + email + "', enabled=" + enabled + "}";
    }
}