package com.shixin.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.shixin.entity.MonitorTask;
import com.shixin.entity.MonitorTaskListDTO;

public interface MonitorTaskRepository extends JpaRepository<MonitorTask, Long> {

    // 根据任务ID列表查询任务
    List<MonitorTask> findByIdIn(List<Long> ids);

    // 查询所有任务
    List<MonitorTask> findAll();

    // 更新任务的关键词
    @Modifying
    @Transactional
    @Query("update MonitorTask m set m.keywords = :keywords where m.id = :id")
    int updateKeywordsById(@Param("keywords") String keywords, @Param("id") Long id);

    // 根据ID查询任务
    MonitorTask findById(long id);

    @Query("SELECT new com.shixin.entity.MonitorTaskListDTO(" +
           "t.id, t.name, t.keywords, t.enabled, " +
           "t.scheduleConfig.startTime, t.scheduleConfig.timePoint, " +
           "t.scheduleConfig.intervalMillis / 60000, t.scheduleConfig.endTime) " +
           "FROM MonitorTask t WHERE t.user.id = :userId")
    List<MonitorTaskListDTO> findAllByUserId(Long userId);
}
