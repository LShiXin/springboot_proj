package com.shixin.entity;


/**
 * 监控链接数据传输对象，用于接口数据交互
 * 包含监控URL的核心信息
 */
public class MonitorUrlListDTO {

    private Long id;
    private Long taskId;  // 可选：关联的监控任务ID
    private String url;
    private boolean enabled;  // 对应实体的 enabled 字段
    private String remark;

    // 全参构造（必须与 JPQL 中的参数顺序一致）
    public MonitorUrlListDTO(Long id, Long taskId, String url, boolean enabled, String remark) {
        this.id = id;
        this.taskId = taskId;
        this.url = url;
        this.enabled = enabled;
        this.remark = remark;
    }
    public MonitorUrlListDTO() {
    }
    // getter 和 setter 方法
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
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
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

    public String toString() {
        return "MonitorUrlListDTO{" +
                "id=" + id +
                ", taskId=" + taskId +
                ", url='" + url + '\'' +
                ", enabled=" + enabled +
                ", remark='" + remark + '\'' +
                '}';
    }
   
}