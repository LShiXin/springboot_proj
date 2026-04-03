package com.shixin.entity;

public class ConfigItem {
    private int url_id;
    private String class_id;
    private String method_id;
    private String url;
    private String remark;

    // 无参构造（必要，用于 Jackson/Gson 反序列化）
    public ConfigItem() {
    }

    // 全参构造
    public ConfigItem(int url_id,String class_id, String method_id, String url, String remark) {
        this.url_id=url_id;
        this.class_id = class_id;
        this.method_id = method_id;
        this.url = url;
        this.remark = remark;
    }

    // Getter 和 Setter
    public int getUrl_id() { return url_id; }
    public void getUrl_id(int url_id) { this.url_id = url_id; }

    public String getClass_id() { return class_id; }
    public void setClass_id(String class_id) { this.class_id = class_id; }

    public String getMethod_id() { return method_id; }
    public void setMethod_id(String method_id) { this.method_id = method_id; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    @Override
    public String toString() {
        return "ConfigItem{" +
                "class_id='" + class_id + '\'' +
                ", method_id='" + method_id + '\'' +
                ", url='" + url + '\'' +
                ", remark='" + remark + '\'' +
                '}';
    }
}
