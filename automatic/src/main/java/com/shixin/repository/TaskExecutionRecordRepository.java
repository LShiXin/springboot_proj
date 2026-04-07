package com.shixin.repository;

import com.shixin.entity.TaskExecutionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务执行记录数据访问接口
 */
@Repository
public interface TaskExecutionRecordRepository extends JpaRepository<TaskExecutionRecord, Long> {

    /**
     * 根据任务ID查询执行记录，按执行时间倒序排列
     */
    List<TaskExecutionRecord> findByTaskIdOrderByExecutionTimeDesc(Long taskId);

    /**
     * 根据任务ID分页查询执行记录
     */
    Page<TaskExecutionRecord> findByTaskId(Long taskId, Pageable pageable);

    /**
     * 根据任务ID和执行状态查询执行记录
     */
    List<TaskExecutionRecord> findByTaskIdAndStatus(Long taskId, TaskExecutionRecord.ExecutionStatus status);

    /**
     * 根据执行时间范围查询执行记录
     */
    List<TaskExecutionRecord> findByExecutionTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据任务ID和执行时间范围查询执行记录
     */
    List<TaskExecutionRecord> findByTaskIdAndExecutionTimeBetween(Long taskId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询最近N条执行记录
     */
    @Query("SELECT r FROM TaskExecutionRecord r ORDER BY r.executionTime DESC")
    List<TaskExecutionRecord> findRecentRecords(Pageable pageable);

    /**
     * 查询任务最近一次执行记录
     */
    @Query("SELECT r FROM TaskExecutionRecord r WHERE r.taskId = :taskId ORDER BY r.executionTime DESC")
    List<TaskExecutionRecord> findLatestByTaskId(@Param("taskId") Long taskId, Pageable pageable);

    /**
     * 统计任务执行次数
     */
    long countByTaskId(Long taskId);

    /**
     * 统计任务成功执行次数
     */
    long countByTaskIdAndStatus(Long taskId, TaskExecutionRecord.ExecutionStatus status);

    /**
     * 删除任务的所有执行记录
     */
    void deleteByTaskId(Long taskId);
}