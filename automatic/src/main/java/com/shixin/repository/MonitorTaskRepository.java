package com.shixin.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.shixin.entity.MonitorTask;
import com.shixin.entity.MonitorTaskListDTO;
import com.shixin.entity.MonitorUrlListDTO;

public interface MonitorTaskRepository extends JpaRepository<MonitorTask, Long> {

        // 根据任务ID列表查询任务
        List<MonitorTask> findByIdIn(List<Long> ids);

        // 查询所有任务
        List<MonitorTask> findAll();

        // 根据ID查询任务
        MonitorTask findById(long id);

        // 根据用户ID查询所有监控任务，并转换为 MonitorTaskListDTO
        @Query("SELECT new com.shixin.entity.MonitorTaskListDTO(" +
                        "t.id, t.name, t.keywords, t.enabled, " +
                        "t.scheduleConfig.startTime, t.scheduleConfig.timePoint, " +
                        "(t.scheduleConfig.intervalMillis / 60000), t.scheduleConfig.endTime, " +
                        "t.scheduleConfig.lastFireTime, t.scheduleConfig.nextFireTime) " +
                        "FROM MonitorTask t WHERE t.user.id = :userId")
        List<MonitorTaskListDTO> findAllByUserId(@Param("userId") Long userId);

        // 根据用户ID查询所有的监控链接，并转换为 MonitorUrlListDTO
        @Query("SELECT new com.shixin.entity.MonitorUrlListDTO(u.id, u.task.id, u.url, u.enabled, u.remark) " +
                        "FROM MonitorUrl u WHERE u.task.user.id = :userId")
        List<MonitorUrlListDTO> findListDtoByUserId(@Param("userId") Long userId);

}
