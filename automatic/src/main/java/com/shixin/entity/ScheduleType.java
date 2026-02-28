package com.shixin.entity;

public enum ScheduleType {
    CRON,           // Cron 表达式
    FIXED_RATE,     // 固定频率（两次执行之间间隔固定时间）
    FIXED_DELAY,    // 固定延迟（上一次执行结束后延迟固定时间）
    ONCE            // 一次性任务
}
