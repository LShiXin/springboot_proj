package com.shixin.repository;

import com.shixin.entity.TaskScheduleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    @Query("SELECT c FROM TaskScheduleConfig c " +
           "LEFT JOIN FETCH c.monitorTask mt " +
           "LEFT JOIN FETCH mt.urls " +
           "LEFT JOIN FETCH mt.user " +
           "WHERE c.nextFireTime <= :now AND c.enabled = true")
    List<TaskScheduleConfig> findSchedulesDueForExecution(@Param("now") LocalDateTime now);

    /**
     * 根据ID查询任务配置及其关联的监控任务、URLs和用户
     */
    @Query("SELECT c FROM TaskScheduleConfig c " +
           "LEFT JOIN FETCH c.monitorTask mt " +
           "LEFT JOIN FETCH mt.urls " +
           "LEFT JOIN FETCH mt.user " +
           "WHERE c.id = :id")
    Optional<TaskScheduleConfig> findByIdWithAssociations(@Param("id") Long id);
}
