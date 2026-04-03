package com.shixin.repository;

import com.shixin.entity.TaskScheduleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskScheduleConfigRepository extends JpaRepository<TaskScheduleConfig, Long> {

    /**
     * 查询所有启用的定时任务配置
     */
    List<TaskScheduleConfig> findByEnabledTrue();

    /**
     * 查询需要执行的定时任务配置：
     * 1. enabled = true
     * 2. startTime <= currentTime (如果startTime不为null)
     * 3. endTime > currentTime 或 endTime为null (如果endTime不为null)
     * 4. 当前时间在startTime和endTime之间
     */
    @Query("SELECT t FROM TaskScheduleConfig t WHERE " +
           "t.enabled = true AND " +
           "(t.startTime IS NULL OR t.startTime <= :currentTime) AND " +
           "(t.endTime IS NULL OR t.endTime > :currentTime)")
    List<TaskScheduleConfig> findActiveSchedules(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 根据下次执行时间查询需要执行的任务
     */
    @Query("SELECT t FROM TaskScheduleConfig t WHERE " +
           "t.enabled = true AND " +
           "t.nextFireTime IS NOT NULL AND " +
           "t.nextFireTime <= :currentTime AND " +
           "(t.endTime IS NULL OR t.endTime > :currentTime)")
    List<TaskScheduleConfig> findSchedulesDueForExecution(@Param("currentTime") LocalDateTime currentTime);
}